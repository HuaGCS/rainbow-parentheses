package com.hua.rainbow_parentheses

import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.util.TextRange
import com.intellij.psi.tree.IElementType
import java.util.*

/**
 * 扫描文本中的成对括号，并标注每个括号对的嵌套层级。
 *
 * 首选 [findMatchingBrackets] 的 lexer 版本：借助语言的词法分析器把文本切成 token，
 * 只有"长度为 1 且不属于字符串 / 注释"的 token 才会被当成括号。这样：
 *  - 字符串 `"("`、字符字面量 `'('`、注释里的括号会作为整段 token 被跳过；
 *  - 多字符运算符（`<=` `<<` `->` `=>`）是长度 ≥ 2 的 token，不会被误当成括号。
 *
 * 对没有词法分析器的语言，回退到纯文本扫描（[collectViaTextScan]），并沿用基于字符的
 * 字符串 / 注释启发式过滤。
 *
 * @author Hua
 * @since 2025/9/2 15:30
 */
object ParenthesesMatcher {

    data class ParenthesesPair(
        val openRange: TextRange,
        val closeRange: TextRange,
        val level: Int,
        val type: ParenthesesType
    )

    private data class ParenthesesInfo(
        val type: ParenthesesType,
        val offset: Int,
        val level: Int
    )

    /** 已定位到的单个括号字符及其在文档中的绝对偏移。 */
    private data class Bracket(val offset: Int, val ch: Char)

    // ---------------------------------------------------------------------
    // 公共 API
    // ---------------------------------------------------------------------

    /**
     * 基于词法分析器的括号匹配（首选）。[baseOffset] 是 [text] 在文档中的起始偏移，
     * 用于把 token 的相对位置换算回文档绝对偏移。
     */
    fun findMatchingBrackets(
        text: CharSequence,
        baseOffset: Int,
        syntaxHighlighter: SyntaxHighlighter
    ): List<ParenthesesPair> = buildPairs(collectViaLexer(text, baseOffset, syntaxHighlighter))

    /** 纯文本扫描版本（无词法分析器时的兜底，亦供既有调用方 / 测试使用）。 */
    fun findMatchingBrackets(document: Document, range: TextRange): List<ParenthesesPair> =
        findMatchingBrackets(document, range.startOffset, range.endOffset)

    fun findMatchingBrackets(document: Document, startOffset: Int, endOffset: Int): List<ParenthesesPair> {
        val text = document.getText(TextRange(startOffset, endOffset))
        return buildPairs(collectViaTextScan(text, startOffset))
    }

    // ---------------------------------------------------------------------
    // 内部实现
    // ---------------------------------------------------------------------

    /**
     * 把按文档顺序排列的括号字符序列配对，并标注嵌套层级。
     * 层级为压栈时的栈深度，与历史着色语义保持一致。
     */
    private fun buildPairs(brackets: List<Bracket>): List<ParenthesesPair> {
        val pairs = ArrayList<ParenthesesPair>(brackets.size / 2 + 1)
        val stack = Stack<ParenthesesInfo>()

        for (b in brackets) {
            val type = ParenthesesType.fromChar(b.ch) ?: continue
            when {
                type.isOpen(b.ch) -> stack.push(ParenthesesInfo(type, b.offset, stack.size))
                type.isClose(b.ch) && stack.isNotEmpty() -> {
                    val openBracketIndex = stack.indexOfLast { it.type == type }
                    if (openBracketIndex >= 0) {
                        val openBracket = stack.removeAt(openBracketIndex)
                        pairs.add(
                            ParenthesesPair(
                                TextRange(openBracket.offset, openBracket.offset + 1),
                                TextRange(b.offset, b.offset + 1),
                                openBracket.level,
                                type
                            )
                        )
                    }
                }
            }
        }

        return pairs.sortedBy { it.openRange.startOffset }
    }

    private fun collectViaLexer(
        text: CharSequence,
        baseOffset: Int,
        syntaxHighlighter: SyntaxHighlighter
    ): List<Bracket> {
        val result = ArrayList<Bracket>()
        val lexer = syntaxHighlighter.highlightingLexer
        lexer.start(text, 0, text.length, 0)

        while (true) {
            val tokenType = lexer.tokenType ?: break
            val start = lexer.tokenStart
            val end = lexer.tokenEnd

            // 仅考虑长度为 1 的 token：多字符运算符（<=, <<, ->, =>）天然被排除，
            // 字符串 / 字符 / 注释作为整段 token 长度 > 1，也不会进入这里。
            if (end - start == 1) {
                val ch = text[start]
                if (ParenthesesType.fromChar(ch) != null && !isCommentOrString(syntaxHighlighter, tokenType)) {
                    result.add(Bracket(baseOffset + start, ch))
                }
            }
            lexer.advance()
        }
        return result
    }

    /**
     * 通过 token 的高亮 key（含 fallback 链）判断其是否属于字符串 / 注释。
     * 长度为 1 的括号 token 一般不会落在字符串 / 注释里，此处仅作额外保险，
     * 应对个别按字符逐个切分字符串内容的词法分析器。
     */
    private fun isCommentOrString(syntaxHighlighter: SyntaxHighlighter, tokenType: IElementType): Boolean =
        SyntaxTokenClassifier.hasAnyHighlightKey(syntaxHighlighter, tokenType, SKIP_KEY_NAMES)

    private val SKIP_KEY_NAMES: Set<String> = hashSetOf(
        DefaultLanguageHighlighterColors.STRING.externalName,
        DefaultLanguageHighlighterColors.LINE_COMMENT.externalName,
        DefaultLanguageHighlighterColors.BLOCK_COMMENT.externalName,
        DefaultLanguageHighlighterColors.DOC_COMMENT.externalName,
        DefaultLanguageHighlighterColors.DOC_COMMENT_MARKUP.externalName,
        DefaultLanguageHighlighterColors.VALID_STRING_ESCAPE.externalName,
        DefaultLanguageHighlighterColors.INVALID_STRING_ESCAPE.externalName
    )

    private fun collectViaTextScan(text: CharSequence, baseOffset: Int): List<Bracket> {
        val result = ArrayList<Bracket>()

        // 单次遍历：在扫描括号的同时增量维护字符串 / 注释状态。
        var inString = false
        var inComment = false
        var quote = ' '

        for (i in 0 until text.length) {
            val ch = text[i]
            val nextCh = if (i + 1 < text.length) text[i + 1] else ' '

            // 此处状态反映 [0, i) 区间，可直接用于判断位置 i 是否在字符串 / 注释中。
            if (!inString && !inComment && ParenthesesType.fromChar(ch) != null) {
                result.add(Bracket(baseOffset + i, ch))
            }

            // 根据当前字符推进字符串 / 注释状态。
            when {
                !inString && !inComment -> when {
                    ch == '"' || ch == '\'' -> {
                        inString = true
                        quote = ch
                    }
                    ch == '/' && (nextCh == '/' || nextCh == '*') -> inComment = true
                }
                inString -> {
                    if (ch == quote && (i == 0 || text[i - 1] != '\\')) {
                        inString = false
                    }
                }
                else -> {
                    if (ch == '\n' || (ch == '*' && nextCh == '/')) {
                        inComment = false
                    }
                }
            }
        }
        return result
    }
}