package com.hua.rainbow_parentheses

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.fileTypes.SyntaxHighlighter

/**
 * 基于语言词法分析器扫描标识符 token（变量 / 字段 / 方法 / 类型名等），供变量彩虹着色使用。
 *
 * 词法层面无法区分"变量"与"类型 / 方法名"，因此这里采集所有标识符 token，由调用方按名字
 * 哈希取色——同名同色。关键字 / 字符串 / 注释 / 数字等通过高亮 key 过滤掉。
 *
 * @author Hua
 */
object IdentifierScanner {

    data class IdentifierToken(val startOffset: Int, val endOffset: Int, val name: String)

    private val SKIP_KEY_NAMES: Set<String> = hashSetOf(
        DefaultLanguageHighlighterColors.KEYWORD.externalName,
        DefaultLanguageHighlighterColors.STRING.externalName,
        DefaultLanguageHighlighterColors.LINE_COMMENT.externalName,
        DefaultLanguageHighlighterColors.BLOCK_COMMENT.externalName,
        DefaultLanguageHighlighterColors.DOC_COMMENT.externalName,
        DefaultLanguageHighlighterColors.DOC_COMMENT_MARKUP.externalName,
        DefaultLanguageHighlighterColors.NUMBER.externalName,
        DefaultLanguageHighlighterColors.METADATA.externalName,
        DefaultLanguageHighlighterColors.VALID_STRING_ESCAPE.externalName,
        DefaultLanguageHighlighterColors.INVALID_STRING_ESCAPE.externalName
    )

    /**
     * 扫描 [text] 中的标识符 token。[baseOffset] 是 [text] 在文档中的起始偏移。
     */
    fun collectIdentifiers(
        text: CharSequence,
        baseOffset: Int,
        syntaxHighlighter: SyntaxHighlighter
    ): List<IdentifierToken> {
        val result = ArrayList<IdentifierToken>()
        val lexer = syntaxHighlighter.highlightingLexer
        lexer.start(text, 0, text.length, 0)

        while (true) {
            val tokenType = lexer.tokenType ?: break
            val start = lexer.tokenStart
            val end = lexer.tokenEnd

            if (end > start &&
                isIdentifier(text, start, end) &&
                !SyntaxTokenClassifier.hasAnyHighlightKey(syntaxHighlighter, tokenType, SKIP_KEY_NAMES)
            ) {
                result.add(
                    IdentifierToken(baseOffset + start, baseOffset + end, text.subSequence(start, end).toString())
                )
            }
            lexer.advance()
        }
        return result
    }

    /** token 文本是否是一个标识符：首字符为字母 / `_` / `$`，其余为字母数字 / `_` / `$`。 */
    private fun isIdentifier(text: CharSequence, start: Int, end: Int): Boolean {
        val first = text[start]
        if (!(first.isLetter() || first == '_' || first == '$')) return false
        for (i in start + 1 until end) {
            val c = text[i]
            if (!(c.isLetterOrDigit() || c == '_' || c == '$')) return false
        }
        return true
    }
}
