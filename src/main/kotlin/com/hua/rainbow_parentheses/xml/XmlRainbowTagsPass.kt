package com.hua.rainbow_parentheses.xml

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
import com.intellij.psi.PsiFile
import com.intellij.psi.XmlRecursiveElementVisitor
import com.intellij.psi.xml.XmlTag
import com.intellij.psi.xml.XmlToken
import com.intellij.xml.util.XmlTagUtil

/**
 * XML / HTML 标签名按嵌套深度着色 pass。
 *
 * 遍历 [XmlTag]，按嵌套深度给开标签名与闭标签名上色（HTML 的 `HtmlTag` 即 `XmlTag`，一并覆盖）。
 * 与彩虹括号一致，用 markup 直接着色，图层抬到语法高亮之上，确保颜色生效。
 *
 * @author Hua
 */
class XmlRainbowTagsPass(
    private val file: PsiFile,
    private val editor: Editor
) : TextEditorHighlightingPass(file.project, editor.document, false) {

    private data class TagHighlight(
        val startOffset: Int,
        val endOffset: Int,
        val colorKey: TextAttributesKey
    )

    private var highlights: List<TagHighlight> = emptyList()

    override fun doCollectInformation(progress: ProgressIndicator) {
        val settings = RainbowParenthesesSettings.getInstance()
        if (!settings.enabled || !settings.enableRainbowTags) {
            highlights = emptyList()
            return
        }

        val result = ArrayList<TagHighlight>()
        file.accept(object : XmlRecursiveElementVisitor() {
            private var depth = 0

            override fun visitXmlTag(tag: XmlTag) {
                val colorKey = RainbowColorsManager.getTagColorKey(depth)
                addToken(XmlTagUtil.getStartTagNameElement(tag), colorKey, result)
                addToken(XmlTagUtil.getEndTagNameElement(tag), colorKey, result)

                depth++
                super.visitXmlTag(tag)
                depth--
            }
        })
        highlights = result
    }

    private fun addToken(token: XmlToken?, colorKey: TextAttributesKey, out: MutableList<TagHighlight>) {
        if (token == null) return
        val range = token.textRange
        out.add(TagHighlight(range.startOffset, range.endOffset, colorKey))
    }

    override fun doApplyInformationToEditor() {
        val markupModel = editor.markupModel

        editor.getUserData(TAG_HIGHLIGHTERS_KEY)?.let { old ->
            old.forEach { markupModel.removeHighlighter(it) }
            old.clear()
        }

        if (highlights.isEmpty()) {
            editor.putUserData(TAG_HIGHLIGHTERS_KEY, null)
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
        editor.putUserData(TAG_HIGHLIGHTERS_KEY, newHighlighters)
    }

    private companion object {
        const val LAYER = HighlighterLayer.ADDITIONAL_SYNTAX + 1
        val TAG_HIGHLIGHTERS_KEY =
            Key.create<MutableList<RangeHighlighter>>("RAINBOW_XML_TAG_HIGHLIGHTERS")
    }
}
