package com.ukenoveldiyar.rcc.dump

import com.ukenoveldiyar.rcc.backend.compiler.annotation.RccEntryPoint

@RccEntryPoint
fun max(a: Int, b: Int): Int {
    return if (a > b) a else b
}

fun box(): String {
    return "OK"
}
