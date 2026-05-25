package com.hua.rainbow_parentheses.indents

import com.hua.rainbow_parentheses.RainbowColorsManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.markup.CustomHighlighterRenderer
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.ui.paint.LinePainter2D
import com.intellij.util.text.CharArrayUtil
import java.awt.Color
import java.awt.Graphics
import java.awt.Graphics2D

/**
 * 彩虹缩进引导线渲染器。
 *
 * 几何逻辑（起止行定位、向上跳过空行、折叠处理、`start.y += lineHeight`、`maxY = end.y`）
 * 派生自 JetBrains IntelliJ Community Edition 中的
 * `com.intellij.codeInsight.daemon.impl.IndentGuideRenderer`，使用 Apache License 2.0。
 *   原文件：https://github.com/JetBrains/intellij-community/blob/master/platform/lang-impl/src/com/intellij/codeInsight/daemon/impl/IndentGuideRenderer.java
 *   许可证：https://www.apache.org/licenses/LICENSE-2.0
 *
 * 本渲染器被直接挂到平台原生缩进线的 [RangeHighlighter] 上，因此与原生缩进线的位置、
 * 长度完全一致；区别仅是按缩进层级着不同颜色，并把颜色与编辑器背景混合后淡显。
 *
 * @author Hua
 */
class RainbowIndentGuideRenderer(private val indentSize: Int) : CustomHighlighterRenderer {

    override fun paint(editor: Editor, highlighter: RangeHighlighter, g: Graphics) {
        val document = editor.document
        val startOffset = highlighter.startOffset
        if (startOffset >= document.textLength) return
        val endOffset = highlighter.endOffset

        val chars = document.charsSequence

        // 从高亮起始行向上跳过纯空白行，定位到引导线所属的非空行首个非空白字符。
        var off = startOffset
        var line = document.getLineNumber(startOffset)
        do {
            val lineStart = document.getLineStartOffset(line)
            val lineEnd = document.getLineEndOffset(line)
            off = CharArrayUtil.shiftForward(chars, lineStart, lineEnd, " \t")
            line--
        } while (line > 1 && off < document.textLength && chars[off] == '\n')

        val startPosition = editor.offsetToVisualPosition(off)
        val indentColumn = startPosition.column
        if (indentColumn <= 0) return

        if (editor.foldingModel.isOffsetCollapsed(off)) return

        val lineHeight = editor.lineHeight
        val start = editor.visualPositionToXY(startPosition)
        start.y += lineHeight

        val endPosition = editor.offsetToVisualPosition(endOffset)
        var maxY = editor.visualPositionToXY(endPosition).y
        if (endPosition.line == editor.offsetToVisualPosition(document.textLength).line) {
            maxY += lineHeight
        }
        if (start.y >= maxY) return

        val level = (indentColumn / indentSize.coerceAtLeast(1) - 1).coerceAtLeast(0)
        val colorKey = RainbowColorsManager.getIndentColorKey(level)
        val baseColor = editor.colorsScheme.getAttributes(colorKey)?.foregroundColor
            ?: colorKey.defaultAttributes?.foregroundColor
            ?: return

        // 光标所在块的引导线（缩进列等于光标行缩进、且竖直范围覆盖光标行）用全色高亮，
        // 其余引导线与背景混合后淡显。
        g.color = if (isCaretGuide(editor, document, chars, highlighter, indentColumn)) {
            baseColor
        } else {
            blendWithBackground(baseColor, editor.colorsScheme.defaultBackground)
        }
        val x = start.x.toDouble()
        LinePainter2D.paint(g as Graphics2D, x, start.y.toDouble(), x, (maxY - 1).toDouble())
    }

    /**
     * 判断该引导线是否为光标当前所在块的引导线：
     * 竖直范围覆盖光标所在行，且其缩进列等于光标行自身的缩进列（即最内层那条）。
     */
    private fun isCaretGuide(
        editor: Editor,
        document: com.intellij.openapi.editor.Document,
        chars: CharSequence,
        highlighter: RangeHighlighter,
        indentColumn: Int
    ): Boolean {
        val caretLine = editor.caretModel.logicalPosition.line
        if (caretLine >= document.lineCount) return false

        val guideStartLine = document.getLineNumber(highlighter.startOffset)
        val guideEndLine = document.getLineNumber(highlighter.endOffset)
        if (caretLine !in guideStartLine..guideEndLine) return false

        val lineStart = document.getLineStartOffset(caretLine)
        val lineEnd = document.getLineEndOffset(caretLine)
        val firstNonWs = CharArrayUtil.shiftForward(chars, lineStart, lineEnd, " \t")
        if (firstNonWs >= lineEnd) return false // 光标位于空行
        val caretLineIndentColumn = editor.offsetToVisualPosition(firstNonWs).column
        return indentColumn == caretLineIndentColumn
    }

    private fun blendWithBackground(color: Color, background: Color): Color {
        val w = COLOR_WEIGHT
        return Color(
            (background.red * (1 - w) + color.red * w).toInt().coerceIn(0, 255),
            (background.green * (1 - w) + color.green * w).toInt().coerceIn(0, 255),
            (background.blue * (1 - w) + color.blue * w).toInt().coerceIn(0, 255)
        )
    }

    private companion object {
        /** 引导线保留的原色比例，其余向编辑器背景靠拢，使竖线只是淡淡的一条。 */
        const val COLOR_WEIGHT = 0.22f
    }
}
