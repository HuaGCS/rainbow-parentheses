package com.hua.rainbow_parentheses

import com.intellij.codeHighlighting.TextEditorHighlightingPass
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key

/**
 * 彩虹括号高亮 pass：扫描文档括号对，按嵌套层级给每个括号着色。
 *
 * 早期实现基于 [com.intellij.codeInsight.daemon.impl.HighlightVisitor]，
 * 但在 `analyze()` 阶段（`visit()` 回调之外）加入的 HighlightInfo 不会被平台收割，
 * 故改为 [TextEditorHighlightingPass] + markup model 直接着色。
 *
 * @author Hua
 */
class RainbowBracketsPass(
    project: Project,
    private val editor: Editor
) : TextEditorHighlightingPass(project, editor.document, false) {

    private data class BracketHighlight(
        val startOffset: Int,
        val endOffset: Int,
        val type: ParenthesesType,
        val level: Int
    )

    private var highlights: List<BracketHighlight> = emptyList()

    companion object {
        private val BRACKET_HIGHLIGHTERS_KEY =
            Key.create<MutableList<RangeHighlighter>>("RAINBOW_BRACKET_HIGHLIGHTERS")
    }

    override fun doCollectInformation(progress: ProgressIndicator) {
        val settings = RainbowParenthesesSettings.getInstance()
        if (!settings.enabled) {
            highlights = emptyList()
            return
        }

        val document = editor.document
        val pairs = ParenthesesMatcher.findMatchingBrackets(document, 0, document.textLength)

        val result = ArrayList<BracketHighlight>(pairs.size * 2)
        for (pair in pairs) {
            if (!settings.isEnabledForBracketType(pair.type)) continue
            result.add(
                BracketHighlight(pair.openRange.startOffset, pair.openRange.endOffset, pair.type, pair.level)
            )
            result.add(
                BracketHighlight(pair.closeRange.startOffset, pair.closeRange.endOffset, pair.type, pair.level)
            )
        }
        highlights = result
    }

    override fun doApplyInformationToEditor() {
        val markupModel = editor.markupModel

        // 清除上一次本 pass 添加的高亮
        editor.getUserData(BRACKET_HIGHLIGHTERS_KEY)?.let { old ->
            old.forEach { markupModel.removeHighlighter(it) }
            old.clear()
        }

        if (highlights.isEmpty()) {
            editor.putUserData(BRACKET_HIGHLIGHTERS_KEY, null)
            return
        }

        val textLength = editor.document.textLength
        val newHighlighters = ArrayList<RangeHighlighter>(highlights.size)
        for (h in highlights) {
            if (h.startOffset < 0 || h.endOffset > textLength || h.startOffset >= h.endOffset) continue
            val colorKey = RainbowColorsManager.getColorKey(h.type, h.level)
            val highlighter = markupModel.addRangeHighlighter(
                colorKey,
                h.startOffset,
                h.endOffset,
                HighlighterLayer.ADDITIONAL_SYNTAX,
                HighlighterTargetArea.EXACT_RANGE
            )
            newHighlighters.add(highlighter)
        }

        editor.putUserData(BRACKET_HIGHLIGHTERS_KEY, newHighlighters)
    }
}
