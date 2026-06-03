package com.hua.rainbow_parentheses.actions

import com.hua.rainbow_parentheses.RainbowParenthesesSettings
import com.hua.rainbow_parentheses.scope.ScopeResolver
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiDocumentManager
import com.intellij.util.concurrency.AppExecutorUtil
import java.awt.Color
import java.util.concurrent.Callable

/**
 * 高亮光标所在作用域：
 * - 有括号的语言：包含光标的最内层括号对，给开括号到闭括号的整个范围加底色（沿用原行为）。
 * - 无括号包裹时（Python / YAML 等缩进结构，或括号外的代码）：回退到包含光标的最内层 PSI 代码块。
 *
 * 作用域解析在后台线程的 ReadAction 中完成（见 [ScopeResolver]），避免阻塞 EDT；
 * 找到结果后回到 EDT 写入 markup。
 *
 * @author Hua
 * @since 2026/5/22
 */
class ScopeHighlightingAction : AnAction() {

    companion object {
        private val SCOPE_HIGHLIGHTER_KEY = Key.create<RangeHighlighter>("RAINBOW_SCOPE_HIGHLIGHTER")
        private const val BLEND_WEIGHT = 0.12f
    }

    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val project = e.project ?: editor.project

        // 立即清除上一次的作用域高亮（EDT 上 markup 操作）
        clearPreviousHighlight(editor)

        if (!RainbowParenthesesSettings.getInstance().enableScopeHighlighting) return

        val caret = editor.caretModel.offset
        val document = editor.document
        val textLength = document.textLength
        val modalityState = ModalityState.stateForComponent(editor.component)

        ReadAction.nonBlocking(Callable<ScopeResolver.Scope?> {
            val psiFile = project?.let { PsiDocumentManager.getInstance(it).getPsiFile(document) }
            val syntaxHighlighter = psiFile?.let {
                SyntaxHighlighterFactory.getSyntaxHighlighter(it.language, project, it.virtualFile)
            }
            ScopeResolver.resolve(psiFile, document, caret, syntaxHighlighter)
        })
            .expireWhen { editor.isDisposed }
            .finishOnUiThread(modalityState) { scope ->
                if (scope != null) {
                    applyScopeHighlight(editor, scope, textLength)
                }
            }
            .submit(AppExecutorUtil.getAppExecutorService())
    }

    private fun applyScopeHighlight(editor: Editor, scope: ScopeResolver.Scope, textLength: Int) {
        if (editor.isDisposed) return

        val start = scope.startOffset
        val end = scope.endOffset
        if (start < 0 || end > textLength || start >= end) return

        val fg = editor.colorsScheme.getAttributes(scope.colorKey)?.foregroundColor ?: return
        val bg = editor.colorsScheme.defaultBackground
        val attributes = TextAttributes().apply {
            backgroundColor = blendColors(bg, fg, BLEND_WEIGHT)
        }

        // 在 await 期间用户可能已再点别处触发了新的清理；为防重叠，重做一次清理
        clearPreviousHighlight(editor)

        val highlighter = editor.markupModel.addRangeHighlighter(
            start,
            end,
            HighlighterLayer.SELECTION - 1,
            attributes,
            HighlighterTargetArea.EXACT_RANGE
        )
        editor.putUserData(SCOPE_HIGHLIGHTER_KEY, highlighter)
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible =
            e.getData(CommonDataKeys.EDITOR) != null &&
            RainbowParenthesesSettings.getInstance().enableScopeHighlighting
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    private fun clearPreviousHighlight(editor: Editor) {
        val old = editor.getUserData(SCOPE_HIGHLIGHTER_KEY) ?: return
        editor.markupModel.removeHighlighter(old)
        editor.putUserData(SCOPE_HIGHLIGHTER_KEY, null)
    }

    private fun blendColors(bg: Color, fg: Color, weight: Float): Color {
        val invWeight = 1f - weight
        return Color(
            (bg.red * invWeight + fg.red * weight).toInt().coerceIn(0, 255),
            (bg.green * invWeight + fg.green * weight).toInt().coerceIn(0, 255),
            (bg.blue * invWeight + fg.blue * weight).toInt().coerceIn(0, 255)
        )
    }
}
