package com.ukenoveldiyar.rcc.external.ksp.generator

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSValueParameter
import com.google.devtools.ksp.symbol.Modifier
import com.google.devtools.ksp.symbol.Origin
import com.squareup.kotlinpoet.ANY
import com.squareup.kotlinpoet.ARRAY
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.STAR
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeVariableName
import com.squareup.kotlinpoet.buildCodeBlock
import com.squareup.kotlinpoet.ksp.toTypeName
import com.ukenoveldiyar.rcc.external.ksp.RccComponentEntityNames
import com.ukenoveldiyar.rcc.external.ksp.RccComponentFqName

fun codeBlockBindInvoke(
    stableKey: String,
    declaration: KSFunctionDeclaration,
) = buildCodeBlock {
    add("%M(%S) { call ->\n", RccComponentEntityNames.bindNamed, stableKey)
    indent()

    val parameters = declaration.parameters
    val receiverClass = declaration.parentDeclaration as? KSClassDeclaration

    val isJavaStaticMember = Modifier.JAVA_STATIC in declaration.modifiers
    val extensionReceiver = declaration.extensionReceiver
    val hasDispatchReceiver = receiverClass != null && !isJavaStaticMember
    val hasExtensionReceiver = extensionReceiver != null
    val paramOffset = (if (hasDispatchReceiver) 1 else 0) + (if (hasExtensionReceiver) 1 else 0)

    for (i in parameters.indices.reversed()) {
        val type = popTypeFor(parameters[i])
        addPop(i + paramOffset, type, parameters[i].valueClassRawTypeOrNull())
    }

    if (hasExtensionReceiver) {
        addPop(if (hasDispatchReceiver) 1 else 0, extensionReceiver.toTypeName())
    }
    if (hasDispatchReceiver) {
        addPop(0, classTypeName(receiverClass, starProjected = true))
    }

    if (parameters.isNotEmpty() || hasDispatchReceiver || hasExtensionReceiver) add("\n")

    val returnType = declaration.returnType!!.toTypeName()

    if (hasDispatchReceiver && hasExtensionReceiver) {
        val innerCall = buildCodeBlock {
            add("p1.%N%L(", declaration.simpleName.asString(), declaration.explicitTypeArgsIfNeeded(parameters))
            addCallArgs(parameters, paramOffset, declaration.isJavaOrigin())
        }
        val withBlock = buildCodeBlock {
            add("with(p0) {\n")
            indent()
            add("%L\n", innerCall)
            unindent()
            add("}")
        }
        if (isUnit(returnType)) {
            add("%L\n", withBlock)
        } else {
            add("call.push(\n")
            indent()
            add("%L\n", withBlock)
            unindent()
            add(")\n")
        }
    } else {
        val callExpr = buildCodeBlock {
            val typeArgs = declaration.explicitTypeArgsIfNeeded(parameters)
            when {
                hasExtensionReceiver -> add("p0.%M%L(", declaration.memberName(), typeArgs) // top-level extension: needs import
                hasDispatchReceiver  -> add("p0.%N%L(", declaration.simpleName.asString(), typeArgs) // member: resolved from type
                receiverClass != null -> add( // Java static member: receiver — сам класс, не значение со стека (см. isJavaStaticMember выше)
                    "%T.%N%L(", classTypeName(receiverClass, starProjected = false), declaration.simpleName.asString(), typeArgs
                )
                else                 -> add("%M%L(", declaration.memberName(), typeArgs)
            }
            addCallArgs(parameters, paramOffset, declaration.isJavaOrigin())
        }
        if (isUnit(returnType)) {
            add("%L\n", callExpr)
        } else {
            add("call.push(%L)\n", callExpr)
        }
    }

    unindent()
    add("}")
}

/**
 * Обычный (не-рефлексивный) Kotlin-вызов composable-функции — ЕДИНСТВЕННЫЙ путь для composable-
 * биндингов теперь, с default-параметрами у цели или без: компилятор (RccMethodVisitor.kt,
 * DefaultArgumentResolver/spliceComposableDefaults) резолвит default-значения ДО эмита HOST_INVOKE,
 * подставляя их прямо в call site и убирая synthetic defaultMask-параметр из потока — со стороны
 * этого генератора вызов, изначально опустивший аргументы, неотличим от вызова со всеми аргументами
 * явно. Раньше это было два разных пути (обычный + отдельный рефлексивный
 * codeBlockBindComposableInvokeWithDefaults, удалён).
 */
fun codeBlockBindComposableInvoke(
    stableKey: String,
    declaration: KSFunctionDeclaration,
) = buildCodeBlock {
    add(
        "%M(%S) { call ->\n",
        RccComponentEntityNames.bindComposableNamed,
        stableKey,
    )
    indent()

    val parameters = declaration.parameters
    // Composable-биндинги в YAML — top-level функции (Text, Box, Column, ...) либо члены
    // object-а с default-параметрами (ButtonDefaults.buttonColors); собственного
    // heap-инстанса-receiver'а у них не бывает, поэтому receiver из стека не снимается.
    val receiverClass = declaration.parentDeclaration as? KSClassDeclaration

    for (i in parameters.indices.reversed()) {
        val type = popTypeFor(parameters[i])
        addPop(i, type, parameters[i].valueClassRawTypeOrNull())
    }

    if (parameters.isNotEmpty()) add("\n")

    val callExpr = buildCodeBlock {
        val typeArgs = declaration.explicitTypeArgsIfNeeded(parameters)
        if (receiverClass != null) {
            val objectType = classTypeName(receiverClass, starProjected = false)
            add("%T.%N%L(", objectType, declaration.simpleName.asString(), typeArgs)
        } else {
            add("%M%L(", declaration.memberName(), typeArgs)
        }
        addCallArgs(parameters, 0, declaration.isJavaOrigin())
    }

    val returnType = declaration.returnType!!.toTypeName()
    if (isUnit(returnType)) {
        add("%L\n", callExpr)
    } else {
        add("call.push(%L)\n", callExpr)
    }

    unindent()
    add("}")
}

/** Геттер без параметров — default-параметров у него по определению нет, отсеивать нечего
 *  (в отличие от [codeBlockBindComposableInvoke]). */
fun codeBlockBindComposableInstance(stableKey: String, fqName: String) = buildCodeBlock {
    add("%M(%S) { call ->\n", RccComponentEntityNames.bindComposableNamed, stableKey)
    indent()
    val parts = fqName.split(".")
    val classIdx = parts.indexOfFirst { it[0].isUpperCase() }
    if (classIdx < 0) {
        // Нет ни одного заглавного сегмента вообще — это НЕ "Owner.member" (объект/companion),
        // а обычное top-level @Composable-свойство без receiver'а (например
        // `currentCompositeKeyHashCode` — читает Composer неявно внутри своего тела, не через
        // receiver). Последний сегмент — само имя свойства, всё остальное — пакет.
        val pkg = parts.dropLast(1).joinToString(".")
        addStatement("call.push(%M)", MemberName(pkg, parts.last()))
        unindent()
        add("}")
        return@buildCodeBlock
    }
    val pkg = parts.take(classIdx).joinToString(".")
    val segments = parts.drop(classIdx) // e.g. ["LocalTextStyle", "current"]
    val objectMember = MemberName(pkg, segments.first())
    if (segments.size == 1) {
        addStatement("call.push(%M)", objectMember)
    } else {
        addStatement("call.push(%M.%L)", objectMember, segments.drop(1).joinToString("."))
    }
    unindent()
    add("}")
}

fun codeBlockBindInstance(stableKey: String, fqName: String) = buildCodeBlock {
    val parts = fqName.split(".")
    val classIdx = parts.indexOfFirst { it[0].isUpperCase() }
    check(classIdx >= 0) { "codeBlockBindInstance: no uppercase segment in '$fqName'" }
    val actualPkg = parts.take(classIdx).joinToString(".")
    val classSegments = parts.drop(classIdx)

    add("%M(%S) { call ->\n", RccComponentEntityNames.bindNamed, stableKey)
    indent()
    if (classSegments.size == 1) {
        addStatement("call.push(%M)", MemberName(actualPkg, classSegments[0]))
    } else {
        val classRef = ClassName(actualPkg, classSegments[0])
        val memberName = classSegments.drop(1).joinToString(".")
        addStatement("call.push(%T.%L)", classRef, memberName)
    }
    unindent()
    add("}")
}

fun codeBlockBindConstructor(
    stableKey: String,
    declaration: KSFunctionDeclaration,
) = buildCodeBlock {
    add("%M(%S) { call ->\n", RccComponentEntityNames.bindNamed, stableKey)
    indent()

    val parameters = declaration.parameters
    for (i in parameters.indices.reversed()) {
        addPop(i, popTypeFor(parameters[i]))
    }

    if (parameters.isNotEmpty()) add("\n")

    val classDecl = declaration.parentDeclaration as KSClassDeclaration
    val callExpr = buildCodeBlock {
        add("%T(", classTypeName(classDecl, starProjected = false))
        addCallArgs(parameters, 0, declaration.isJavaOrigin())
    }
    add("call.push(%L)\n", callExpr)

    unindent()
    add("}")
}

// ── Вспомогательные ─────────────────────────────────────────────────────────

fun KSDeclaration.isComposable(): Boolean = annotations.any {
    it.annotationType.resolve().declaration.qualifiedName?.asString() ==
        RccComponentFqName.COMPOSABLE_ANNOTATION_FQNAME
}

fun KSFunctionDeclaration.isConstructor(): Boolean = simpleName.asString() == "<init>"

/** См. doc-комментарий [addCallArgs] — Java-объявления (в т.ч. из JDK/classpath-джаров, не только
 *  исходники текущего модуля) не поддерживают именованные аргументы на стороне вызывающего Kotlin-кода. */
private fun KSFunctionDeclaration.isJavaOrigin(): Boolean =
    origin == Origin.JAVA || origin == Origin.JAVA_LIB

private fun KSFunctionDeclaration.memberName() =
    MemberName(packageName.asString(), simpleName.asString())

private fun KSFunctionDeclaration.explicitTypeArgsIfNeeded(parameters: List<KSValueParameter>): String =
    if (parameters.isEmpty() && typeParameters.isNotEmpty()) {
        typeParameters.joinToString(prefix = "<", postfix = ">") { "Any?" }
    } else {
        ""
    }

private fun popNameFor(type: TypeName): String = when (type) {
    is ClassName -> when ("${type.packageName}.${type.simpleName}") {
        "kotlin.Boolean" -> "popBoolean"
        "kotlin.Byte"    -> "popByte"
        "kotlin.Char"    -> "popChar"
        "kotlin.Short"   -> "popShort"
        "kotlin.Int"     -> "popInt"
        "kotlin.Long"    -> "popLong"
        "kotlin.Float"   -> "popFloat"
        "kotlin.Double"  -> "popDouble"
        // Беззнаковые — та же битовая ширина/представление на стеке, что и их знаковые аналоги
        // (просто другая интерпретация тех же бит) — эмпирически найдено на Color (value class,
        // underlying `ULong`, у которого своя собственная underlying `Long`).
        "kotlin.UByte"   -> "popByte"
        "kotlin.UShort"  -> "popShort"
        "kotlin.UInt"    -> "popInt"
        "kotlin.ULong"   -> "popLong"
        else             -> "popObject"
    }
    else -> "popObject"
}

/** Беззнаковые типы не конвертируются неявно из своих знаковых аналогов (`Long` →/→ `ULong` —
 *  всегда explicit `.toULong()`), в отличие от остальных примитивов в [addPop]. */
private fun unsignedConversionOrNull(type: TypeName): String? = if (type is ClassName) {
    when ("${type.packageName}.${type.simpleName}") {
        "kotlin.UByte" -> "toUByte"
        "kotlin.UShort" -> "toUShort"
        "kotlin.UInt" -> "toUInt"
        "kotlin.ULong" -> "toULong"
        else -> null
    }
} else null

/**
 * Реальный underlying-тип `@JvmInline value class` (единственный параметр его primary-конструктора
 * — напр. `Dp` → `Float`, `Color` → `ULong`) или `null`, если параметр не value class. Не
 * применяется к vararg (маловероятный, не встречавшийся случай — обычная object-array семантика
 * там и так работает).
 *
 * Зачем это вообще нужно: значение на стеке rcc всегда лежит в СЫРОМ, "не заинлайненном" виде (raw Float для Dp и т.п.) —
 * ни компилятор (RccMethodVisitor), ни рантайм никогда не boxing'ят value class сами
 * ("constructor-impl" — identity-опкод, raw остаётся raw). Значит `call.popObject<Dp>()`
 * попытался бы снять и unchecked-cast'ить raw Float КАК ЕСЛИ БЫ это был реальный boxed Dp-объект —
 * этот cast всегда бы "успевал" на уровне erased-generics (никакого runtime instanceof-чека для
 * unchecked cast нет), но дальнейшее использование значения было бы неверным (не настоящий Dp).
 * Пример из официальной документации Kotlin (inline-classes.html#representation) — `Dp(rawFloat)`,
 * вызванный в контексте, требующем boxed-представления (сюда передаётся `Any?`/интерфейс/дженерик),
 * компилятор САМ решает боксить (`constructor-impl` + `box-impl`) — реальный, а не имитированный
 * Dp получается без единой строчки reflection, ручной работы с box-impl или per-type таблицы
 * "как сконструировать X" — просто обычный, компилируемый нормально host-bind вызов.
 */
private fun KSValueParameter.valueClassRawTypeOrNull(): TypeName? {
    if (isVararg) return null
    val resolvedType = type.resolve()
    // Nullable value-class параметр (`FontStyle?`, `TextAlign?`) ВСЕГДА боксится на JVM-уровне —
    // примитив не может представить null, значит на стеке rcc это настоящий boxed-объект
    // (popObject<T?>()), а не сырое представление; raw-путь ниже применим ТОЛЬКО к non-null
    // value-class параметрам. Эмпирически найдено на Text(...)'s FontStyle?/TextAlign? — попытка
    // сырого разворачивания сгенерировала бы недопустимый синтаксис `TextAlign?(rawInt)`.
    if (resolvedType.isMarkedNullable) return null
    val classDecl = resolvedType.declaration as? KSClassDeclaration ?: return null
    // ТОЛЬКО Modifier.VALUE — НЕ Modifier.INLINE (см. предыдущую версию этого комментария: пробовали
    // расширить на INLINE ради androidx.compose.ui.unit.Dp — modifiers=[FINAL, PUBLIC, INLINE],
    // VALUE отсутствует — но это оказалось НЕВЕРНЫМ обобщением. "Сырое представление на стеке"
    // зависит не от того, как KSP видит МОДИФИКАТОР ДЕКЛАРАЦИИ, а от того, откуда РЕАЛЬНО пришло
    // значение на этом конкретном call site (сырой литерал/локальная — raw; результат bare-object-
    // ссылки типа Color.Black, чей codeBlockBindInstance генерирует `call.push(Color.Black)` —
    // ВСЕГДА боксится, т.к. push(Any?) — обычный generic-параметр). Модификатор декларации ЭТОГО
    // не отражает: у Dp и у Color совершенно одинаковый modifiers=[FINAL, PUBLIC, INLINE], но
    // Color.Black как аргумент Text(color=...) реально boxed — раскрытие "как сырое" читало
    // ПОРЧЕНЫЕ биты (ColorSpace-индекс вне диапазона, ArrayIndexOutOfBoundsException в
    // Color.getColorSpace-impl). Оставляем только VALUE — то немногое, что действительно отличает
    // value-class, объявленный В ИСХОДНИКАХ (этому же compilation unit), для которого put этот
    // путь и был изначально нужен и подтверждён.
    // Точечное, не-блэнкет исключение для androidx.compose.ui.unit.Dp: его accessor'ы (`Int.dp`,
    // `Modifier.width(Dp)`, `PaddingValues(Dp)`) объявлены `inline` в самом Compose — Kotlin
    // подставляет их тело прямо в call site (никогда не вызывает реальный getDp()/оставляет
    // constructor-impl как identity-опкод), поэтому на call site всегда остаётся raw Float, а не
    // boxed Dp. Эмпирически подтверждено на реальном устройстве: `popObject<Dp>()` в этой позиции
    // получает null (падает на `Dp.unbox-impl()`), `popFloat()` + `Dp(raw)` — работает. Color/
    // TextUnit ЭТОГО свойства не имеют (их accessor'ы — обычные, не inline, функции; `.sp`
    // подтверждённо резолвится через HOST_INVOKE, а не инлайнится) — им нужен обычный boxed-путь
    // выше (Modifier.VALUE), поэтому это исключение по конкретному fqName, а не по модификатору.
    if (classDecl.qualifiedName?.asString() == "androidx.compose.ui.unit.Dp") {
        val ctorParam = classDecl.primaryConstructor?.parameters?.singleOrNull() ?: return null
        return ctorParam.type.toTypeName()
    }
    if (Modifier.VALUE !in classDecl.modifiers) return null
    val ctorParam = classDecl.primaryConstructor?.parameters?.singleOrNull() ?: return null
    return ctorParam.type.toTypeName()
}

private fun CodeBlock.Builder.addPop(
    index: Int,
    type: TypeName,
    valueClassRawType: TypeName? = null,
) {
    if (valueClassRawType != null) {
        // Снимаем СЫРОЕ представление, затем конструируем реальный value class обычным Kotlin-
        // вызовом конструктора — см. doc-комментарий valueClassRawTypeOrNull выше. `type` здесь —
        // ИМЕННО declared value-class тип (Dp), не сырой — используется как имя конструктора.
        //
        // Беззнаковый raw-тип (`ULong` у Color и т.п.) сам по себе ТОЖЕ value class (обёртка над
        // знаковым Long) — на стеке rcc лежит именно ЗНАКОВОЕ представление той же битовой ширины
        // (ни компилятор, ни рантайм никогда не boxing'ят value class, см. doc выше — это
        // рекурсивно верно и для ВЛОЖЕННЫХ value class), поэтому снимаем знаковым pop'ом и
        // конвертируем explicit'ным `.toULong()`/`.toUInt()`/... перед вызовом конструктора.
        val unsignedConversion = unsignedConversionOrNull(valueClassRawType)
        val popName = popNameFor(valueClassRawType)
        if (unsignedConversion != null) {
            addStatement("val p%LRaw = call.%L().%L()", index, popName, unsignedConversion)
        } else {
            addStatement("val p%LRaw: %T = call.%L()", index, valueClassRawType, popName)
        }
        addStatement("val p%L = %T(p%LRaw)", index, type, index)
        return
    }
    val popName = popNameFor(type)
    if (popName == "popObject") {
        // Стираем неразрешённые дженерик-переменные (напр. T в remember<T>(() -> T)) до Any? —
        // в сгенерированном файле T нигде не объявлен, использовать его как тайп-аргумент нельзя.
        // Дальше T выводится заново на месте вызова (напр. remember<Any?>(...)).
        val erased = type.eraseTypeVariables()
        addStatement("val p%L = call.%M<%T>()", index, RccComponentEntityNames.popObject, erased)
    } else {
        addStatement("val p%L: %T = call.%L()", index, type, popName)
    }
}

private fun TypeName.eraseTypeVariables(): TypeName = when (this) {
    is TypeVariableName -> ANY.copy(nullable = true)
    is LambdaTypeName -> LambdaTypeName.get(
        receiver = receiver?.eraseTypeVariables(),
        parameters = parameters.map {
            ParameterSpec.builder(it.name, it.type.eraseTypeVariables()).build()
        },
        returnType = returnType.eraseTypeVariables(),
    ).copy(nullable = isNullable, annotations = annotations) // LambdaTypeName.get() drops these
    is ParameterizedTypeName -> rawType.parameterizedBy(typeArguments.map { it.eraseTypeVariables() })
    else -> this
}

private fun CodeBlock.Builder.addCallArgs(
    parameters: List<KSValueParameter>,
    paramOffset: Int,
    // Kotlin ЗАПРЕЩАЕТ именованные аргументы на вызовах Java-деклараций ("Named arguments are
    // prohibited for non-Kotlin functions") — эмпирически найдено на java.lang.String.valueOf(int):
    // сгенерированный `String.valueOf(p0 = p0)` не компилировался. Для Java-целей (origin ==
    // JAVA/JAVA_LIB) эмитим позиционные аргументы вместо именованных; для настоящих Kotlin-целей
    // именованные остаются (устойчивее к будущей перестановке параметров в источнике).
    isJavaOrigin: Boolean,
) {
    if (parameters.isEmpty()) {
        add(")")
        return
    }
    add("\n")
    indent()
    parameters.forEachIndexed { i, p ->
        if (isJavaOrigin) {
            add("p%L", i + paramOffset)
        } else {
            add("%L = p%L", p.name!!.asString(), i + paramOffset)
        }
        add(if (i != parameters.lastIndex) ",\n" else "\n")
    }
    unindent()
    add(")")
}

private fun isUnit(type: TypeName): Boolean =
    type is ClassName && "${type.packageName}.${type.simpleName}" == "kotlin.Unit"

fun codeBlockBindPropertyGetter(
    stableKey: String,
    property: KSPropertyDeclaration,
) = buildCodeBlock {
    add("%M(%S) { call ->\n", RccComponentEntityNames.bindNamed, stableKey)
    indent()

    val isExtension = property.extensionReceiver != null
    val receiverType = if (isExtension) {
        property.extensionReceiver!!.toTypeName()
    } else {
        property.declaringClassTypeName(starProjected = true)
    }
    addPop(0, receiverType)

    val callExpr = if (isExtension) {
        val extMember = MemberName(property.packageName.asString(), property.simpleName.asString())
        CodeBlock.of("p0.%M", extMember)
    } else {
        CodeBlock.of("p0.%L", property.simpleName.asString())
    }
    addStatement("call.push(%L)", callExpr)

    unindent()
    add("}")
}

fun codeBlockBindPropertySetter(
    stableKey: String,
    property: KSPropertyDeclaration,
) = buildCodeBlock {
    add("%M(%S) { call ->\n", RccComponentEntityNames.bindNamed, stableKey)
    indent()

    val valueType = property.type.toTypeName()
    addPop(1, valueType)

    val receiverType = property.declaringClassTypeName(starProjected = false)
    addPop(0, receiverType)
    addStatement("p0.%L = p1", property.simpleName.asString())

    unindent()
    add("}")
}

private fun classTypeName(classDecl: KSClassDeclaration, starProjected: Boolean): TypeName {
    val pkg = classDecl.packageName.asString()
    val localPart = classDecl.qualifiedName!!.asString()
        .let { if (pkg.isEmpty()) it else it.removePrefix("$pkg.") }
        .split(".")
    val className = ClassName(pkg, localPart.first(), *localPart.drop(1).toTypedArray())
    return if (classDecl.typeParameters.isEmpty()) {
        className
    } else if (starProjected) {
        className.parameterizedBy(classDecl.typeParameters.map { STAR })
    } else {
        className.parameterizedBy(classDecl.typeParameters.map { ANY.copy(nullable = true) })
    }
}

private fun KSPropertyDeclaration.declaringClassTypeName(starProjected: Boolean): TypeName {
    val parent = requireNotNull(parentDeclaration as? KSClassDeclaration) {
        "Property '${qualifiedName?.asString()}' must be declared inside a class/interface"
    }
    return classTypeName(parent, starProjected)
}

private val COMPOSABLE_ANNOTATION = ClassName.bestGuess(RccComponentFqName.COMPOSABLE_ANNOTATION_FQNAME)

private fun resolvedTypeName(parameter: KSValueParameter): TypeName {
    val base = parameter.type.toTypeName()
    val hasComposable = parameter.type.annotations.any {
        it.annotationType.resolve().declaration.qualifiedName?.asString() ==
            RccComponentFqName.COMPOSABLE_ANNOTATION_FQNAME
    }
    return if (hasComposable && base is LambdaTypeName) {
        base.copy(annotations = base.annotations + AnnotationSpec
            .builder(COMPOSABLE_ANNOTATION).build())
    } else {
        base
    }
}

private fun popTypeFor(parameter: KSValueParameter): TypeName {
    val elementType = resolvedTypeName(parameter)
    return if (parameter.isVararg) ARRAY.parameterizedBy(elementType) else elementType
}
