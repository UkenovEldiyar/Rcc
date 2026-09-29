package com.ukenoveldiyar.rcc.external.types.parser

import com.ukenoveldiyar.rcc.external.declarations.ExternalAnnotation
import com.ukenoveldiyar.rcc.external.names.FqName
import com.ukenoveldiyar.rcc.external.source.SourceElement
import com.ukenoveldiyar.rcc.external.types.ExternalFunctionType
import com.ukenoveldiyar.rcc.external.types.ExternalType
import com.ukenoveldiyar.rcc.external.types.ExternalTypeRef

class ExternalTypeRefParser(
    private val text: String,
    private val sourceElement: SourceElement,
) {
    private val tokens = Tokenizer(text).tokenize()
    private var position = 0

    fun parse(): ExternalTypeRef {
        val type = parseTypeRef()
        expectEnd()
        return type
    }

    private fun parseTypeRef(): ExternalTypeRef {
        val annotations = parseAnnotations()

        if (peek() is Token.LParen) {
            return parseFunctionType(receiver = null, annotations)
        }

        return parseNamedTypeOrFunctionType(annotations)
    }

    private fun parseNamedTypeOrFunctionType(annotations: List<ExternalAnnotation>): ExternalTypeRef {
        val fqName = parseDottedName()

        if (peek() is Token.Dot) {
            advance()
            val receiver = ExternalType(fqName, sourceElement)
            return parseFunctionType(receiver, annotations)
        }

        if (annotations.isNotEmpty()) {
            error("annotations are only supported on function types: \"$text\"")
        }

        return ExternalType(fqName, sourceElement)
    }

    private fun parseFunctionType(
        receiver: ExternalTypeRef?,
        annotations: List<ExternalAnnotation>,
    ): ExternalFunctionType {
        val parameters = parseParameterList()
        expect<Token.Arrow>("->")
        val returnType = parseTypeRef()

        return ExternalFunctionType(receiver, parameters, returnType, annotations, sourceElement)
    }

    private fun parseParameterList(): List<ExternalTypeRef> {
        expect<Token.LParen>("(")

        val parameters = mutableListOf<ExternalTypeRef>()
        if (peek() !is Token.RParen) {
            parameters += parseTypeRef()
            while (peek() is Token.Comma) {
                advance()
                parameters += parseTypeRef()
            }
        }

        expect<Token.RParen>(")")
        return parameters
    }

    private fun parseAnnotations(): List<ExternalAnnotation> {
        val annotations = mutableListOf<ExternalAnnotation>()
        while (peek() is Token.At) {
            advance()
            annotations += ExternalAnnotation(parseDottedName(), sourceElement)
        }
        return annotations
    }

    private fun parseDottedName(): FqName {
        val segments = mutableListOf(expectIdent())
        while (peek() is Token.Dot && peekAt(1) is Token.Ident) {
            advance()
            segments += expectIdent()
        }
        return FqName(segments.joinToString("."))
    }

    private fun expectIdent(): String = expect<Token.Ident>("identifier").value

    private inline fun <reified T : Token> expect(expected: String): T {
        val token = peek()
        if (token !is T) error("expected $expected but was \"${token.text}\" in \"$text\"")
        advance()
        return token
    }

    private fun expectEnd() {
        if (peek() !is Token.End) error("unexpected trailing input \"${peek().text}\" in \"$text\"")
    }

    private fun peek(): Token = tokens[position]

    private fun peekAt(offset: Int): Token = tokens.getOrElse(position + offset) { Token.End() }

    private fun advance() {
        position++
    }

    private sealed class Token(val text: String) {
        class Ident(val value: String) : Token(value)
        class At : Token("@")
        class Dot : Token(".")
        class Comma : Token(",")
        class LParen : Token("(")
        class RParen : Token(")")
        class Arrow : Token("->")
        class End : Token("<end>")
    }

    private class Tokenizer(private val text: String) {
        private var index = 0

        fun tokenize(): List<Token> {
            val tokens = mutableListOf<Token>()
            while (true) {
                skipWhitespace()
                if (index >= text.length) break
                tokens += nextToken()
            }
            tokens += Token.End()
            return tokens
        }

        private fun skipWhitespace() {
            while (index < text.length && text[index].isWhitespace()) index++
        }

        private fun nextToken(): Token = when (val c = text[index]) {
            '@' -> Token.At().also { index++ }
            '.' -> Token.Dot().also { index++ }
            ',' -> Token.Comma().also { index++ }
            '(' -> Token.LParen().also { index++ }
            ')' -> Token.RParen().also { index++ }
            '-' -> {
                if (index + 1 < text.length && text[index + 1] == '>') {
                    Token.Arrow().also { index += 2 }
                } else {
                    error("unexpected character '$c' at $index in \"$text\"")
                }
            }

            else -> when {
                c.isLetter() || c == '_' -> readIdent()
                else -> error("unexpected character '$c' at $index in \"$text\"")
            }
        }

        private fun readIdent(): Token.Ident {
            val start = index
            while (index < text.length && (text[index].isLetterOrDigit() || text[index] == '_')) index++
            return Token.Ident(text.substring(start, index))
        }
    }
}
