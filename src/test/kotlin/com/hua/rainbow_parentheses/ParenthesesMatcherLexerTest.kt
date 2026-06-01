package com.hua.rainbow_parentheses

import com.intellij.lexer.LexerBase
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.psi.tree.IElementType
import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * 验证基于词法分析器的括号匹配：字符串 / 注释里的括号被跳过，
 * 多字符运算符（`<=`、`->` 等）不会被误当成括号。
 *
 * 使用一个玩具词法分析器精确控制 token 边界与类型，从而无需依赖具体语言插件。
 *
 * @author Hua
 */
class ParenthesesMatcherLexerTest : BasePlatformTestCase() {

    private val highlighter = ToySyntaxHighlighter()

    fun testStringContentIsSkipped() {
        val code = "f(\"(x)\")"
        val pairs = ParenthesesMatcher.findMatchingBrackets(code, 0, highlighter)

        // 仅 f(...) 的一对圆括号，字符串 "(x)" 内的括号被跳过
        assertEquals(1, pairs.size)
        assertEquals(ParenthesesType.ROUND, pairs[0].type)
        assertEquals(1, pairs[0].openRange.startOffset)
        assertEquals(code.length - 1, pairs[0].closeRange.startOffset)
    }

    fun testMultiCharOperatorsAreNotBrackets() {
        // `<=` 与 `->` 是双字符 token，其中的 `<`/`>` 不应被当成尖括号
        val code = "(a<=b)"
        val pairs = ParenthesesMatcher.findMatchingBrackets(code, 0, highlighter)

        assertEquals(1, pairs.size)
        assertEquals(ParenthesesType.ROUND, pairs[0].type)
        assertTrue(pairs.none { it.type == ParenthesesType.ANGLE })
    }

    fun testStandaloneAngleBracketsMatch() {
        val code = "<x>"
        val pairs = ParenthesesMatcher.findMatchingBrackets(code, 0, highlighter)

        assertEquals(1, pairs.size)
        assertEquals(ParenthesesType.ANGLE, pairs[0].type)
    }

    fun testBaseOffsetIsApplied() {
        val code = "()"
        val pairs = ParenthesesMatcher.findMatchingBrackets(code, 100, highlighter)

        assertEquals(1, pairs.size)
        assertEquals(100, pairs[0].openRange.startOffset)
        assertEquals(101, pairs[0].closeRange.startOffset)
    }

    // -----------------------------------------------------------------
    // 玩具词法分析器：双引号区段为 STRING token；预定义双字符运算符为单个
    // token；其余按单字符切分。
    // -----------------------------------------------------------------

    private class ToySyntaxHighlighter : SyntaxHighlighterBase() {
        override fun getHighlightingLexer() = ToyLexer()

        override fun getTokenHighlights(tokenType: IElementType?): Array<TextAttributesKey> =
            if (tokenType === STRING) pack(DefaultLanguageHighlighterColors.STRING)
            else TextAttributesKey.EMPTY_ARRAY
    }

    private class ToyLexer : LexerBase() {
        private lateinit var buffer: CharSequence
        private var endOffset = 0
        private var tokenStart = 0
        private var tokenEnd = 0
        private var tokenType: IElementType? = null

        override fun start(buffer: CharSequence, startOffset: Int, endOffset: Int, initialState: Int) {
            this.buffer = buffer
            this.endOffset = endOffset
            this.tokenStart = startOffset
            this.tokenEnd = startOffset
            advance()
        }

        override fun getState(): Int = 0
        override fun getTokenType(): IElementType? = tokenType
        override fun getTokenStart(): Int = tokenStart
        override fun getTokenEnd(): Int = tokenEnd
        override fun getBufferSequence(): CharSequence = buffer
        override fun getBufferEnd(): Int = endOffset

        override fun advance() {
            tokenStart = tokenEnd
            if (tokenStart >= endOffset) {
                tokenType = null
                return
            }
            val ch = buffer[tokenStart]
            when {
                ch == '"' -> {
                    var i = tokenStart + 1
                    while (i < endOffset && buffer[i] != '"') i++
                    if (i < endOffset) i++ // 含收尾引号
                    tokenEnd = i
                    tokenType = STRING
                }
                tokenStart + 1 < endOffset &&
                    TWO_CHAR_OPS.contains("" + ch + buffer[tokenStart + 1]) -> {
                    tokenEnd = tokenStart + 2
                    tokenType = OPERATOR
                }
                else -> {
                    tokenEnd = tokenStart + 1
                    tokenType = OTHER
                }
            }
        }
    }

    private companion object {
        val STRING = IElementType("TOY_STRING", null)
        val OPERATOR = IElementType("TOY_OPERATOR", null)
        val OTHER = IElementType("TOY_OTHER", null)
        val TWO_CHAR_OPS = setOf("<=", ">=", "->", "=>", "<<", ">>", "==", "!=")
    }
}
