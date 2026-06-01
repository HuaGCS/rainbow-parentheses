package com.hua.rainbow_parentheses

import com.intellij.lexer.LexerBase
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.psi.tree.IElementType
import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * 验证标识符扫描与颜色生成器：关键字 / 数字 / 字符串被跳过，标识符被采集且同名同色。
 *
 * @author Hua
 */
class IdentifierScannerTest : BasePlatformTestCase() {

    private val highlighter = ToySyntaxHighlighter()

    fun testIdentifiersCollectedKeywordsAndLiteralsSkipped() {
        val code = "val count = count + 10"
        val ids = IdentifierScanner.collectIdentifiers(code, 0, highlighter)

        // 仅两个 count，val(关键字) 与 10(数字) 被排除
        assertEquals(2, ids.size)
        assertTrue(ids.all { it.name == "count" })
        val first = code.indexOf("count")
        assertEquals(first, ids[0].startOffset)
        assertEquals(first + 5, ids[0].endOffset)
    }

    fun testStringContentSkipped() {
        val code = "name = \"hello world\""
        val ids = IdentifierScanner.collectIdentifiers(code, 0, highlighter)

        assertEquals(1, ids.size)
        assertEquals("name", ids[0].name)
    }

    fun testBaseOffsetApplied() {
        val ids = IdentifierScanner.collectIdentifiers("foo", 50, highlighter)
        assertEquals(1, ids.size)
        assertEquals(50, ids[0].startOffset)
        assertEquals(53, ids[0].endOffset)
    }

    fun testColorGeneratorIsDeterministicAndStable() {
        val keys = RainbowColorsManager.getVariableColorKeys()
        assertEquals(10, keys.size)

        // 同名同色
        assertSame(
            RainbowColorsManager.getVariableColorKey("count"),
            RainbowColorsManager.getVariableColorKey("count")
        )

        // 映射与 floorMod(hashCode, colorCount) 一致
        val expectedIndex = Math.floorMod("count".hashCode(), 10)
        assertEquals(
            "HUA_RAINBOW_VAR_$expectedIndex",
            RainbowColorsManager.getVariableColorKey("count").externalName
        )
    }

    // -----------------------------------------------------------------
    // 玩具词法分析器：按空白切词，单词区分关键字 / 标识符，另含数字与字符串。
    // -----------------------------------------------------------------

    private class ToySyntaxHighlighter : SyntaxHighlighterBase() {
        override fun getHighlightingLexer() = ToyLexer()

        override fun getTokenHighlights(tokenType: IElementType?): Array<TextAttributesKey> = when (tokenType) {
            KEYWORD -> pack(DefaultLanguageHighlighterColors.KEYWORD)
            NUMBER -> pack(DefaultLanguageHighlighterColors.NUMBER)
            STRING -> pack(DefaultLanguageHighlighterColors.STRING)
            else -> TextAttributesKey.EMPTY_ARRAY
        }
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
                ch.isWhitespace() -> {
                    var i = tokenStart
                    while (i < endOffset && buffer[i].isWhitespace()) i++
                    tokenEnd = i
                    tokenType = WHITESPACE
                }
                ch == '"' -> {
                    var i = tokenStart + 1
                    while (i < endOffset && buffer[i] != '"') i++
                    if (i < endOffset) i++
                    tokenEnd = i
                    tokenType = STRING
                }
                ch.isDigit() -> {
                    var i = tokenStart
                    while (i < endOffset && buffer[i].isDigit()) i++
                    tokenEnd = i
                    tokenType = NUMBER
                }
                ch.isLetter() || ch == '_' || ch == '$' -> {
                    var i = tokenStart
                    while (i < endOffset && (buffer[i].isLetterOrDigit() || buffer[i] == '_' || buffer[i] == '$')) i++
                    tokenEnd = i
                    tokenType = if (buffer.subSequence(tokenStart, i).toString() in KEYWORDS) KEYWORD else IDENTIFIER
                }
                else -> {
                    tokenEnd = tokenStart + 1
                    tokenType = OTHER
                }
            }
        }
    }

    private companion object {
        val KEYWORD = IElementType("TOY_KEYWORD", null)
        val IDENTIFIER = IElementType("TOY_IDENTIFIER", null)
        val NUMBER = IElementType("TOY_NUMBER", null)
        val STRING = IElementType("TOY_STRING", null)
        val WHITESPACE = IElementType("TOY_WS", null)
        val OTHER = IElementType("TOY_OTHER", null)
        val KEYWORDS = setOf("val", "var", "if", "else", "fun", "return")
    }
}
