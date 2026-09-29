package com.ukenoveldiyar.rcc.compiler.plugin

import com.ukenoveldiiyar.rcc.compiler.plugin.ir.defaults.DefaultArgumentResolver
import org.jetbrains.org.objectweb.asm.tree.AbstractInsnNode
import org.jetbrains.org.objectweb.asm.tree.MethodInsnNode
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class DefaultArgumentResolverRealBytecodeTest {

    private val classpathRoots: List<File> =
        System.getProperty("composeRuntime.jvm.classpath")
            .split(File.pathSeparator)
            .map { File(it) }

    @Test
    fun ordinaryMutableStateOfPolicyDefault() {
        val resolver = DefaultArgumentResolver(classpathRoots)

        val resolved = resolver.resolveOrdinaryDefault(
            ownerInternalName = "androidx/compose/runtime/SnapshotStateKt",
            methodName = "mutableStateOf",
            defaultBridgeDescriptor = "(Ljava/lang/Object;Landroidx/compose/runtime/SnapshotMutationPolicy;ILjava/lang/Object;)Landroidx/compose/runtime/MutableState;",
            maskSlot = 2,
            paramIndex = 1,
            paramSlot = 1,
        )
        assertNotNull(resolved, "mutableStateOf's policy default expression must resolve")

        val call = resolved.single { it is MethodInsnNode } as MethodInsnNode
        assertEquals("structuralEqualityPolicy", call.name)
    }

    @Test
    fun composableColumnDefaults() {
        val resolver = DefaultArgumentResolver(classpathRoots)

        val descriptor = "(Landroidx/compose/ui/Modifier;Landroidx/compose/foundation/layout/Arrangement\$Vertical;" +
            "Landroidx/compose/ui/Alignment\$Horizontal;Lkotlin/jvm/functions/Function3;" +
            "Landroidx/compose/runtime/Composer;II)V"

        val modifierDefault = resolver.resolveComposableDefault(
            ownerInternalName = "androidx/compose/foundation/layout/ColumnKt",
            methodName = "Column",
            methodDescriptor = descriptor,
            maskSlot = 6,
            paramIndex = 0,
            paramSlot = 0,
        )
        assertNotNull(modifierDefault, "Column's modifier default expression must resolve")
        assertGetStatic(modifierDefault, "androidx/compose/ui/Modifier", "Companion")

        val arrangementDefault = resolver.resolveComposableDefault(
            ownerInternalName = "androidx/compose/foundation/layout/ColumnKt",
            methodName = "Column",
            methodDescriptor = descriptor,
            maskSlot = 6,
            paramIndex = 1,
            paramSlot = 1,
        )
        assertNotNull(arrangementDefault, "Column's verticalArrangement default expression must resolve")
        assertEquals(
            "getTop",
            arrangementDefault.filterIsInstance<MethodInsnNode>().single().name,
        )

        val alignmentDefault = resolver.resolveComposableDefault(
            ownerInternalName = "androidx/compose/foundation/layout/ColumnKt",
            methodName = "Column",
            methodDescriptor = descriptor,
            maskSlot = 6,
            paramIndex = 2,
            paramSlot = 2,
        )
        assertNotNull(alignmentDefault, "Column's horizontalAlignment default expression must resolve")
        assertEquals(
            "getStart",
            alignmentDefault.filterIsInstance<MethodInsnNode>().single().name,
        )
    }

    private fun assertGetStatic(insns: List<AbstractInsnNode>, ownerSuffix: String, fieldNameSuffix: String) {
        val getStatic = insns.firstOrNull { it.opcode == org.jetbrains.org.objectweb.asm.Opcodes.GETSTATIC }
            as? org.jetbrains.org.objectweb.asm.tree.FieldInsnNode
        assertNotNull(getStatic, "expected a GETSTATIC instruction")
        assertEquals(ownerSuffix, getStatic.owner)
        assertEquals(fieldNameSuffix, getStatic.name)
    }
}