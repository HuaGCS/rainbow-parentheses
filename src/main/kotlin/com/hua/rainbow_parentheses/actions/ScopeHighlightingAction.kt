package com.hua.rainbow_parentheses.actions

import com.hua.rainbow_parentheses.RainbowParenthesesSettings
import com.hua.rainbow_parentheses.scope.ScopeHighlightPainter
import com.hua.rainbow_parentheses.scope.ScopeResolver
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiDocumentManager
import com.intellij.util.concurrency.AppExecutorUtil
import java.awt.Point
import java.awt.event.MouseEvent
import java.util.concurrent.Callable
import javax.swing.SwingUtilities

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

    private fun clearPreviousHighlight(editor: Editor) =
        ScopeHighlightPainter.clear(editor, SCOPE_HIGHLIGHTER_KEY)

    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val project = e.project ?: editor.project

        if (!RainbowParenthesesSettings.getInstance().enableScopeHighlighting) {
            clearPreviousHighlight(editor)
            return
        }

        // 鼠标快捷键（Ctrl+右键）不会移动光标，必须用实际点击位置；否则回退到光标。
        val caret = offsetAtClickOrCaret(e, editor)
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
                applyOrToggleScope(editor, scope, textLength)
            }
            .submit(AppExecutorUtil.getAppExecutorService())
    }

    /**
     * 应用或切换作用域高亮：
     * - 本次解析出的作用域与当前正显示的完全相同 → 关闭高亮（再次右键即取消）。
     * - 不同作用域 → 切换到新的；解析为空 → 清除当前高亮。
     */
    private fun applyOrToggleScope(editor: Editor, scope: ScopeResolver.Scope?, @Suppress("UNUSED_PARAMETER") textLength: Int) {
        if (editor.isDisposed) return

        val existing = ScopeHighlightPainter.current(editor, SCOPE_HIGHLIGHTER_KEY)
        // 同一作用域再次触发 -> 关闭高亮（toggle off）
        if (scope != null && existing != null &&
            existing.startOffset == scope.startOffset && existing.endOffset == scope.endOffset
        ) {
            clearPreviousHighlight(editor)
            return
        }

        clearPreviousHighlight(editor)
        if (scope != null) ScopeHighlightPainter.apply(editor, scope, SCOPE_HIGHLIGHTER_KEY, BLEND_WEIGHT)
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible =
            e.getData(CommonDataKeys.EDITOR) != null &&
            RainbowParenthesesSettings.getInstance().enableScopeHighlighting
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    /**
     * 鼠标触发时取点击处偏移（右键不移动光标），否则用当前光标偏移。
     *
     * [MouseEvent.point] 是相对于事件源组件的，未必是编辑器内容组件；直接喂给
     * [Editor.xyToLogicalPosition] 会按错误原点解析导致行错位。故经屏幕坐标换算到
     * 编辑器内容组件（其坐标系即文档坐标，已含滚动），再求逻辑位置。
     */
    private fun offsetAtClickOrCaret(e: AnActionEvent, editor: Editor): Int {
        val mouse = e.inputEvent as? MouseEvent ?: return editor.caretModel.offset
        val onScreen = runCatching { mouse.locationOnScreen }.getOrNull() ?: return editor.caretModel.offset
        val point = Point(onScreen)
        SwingUtilities.convertPointFromScreen(point, editor.contentComponent)
        val logicalPosition = editor.xyToLogicalPosition(point)
        return editor.logicalPositionToOffset(logicalPosition)
    }
}
