package com.hua.rainbow_parentheses.actions

import com.hua.rainbow_parentheses.ParenthesesMatcher
import com.hua.rainbow_parentheses.RainbowColorsManager
import com.hua.rainbow_parentheses.RainbowParenthesesSettings
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
 * 高亮光标所在括号作用域：找到包含光标的最内层括号对，
 * 给从开括号到闭括号的整个范围加一层背景高亮。
 *
 * 全文档括号扫描在后台线程的 ReadAction 中完成，避免阻塞 EDT；
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

        // 后台扫描全文档括号、定位最内层括号对
        ReadAction.nonBlocking(Callable<ParenthesesMatcher.ParenthesesPair?> {
            val syntaxHighlighter = project?.let {
                PsiDocumentManager.getInstance(it).getPsiFile(document)?.let { psiFile ->
                    SyntaxHighlighterFactory.getSyntaxHighlighter(psiFile.language, it, psiFile.virtualFile)
                }
            }
            val allPairs = if (syntaxHighlighter != null) {
                ParenthesesMatcher.findMatchingBrackets(document.immutableCharSequence, 0, syntaxHighlighter)
            } else {
                ParenthesesMatcher.findMatchingBrackets(document, 0, textLength)
            }
            allPairs
                .filter { pair ->
                    pair.openRange.startOffset <= caret && caret <= pair.closeRange.endOffset
                }
                .minByOrNull { pair ->
                    pair.closeRange.endOffset - pair.openRange.startOffset
                }
        })
            .expireWhen { editor.isDisposed }
            .finishOnUiThread(modalityState) { innermost ->
                if (innermost != null) {
                    applyScopeHighlight(editor, innermost, textLength)
                }
            }
            .submit(AppExecutorUtil.getAppExecutorService())
    }

    private fun applyScopeHighlight(
        editor: Editor,
        pair: ParenthesesMatcher.ParenthesesPair,
        textLength: Int
    ) {
        if (editor.isDisposed) return

        val openStart = pair.openRange.startOffset
        val closeEnd = pair.closeRange.endOffset
        if (openStart < 0 || closeEnd > textLength || openStart >= closeEnd) return

        val colorKey = RainbowColorsManager.getColorKey(pair.type, pair.level)
        val fg = editor.colorsScheme.getAttributes(colorKey)?.foregroundColor ?: return
        val bg = editor.colorsScheme.defaultBackground
        val attributes = TextAttributes().apply {
            backgroundColor = blendColors(bg, fg, BLEND_WEIGHT)
        }

        // 在 await 期间用户可能已再点别处触发了新的清理；为防重叠，重做一次清理
        clearPreviousHighlight(editor)

        val highlighter = editor.markupModel.addRangeHighlighter(
            openStart,
            closeEnd,
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
