package com.hua.rainbow_parentheses.kotlin

import com.hua.rainbow_parentheses.RainbowColorsManager
import com.hua.rainbow_parentheses.RainbowParenthesesSettings
import com.intellij.codeHighlighting.TextEditorHighlightingPass
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid

/**
 * Kotlin lambda 的 `->` 箭头按 lambda 嵌套深度着色。
 *
 * lambda 花括号已由彩虹括号覆盖；这里只给 `->` 上色，让 lambda 结构更醒目。深度 = 外层 lambda 数，
 * 故嵌套 lambda 的箭头颜色逐层变化。与 Kotlin 变量着色一致，用 markup 把图层抬到语义高亮之上才显色。
 *
 * @author Hua
 */
class KotlinLambdaArrowPass(
    private val file: KtFile,
    private val editor: Editor
) : TextEditorHighlightingPass(file.project, editor.document, false) {

    private data class ArrowHighlight(
        val startOffset: Int,
        val endOffset: Int,
        val colorKey: TextAttributesKey
    )

    private var highlights: List<ArrowHighlight> = emptyList()

    override fun doCollectInformation(progress: ProgressIndicator) {
        val settings = RainbowParenthesesSettings.getInstance()
        if (!settings.enabled || !settings.enableRainbowLambdaArrow) {
            highlights = emptyList()
            return
        }

        val result = ArrayList<ArrowHighlight>()
        file.accept(object : KtTreeVisitorVoid() {
            private var depth = 0

            override fun visitLambdaExpression(expression: KtLambdaExpression) {
                addArrow(expression.functionLiteral.arrow, depth, result)
                depth++
                super.visitLambdaExpression(expression)
                depth--
            }
        })
        highlights = result
    }

    private fun addArrow(arrow: PsiElement?, depth: Int, out: MutableList<ArrowHighlight>) {
        if (arrow == null) return
        val range = arrow.textRange
        out.add(ArrowHighlight(range.startOffset, range.endOffset, RainbowColorsManager.getArrowColorKey(depth)))
    }

    override fun doApplyInformationToEditor() {
        val markupModel = editor.markupModel

        editor.getUserData(ARROW_HIGHLIGHTERS_KEY)?.let { old ->
            old.forEach { markupModel.removeHighlighter(it) }
            old.clear()
        }

        if (highlights.isEmpty()) {
            editor.putUserData(ARROW_HIGHLIGHTERS_KEY, null)
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
        editor.putUserData(ARROW_HIGHLIGHTERS_KEY, newHighlighters)
    }

    private companion object {
        const val LAYER = HighlighterLayer.ADDITIONAL_SYNTAX + 1
        val ARROW_HIGHLIGHTERS_KEY =
            Key.create<MutableList<RangeHighlighter>>("RAINBOW_KOTLIN_LAMBDA_ARROW_HIGHLIGHTERS")
    }
}
