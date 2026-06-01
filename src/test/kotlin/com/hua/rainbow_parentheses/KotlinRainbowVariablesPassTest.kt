package com.hua.rainbow_parentheses

import com.intellij.openapi.util.TextRange
import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * 验证作用域感知的 Kotlin 变量着色（[com.hua.rainbow_parentheses.kotlin.KotlinRainbowVariablesPass]）：
 * 局部变量与参数被染、成员属性 / val 构造参数 / 函数名不染；同名不同作用域不同色；同一变量定义与使用同色。
 *
 * Kotlin 着色走 markup（与彩虹括号一致），因此这里检查 editor markup 里带 HUA_RAINBOW_VAR_ key 的高亮。
 *
 * @author Hua
 */
class KotlinRainbowVariablesPassTest : BasePlatformTestCase() {

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

    /** 触发高亮后，收集我们 pass 加到 markup 上的变量高亮：文本 -> 颜色 key 外部名。 */
    private fun coloredTextToKey(): List<Pair<String, String>> {
        myFixture.doHighlighting()
        val doc = myFixture.editor.document
        return myFixture.editor.markupModel.allHighlighters
            .mapNotNull { hl ->
                val key = hl.textAttributesKey?.externalName ?: return@mapNotNull null
                if (!key.startsWith("HUA_RAINBOW_VAR_")) return@mapNotNull null
                doc.getText(TextRange(hl.startOffset, hl.endOffset)) to key
            }
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

        val colored = coloredTextToKey().map { it.first }.toSet()
        assertTrue("局部变量应着色", "local" in colored)
        assertTrue("参数应着色", "param" in colored)
        assertFalse("val 构造参数（成员属性）不应着色", "member" in colored)
        assertFalse("函数名不应着色", "doWork" in colored)
        assertFalse("类型名不应着色", "T" in colored)
    }

    fun testDeclarationAndUsageShareOneColor() {
        myFixture.configureByText(
            "T.kt",
            """
            fun f() {
                val value = 1
                println(value + value)
            }
            """.trimIndent()
        )

        val valueKeys = coloredTextToKey().filter { it.first == "value" }.map { it.second }
        assertTrue("定义与使用都应着色（至少 3 处）", valueKeys.size >= 3)
        assertEquals("同一变量的定义与使用应同色", 1, valueKeys.toSet().size)
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

        val valueKeys = coloredTextToKey().filter { it.first == "value" }.map { it.second }.toSet()
        assertEquals("两处 value 应得到不同颜色 key", 2, valueKeys.size)
    }
}
