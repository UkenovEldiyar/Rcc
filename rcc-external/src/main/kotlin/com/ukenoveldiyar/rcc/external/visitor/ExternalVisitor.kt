package com.ukenoveldiyar.rcc.external.visitor

import com.ukenoveldiyar.rcc.external.ExternalElement
import com.ukenoveldiyar.rcc.external.declarations.ExternalAnnotation
import com.ukenoveldiyar.rcc.external.declarations.ExternalClass
import com.ukenoveldiyar.rcc.external.declarations.ExternalConstructor
import com.ukenoveldiyar.rcc.external.declarations.ExternalDeclaration
import com.ukenoveldiyar.rcc.external.declarations.ExternalFile
import com.ukenoveldiyar.rcc.external.declarations.ExternalFunction
import com.ukenoveldiyar.rcc.external.declarations.ExternalProperty
import com.ukenoveldiyar.rcc.external.types.ExternalTypeRef

abstract class ExternalVisitor<out R, in D> {
    abstract fun visitElement(element: ExternalElement, data: D): R

    open fun visitDeclaration(declaration: ExternalDeclaration, data: D) =
        visitElement(declaration, data)

    open fun visitFile(declaration: ExternalFile, data: D) =
        visitDeclaration(declaration, data)

    open fun visitFunction(declaration: ExternalFunction, data: D) =
        visitDeclaration(declaration, data)

    open fun visitInterface(declaration: ExternalClass, data: D) =
        visitDeclaration(declaration, data)

    open fun visitProperty(declaration: ExternalProperty, data: D) =
        visitDeclaration(declaration, data)

    open fun visitConstructor(declaration: ExternalConstructor, data: D) =
        visitDeclaration(declaration, data)

    open fun visitAnnotation(expression: ExternalAnnotation, data: D) =
        visitElement(expression, data)

    open fun visitTypeRef(typeRef: ExternalTypeRef, data: D) =
        visitElement(typeRef, data)
}