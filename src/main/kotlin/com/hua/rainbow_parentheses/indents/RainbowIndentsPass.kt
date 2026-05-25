package com.hua.rainbow_parentheses.indents

import com.hua.rainbow_parentheses.RainbowParenthesesSettings
import com.intellij.application.options.CodeStyle
import com.intellij.codeHighlighting.TextEditorHighlightingPass
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.event.CaretEvent
import com.intellij.openapi.editor.event.CaretListener
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiFile

/**
 * 彩虹缩进线 pass。
 *
 * 不自行计算缩进引导线，而是等平台原生 `IndentsPass` 生成缩进线高亮后（本 pass
 * 注册在 LAST 锚点，晚于原生），把这些高亮的渲染器替换为 [RainbowIndentGuideRenderer]。
 * 这样引导线的范围完全沿用原生计算结果，位置与长度与原生一致，且不会出现两条线。
 *
 * @author Hua
 */
class RainbowIndentsPass(
    project: Project,
    private val editor: Editor,
    private val file: PsiFile
) : TextEditorHighlightingPass(project, editor.document, false) {

    private var indentSize: Int = DEFAULT_INDENT_SIZE

    override fun doCollectInformation(progress: ProgressIndicator) {
        indentSize = try {
            CodeStyle.getIndentSize(file).coerceAtLeast(1)
        } catch (e: Throwable) {
            DEFAULT_INDENT_SIZE
        }
    }

    override fun doApplyInformationToEditor() {
        if (!RainbowParenthesesSettings.getInstance().showIndentGuides) return

        ensureCaretListener()

        val renderer = RainbowIndentGuideRenderer(indentSize)
        for (highlighter in editor.markupModel.allHighlighters) {
            val current = highlighter.customRenderer ?: continue
            if (current is RainbowIndentGuideRenderer) continue
            // 仅替换平台原生缩进线高亮的渲染器
            if (current.javaClass.name == NATIVE_INDENT_RENDERER) {
                highlighter.customRenderer = renderer
            }
        }
    }

    /**
     * 缩进引导线的高亮取决于光标所在块，光标移动时需重绘整个编辑器内容区，
     * 否则引导线中段不会刷新。每个编辑器只注册一次监听器。
     */
    private fun ensureCaretListener() {
        if (editor.getUserData(CARET_LISTENER_KEY) != null) return
        val listener = object : CaretListener {
            override fun caretPositionChanged(event: CaretEvent) {
                editor.contentComponent.repaint()
            }
        }
        editor.caretModel.addCaretListener(listener)
        editor.putUserData(CARET_LISTENER_KEY, listener)
    }

    private companion object {
        const val DEFAULT_INDENT_SIZE = 4
        const val NATIVE_INDENT_RENDERER =
            "com.intellij.codeInsight.daemon.impl.IndentGuideRenderer"
        val CARET_LISTENER_KEY = Key.create<CaretListener>("RAINBOW_INDENT_CARET_LISTENER")
    }
}
