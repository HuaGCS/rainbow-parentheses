package com.hua.rainbow_parentheses

import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.intellij.openapi.util.TextRange
import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * 验证作用域感知的 Java 变量着色：局部变量与参数被染色、字段不染；同名不同作用域不同色。
 *
 * @author Hua
 */
class JavaRainbowVariablesAnnotatorTest : BasePlatformTestCase() {

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

    fun testLocalsAndParamsColoredFieldsAndMethodsNot() {
        myFixture.configureByText(
            "T.java",
            """
            class T {
                int field = 0;
                int doWork(int param) {
                    int local = param;
                    return local + field + doWork(param);
                }
            }
            """.trimIndent()
        )

        val colored = coloredTexts()
        assertTrue("局部变量应着色", "local" in colored)
        assertTrue("参数应着色", "param" in colored)
        assertFalse("字段不应着色", "field" in colored)
        assertFalse("方法名不应着色", "doWork" in colored)
        assertFalse("类型名不应着色", "T" in colored)
    }

    fun testSameNameDifferentScopeDifferentColor() {
        myFixture.configureByText(
            "T.java",
            """
            class T {
                void a() { int value = 1; System.out.println(value); }
                void b() { int value = 2; System.out.println(value); }
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

        // 两个方法里各有一个名为 value 的局部变量，作用域种子不同 -> 颜色 key 不同
        assertEquals("两处 value 应得到不同颜色 key", 2, keysForValue.size)
    }

    fun testLanguageRegistry() {
        assertTrue(PsiScopeRainbowLanguages.handles(JavaLanguage.INSTANCE))
        assertFalse(PsiScopeRainbowLanguages.handles(PlainTextLanguage.INSTANCE))
    }

    fun testScopeSeedDifferentiatesColors() {
        val a = RainbowColorsManager.getVariableColorKey("value", 10)
        val b = RainbowColorsManager.getVariableColorKey("value", 10)
        assertSame("同名同种子应同色", a, b)
        // 种子相差恰好抵消 colorCount 时仍同色，相差 1 时（默认 10 色）应不同
        assertNotSame(
            "同名不同种子通常不同色",
            RainbowColorsManager.getVariableColorKey("value", 0),
            RainbowColorsManager.getVariableColorKey("value", 1)
        )
    }
}
