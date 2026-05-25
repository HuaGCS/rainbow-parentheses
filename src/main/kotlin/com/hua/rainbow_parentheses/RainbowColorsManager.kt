package com.hua.rainbow_parentheses

import com.intellij.openapi.editor.colors.TextAttributesKey

/**
 * 管理彩虹括号 / 缩进线使用的 [TextAttributesKey]。
 *
 * 每个 key 的默认色由 `colorSchemes/rainbow-color-default.xml` / `-darcula.xml`
 * 通过 `additionalTextAttributes` 扩展点注入；用户也可在
 * `Settings → Editor → Color Scheme → Rainbow Parentheses` 中覆盖。
 *
 * @author Hua
 * @since 2025/9/2 15:29
 */
object RainbowColorsManager {

    private const val ROUND_PREFIX  = "HUA_RAINBOW_ROUND_"
    private const val SQUARE_PREFIX = "HUA_RAINBOW_SQUARE_"
    private const val CURLY_PREFIX  = "HUA_RAINBOW_CURLY_"
    private const val ANGLE_PREFIX  = "HUA_RAINBOW_ANGLE_"
    private const val INDENT_PREFIX = "HUA_RAINBOW_INDENT_"

    private val prefixMap = mapOf(
        ParenthesesType.ROUND  to ROUND_PREFIX,
        ParenthesesType.SQUARE to SQUARE_PREFIX,
        ParenthesesType.CURLY  to CURLY_PREFIX,
        ParenthesesType.ANGLE  to ANGLE_PREFIX
    )

    private val colorKeys: Map<ParenthesesType, List<TextAttributesKey>> =
        ParenthesesType.entries.associateWith { type ->
            val prefix = prefixMap[type]!!
            (0..9).map { i -> TextAttributesKey.createTextAttributesKey("$prefix$i") }
        }

    private val indentColorKeys: List<TextAttributesKey> =
        (0..9).map { i -> TextAttributesKey.createTextAttributesKey("$INDENT_PREFIX$i") }

    fun initialize() {
        // 初始化逻辑
    }

    private fun colorCount(): Int =
        RainbowParenthesesSettings.getInstance().numberOfColors.coerceIn(1, 10)

    fun getColorKey(type: ParenthesesType, level: Int): TextAttributesKey =
        colorKeys[type]!![level % colorCount()]

    fun getColorKeys(type: ParenthesesType): List<TextAttributesKey> = colorKeys[type]!!

    fun getIndentColorKey(level: Int): TextAttributesKey =
        indentColorKeys[level % colorCount()]
}
