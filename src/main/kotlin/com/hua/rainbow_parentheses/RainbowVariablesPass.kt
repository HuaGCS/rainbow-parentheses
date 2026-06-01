package com.hua.rainbow_parentheses

import com.intellij.codeHighlighting.TextEditorHighlightingPass
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiFile

/**
 * 变量彩虹高亮 pass（词法版）：扫描标识符 token，按名字哈希给同名标识符统一着色。
 *
 * 这是"先词法、后按语言精修"路线的第一步：语言无关、即时生效，但不感知作用域，
 * 类型名 / 方法名也会一并着色。默认关闭，由设置项 [RainbowParenthesesSettings.enableRainbowVariables] 控制。
 *
 * @author Hua
 */
class RainbowVariablesPass(
    private val file: PsiFile,
    private val editor: Editor
) : TextEditorHighlightingPass(file.project, editor.document, false) {

    private data class VariableHighlight(
        val startOffset: Int,
        val endOffset: Int,
        val colorKey: TextAttributesKey
    )

    private var highlights: List<VariableHighlight> = emptyList()

    companion object {
        private val VARIABLE_HIGHLIGHTERS_KEY =
            Key.create<MutableList<RangeHighlighter>>("RAINBOW_VARIABLE_HIGHLIGHTERS")
    }

    override fun doCollectInformation(progress: ProgressIndicator) {
        val settings = RainbowParenthesesSettings.getInstance()
        if (!settings.enabled || !settings.enableRainbowVariables) {
            highlights = emptyList()
            return
        }

        val syntaxHighlighter = SyntaxHighlighterFactory.getSyntaxHighlighter(
            file.language, file.project, file.virtualFile
        )
        if (syntaxHighlighter == null) {
            highlights = emptyList()
            return
        }

        val identifiers = IdentifierScanner.collectIdentifiers(
            editor.document.immutableCharSequence, 0, syntaxHighlighter
        )
        highlights = identifiers.map { id ->
            VariableHighlight(id.startOffset, id.endOffset, RainbowColorsManager.getVariableColorKey(id.name))
        }
    }

    override fun doApplyInformationToEditor() {
        val markupModel = editor.markupModel

        // 清除上一次本 pass 添加的高亮
        editor.getUserData(VARIABLE_HIGHLIGHTERS_KEY)?.let { old ->
            old.forEach { markupModel.removeHighlighter(it) }
            old.clear()
        }

        if (highlights.isEmpty()) {
            editor.putUserData(VARIABLE_HIGHLIGHTERS_KEY, null)
            return
        }

        val textLength = editor.document.textLength
        val newHighlighters = ArrayList<RangeHighlighter>(highlights.size)
        for (h in highlights) {
            if (h.startOffset < 0 || h.endOffset > textLength || h.startOffset >= h.endOffset) continue
            val highlighter = markupModel.addRangeHighlighter(
                h.colorKey,
                h.startOffset,
                h.endOffset,
                HighlighterLayer.ADDITIONAL_SYNTAX,
                HighlighterTargetArea.EXACT_RANGE
            )
            newHighlighters.add(highlighter)
        }

        editor.putUserData(VARIABLE_HIGHLIGHTERS_KEY, newHighlighters)
    }
}
