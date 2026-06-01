package com.hua.rainbow_parentheses

import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.psi.tree.IElementType

/**
 * 借助语言 [SyntaxHighlighter] 给 token 分类的小工具：判断某 token 的高亮 key
 * （含 fallback 链）是否命中给定的一组默认高亮 key 外部名。
 *
 * 用于在词法层面区分字符串 / 注释 / 关键字 / 数字等 token，无需 PSI。
 *
 * @author Hua
 */
internal object SyntaxTokenClassifier {

    fun hasAnyHighlightKey(
        syntaxHighlighter: SyntaxHighlighter,
        tokenType: IElementType,
        keyNames: Set<String>
    ): Boolean {
        for (key in syntaxHighlighter.getTokenHighlights(tokenType)) {
            var k: TextAttributesKey? = key
            while (k != null) {
                if (k.externalName in keyNames) return true
                k = k.fallbackAttributeKey
            }
        }
        return false
    }
}
