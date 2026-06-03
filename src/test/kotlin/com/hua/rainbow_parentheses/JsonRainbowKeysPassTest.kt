package com.hua.rainbow_parentheses

import com.intellij.openapi.util.TextRange
import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * 验证按嵌套深度着色的 JSON 键名（[com.hua.rainbow_parentheses.json.JsonRainbowKeysPass]）。
 *
 * @author Hua
 */
class JsonRainbowKeysPassTest : BasePlatformTestCase() {

    override fun setUp() {
        super.setUp()
        RainbowParenthesesSettings.getInstance().enableRainbowKeys = true
    }

    override fun tearDown() {
        try {
            RainbowParenthesesSettings.getInstance().enableRainbowKeys = false
        } finally {
            super.tearDown()
        }
    }

    private fun coloredTextToKey(): List<Pair<String, String>> {
        myFixture.doHighlighting()
        val doc = myFixture.editor.document
        return myFixture.editor.markupModel.allHighlighters
            .mapNotNull { hl ->
                val key = hl.textAttributesKey?.externalName ?: return@mapNotNull null
                if (!key.startsWith("HUA_RAINBOW_KEY_")) return@mapNotNull null
                doc.getText(TextRange(hl.startOffset, hl.endOffset)) to key
            }
    }

    fun testKeysColoredByDepth() {
        myFixture.configureByText(
            "a.json",
            """
            {
              "top": {
                "nested": {
                  "leaf": 1
                }
              }
            }
            """.trimIndent()
        )

        val byName = coloredTextToKey()
        val texts = byName.map { it.first }
        // nameElement 含引号，故文本是 "top" 形式
        assertTrue("顶层键应着色", texts.any { it.contains("top") })
        assertTrue("嵌套键应着色", texts.any { it.contains("nested") })
        assertTrue("叶子键应着色", texts.any { it.contains("leaf") })

        val topKey = byName.first { it.first.contains("top") }.second
        val nestedKey = byName.first { it.first.contains("nested") }.second
        val leafKey = byName.first { it.first.contains("leaf") }.second
        assertEquals("不同深度应得到不同颜色 key", 3, setOf(topKey, nestedKey, leafKey).size)
    }

    fun testDisabledWhenSettingOff() {
        RainbowParenthesesSettings.getInstance().enableRainbowKeys = false
        myFixture.configureByText("a.json", """{"k": 1}""")
        assertTrue("关闭开关后不应有键名高亮", coloredTextToKey().isEmpty())
    }
}
