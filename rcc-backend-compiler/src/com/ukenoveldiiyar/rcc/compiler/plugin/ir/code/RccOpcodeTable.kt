package com.ukenoveldiiyar.rcc.compiler.plugin.ir.code

import com.ukenoveldiiyar.rcc.compiler.plugin.ir.code.RccCondition.*
import com.ukenoveldiiyar.rcc.compiler.plugin.ir.code.RccFlag.*
import com.ukenoveldiiyar.rcc.compiler.plugin.ir.code.RccFlow.*
import com.ukenoveldiiyar.rcc.compiler.plugin.ir.code.RccOpcodeKind.*
import com.ukenoveldiiyar.rcc.compiler.plugin.ir.code.RccOperandKind.*
import com.ukenoveldiiyar.rcc.compiler.plugin.ir.code.RccTypeTable.*

enum class RccOpcodeKind {
    NO_OP,
    CONSTANT,
    LOCAL_LOAD,
    LOCAL_STORE,
    STACK_SHUFFLE,
    ARITHMETIC,
    CONVERSION,
    COMPARISON,
    BRANCH,
    METHOD_RETURN,
    INVOKE,
    COERCE
}

enum class RccFlow {
    NEXT,
    CONDITIONAL_JUMP,
    JUMP,
    RETURN_EXIT,
}

enum class RccFlag {

    PURE,

    COMMUTATIVE,

    ASSOCIATIVE,
}

enum class RccCondition {
    EQ, NE, LT, GE, GT, LE;

    fun negate(): RccCondition = when (this) {
        EQ -> NE; NE -> EQ; LT -> GE; GE -> LT; GT -> LE; LE -> GT
    }

    fun swap(): RccCondition = when (this) {
        EQ -> EQ; NE -> NE; LT -> GT; GT -> LT; LE -> GE; GE -> LE
    }

    fun test(comparison: Int): Boolean = when (this) {
        EQ -> comparison == 0; NE -> comparison != 0
        LT -> comparison < 0; GE -> comparison >= 0
        GT -> comparison > 0; LE -> comparison <= 0
    }
}

enum class RccArithmeticOp { ADD, SUB, MUL, DIV, REM, NEG, AND, OR, XOR, SHL, SHR, USHR }

enum class RccOperandKind(val size: Int) {

    LOCAL_INDEX(2),

    CONST_I32(4),

    CONST_I64(8),

    CP_INDEX(2),

    BRANCH_OFFSET(4),
    FUNCTION_ID(4),
    TYPE_TAG(1);

    val isBranchOffset: Boolean get() = this == BRANCH_OFFSET
}

sealed interface RccStackEffect {

    val pops: Int?

    val pushes: Int?

    data class Typed(
        val consumes: List<RccStackValueTable>,
        val produces: List<RccStackValueTable>
    ) : RccStackEffect {
        override val pops: Int get() = consumes.sumOf { it.slots }
        override val pushes: Int get() = produces.sumOf { it.slots }
    }

    data class Shuffle(override val pops: Int, override val pushes: Int) : RccStackEffect

    data object FromCallSite : RccStackEffect {
        override val pops: Int? get() = null
        override val pushes: Int? get() = null
    }
}

private fun st(consumes: List<RccStackValueTable>, produces: List<RccStackValueTable>) =
    RccStackEffect.Typed(consumes, produces)

private fun shuffle(pops: Int, pushes: Int) = RccStackEffect.Shuffle(pops, pushes)
private fun ops(vararg kinds: RccOperandKind): List<RccOperandKind> = kinds.toList()
private fun flagSet(vararg f: RccFlag): Set<RccFlag> =
    if (f.isEmpty()) emptySet() else f.toHashSet()

private fun pure() = flagSet(PURE)
private fun symmetric() = flagSet(PURE, COMMUTATIVE)
private fun algebraic() = flagSet(PURE, COMMUTATIVE, ASSOCIATIVE)

private val I = RccStackValueTable.INT
private val L = RccStackValueTable.LONG
private val F = RccStackValueTable.FLOAT
private val D = RccStackValueTable.DOUBLE
private val R = RccStackValueTable.REFERENCE

typealias RccOpcode = Int

enum class RccOpcodeTable(
    val code: RccOpcode,
    val kind: RccOpcodeKind,
    val stack: RccStackEffect,
    val operands: List<RccOperandKind> = emptyList(),
    val flow: RccFlow = NEXT,

    val type: RccTypeTable? = null,
    val flags: Set<RccFlag> = emptySet(),

    val targetType: RccTypeTable? = null,
) {

    NOP(0x00, NO_OP, shuffle(0, 0), flags = pure()),

    ACONST_NULL(0x01, CONSTANT, st(emptyList(), listOf(R)), flags = pure()),

    I32_PUSH(
        0x02,
        CONSTANT,
        st(emptyList(), listOf(I)),
        ops(CONST_I32),
        type = INT,
        flags = pure()
    ),

    I64_PUSH(
        0x03,
        CONSTANT,
        st(emptyList(), listOf(L)),
        ops(CONST_I64),
        type = LONG,
        flags = pure()
    ),

    F32_PUSH(
        0x04,
        CONSTANT,
        st(emptyList(), listOf(F)),
        ops(CONST_I32),
        type = FLOAT,
        flags = pure()
    ),

    F64_PUSH(
        0x05,
        CONSTANT,
        st(emptyList(), listOf(D)),
        ops(CONST_I64),
        type = DOUBLE,
        flags = pure()
    ),

    CP_LOAD(0x06, CONSTANT, st(emptyList(), listOf(R)), ops(CP_INDEX), flags = pure()),

    I_LOAD(
        0x10,
        LOCAL_LOAD,
        st(emptyList(), listOf(I)),
        ops(LOCAL_INDEX),
        type = INT,
        flags = pure()
    ),
    L_LOAD(
        0x11,
        LOCAL_LOAD,
        st(emptyList(), listOf(L)),
        ops(LOCAL_INDEX),
        type = LONG,
        flags = pure()
    ),
    F_LOAD(
        0x12,
        LOCAL_LOAD,
        st(emptyList(), listOf(F)),
        ops(LOCAL_INDEX),
        type = FLOAT,
        flags = pure()
    ),
    D_LOAD(
        0x13,
        LOCAL_LOAD,
        st(emptyList(), listOf(D)),
        ops(LOCAL_INDEX),
        type = DOUBLE,
        flags = pure()
    ),

    A_LOAD(
        0x14,
        LOCAL_LOAD,
        st(emptyList(), listOf(R)),
        ops(LOCAL_INDEX),
        type = REFERENCE,
        flags = pure()
    ),

    I_STORE(0x18, LOCAL_STORE, st(listOf(I), emptyList()), ops(LOCAL_INDEX), type = INT),
    L_STORE(0x19, LOCAL_STORE, st(listOf(L), emptyList()), ops(LOCAL_INDEX), type = LONG),
    F_STORE(0x1A, LOCAL_STORE, st(listOf(F), emptyList()), ops(LOCAL_INDEX), type = FLOAT),
    D_STORE(0x1B, LOCAL_STORE, st(listOf(D), emptyList()), ops(LOCAL_INDEX), type = DOUBLE),
    A_STORE(0x1C, LOCAL_STORE, st(listOf(R), emptyList()), ops(LOCAL_INDEX), type = REFERENCE),

    I_INC(0x1D, LOCAL_STORE, shuffle(0, 0), ops(LOCAL_INDEX, CONST_I32), type = INT),

    POP(0x20, STACK_SHUFFLE, shuffle(1, 0)),
    DUP(0x21, STACK_SHUFFLE, shuffle(1, 2), flags = pure()),
    DUP_X1(0x22, STACK_SHUFFLE, shuffle(2, 3), flags = pure()),
    SWAP(0x23, STACK_SHUFFLE, shuffle(2, 2), flags = pure()),

    I_ADD(0x30, ARITHMETIC, st(listOf(I, I), listOf(I)), type = INT, flags = algebraic()),
    L_ADD(0x31, ARITHMETIC, st(listOf(L, L), listOf(L)), type = LONG, flags = algebraic()),
    F_ADD(0x32, ARITHMETIC, st(listOf(F, F), listOf(F)), type = FLOAT, flags = symmetric()),
    D_ADD(0x33, ARITHMETIC, st(listOf(D, D), listOf(D)), type = DOUBLE, flags = symmetric()),
    I_SUB(0x34, ARITHMETIC, st(listOf(I, I), listOf(I)), type = INT, flags = pure()),
    L_SUB(0x35, ARITHMETIC, st(listOf(L, L), listOf(L)), type = LONG, flags = pure()),
    F_SUB(0x36, ARITHMETIC, st(listOf(F, F), listOf(F)), type = FLOAT, flags = pure()),
    D_SUB(0x37, ARITHMETIC, st(listOf(D, D), listOf(D)), type = DOUBLE, flags = pure()),
    I_MUL(0x38, ARITHMETIC, st(listOf(I, I), listOf(I)), type = INT, flags = algebraic()),
    L_MUL(0x39, ARITHMETIC, st(listOf(L, L), listOf(L)), type = LONG, flags = algebraic()),
    F_MUL(0x3A, ARITHMETIC, st(listOf(F, F), listOf(F)), type = FLOAT, flags = symmetric()),
    D_MUL(0x3B, ARITHMETIC, st(listOf(D, D), listOf(D)), type = DOUBLE, flags = symmetric()),
    I_DIV(0x3C, ARITHMETIC, st(listOf(I, I), listOf(I)), type = INT),
    L_DIV(0x3D, ARITHMETIC, st(listOf(L, L), listOf(L)), type = LONG),
    F_DIV(0x3E, ARITHMETIC, st(listOf(F, F), listOf(F)), type = FLOAT, flags = pure()),
    D_DIV(0x3F, ARITHMETIC, st(listOf(D, D), listOf(D)), type = DOUBLE, flags = pure()),
    I_REM(0x40, ARITHMETIC, st(listOf(I, I), listOf(I)), type = INT),
    L_REM(0x41, ARITHMETIC, st(listOf(L, L), listOf(L)), type = LONG),
    F_REM(0x42, ARITHMETIC, st(listOf(F, F), listOf(F)), type = FLOAT, flags = pure()),
    D_REM(0x43, ARITHMETIC, st(listOf(D, D), listOf(D)), type = DOUBLE, flags = pure()),
    I_NEG(0x44, ARITHMETIC, st(listOf(I), listOf(I)), type = INT, flags = pure()),
    L_NEG(0x45, ARITHMETIC, st(listOf(L), listOf(L)), type = LONG, flags = pure()),
    F_NEG(0x46, ARITHMETIC, st(listOf(F), listOf(F)), type = FLOAT, flags = pure()),
    D_NEG(0x47, ARITHMETIC, st(listOf(D), listOf(D)), type = DOUBLE, flags = pure()),

    I_AND(0x50, ARITHMETIC, st(listOf(I, I), listOf(I)), type = INT, flags = algebraic()),
    L_AND(0x51, ARITHMETIC, st(listOf(L, L), listOf(L)), type = LONG, flags = algebraic()),
    I_OR(0x52, ARITHMETIC, st(listOf(I, I), listOf(I)), type = INT, flags = algebraic()),
    L_OR(0x53, ARITHMETIC, st(listOf(L, L), listOf(L)), type = LONG, flags = algebraic()),
    I_XOR(0x54, ARITHMETIC, st(listOf(I, I), listOf(I)), type = INT, flags = algebraic()),
    L_XOR(0x55, ARITHMETIC, st(listOf(L, L), listOf(L)), type = LONG, flags = algebraic()),

    I_SHL(0x56, ARITHMETIC, st(listOf(I, I), listOf(I)), type = INT, flags = pure()),
    L_SHL(0x57, ARITHMETIC, st(listOf(L, I), listOf(L)), type = LONG, flags = pure()),
    I_SHR(0x58, ARITHMETIC, st(listOf(I, I), listOf(I)), type = INT, flags = pure()),
    L_SHR(0x59, ARITHMETIC, st(listOf(L, I), listOf(L)), type = LONG, flags = pure()),
    I_USHR(0x5A, ARITHMETIC, st(listOf(I, I), listOf(I)), type = INT, flags = pure()),
    L_USHR(0x5B, ARITHMETIC, st(listOf(L, I), listOf(L)), type = LONG, flags = pure()),

    I2L(0x60, CONVERSION, st(listOf(I), listOf(L)), type = INT, targetType = LONG, flags = pure()),
    I2F(0x61, CONVERSION, st(listOf(I), listOf(F)), type = INT, targetType = FLOAT, flags = pure()),
    I2D(
        0x62,
        CONVERSION,
        st(listOf(I), listOf(D)),
        type = INT,
        targetType = DOUBLE,
        flags = pure()
    ),
    L2I(0x63, CONVERSION, st(listOf(L), listOf(I)), type = LONG, targetType = INT, flags = pure()),
    L2F(
        0x64,
        CONVERSION,
        st(listOf(L), listOf(F)),
        type = LONG,
        targetType = FLOAT,
        flags = pure()
    ),
    L2D(
        0x65,
        CONVERSION,
        st(listOf(L), listOf(D)),
        type = LONG,
        targetType = DOUBLE,
        flags = pure()
    ),
    F2I(0x66, CONVERSION, st(listOf(F), listOf(I)), type = FLOAT, targetType = INT, flags = pure()),
    F2L(
        0x67,
        CONVERSION,
        st(listOf(F), listOf(L)),
        type = FLOAT,
        targetType = LONG,
        flags = pure()
    ),
    F2D(
        0x68,
        CONVERSION,
        st(listOf(F), listOf(D)),
        type = FLOAT,
        targetType = DOUBLE,
        flags = pure()
    ),
    D2I(
        0x69,
        CONVERSION,
        st(listOf(D), listOf(I)),
        type = DOUBLE,
        targetType = INT,
        flags = pure()
    ),
    D2L(
        0x6A,
        CONVERSION,
        st(listOf(D), listOf(L)),
        type = DOUBLE,
        targetType = LONG,
        flags = pure()
    ),
    D2F(
        0x6B,
        CONVERSION,
        st(listOf(D), listOf(F)),
        type = DOUBLE,
        targetType = FLOAT,
        flags = pure()
    ),
    I2B(0x6C, CONVERSION, st(listOf(I), listOf(I)), type = INT, targetType = BYTE, flags = pure()),
    I2C(0x6D, CONVERSION, st(listOf(I), listOf(I)), type = INT, targetType = CHAR, flags = pure()),
    I2S(0x6E, CONVERSION, st(listOf(I), listOf(I)), type = INT, targetType = SHORT, flags = pure()),

    L_CMP(0x70, COMPARISON, st(listOf(L, L), listOf(I)), type = LONG, flags = pure()),

    F_CMPL(0x71, COMPARISON, st(listOf(F, F), listOf(I)), type = FLOAT, flags = pure()),

    F_CMPG(0x72, COMPARISON, st(listOf(F, F), listOf(I)), type = FLOAT, flags = pure()),
    D_CMPL(0x73, COMPARISON, st(listOf(D, D), listOf(I)), type = DOUBLE, flags = pure()),
    D_CMPG(0x74, COMPARISON, st(listOf(D, D), listOf(I)), type = DOUBLE, flags = pure()),

    IF_EQ(0x80, BRANCH, st(listOf(I), emptyList()), ops(BRANCH_OFFSET), CONDITIONAL_JUMP, INT),
    IF_NE(0x81, BRANCH, st(listOf(I), emptyList()), ops(BRANCH_OFFSET), CONDITIONAL_JUMP, INT),
    IF_LT(0x82, BRANCH, st(listOf(I), emptyList()), ops(BRANCH_OFFSET), CONDITIONAL_JUMP, INT),
    IF_GE(0x83, BRANCH, st(listOf(I), emptyList()), ops(BRANCH_OFFSET), CONDITIONAL_JUMP, INT),
    IF_GT(0x84, BRANCH, st(listOf(I), emptyList()), ops(BRANCH_OFFSET), CONDITIONAL_JUMP, INT),
    IF_LE(0x85, BRANCH, st(listOf(I), emptyList()), ops(BRANCH_OFFSET), CONDITIONAL_JUMP, INT),
    IF_ICMP_EQ(
        0x86,
        BRANCH,
        st(listOf(I, I), emptyList()),
        ops(BRANCH_OFFSET),
        CONDITIONAL_JUMP,
        INT
    ),
    IF_ICMP_NE(
        0x87,
        BRANCH,
        st(listOf(I, I), emptyList()),
        ops(BRANCH_OFFSET),
        CONDITIONAL_JUMP,
        INT
    ),
    IF_ICMP_LT(
        0x88,
        BRANCH,
        st(listOf(I, I), emptyList()),
        ops(BRANCH_OFFSET),
        CONDITIONAL_JUMP,
        INT
    ),
    IF_ICMP_GE(
        0x89,
        BRANCH,
        st(listOf(I, I), emptyList()),
        ops(BRANCH_OFFSET),
        CONDITIONAL_JUMP,
        INT
    ),
    IF_ICMP_GT(
        0x8A,
        BRANCH,
        st(listOf(I, I), emptyList()),
        ops(BRANCH_OFFSET),
        CONDITIONAL_JUMP,
        INT
    ),
    IF_ICMP_LE(
        0x8B,
        BRANCH,
        st(listOf(I, I), emptyList()),
        ops(BRANCH_OFFSET),
        CONDITIONAL_JUMP,
        INT
    ),

    IF_RCMP_EQ(
        0x8C,
        BRANCH,
        st(listOf(R, R), emptyList()),
        ops(BRANCH_OFFSET),
        CONDITIONAL_JUMP,
        REFERENCE
    ),
    IF_RCMP_NE(
        0x8D,
        BRANCH,
        st(listOf(R, R), emptyList()),
        ops(BRANCH_OFFSET),
        CONDITIONAL_JUMP,
        REFERENCE
    ),
    IF_NULL(
        0x8E,
        BRANCH,
        st(listOf(R), emptyList()),
        ops(BRANCH_OFFSET),
        CONDITIONAL_JUMP,
        REFERENCE
    ),
    IF_NONNULL(
        0x8F,
        BRANCH,
        st(listOf(R), emptyList()),
        ops(BRANCH_OFFSET),
        CONDITIONAL_JUMP,
        REFERENCE
    ),
    GOTO(0x90, BRANCH, shuffle(0, 0), ops(BRANCH_OFFSET), JUMP),

    RETURN_VOID(0x98, METHOD_RETURN, shuffle(0, 0), flow = RETURN_EXIT, type = VOID),
    RETURN_I(0x99, METHOD_RETURN, st(listOf(I), emptyList()), flow = RETURN_EXIT, type = INT),
    RETURN_L(0x9A, METHOD_RETURN, st(listOf(L), emptyList()), flow = RETURN_EXIT, type = LONG),
    RETURN_F(0x9B, METHOD_RETURN, st(listOf(F), emptyList()), flow = RETURN_EXIT, type = FLOAT),
    RETURN_D(0x9C, METHOD_RETURN, st(listOf(D), emptyList()), flow = RETURN_EXIT, type = DOUBLE),
    RETURN_A(0x9D, METHOD_RETURN, st(listOf(R), emptyList()), flow = RETURN_EXIT, type = REFERENCE),

    INVOKE(0xA0, RccOpcodeKind.INVOKE, RccStackEffect.FromCallSite, ops(FUNCTION_ID)),

    HOST_INVOKE(0xA1, RccOpcodeKind.INVOKE, RccStackEffect.FromCallSite, ops(CP_INDEX)),

    HOST_LAMBDA(0xA2, RccOpcodeKind.INVOKE, RccStackEffect.FromCallSite, ops(FUNCTION_ID)),

    BOX(0xB0, COERCE, RccStackEffect.FromCallSite, ops(TYPE_TAG)),

    UNBOX(0xB1, COERCE, RccStackEffect.FromCallSite, ops(TYPE_TAG)),

    ;

    val mnemonic: String = name.lowercase()

    val size: Int = 1 + operands.sumOf { it.size }

    val isPure: Boolean get() = PURE in flags
    val isCommutative: Boolean get() = COMMUTATIVE in flags
    val isAssociative: Boolean get() = ASSOCIATIVE in flags
    val isConditionalBranch: Boolean get() = flow == CONDITIONAL_JUMP
    val isUnconditionalJump: Boolean get() = flow == JUMP

    val endsBlock: Boolean get() = flow != NEXT

    val negated: RccOpcodeTable?
        get() = when (this) {
            IF_EQ -> IF_NE; IF_NE -> IF_EQ
            IF_LT -> IF_GE; IF_GE -> IF_LT
            IF_GT -> IF_LE; IF_LE -> IF_GT
            IF_ICMP_EQ -> IF_ICMP_NE; IF_ICMP_NE -> IF_ICMP_EQ
            IF_ICMP_LT -> IF_ICMP_GE; IF_ICMP_GE -> IF_ICMP_LT
            IF_ICMP_GT -> IF_ICMP_LE; IF_ICMP_LE -> IF_ICMP_GT
            IF_RCMP_EQ -> IF_RCMP_NE; IF_RCMP_NE -> IF_RCMP_EQ
            IF_NULL -> IF_NONNULL; IF_NONNULL -> IF_NULL
            else -> null
        }

    val condition: RccCondition?
        get() = when (this) {
            IF_EQ, IF_ICMP_EQ, IF_RCMP_EQ, IF_NULL -> EQ
            IF_NE, IF_ICMP_NE, IF_RCMP_NE, IF_NONNULL -> NE
            IF_LT, IF_ICMP_LT -> LT
            IF_GE, IF_ICMP_GE -> GE
            IF_GT, IF_ICMP_GT -> GT
            IF_LE, IF_ICMP_LE -> LE
            else -> null
        }

    companion object {
        private val byCode: Map<RccOpcode, RccOpcodeTable> = entries.associateBy { it.code }
        private val byMnemonic: Map<String, RccOpcodeTable> = entries.associateBy { it.mnemonic }

        private val loads = mapOf(
            INT to I_LOAD,
            LONG to L_LOAD,
            FLOAT to F_LOAD,
            DOUBLE to D_LOAD,
            REFERENCE to A_LOAD
        )
        private val stores = mapOf(
            INT to I_STORE,
            LONG to L_STORE,
            FLOAT to F_STORE,
            DOUBLE to D_STORE,
            REFERENCE to A_STORE
        )
        private val returns = mapOf(
            INT to RETURN_I,
            LONG to RETURN_L,
            FLOAT to RETURN_F,
            DOUBLE to RETURN_D,
            REFERENCE to RETURN_A,
        )

        private val arithmeticTable: Map<RccArithmeticOp, Array<RccOpcodeTable?>> = mapOf(
            RccArithmeticOp.ADD to arrayOf(I_ADD, L_ADD, F_ADD, D_ADD),
            RccArithmeticOp.SUB to arrayOf(I_SUB, L_SUB, F_SUB, D_SUB),
            RccArithmeticOp.MUL to arrayOf(I_MUL, L_MUL, F_MUL, D_MUL),
            RccArithmeticOp.DIV to arrayOf(I_DIV, L_DIV, F_DIV, D_DIV),
            RccArithmeticOp.REM to arrayOf(I_REM, L_REM, F_REM, D_REM),
            RccArithmeticOp.NEG to arrayOf(I_NEG, L_NEG, F_NEG, D_NEG),
            RccArithmeticOp.AND to arrayOf(I_AND, L_AND, null, null),
            RccArithmeticOp.OR to arrayOf(I_OR, L_OR, null, null),
            RccArithmeticOp.XOR to arrayOf(I_XOR, L_XOR, null, null),
            RccArithmeticOp.SHL to arrayOf(I_SHL, L_SHL, null, null),
            RccArithmeticOp.SHR to arrayOf(I_SHR, L_SHR, null, null),
            RccArithmeticOp.USHR to arrayOf(I_USHR, L_USHR, null, null),
        )

        init {
            validate()
        }

        fun of(code: RccOpcode): RccOpcodeTable =
            byCode[code] ?: throw IllegalArgumentException("Нет опкода с кодом $code")

        fun ofMnemonic(mnemonic: String): RccOpcodeTable? = byMnemonic[mnemonic.lowercase()]

        fun load(type: RccTypeTable): RccOpcodeTable = loads.getValue(family(type))
        fun store(type: RccTypeTable): RccOpcodeTable = stores.getValue(family(type))

        fun returnOf(type: RccTypeTable): RccOpcodeTable =
            if (type == VOID) RETURN_VOID else returns.getValue(family(type))

        fun arithmetic(op: RccArithmeticOp, type: RccTypeTable): RccOpcodeTable {
            require(type.isNumeric) { "$op не определена для $type" }
            return arithmeticTable.getValue(op)[arithmeticColumn(type)]
                ?: throw IllegalArgumentException("$op не определена для $type")
        }

        fun compareAndBranch(
            type: RccTypeTable,
            condition: RccCondition,
            jumpIf: Boolean = true
        ): List<RccOpcodeTable> {
            val c = if (jumpIf) condition else condition.negate()
            return when (family(type)) {
                INT -> listOf(intCompareBranch(c))
                LONG -> listOf(L_CMP, zeroCompareBranch(c))
                FLOAT, DOUBLE -> {
                    val nanIsGreater = condition == LT || condition == LE
                    val cmp = if (family(type) == FLOAT) {
                        if (nanIsGreater) F_CMPG else F_CMPL
                    } else {
                        if (nanIsGreater) D_CMPG else D_CMPL
                    }
                    listOf(cmp, zeroCompareBranch(c))
                }

                REFERENCE -> {
                    require(c == EQ || c == NE) { "Ссылки сравниваются только на == и !=" }
                    listOf(if (c == EQ) IF_RCMP_EQ else IF_RCMP_NE)
                }

                else -> throw IllegalArgumentException("$type нельзя сравнивать")
            }
        }

        fun nullCheckBranch(jumpIfNull: Boolean): RccOpcodeTable =
            if (jumpIfNull) IF_NULL else IF_NONNULL

        private fun zeroCompareBranch(c: RccCondition): RccOpcodeTable = when (c) {
            EQ -> IF_EQ; NE -> IF_NE; LT -> IF_LT; GE -> IF_GE; GT -> IF_GT; LE -> IF_LE
        }

        private fun intCompareBranch(c: RccCondition): RccOpcodeTable = when (c) {
            EQ -> IF_ICMP_EQ; NE -> IF_ICMP_NE; LT -> IF_ICMP_LT
            GE -> IF_ICMP_GE; GT -> IF_ICMP_GT; LE -> IF_ICMP_LE
        }

        private fun family(type: RccTypeTable): RccTypeTable = when (type) {
            BOOLEAN, BYTE, CHAR, SHORT, INT -> INT
            LONG -> LONG
            FLOAT -> FLOAT
            DOUBLE -> DOUBLE
            REFERENCE, REFERENCE_EXTERN -> REFERENCE
            VOID -> throw IllegalArgumentException("У VOID нет значения")
        }

        private fun arithmeticColumn(type: RccTypeTable): Int = when (family(type)) {
            INT -> 0; LONG -> 1; FLOAT -> 2; DOUBLE -> 3
            else -> throw IllegalArgumentException("$type не числовой")
        }

        private fun validate() {
            for (op in entries) {
                val n = op.name
                val jumps = op.flow == CONDITIONAL_JUMP || op.flow == JUMP
                check(op.operands.count { it.isBranchOffset } == (if (jumps) 1 else 0)) {
                    "$n: смещение перехода не согласовано с flow"
                }
                check(ASSOCIATIVE !in op.flags || (COMMUTATIVE in op.flags && (op.type == INT || op.type == LONG))) {
                    "$n: ассоциативность допустима только для целочисленных операций"
                }
                check((op.kind == CONVERSION) == (op.targetType != null)) { "$n: targetType только у преобразований" }
                if (op.flow == CONDITIONAL_JUMP) {
                    val neg = checkNotNull(op.negated) { "$n: у условного перехода нет отрицания" }
                    check(neg.negated == op && neg.stack == op.stack) { "$n: отрицание не инволюция" }
                    check(op.condition != null) { "$n: нет условия" }
                }
            }
        }
    }
}
