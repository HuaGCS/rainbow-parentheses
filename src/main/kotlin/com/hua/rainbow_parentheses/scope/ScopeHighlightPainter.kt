package com.hua.rainbow_parentheses.scope

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.util.Key
import java.awt.Color

/**
 * 把一个 [ScopeResolver.Scope] 画成编辑器里的一层淡背景（前景色按权重混入默认背景）。
 *
 * 高亮句柄存在 editor 的 user data（以传入的 [Key] 区分不同特性：手动作用域高亮 / 当前块持续高亮），
 * 供切换与清除复用。手动高亮与持续高亮各用各的 key，互不干扰。
 *
 * @author Hua
 */
object ScopeHighlightPainter {

    /** 当前该 key 下仍有效的高亮句柄。 */
    fun current(editor: Editor, key: Key<RangeHighlighter>): RangeHighlighter? =
        editor.getUserData(key)?.takeIf { it.isValid }

    fun clear(editor: Editor, key: Key<RangeHighlighter>) {
        val old = editor.getUserData(key) ?: return
        editor.markupModel.removeHighlighter(old)
        editor.putUserData(key, null)
    }

    /** 画出 [scope]（先清除该 key 下旧的）；范围非法或取不到颜色时返回 null。 */
    fun apply(
        editor: Editor,
        scope: ScopeResolver.Scope,
        key: Key<RangeHighlighter>,
        blendWeight: Float
    ): RangeHighlighter? {
        if (editor.isDisposed) return null

        val start = scope.startOffset
        val end = scope.endOffset
        if (start < 0 || end > editor.document.textLength || start >= end) return null

        val fg = editor.colorsScheme.getAttributes(scope.colorKey)?.foregroundColor ?: return null
        val bg = editor.colorsScheme.defaultBackground
        val attributes = TextAttributes().apply {
            backgroundColor = blend(bg, fg, blendWeight)
        }

        clear(editor, key)
        val highlighter = editor.markupModel.addRangeHighlighter(
            start,
            end,
            HighlighterLayer.SELECTION - 1,
            attributes,
            HighlighterTargetArea.EXACT_RANGE
        )
        editor.putUserData(key, highlighter)
        return highlighter
    }

    private fun blend(bg: Color, fg: Color, weight: Float): Color {
        val invWeight = 1f - weight
        return Color(
            (bg.red * invWeight + fg.red * weight).toInt().coerceIn(0, 255),
            (bg.green * invWeight + fg.green * weight).toInt().coerceIn(0, 255),
            (bg.blue * invWeight + fg.blue * weight).toInt().coerceIn(0, 255)
        )
    }
}
