package com.hua.rainbow_parentheses.scope

import com.hua.rainbow_parentheses.RainbowParenthesesSettings
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiDocumentManager
import com.intellij.util.concurrency.AppExecutorUtil
import java.util.concurrent.Callable

/**
 * 当前块随光标持续高亮：光标移动时自动把所在代码块淡染，无需手动触发。
 *
 * 复用 [ScopeResolver]：括号优先 → 跨行 PSI 块 → 无块时回退到光标所在行（顶层单行条目也有反应）。
 * 解析在后台 ReadAction 完成并按 editor 合并，快速移动光标时只跑最后一次。
 *
 * @author Hua
 */
object CurrentBlockHighlighter {

    private val HIGHLIGHTER_KEY = Key.create<RangeHighlighter>("RAINBOW_CURRENT_BLOCK_HIGHLIGHTER")
    private const val BLEND_WEIGHT = 0.10f

    fun clear(editor: Editor) = ScopeHighlightPainter.clear(editor, HIGHLIGHTER_KEY)

    /** 根据当前光标重算并刷新当前块高亮。关闭 / 不适用时清除已有高亮。 */
    fun refresh(editor: Editor) {
        if (editor.isDisposed) return

        val settings = RainbowParenthesesSettings.getInstance()
        val project = editor.project
        val document = editor.document
        if (!settings.enabled || !settings.enableCurrentBlockHighlight || project == null) {
            ScopeHighlightPainter.clear(editor, HIGHLIGHTER_KEY)
            return
        }
        if (settings.doNotRainbowifyBigFiles && document.lineCount > settings.bigFilesLineThreshold) {
            ScopeHighlightPainter.clear(editor, HIGHLIGHTER_KEY)
            return
        }

        val caret = editor.caretModel.offset
        val modalityState = ModalityState.stateForComponent(editor.component)

        ReadAction.nonBlocking(Callable<ScopeResolver.Scope?> {
            val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(document)
            val syntaxHighlighter = psiFile?.let {
                SyntaxHighlighterFactory.getSyntaxHighlighter(it.language, project, it.virtualFile)
            }
            ScopeResolver.resolve(psiFile, document, caret, syntaxHighlighter)
        })
            .expireWhen { editor.isDisposed }
            .coalesceBy(CurrentBlockHighlighter, editor)
            .finishOnUiThread(modalityState) { scope ->
                if (editor.isDisposed) return@finishOnUiThread
                if (scope == null) {
                    ScopeHighlightPainter.clear(editor, HIGHLIGHTER_KEY)
                    return@finishOnUiThread
                }
                val existing = ScopeHighlightPainter.current(editor, HIGHLIGHTER_KEY)
                if (existing == null ||
                    existing.startOffset != scope.startOffset ||
                    existing.endOffset != scope.endOffset
                ) {
                    ScopeHighlightPainter.apply(editor, scope, HIGHLIGHTER_KEY, BLEND_WEIGHT)
                }
            }
            .submit(AppExecutorUtil.getAppExecutorService())
    }
}
