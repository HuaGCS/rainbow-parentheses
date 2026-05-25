package com.hua.rainbow_parentheses

import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.TextRange
import java.util.*

/**
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

    // 🔧 修复：统一方法签名，支持两种调用方式
    fun findMatchingBrackets(document: Document, range: TextRange): List<ParenthesesPair> {
        return findMatchingBrackets(document, range.startOffset, range.endOffset)
    }

    fun findMatchingBrackets(document: Document, startOffset: Int, endOffset: Int): List<ParenthesesPair> {
        val text = document.getText(TextRange(startOffset, endOffset))
        val pairs = mutableListOf<ParenthesesPair>()
        val stack = Stack<ParenthesesInfo>()

        // 单次遍历：在扫描括号的同时增量维护字符串/注释状态，
        // 避免原先对每个字符都从头重扫一遍（O(n²) -> O(n)）。
        var inString = false
        var inComment = false
        var quote = ' '

        for (i in text.indices) {
            val ch = text[i]
            val nextCh = text.getOrNull(i + 1) ?: ' '

            // 此处的状态反映的是 [0, i) 区间，可直接用于判断位置 i 是否在字符串/注释中。
            if (!inString && !inComment) {
                val type = ParenthesesType.fromChar(ch)
                if (type != null) {
                    val absoluteOffset = startOffset + i

                    when {
                        type.isOpen(ch) -> {
                            stack.push(ParenthesesInfo(type, absoluteOffset, stack.size))
                        }
                        type.isClose(ch) && stack.isNotEmpty() -> {
                            // 从栈中找到匹配的开括号
                            val openBracketIndex = stack.indexOfLast { it.type == type }
                            if (openBracketIndex >= 0) {
                                val openBracket = stack.removeAt(openBracketIndex)
                                pairs.add(
                                    ParenthesesPair(
                                        TextRange(openBracket.offset, openBracket.offset + 1),
                                        TextRange(absoluteOffset, absoluteOffset + 1),
                                        openBracket.level,
                                        type
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // 根据当前字符推进字符串/注释状态。
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

        // 按开括号位置排序，使返回结果与文档顺序一致（而非闭合顺序）。
        return pairs.sortedBy { it.openRange.startOffset }
    }
}
