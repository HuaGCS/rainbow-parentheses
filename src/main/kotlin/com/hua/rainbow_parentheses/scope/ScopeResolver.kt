package com.hua.rainbow_parentheses.scope

import com.hua.rainbow_parentheses.ParenthesesMatcher
import com.hua.rainbow_parentheses.ParenthesesType
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

    /**
     * 括号优先（不含尖括号）；无括号包裹时回退到包含光标的最内层跨行 PSI 块；再无块则回退到光标所在行。
     */
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
            // 尖括号 `<>` 不作为作用域括号：泛型 / 比较运算符 / XML 自闭合标签都会让其错配（如
            // `<ref .../>` 的 `<` 因闭合是 `/>` 双字符 token 而与后面无关的 `>` 配对）。
            .filter { it.type != ParenthesesType.ANGLE }
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
     * PSI 块作用域回退：收集包含光标、跨多行、且不覆盖整篇文件的复合元素，取其中**范围最小**的一个。
     *
     * 同时从 `caret` 与 `caret-1` 两处探针向上遍历——点击常落在某块的结束边界（值在行尾，如 YAML
     * 的 `https: 443`，偏移正好等于该块 endOffset）：此时 `findElementAt(caret)` 命中的是块之后、
     * 不含光标的兄弟元素，必须靠 `caret-1` 探针才能找回真正的所在块。
     *
     * 颜色按所选块上方的跨行祖先数量（嵌套深度）取缩进色，与彩虹缩进线的块语义一致。
     */
    private fun findPsiBlockScope(psiFile: PsiFile, document: Document, caret: Int): Scope? {
        val probes = LinkedHashSet<PsiElement>()
        psiFile.findElementAt(caret)?.let { probes.add(it) }
        psiFile.findElementAt((caret - 1).coerceAtLeast(0))?.let { probes.add(it) }
        if (probes.isEmpty()) return null

        val candidates = LinkedHashSet<PsiElement>()
        for (probe in probes) {
            var cur: PsiElement? = probe
            while (cur != null && cur !is PsiFile) {
                val range = cur.textRange
                if (cur !is PsiWhiteSpace &&
                    range != null &&
                    range.startOffset <= caret && caret <= range.endOffset &&
                    spansMultipleLines(document, range.startOffset, range.endOffset) &&
                    !coversWholeFile(document, range.startOffset, range.endOffset) &&
                    !isBlankRange(document, range.startOffset, range.endOffset)
                ) {
                    candidates.add(cur)
                }
                cur = cur.parent
            }
        }

        val block = candidates.minByOrNull { it.textRange.endOffset - it.textRange.startOffset }
        if (block != null) {
            val range = block.textRange
            return Scope(
                range.startOffset,
                range.endOffset,
                RainbowColorsManager.getIndentColorKey(countMultiLineAncestors(block, document))
            )
        }

        // 回退：没有可用子块（只剩整篇文件）时，高亮光标所在行本身——顶层单行条目也有反应。
        return lineScope(document, caret, probes)
    }

    /** 范围内是否全为空白（用于跳过 XML 标签间只含换行/缩进的 XmlText 节点）。 */
    private fun isBlankRange(document: Document, startOffset: Int, endOffset: Int): Boolean {
        val chars = document.immutableCharSequence
        val s = startOffset.coerceIn(0, document.textLength)
        val e = endOffset.coerceIn(0, document.textLength)
        for (i in s until e) if (!chars[i].isWhitespace()) return false
        return true
    }

    /** 高亮光标所在行的内容（跳过行首缩进）；空行返回 null。 */
    private fun lineScope(document: Document, caret: Int, probes: Set<PsiElement>): Scope? {
        if (document.textLength == 0) return null
        val line = document.getLineNumber(caret.coerceIn(0, document.textLength - 1))
        val lineEnd = document.getLineEndOffset(line)
        val chars = document.immutableCharSequence
        var start = document.getLineStartOffset(line)
        while (start < lineEnd && chars[start].isWhitespace()) start++
        if (start >= lineEnd) return null

        val depth = probes.firstOrNull()?.let { countMultiLineAncestors(it, document) } ?: 0
        return Scope(start, lineEnd, RainbowColorsManager.getIndentColorKey(depth))
    }

    /** 统计 [element] 到根之间跨多行的祖先数量，用作嵌套深度（决定缩进色档位）。 */
    private fun countMultiLineAncestors(element: PsiElement, document: Document): Int {
        var depth = 0
        var cur: PsiElement? = element.parent
        while (cur != null && cur !is PsiFile) {
            val range = cur.textRange
            if (range != null && spansMultipleLines(document, range.startOffset, range.endOffset)) depth++
            cur = cur.parent
        }
        return depth
    }

    /** 某范围是否从文件首个文本行延伸到末个文本行（即“占满整篇”）。 */
    private fun coversWholeFile(document: Document, startOffset: Int, endOffset: Int): Boolean {
        if (document.textLength == 0) return true
        val lastLine = document.getLineNumber((document.textLength - 1).coerceAtLeast(0))
        val startLine = document.getLineNumber(startOffset.coerceIn(0, document.textLength - 1))
        val endLine = document.getLineNumber((endOffset - 1).coerceIn(0, document.textLength - 1))
        return startLine == 0 && endLine == lastLine
    }

    private fun spansMultipleLines(document: Document, startOffset: Int, endOffset: Int): Boolean {
        if (startOffset < 0 || endOffset > document.textLength || startOffset >= endOffset) return false
        return document.getLineNumber(startOffset) != document.getLineNumber(endOffset - 1)
    }
}
