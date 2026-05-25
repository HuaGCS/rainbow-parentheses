package com.hua.rainbow_parentheses

import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.util.TextRange
import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * @author Hua
 * @since 2025/9/2 15:53
 */
class ParenthesesMatcherTest : BasePlatformTestCase() {

    fun testSimpleBracketMatching() {
        val code = "()"
        val document = EditorFactory.getInstance().createDocument(code)

        // 🔧 修复：使用正确的方法调用
        val pairs = ParenthesesMatcher.findMatchingBrackets(document, 0, code.length)

        assertEquals(1, pairs.size)
        val pair = pairs[0]
        assertEquals(0, pair.openRange.startOffset)
        assertEquals(1, pair.closeRange.startOffset)
        assertEquals(ParenthesesType.ROUND, pair.type)
    }

    fun testNestedBrackets() {
        val code = "((()))"
        val document = EditorFactory.getInstance().createDocument(code)

        val pairs = ParenthesesMatcher.findMatchingBrackets(document, 0, code.length)

        assertEquals(3, pairs.size)
        assertEquals(0, pairs[0].level)
        assertEquals(1, pairs[1].level)
        assertEquals(2, pairs[2].level)
    }

    fun testMixedBrackets() {
        val code = "{[()]}"
        val document = EditorFactory.getInstance().createDocument(code)

        val pairs = ParenthesesMatcher.findMatchingBrackets(document, 0, code.length)

        assertEquals(3, pairs.size)
        assertTrue(pairs.any { it.type == ParenthesesType.CURLY })
        assertTrue(pairs.any { it.type == ParenthesesType.SQUARE })
        assertTrue(pairs.any { it.type == ParenthesesType.ROUND })
    }

    fun testBracketsInStrings() {
        val code = "val s = \"(hello)\"; method()"
        val document = EditorFactory.getInstance().createDocument(code)

        val pairs = ParenthesesMatcher.findMatchingBrackets(document, 0, code.length)

        // 应该只匹配 method() 的括号，忽略字符串内的括号
        assertEquals(1, pairs.size)
        assertTrue(pairs[0].openRange.startOffset > code.indexOf("method"))
    }

    fun testKotlinCode() {
        val code = """
            fun example() {
                val list = listOf(1, 2, 3)
                list.forEach { item ->
                    if (item > 0) {
                        println("Item: ${'$'}item")
                    }
                }
            }
        """.trimIndent()

        val document = EditorFactory.getInstance().createDocument(code)
        val pairs = ParenthesesMatcher.findMatchingBrackets(document, 0, code.length)

        assertTrue("应该找到匹配的括号对", pairs.isNotEmpty())
        assertTrue("应该包含花括号", pairs.any { it.type == ParenthesesType.CURLY })
        assertTrue("应该包含圆括号", pairs.any { it.type == ParenthesesType.ROUND })
    }

    // 🔧 新增：测试 TextRange 版本的方法调用
    fun testWithTextRange() {
        val code = "function() { return [1, 2, 3]; }"
        val document = EditorFactory.getInstance().createDocument(code)
        val range = TextRange(0, code.length)

        val pairs = ParenthesesMatcher.findMatchingBrackets(document, range)

        assertTrue("应该找到匹配的括号对", pairs.isNotEmpty())
        assertTrue("应该包含圆括号", pairs.any { it.type == ParenthesesType.ROUND })
        assertTrue("应该包含花括号", pairs.any { it.type == ParenthesesType.CURLY })
        assertTrue("应该包含方括号", pairs.any { it.type == ParenthesesType.SQUARE })
    }
}