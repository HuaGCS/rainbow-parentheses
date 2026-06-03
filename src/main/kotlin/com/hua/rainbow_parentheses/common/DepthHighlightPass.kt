package com.hua.rainbow_parentheses.common

import com.intellij.codeHighlighting.TextEditorHighlightingPass
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile

/**
 * 通用「按嵌套深度着色」着色 pass 基类。
 *
 * 子类只需在 [collectHighlights] 里把要着色的元素与颜色 key 收集出来；
 * 与彩虹括号一致用 markup 直接着色，图层抬到语法高亮之上，确保颜色不被语言语义高亮盖掉。
 *
 * @author Hua
 */
abstract class DepthHighlightPass(
    protected val file: PsiFile,
    protected val editor: Editor,
    private val highlightersKey: Key<MutableList<RangeHighlighter>>
) : TextEditorHighlightingPass(file.project, editor.document, false) {

    protected data class Highlight(
        val startOffset: Int,
        val endOffset: Int,
        val colorKey: TextAttributesKey
    )

    private var highlights: List<Highlight> = emptyList()

    /** 收集本文件中要着色的 (元素范围, 颜色 key)；返回空列表表示本次不着色。 */
    protected abstract fun collectHighlights(): List<Highlight>

    /** 统计 [element] 到根之间类型为 [ancestorClass] 的祖先数量，用作嵌套深度。 */
    protected fun <T : PsiElement> ancestorDepth(element: PsiElement, ancestorClass: Class<T>): Int {
        var depth = 0
        var parent = element.parent
        while (parent != null) {
            if (ancestorClass.isInstance(parent)) depth++
            parent = parent.parent
        }
        return depth
    }

    protected fun highlightFor(element: PsiElement?, colorKey: TextAttributesKey): Highlight? {
        if (element == null) return null
        val range = element.textRange ?: return null
        return Highlight(range.startOffset, range.endOffset, colorKey)
    }

    override fun doCollectInformation(progress: ProgressIndicator) {
        highlights = collectHighlights()
    }

    override fun doApplyInformationToEditor() {
        val markupModel = editor.markupModel

        editor.getUserData(highlightersKey)?.let { old ->
            old.forEach { markupModel.removeHighlighter(it) }
            old.clear()
        }

        if (highlights.isEmpty()) {
            editor.putUserData(highlightersKey, null)
            return
        }

        val textLength = editor.document.textLength
        val newHighlighters = ArrayList<RangeHighlighter>(highlights.size)
        for (h in highlights) {
            if (h.startOffset < 0 || h.endOffset > textLength || h.startOffset >= h.endOffset) continue
            newHighlighters.add(
                markupModel.addRangeHighlighter(
                    h.colorKey,
                    h.startOffset,
                    h.endOffset,
                    LAYER,
                    HighlighterTargetArea.EXACT_RANGE
                )
            )
        }
        editor.putUserData(highlightersKey, newHighlighters)
    }

    protected companion object {
        const val LAYER = HighlighterLayer.ADDITIONAL_SYNTAX + 1
    }
}
