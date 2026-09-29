package com.ukenoveldiiyar.rcc.compiler.plugin.ir.jvm

fun topLevelFqName(ownerFqName: String, methodName: String): String {
    val lastSegment = ownerFqName.substringAfterLast('.')
    val looksLikeFileFacade = lastSegment.length > 2 && lastSegment.endsWith("Kt") &&
        lastSegment.first().isUpperCase()
    val packageName = if (looksLikeFileFacade) ownerFqName.substringBeforeLast('.', missingDelimiterValue = "")
    else ownerFqName
    return if (packageName.isEmpty()) methodName else "$packageName.$methodName"
}

fun declFqName(isStatic: Boolean, ownerInternalName: String, name: String): String {
    val ownerFqName = ownerInternalName.toKotlinFqName()
    return if (isStatic) topLevelFqName(ownerFqName, name) else "$ownerFqName.$name"
}
