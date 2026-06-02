package com.hua.rainbow_parentheses

import com.intellij.openapi.util.TextRange
import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * 验证按嵌套深度着色的 XML / HTML 标签名（[com.hua.rainbow_parentheses.xml.XmlRainbowTagsPass]）：
 * 开标签名与闭标签名被染、不同深度得到不同颜色 key、同名标签按深度区分。
 *
 * 与彩虹括号一致走 markup，因此这里检查 editor markup 里带 HUA_RAINBOW_TAG_ key 的高亮。
 *
 * @author Hua
 */
class XmlRainbowTagsPassTest : BasePlatformTestCase() {

    override fun setUp() {
        super.setUp()
        RainbowParenthesesSettings.getInstance().enableRainbowTags = true
    }

    override fun tearDown() {
        try {
            RainbowParenthesesSettings.getInstance().enableRainbowTags = false
        } finally {
            super.tearDown()
        }
    }

    /** 触发高亮后，收集 pass 加到 markup 上的标签高亮：文本 -> 颜色 key 外部名。 */
    private fun coloredTextToKey(): List<Pair<String, String>> {
        myFixture.doHighlighting()
        val doc = myFixture.editor.document
        return myFixture.editor.markupModel.allHighlighters
            .mapNotNull { hl ->
                val key = hl.textAttributesKey?.externalName ?: return@mapNotNull null
                if (!key.startsWith("HUA_RAINBOW_TAG_")) return@mapNotNull null
                doc.getText(TextRange(hl.startOffset, hl.endOffset)) to key
            }
    }

    fun testTagNamesColoredByDepth() {
        myFixture.configureByText(
            "a.xml",
            """
            <root>
                <child>
                    <leaf>text</leaf>
                </child>
            </root>
            """.trimIndent()
        )

        val byName = coloredTextToKey()
        val names = byName.map { it.first }.toSet()
        assertTrue("根标签名应着色", "root" in names)
        assertTrue("子标签名应着色", "child" in names)
        assertTrue("叶标签名应着色", "leaf" in names)

        val rootKey = byName.first { it.first == "root" }.second
        val childKey = byName.first { it.first == "child" }.second
        val leafKey = byName.first { it.first == "leaf" }.second
        assertEquals("不同深度应得到不同颜色 key", 3, setOf(rootKey, childKey, leafKey).size)
    }

    fun testStartAndEndTagShareDepthColor() {
        myFixture.configureByText(
            "a.xml",
            "<root><child>x</child></root>"
        )

        val childKeys = coloredTextToKey().filter { it.first == "child" }.map { it.second }
        assertEquals("开闭标签名都应着色（2 处）", 2, childKeys.size)
        assertEquals("同一标签的开闭名应同色", 1, childKeys.toSet().size)
    }

    fun testDisabledWhenSettingOff() {
        RainbowParenthesesSettings.getInstance().enableRainbowTags = false
        myFixture.configureByText("a.xml", "<root><child>x</child></root>")
        assertTrue("关闭开关后不应有标签高亮", coloredTextToKey().isEmpty())
    }
}
