package com.ukenoveldiyar.rcc.runtime.external.declarations

import com.ukenoveldiyar.rcc.runtime.external.names.ClassId

abstract class ExternalClass : ExternalDeclaration() {
    abstract val classId: ClassId
}