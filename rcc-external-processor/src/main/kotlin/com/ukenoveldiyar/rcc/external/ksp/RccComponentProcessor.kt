package com.ukenoveldiyar.rcc.external.ksp

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSTypeParameter
import com.google.devtools.ksp.symbol.KSValueParameter
import com.google.devtools.ksp.symbol.Modifier
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.buildCodeBlock
import com.squareup.kotlinpoet.ksp.writeTo
import com.ukenoveldiyar.rcc.external.binding.BindCollector
import com.ukenoveldiyar.rcc.external.binding.ResolvedBind
import com.ukenoveldiyar.rcc.external.binding.asFqNameOrNull
import com.ukenoveldiyar.rcc.external.binding.matchFqName
import com.ukenoveldiyar.rcc.external.config.ExternalConfig
import com.ukenoveldiyar.rcc.external.declarations.ExternalFile
import com.ukenoveldiyar.rcc.external.ksp.generator.codeBlockBindComposableInstance
import com.ukenoveldiyar.rcc.external.ksp.generator.codeBlockBindComposableInvoke
import com.ukenoveldiyar.rcc.external.ksp.generator.codeBlockBindConstructor
import com.ukenoveldiyar.rcc.external.ksp.generator.codeBlockBindInstance
import com.ukenoveldiyar.rcc.external.ksp.generator.codeBlockBindInvoke
import com.ukenoveldiyar.rcc.external.ksp.generator.codeBlockBindPropertyGetter
import com.ukenoveldiyar.rcc.external.ksp.generator.codeBlockBindPropertySetter
import com.ukenoveldiyar.rcc.external.ksp.generator.isComposable
import com.ukenoveldiyar.rcc.external.ksp.generator.isConstructor
import com.ukenoveldiyar.rcc.external.ksp.sorce.SourceComponentMetadata

class RccComponentProcessor(
    private val source: SourceComponentMetadata,
    private val environment: SymbolProcessorEnvironment,
) : SymbolProcessor {

    private var processed = false

    override fun process(resolver: Resolver): List<KSAnnotated> {
        if (processed) return emptyList()
        processed = true

        source.listFiles().forEach { pb ->
            try {
                val config = source.decode(pb)
                val file = config.element as? ExternalFile
                    ?: error("rcc-component: expected a file declaration at the root of '${pb.name}', was ${config.element}")
                generateModuleFile(resolver, config.packageName, config.name, file)
            } catch (e: Exception) {
                environment.logger.error(
                    "rcc-component: не удалось сгенерировать модуль '${pb.name}': ${e.stackTraceToString()}"
                )
            }
        }

        return emptyList()
    }

    private fun generateModuleFile(resolver: Resolver, packageName: String, moduleName: String, file: ExternalFile) {
        val optInMarkers = LinkedHashSet<ClassName>()
        val suppressDiagnostics = LinkedHashSet<String>()

        val binds = file.accept(BindCollector(), null)
        val bindBlocks = binds.mapNotNull { bind ->
            resolveBindBlock(resolver, bind, optInMarkers, suppressDiagnostics)
        }

        val fileName = file.name.removeSuffix(".kt")

        val moduleFun = FunSpec.builder("create$fileName")
            .returns(RccComponentEntityNames.HOST_MODULE_CLASS)
            .addCode(buildModuleBody(moduleName, bindBlocks))
            .build()

        val fileSpec = FileSpec.builder(packageName, fileName)
            .indent("    ")
            .addFunction(moduleFun)
        if (optInMarkers.isNotEmpty()) {
            // Некоторые реальные Composer-методы (напр. shouldExecute/rememberedValue) помечены
            // @RequiresOptIn-маркерами вроде InternalComposeApi — сгенерированный файл реально их
            // ВЫЗЫВАЕТ (не имитирует), поэтому обязан самостоятельно дать @OptIn, иначе реальный
            // Kotlin-компилятор (компилирующий ЭТОТ файл нормально, без rcc-перехвата) откажет
            // ошибкой "internal API". Маркеры собираются автоматически по резолвнутым декларациям
            // (см. collectOptInMarkers), а не захардкожены — обобщается на любой будущий маркер.
            fileSpec.addAnnotation(
                AnnotationSpec.builder(ClassName("kotlin", "OptIn"))
                    .addMember(
                        optInMarkers.joinToString(", ") { "%T::class" },
                        *optInMarkers.toTypedArray(),
                    )
                    .useSiteTarget(AnnotationSpec.UseSiteTarget.FILE)
                    .build()
            )
        }
        if (suppressDiagnostics.isNotEmpty()) {
            // internal top-level функция из ДРУГОГО Gradle-модуля (см. doc-комментарий на месте
            // сбора suppressDiagnostics в resolveBindBlock) — `@Suppress("INVISIBLE_REFERENCE",
            // "INVISIBLE_MEMBER")` даёт реальному Kotlin-компилятору обычным (не рефлексивным)
            // способом сослаться на неё; компилятор в ответ печатает предупреждение "compiler
            // behavior is UNSPECIFIED" — эмпирически подтверждено, это WARNING, не ERROR, сборка
            // проходит, сгенерированный байткод — обычный прямой INVOKESTATIC (проверено javap).
            fileSpec.addAnnotation(
                AnnotationSpec.builder(ClassName("kotlin", "Suppress"))
                    .addMember(suppressDiagnostics.joinToString(", ") { "%S" }, *suppressDiagnostics.toTypedArray())
                    .useSiteTarget(AnnotationSpec.UseSiteTarget.FILE)
                    .build()
            )
        }

        fileSpec.build().writeTo(environment.codeGenerator, Dependencies(aggregating = false))
    }

    /**
     * `internal`-декларация, доступная СНАРУЖИ модуля по факту (JVM-байткод физически public), но
     * запрещённая обычным Kotlin-вызовом с фронтенда — см. doc-комментарий у ветки INTERNAL в
     * [resolveBindBlock]. В отличие от той ветки (только top-level функции, `decl.modifiers` самой
     * функции), здесь нужно пройтись по ВСЕЙ цепочке `parentDeclaration` — эмпирически найдено на
     * `androidx.compose.material3.tokens.FilledButtonTokens` (internal `object`, публичные
     * геттеры внутри него): эффективная видимость члена — это видимость САМОГО ограничительного
     * звена в цепочке, не только его собственная.
     */
    private fun KSDeclaration.collectVisibilitySuppress(into: MutableSet<String>) {
        var decl: KSDeclaration? = this
        while (decl != null) {
            if (Modifier.INTERNAL in decl.modifiers) {
                into += "INVISIBLE_REFERENCE"
                into += "INVISIBLE_MEMBER"
                return
            }
            decl = decl.parentDeclaration
        }
    }

    /**
     * Для non-nullable value-class параметра (см. GeneratorMetaComponent.kt's
     * `valueClassRawTypeOrNull` — та же raw-unwrap ветка кодогенерации), чей primary-конструктор
     * `internal` (эмпирически найдено на BlendMode/CompositingStrategy/TransformOrigin/TextOverflow/
     * TextUnit — сгенерированный код напрямую вызывает `Type(rawValue)`, что требует того же
     * @Suppress("INVISIBLE_REFERENCE","INVISIBLE_MEMBER"), что и обычные internal-функции.
     */
    private fun KSValueParameter.collectValueClassCtorVisibilitySuppress(into: MutableSet<String>) {
        val resolvedType = type.resolve()
        if (resolvedType.isMarkedNullable) return
        val classDecl = resolvedType.declaration as? KSClassDeclaration ?: return
        // См. doc-комментарий valueClassRawTypeOrNull (GeneratorMetaComponent.kt) — этот метод
        // должен матчить РОВНО ТУ ЖЕ raw-unwrap ветку, иначе suppress добавлялся бы для
        // конструктора, который на самом деле не вызывается напрямую (INLINE-only классы теперь
        // всегда идут через popObject<T>(), не через Type(rawValue)).
        val isDp = classDecl.qualifiedName?.asString() == "androidx.compose.ui.unit.Dp"
        if (!isDp && Modifier.VALUE !in classDecl.modifiers) return
        val ctor = classDecl.primaryConstructor ?: return
        if (Modifier.INTERNAL in ctor.modifiers) {
            into += "INVISIBLE_REFERENCE"
            into += "INVISIBLE_MEMBER"
        }
    }

    private fun KSAnnotated.collectOptInMarkers(into: MutableSet<ClassName>) {
        for (ann in annotations) {
            val annDecl = ann.annotationType.resolve().declaration as? KSClassDeclaration ?: continue
            val isOptInMarker = annDecl.annotations.any {
                it.annotationType.resolve().declaration.qualifiedName?.asString() == "kotlin.RequiresOptIn"
            }
            if (isOptInMarker) {
                into += ClassName(annDecl.packageName.asString(), annDecl.simpleName.asString())
            }
        }
    }

    private fun resolveBindBlock(
        resolver: Resolver,
        bind: ResolvedBind,
        optInMarkers: MutableSet<ClassName>,
        suppressDiagnostics: MutableSet<String>,
    ): CodeBlock? = when (bind) {
        is ResolvedBind.Fun -> {
            val fqName = bind.decl.fqName.asString()
            val parameters = bind.decl.parameters.map { it.matchFqName() }
            val decl = resolveFunctionDeclaration(resolver, fqName, parameters, bind.receiver)
            decl?.collectOptInMarkers(optInMarkers)
            decl?.collectVisibilitySuppress(suppressDiagnostics)
            decl?.parameters?.forEach { it.collectValueClassCtorVisibilitySuppress(suppressDiagnostics) }
            when {
                // Default-параметры (с ними или без) больше не влияют на выбор пути: компилятор
                // (RccMethodVisitor.kt, DefaultArgumentResolver/spliceComposableDefaults) резолвит
                // их сам до эмита HOST_INVOKE, подставляя реальные значения прямо в call site —
                // сюда всегда приходит вызов, у которого ни один аргумент фактически не опущен.
                // Раньше здесь была развилка на рефлексивный codeBlockBindComposableInvokeWithDefaults
                // (удалён).
                decl != null && decl.isComposable() -> codeBlockBindComposableInvoke(bind.stableKey, decl)

                // private top-level функция из ДРУГОГО Gradle-модуля — намеренно не поддерживается:
                // связывание с private-декларацией — нарушение видимости, а не то, что нужно
                // обходить рефлексией. Раньше здесь был рефлексивный путь (codeBlockBindInvokeReflective,
                // удалён) — теперь это явная ошибка биндинга.
                decl != null && decl.parentDeclaration !is KSClassDeclaration &&
                    Modifier.PRIVATE in decl.modifiers -> {
                    environment.logger.error(
                        "rcc-component: cannot bind private declaration '$fqName' — binding private declarations is not supported"
                    )
                    return null
                }

                // internal top-level функция из ДРУГОГО Gradle-модуля — физически public на
                // JVM-уровне (эмпирически: androidx.compose.runtime.updateChangedFlags,
                // internal, нужна ЛЮБОЙ restartable composable-функции), но реальный
                // Kotlin-фронтенд отвергает межмодульный вызов ИЗ ИСХОДНИКА (`internal` — module-
                // private на уровне компилятора, не JVM). `@Suppress("INVISIBLE_REFERENCE",
                // "INVISIBLE_MEMBER")` на файле снимает именно эту фронтенд-проверку и позволяет
                // обычный (не рефлексивный) вызов — эмпирически подтверждено отдельным пробником в
                // этой сессии на РЕАЛЬНОМ updateChangedFlags из androidx.compose.runtime 1.12.0:
                // компилятор компилирует, печатая лишь предупреждение "UNSPECIFIED behavior", а
                // итоговый байткод — прямой INVOKESTATIC (проверено javap). suppressDiagnostics
                // собирается по всем бинд-блокам модуля и применяется файлово в generateModuleFile
                // — тем же способом, что и optInMarkers/@OptIn чуть выше по этому же файлу.
                decl != null && decl.parentDeclaration !is KSClassDeclaration &&
                    Modifier.INTERNAL in decl.modifiers -> {
                    suppressDiagnostics += "INVISIBLE_REFERENCE"
                    suppressDiagnostics += "INVISIBLE_MEMBER"
                    codeBlockBindInvoke(bind.stableKey, decl)
                }

                decl != null -> codeBlockBindInvoke(bind.stableKey, decl)

                isBareObjectReference(resolver, fqName) -> codeBlockBindInstance(bind.stableKey, fqName)

                else -> {
                    val prop = resolveClassProperty(resolver, fqName)
                        ?: resolveExtensionPropertyOnValue(resolver, fqName)
                    if (prop == null) {
                        environment.logger.error(
                            "rcc-component: cannot resolve '$fqName' as a function or a value" +
                                    if (parameters.isNotEmpty()) " with parameters $parameters" else ""
                        )
                        return null
                    }
                    prop.collectOptInMarkers(optInMarkers)
                    prop.collectVisibilitySuppress(suppressDiagnostics)
                    if (prop.isComposableGetter()) {
                        codeBlockBindComposableInstance(bind.stableKey, fqName)
                    } else {
                        codeBlockBindInstance(bind.stableKey, fqName)
                    }
                }
            }
        }

        is ResolvedBind.Ctor -> {
            val fqName = bind.decl.fqName.asString()
            val parameters = bind.decl.parameters.map { it.matchFqName() }
            val decl = resolveConstructorDeclaration(resolver, fqName, parameters)
            if (decl == null) {
                environment.logger.error(
                    "rcc-component: cannot resolve constructor '$fqName'" +
                            if (parameters.isNotEmpty()) " with parameters $parameters" else ""
                )
                return null
            }
            decl.collectOptInMarkers(optInMarkers)
            decl.collectVisibilitySuppress(suppressDiagnostics)
            decl.parameters.forEach { it.collectValueClassCtorVisibilitySuppress(suppressDiagnostics) }
            codeBlockBindConstructor(bind.stableKey, decl)
        }

        is ResolvedBind.Getter -> {
            val fqName = bind.decl.fqName.asString()
            if (isBareObjectReference(resolver, fqName)) {
                codeBlockBindInstance(bind.stableKey, fqName)
            } else {
                val prop = resolveClassProperty(resolver, fqName, bind.receiver)
                if (prop == null) {
                    environment.logger.error(
                        "rcc-component: cannot resolve property getter '$fqName'" +
                                if (bind.receiver != null) " with receiver ${bind.receiver}" else ""
                    )
                    return null
                }
                prop.collectOptInMarkers(optInMarkers)
                prop.collectVisibilitySuppress(suppressDiagnostics)
                if (prop.hasNoReceiver()) {
                    if (prop.isComposableGetter()) {
                        codeBlockBindComposableInstance(bind.stableKey, fqName)
                    } else {
                        codeBlockBindInstance(bind.stableKey, fqName)
                    }
                } else {
                    codeBlockBindPropertyGetter(bind.stableKey, prop)
                }
            }
        }

        is ResolvedBind.Setter -> {
            val fqName = bind.decl.fqName.asString()
            val prop = resolveClassProperty(resolver, fqName, bind.receiver)
            if (prop == null) {
                environment.logger.error(
                    "rcc-component: cannot resolve property setter '$fqName'" +
                            if (bind.receiver != null) " with receiver ${bind.receiver}" else ""
                )
                return null
            }
            prop.collectOptInMarkers(optInMarkers)
            prop.collectVisibilitySuppress(suppressDiagnostics)
            codeBlockBindPropertySetter(bind.stableKey, prop)
        }
    }

    private fun resolveFunctionDeclaration(
        resolver: Resolver,
        fqName: String,
        parameters: List<String>,
        receiver: String?,
    ): KSFunctionDeclaration? = resolver
        .getFunctionDeclarationsByName(
            resolver.getKSNameFromString(fqName),
            includeTopLevel = true,
        )
        .filter { decl -> decl.matches(parameters, receiver) }
        .firstOrNull()
        ?: resolveClassMemberFunction(resolver, fqName, parameters, receiver)

    private fun resolveConstructorDeclaration(
        resolver: Resolver,
        fqName: String,
        parameters: List<String>,
    ): KSFunctionDeclaration? {
        val classDecl =
            resolver.getClassDeclarationByName(resolver.getKSNameFromString(fqName))
                ?: return null
        // `object`/companion object объявления (`androidx.compose.ui.Modifier.Companion` и
        // подобные) — Kotlin-синглтоны, КSP тем не менее выдаёт для них реальный (синтетический,
        // приватный) `primaryConstructor` — но `ClassName()` синтаксически недопустим для object'ов
        // (эмпирически: `codeBlockBindConstructor` сгенерировал `Modifier.Companion()`, реальную
        // компиляционную ошибку). Синглтоны биндятся ОТДЕЛЬНЫМ путём — `isBareObjectReference` →
        // `codeBlockBindInstance` (bare-reference push, без вызова) — этот резолвер обязан их
        // пропускать, а не находить "конструктор".
        if (classDecl.classKind == ClassKind.OBJECT) return null

        val constructors = sequence {
            classDecl.primaryConstructor?.let { yield(it) }
            yieldAll(
                classDecl.declarations
                    .filterIsInstance<KSFunctionDeclaration>()
                    .filter { it.isConstructor() }
            )
        }

        return constructors.firstOrNull { it.matchesParameters(parameters) }
    }

    private fun resolveClassMemberFunction(
        resolver: Resolver,
        fqName: String,
        parameters: List<String>,
        receiver: String?,
    ): KSFunctionDeclaration? {
        val lastDot = fqName.lastIndexOf('.')
        if (lastDot < 0) return null
        val className = fqName.substring(0, lastDot)
        val funcName = fqName.substring(lastDot + 1)

        val classDecl = resolver.getClassDeclarationByName(resolver.getKSNameFromString(className))
            ?: return null

        return classDecl.getAllFunctions().firstOrNull { fn ->
            fn.simpleName.asString() == funcName &&
                    (fn.matches(parameters, receiver) || fn.matchesInheritedMember(parameters, receiver, classDecl))
        }
    }

    private fun KSFunctionDeclaration.matchesInheritedMember(
        parameters: List<String>,
        receiver: String?,
        classDecl: KSClassDeclaration,
    ): Boolean {
        if (!matchesParameters(parameters)) return false
        if (receiver != null && receiver != classDecl.qualifiedName?.asString()) return false
        return true
    }

    private fun KSFunctionDeclaration.matches(parameters: List<String>, receiver: String?): Boolean =
        matchesParameters(parameters) && matchesReceiver(receiver)

    private fun KSFunctionDeclaration.matchesParameters(parameters: List<String>): Boolean {
        if (parameters.isEmpty()) return true
        if (this.parameters.size != parameters.size) return false
        return this.parameters.zip(parameters).all { (param, fqn) -> param.matchesType(fqn) }
    }

    private fun KSFunctionDeclaration.matchesReceiver(receiver: String?): Boolean {
        if (receiver == null) return true
        val actual = extensionReceiver?.resolve()?.declaration?.qualifiedName?.asString()
            ?: (parentDeclaration as? KSClassDeclaration)?.qualifiedName?.asString()
        return actual == receiver
    }

    private fun KSValueParameter.matchesType(fqn: String): Boolean {
        if (fqn == "kotlin.Array") return isVararg
        if (isVararg) return false
        val declaration = type.resolve().declaration
        // Параметр, объявленный собственным type-параметром функции (`fun <T> spring(..., v: T)`)
        // — на уровне байткода эрасится в java.lang.Object, у него нет qualifiedName вообще
        // (KSTypeParameter). Тот же смысл, что и у уже принятого соглашения "kotlin.Any" для
        // произвольных/erased параметров (см. Composer.changed(Any)) — реальный кейс:
        // androidx.compose.animation.core.spring<T>(Float, Float, T).
        if (declaration is KSTypeParameter) return fqn == "kotlin.Any"
        return declaration.qualifiedName?.asString() == fqn
    }

    /**
     * Bare-ссылка на объект-синглтон (companion ЛЮБОГО имени — не обязательно "Companion" — или
     * обычный top-level/nested `object`) — структурная проверка через реальное KSP-объявление,
     * а не строковая эвристика по хвосту имени (`substringAfterLast('.') == "Companion"` ловила
     * ТОЛЬКО безымянный companion — реальный баг, найденный пользователем: именованный
     * `companion object Default : X` или обычный `object Foo` мимо неё бы прошли).
     *
     * `ClassKind.ENUM_ENTRY` — тот же bare-GETSTATIC паттерн байткода (статическое поле-константа
     * на самом enum-классе), просто для обычных `enum class` entries (например
     * `Orientation.Horizontal`) вместо `object`/companion — реальный кейс, найденный на
     * `PagerDefaults.pageNestedScrollConnection(state, Orientation.Horizontal)`.
     */
    private fun isBareObjectReference(resolver: Resolver, fqName: String): Boolean {
        val classDecl = resolver.getClassDeclarationByName(resolver.getKSNameFromString(fqName))
        return classDecl?.classKind == ClassKind.OBJECT || classDecl?.classKind == ClassKind.ENUM_ENTRY
    }

    private fun resolveClassProperty(
        resolver: Resolver,
        fqName: String,
        expectedReceiver: String? = null,
    ): KSPropertyDeclaration? {
        val lastDot = fqName.lastIndexOf('.')
        if (lastDot < 0) return null
        val className = fqName.substring(0, lastDot)
        val propName = fqName.substring(lastDot + 1)

        val classDecl = resolver.getClassDeclarationByName(resolver.getKSNameFromString(className))
        if (classDecl != null) {
            classDecl.getAllProperties()
                .firstOrNull {
                    it.simpleName.asString() == propName && it.matchesReceiver(
                        expectedReceiver
                    )
                }
                ?.let { return it }

            classDecl.declarations
                .filterIsInstance<KSClassDeclaration>()
                .firstOrNull { it.isCompanionObject }
                ?.getAllProperties()
                ?.firstOrNull {
                    it.simpleName.asString() == propName && it.matchesReceiver(
                        expectedReceiver
                    )
                }
                ?.let { return it }
        }

        // getPropertyDeclarationByName — прямой индексированный поиск по ПОЛНОМУ fqName, а не
        // скан всего пакета: getDeclarationsFromPackage на реально больших внешних пакетах
        // (androidx.compose.runtime целиком) эмпирически НЕ находит конкретное top-level
        // extension-свойство (найдено на Composer.currentCompositeKeyHashCode — Box'ов
        // maybeCachedBoxMeasurePolicy/rememberBoxMeasurePolicy сплайсинг тянет её внутрь).
        //
        // matchesReceiver здесь намеренно НЕ применяется строго: для этого конкретного свойства
        // extensionReceiver реально присутствует (это extension-property), но
        // `.resolve().declaration.qualifiedName` возвращает null (эмпирически подтверждено debug-
        // логом) — похоже на KSP-ограничение для некоторых common/expect-объявлений из
        // androidx.compose.runtime. Раз найдено ТОЧНО по fqName (уникальное имя, не перегружено
        // разными receiver'ами), доверяем прямому совпадению без доп. проверки receiver'а.
        val direct = resolver.getPropertyDeclarationByName(
            resolver.getKSNameFromString(fqName),
            includeTopLevel = true,
        )
        if (direct != null && direct.extensionReceiver != null) return direct
        direct?.takeIf { it.matchesReceiver(expectedReceiver) }
            ?.let { return it }

        @OptIn(KspExperimental::class)
        val topLevel = resolver.getDeclarationsFromPackage(className)
            .filterIsInstance<KSPropertyDeclaration>()
            .firstOrNull {
                it.simpleName.asString() == propName && it.matchesReceiver(
                    expectedReceiver
                )
            }
        return topLevel
    }

    private fun KSPropertyDeclaration.matchesReceiver(expected: String?): Boolean {
        if (expected == null) return true
        val actual = extensionReceiver?.resolve()?.declaration?.qualifiedName?.asString()
            ?: (parentDeclaration as? KSClassDeclaration)?.qualifiedName?.asString()
        return actual == expected
    }

    private fun resolveExtensionPropertyOnValue(
        resolver: Resolver,
        fqName: String
    ): KSPropertyDeclaration? {
        val lastDot = fqName.lastIndexOf('.')
        if (lastDot < 0) return null
        val receiverFqName = fqName.substring(0, lastDot)
        val propName = fqName.substring(lastDot + 1)

        val receiverDot = receiverFqName.lastIndexOf('.')
        if (receiverDot < 0) return null
        val receiverPkg = receiverFqName.substring(0, receiverDot)
        val receiverName = receiverFqName.substring(receiverDot + 1)

        @OptIn(KspExperimental::class)
        val receiverProperty = resolver.getDeclarationsFromPackage(receiverPkg)
            .filterIsInstance<KSPropertyDeclaration>()
            .firstOrNull { it.simpleName.asString() == receiverName }
            ?: return null

        val receiverType = receiverProperty.type.resolve()

        val typePackage = (receiverType.declaration as? KSClassDeclaration)?.packageName?.asString()
            ?: return null

        val candidate = resolver.getPropertyDeclarationByName(
            resolver.getKSNameFromString("$typePackage.$propName"),
            includeTopLevel = true,
        ) ?: return null

        val extensionType = candidate.extensionReceiver?.resolve() ?: return null
        return candidate.takeIf { extensionType.isAssignableFrom(receiverType) }
    }

    private fun KSPropertyDeclaration.hasNoReceiver(): Boolean {
        val parentClass =
            parentDeclaration as? KSClassDeclaration ?: return extensionReceiver == null
        return extensionReceiver == null && parentClass.classKind == ClassKind.OBJECT
    }

    private fun KSPropertyDeclaration.isComposableGetter(): Boolean =
        isComposable() || getter?.annotations?.any {
            it.annotationType.resolve().declaration.qualifiedName?.asString() ==
                    RccComponentFqName.COMPOSABLE_ANNOTATION_FQNAME
        } == true

    private fun buildModuleBody(moduleName: String, bindBlocks: List<CodeBlock>): CodeBlock =
        buildCodeBlock {
            add("return %M(\n", RccComponentEntityNames.hostModuleFun)
            indent()
            add("name = %S,\n", moduleName)
            add("size = %L\n", bindBlocks.size)
            unindent()
            add(")")
            if (bindBlocks.isNotEmpty()) {
                add(" {\n")
                indent()
                bindBlocks.forEachIndexed { i, block ->
                    if (i > 0) add("\n")
                    add("%L\n", block)
                }
                unindent()
                add("}")
            }
            add("\n")
        }
}
