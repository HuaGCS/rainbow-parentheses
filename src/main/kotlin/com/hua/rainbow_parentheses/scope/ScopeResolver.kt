package com.hua.rainbow_parentheses.scope

import com.hua.rainbow_parentheses.ParenthesesMatcher
import com.hua.rainbow_parentheses.RainbowColorsManager
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiWhiteSpace

/**
 * 解析「光标所在作用域」的纯逻辑（不触碰编辑器 / markup，便于在 ReadAction 内调用并单测）。
 *
 * 策略：括号优先 —— 取包含光标的最内层括号对；无括号包裹时回退到 PSI，
 * 取包含光标、跨多行的最内层复合元素（代码块 / suite / mapping）。
 *
 * @author Hua
 */
object ScopeResolver {

    /** 作用域结果：一段范围 + 取前景色用的颜色 key。 */
    data class Scope(
        val startOffset: Int,
        val endOffset: Int,
        val colorKey: TextAttributesKey
    )

    fun resolve(
        psiFile: PsiFile?,
        document: Document,
        caret: Int,
        syntaxHighlighter: SyntaxHighlighter?
    ): Scope? =
        findBracketScope(document, caret, syntaxHighlighter)
            ?: psiFile?.let { findPsiBlockScope(it, document, caret) }

    /** 括号作用域：包含光标的最内层括号对，按其层级取色。 */
    private fun findBracketScope(
        document: Document,
        caret: Int,
        syntaxHighlighter: SyntaxHighlighter?
    ): Scope? {
        val allPairs = if (syntaxHighlighter != null) {
            ParenthesesMatcher.findMatchingBrackets(document.immutableCharSequence, 0, syntaxHighlighter)
        } else {
            ParenthesesMatcher.findMatchingBrackets(document, 0, document.textLength)
        }
        val pair = allPairs
            .filter { it.openRange.startOffset <= caret && caret <= it.closeRange.endOffset }
            .minByOrNull { it.closeRange.endOffset - it.openRange.startOffset }
            ?: return null

        return Scope(
            pair.openRange.startOffset,
            pair.closeRange.endOffset,
            RainbowColorsManager.getColorKey(pair.type, pair.level)
        )
    }

    /**
     * PSI 块作用域回退：从光标处叶子向上找包含光标、且跨多行的最内层复合元素，
     * 颜色按其上方跨行祖先数量（嵌套深度）取缩进色，与彩虹缩进线的块语义一致。
     */
    private fun findPsiBlockScope(psiFile: PsiFile, document: Document, caret: Int): Scope? {
        val leaf = psiFile.findElementAt(caret)
            ?: psiFile.findElementAt((caret - 1).coerceAtLeast(0))
            ?: return null

        var innermost: PsiElement? = null
        var depth = -1
        var cur: PsiElement? = leaf
        while (cur != null && cur !is PsiFile) {
            val range = cur.textRange
            if (cur !is PsiWhiteSpace &&
                range != null &&
                range.startOffset <= caret && caret <= range.endOffset &&
                spansMultipleLines(document, range.startOffset, range.endOffset)
            ) {
                if (innermost == null) innermost = cur
                depth++
            }
            cur = cur.parent
        }

        val block = innermost ?: return null
        val range = block.textRange
        return Scope(
            range.startOffset,
            range.endOffset,
            RainbowColorsManager.getIndentColorKey(depth.coerceAtLeast(0))
        )
    }

    private fun spansMultipleLines(document: Document, startOffset: Int, endOffset: Int): Boolean {
        if (startOffset < 0 || endOffset > document.textLength || startOffset >= endOffset) return false
        return document.getLineNumber(startOffset) != document.getLineNumber(endOffset - 1)
    }
}
