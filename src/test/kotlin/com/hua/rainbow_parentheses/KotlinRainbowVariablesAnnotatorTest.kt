package com.hua.rainbow_parentheses

import com.intellij.openapi.util.TextRange
import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * 验证作用域感知的 Kotlin 变量着色：局部变量与参数被染、成员属性 / val 构造参数 / 函数名不染；
 * 同名不同作用域不同色。
 *
 * @author Hua
 */
class KotlinRainbowVariablesAnnotatorTest : BasePlatformTestCase() {

    override fun setUp() {
        super.setUp()
        RainbowParenthesesSettings.getInstance().enableRainbowVariables = true
    }

    override fun tearDown() {
        try {
            RainbowParenthesesSettings.getInstance().enableRainbowVariables = false
        } finally {
            super.tearDown()
        }
    }

    private fun coloredTexts(): Set<String> {
        val infos = myFixture.doHighlighting()
        val doc = myFixture.editor.document
        return infos
            .filter { it.forcedTextAttributesKey?.externalName?.startsWith("HUA_RAINBOW_VAR_") == true }
            .map { doc.getText(TextRange(it.startOffset, it.endOffset)) }
            .toSet()
    }

    fun testLocalsAndParamsColoredMembersAndFunctionsNot() {
        myFixture.configureByText(
            "T.kt",
            """
            class T(val member: Int) {
                fun doWork(param: Int): Int {
                    val local = param
                    return local + member + doWork(param)
                }
            }
            """.trimIndent()
        )

        val colored = coloredTexts()
        assertTrue("局部变量应着色", "local" in colored)
        assertTrue("参数应着色", "param" in colored)
        assertFalse("val 构造参数（成员属性）不应着色", "member" in colored)
        assertFalse("函数名不应着色", "doWork" in colored)
        assertFalse("类型名不应着色", "T" in colored)
    }

    fun testSameNameDifferentScopeDifferentColor() {
        myFixture.configureByText(
            "T.kt",
            """
            class T {
                fun a() { val value = 1; print(value) }
                fun b() { val value = 2; print(value) }
            }
            """.trimIndent()
        )

        val infos = myFixture.doHighlighting()
        val doc = myFixture.editor.document
        val keysForValue = infos
            .filter { it.forcedTextAttributesKey?.externalName?.startsWith("HUA_RAINBOW_VAR_") == true }
            .filter { doc.getText(TextRange(it.startOffset, it.endOffset)) == "value" }
            .map { it.forcedTextAttributesKey!!.externalName }
            .toSet()

        assertEquals("两处 value 应得到不同颜色 key", 2, keysForValue.size)
    }
}
