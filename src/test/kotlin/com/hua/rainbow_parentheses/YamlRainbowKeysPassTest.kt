package com.hua.rainbow_parentheses

import com.intellij.openapi.util.TextRange
import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * 验证按嵌套深度着色的 YAML 键名（[com.hua.rainbow_parentheses.yaml.YamlRainbowKeysPass]）。
 *
 * @author Hua
 */
class YamlRainbowKeysPassTest : BasePlatformTestCase() {

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
            "a.yaml",
            """
            top:
              nested:
                leaf: 1
            """.trimIndent()
        )

        val byName = coloredTextToKey()
        val names = byName.map { it.first }.toSet()
        assertTrue("顶层键应着色", "top" in names)
        assertTrue("嵌套键应着色", "nested" in names)
        assertTrue("叶子键应着色", "leaf" in names)

        val topKey = byName.first { it.first == "top" }.second
        val nestedKey = byName.first { it.first == "nested" }.second
        val leafKey = byName.first { it.first == "leaf" }.second
        assertEquals("不同深度应得到不同颜色 key", 3, setOf(topKey, nestedKey, leafKey).size)
    }

    fun testDisabledWhenSettingOff() {
        RainbowParenthesesSettings.getInstance().enableRainbowKeys = false
        myFixture.configureByText("a.yaml", "k: 1")
        assertTrue("关闭开关后不应有键名高亮", coloredTextToKey().isEmpty())
    }
}
