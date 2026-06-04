package com.hua.rainbow_parentheses

import com.intellij.openapi.util.TextRange
import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * 验证 Kotlin lambda 的 `->` 箭头按嵌套深度着色（[com.hua.rainbow_parentheses.kotlin.KotlinLambdaArrowPass]）。
 *
 * @author Hua
 */
class KotlinLambdaArrowPassTest : BasePlatformTestCase() {

    override fun setUp() {
        super.setUp()
        RainbowParenthesesSettings.getInstance().enableRainbowLambdaArrow = true
    }

    override fun tearDown() {
        try {
            RainbowParenthesesSettings.getInstance().enableRainbowLambdaArrow = false
        } finally {
            super.tearDown()
        }
    }

    private fun coloredArrows(): List<Pair<String, String>> {
        myFixture.doHighlighting()
        val doc = myFixture.editor.document
        return myFixture.editor.markupModel.allHighlighters
            .mapNotNull { hl ->
                val key = hl.textAttributesKey?.externalName ?: return@mapNotNull null
                if (!key.startsWith("HUA_RAINBOW_ARROW_")) return@mapNotNull null
                doc.getText(TextRange(hl.startOffset, hl.endOffset)) to key
            }
    }

    fun testLambdaArrowsColoredByDepth() {
        myFixture.configureByText(
            "T.kt",
            """
            fun f(xs: List<List<Int>>) {
                xs.map { row -> row.map { c -> c + 1 } }
            }
            """.trimIndent()
        )

        val arrows = coloredArrows()
        assertEquals("应给两个 lambda 箭头着色", 2, arrows.size)
        assertTrue("着色的应是 -> 箭头", arrows.all { it.first == "->" })
        assertEquals("不同嵌套深度的箭头应得到不同颜色", 2, arrows.map { it.second }.toSet().size)
    }

    fun testDisabledWhenSettingOff() {
        RainbowParenthesesSettings.getInstance().enableRainbowLambdaArrow = false
        myFixture.configureByText("T.kt", "val f = { x: Int -> x }")
        assertTrue("关闭开关后不应给箭头着色", coloredArrows().isEmpty())
    }
}
