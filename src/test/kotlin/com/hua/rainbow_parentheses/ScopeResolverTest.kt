package com.hua.rainbow_parentheses

import com.hua.rainbow_parentheses.scope.ScopeResolver
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory
import com.intellij.openapi.util.TextRange
import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * 验证 [ScopeResolver]：括号语言取最内层括号对作用域；无括号语言回退到最内层跨行 PSI 块。
 *
 * @author Hua
 */
class ScopeResolverTest : BasePlatformTestCase() {

    private fun resolveAtCaret(): ScopeResolver.Scope? {
        val psiFile = myFixture.file
        val document = myFixture.editor.document
        val caret = myFixture.editor.caretModel.offset
        val highlighter = SyntaxHighlighterFactory.getSyntaxHighlighter(
            psiFile.language, project, psiFile.virtualFile
        )
        return ScopeResolver.resolve(psiFile, document, caret, highlighter)
    }

    private fun textOf(scope: ScopeResolver.Scope): String =
        myFixture.editor.document.getText(TextRange(scope.startOffset, scope.endOffset))

    fun testBracketLanguageUsesInnermostBracketScope() {
        myFixture.configureByText(
            "T.java",
            """
            class T {
                void m() {
                    int x = <caret>1;
                }
            }
            """.trimIndent()
        )

        val scope = resolveAtCaret()
        assertNotNull("括号语言应解析出作用域", scope)
        assertTrue(
            "应使用花括号层级色，得到 ${scope!!.colorKey.externalName}",
            scope.colorKey.externalName.startsWith("HUA_RAINBOW_CURLY_")
        )
        val text = textOf(scope)
        assertTrue("作用域应是方法体花括号范围", text.startsWith("{") && text.endsWith("}"))
        assertTrue("作用域应包含方法体语句", text.contains("int x"))
        assertFalse("不应一路扩到类体（最内层优先）", text.contains("void m"))
    }

    fun testBraceLessLanguageFallsBackToInnermostBlockNotWholeFile() {
        myFixture.configureByText(
            "a.yaml",
            """
            name: demo
            server:
              host: localhost
              ports:
                http: 80
                https: 44<caret>3
            """.trimIndent()
        )

        val scope = resolveAtCaret()
        assertNotNull("无括号语言应回退到 PSI 块作用域", scope)
        assertTrue(
            "PSI 块作用域应使用缩进色，得到 ${scope!!.colorKey.externalName}",
            scope.colorKey.externalName.startsWith("HUA_RAINBOW_INDENT_")
        )
        val text = textOf(scope)
        assertTrue("作用域应跨多行", text.contains("\n"))
        assertTrue("作用域应是最内层 ports 块（含 http/https）", text.contains("http: 80") && text.contains("https"))
        assertFalse("不应扩到 server 块", text.contains("host: localhost"))
        assertFalse("绝不应覆盖整篇文件", text.contains("name: demo"))
    }

    fun testClickAtBlockEndBoundaryWithFollowingSibling() {
        // 光标落在块最后一行值的末尾，且该块之后还有兄弟块（features:）——偏移正好等于块 endOffset，
        // findElementAt(caret) 命中的是后面的 features，必须靠 caret-1 探针找回 ports 块（复现实测 bug）
        myFixture.configureByText(
            "a.yaml",
            """
            name: demo
            server:
              host: localhost
              ports:
                http: 80
                https: 443<caret>
            features:
              tags: true
            """.trimIndent()
        )

        val scope = resolveAtCaret()
        assertNotNull("块结束边界点击也应解析出所在块", scope)
        val text = textOf(scope!!)
        assertTrue("应是 ports 块（含 http/https）", text.contains("http: 80") && text.contains("https"))
        assertFalse("不应扩到 server 块", text.contains("host: localhost"))
        assertFalse("不应落到后面的 features 块", text.contains("features"))
    }

    fun testTopLevelClickHighlightsItsLineNotWholeFile() {
        myFixture.configureByText(
            "a.yaml",
            """
            na<caret>me: demo
            server:
              host: localhost
            """.trimIndent()
        )
        val scope = resolveAtCaret()
        assertNotNull("顶层单行条目应回退到高亮该行本身", scope)
        val text = textOf(scope!!)
        assertEquals("应恰为该行内容", "name: demo", text)
        assertFalse("绝不应覆盖整篇文件", text.contains("server"))
    }

    fun testSingleLineFileHighlightsThatLine() {
        myFixture.configureByText("a.yaml", "k: <caret>1")
        val scope = resolveAtCaret()
        assertNotNull("单行文件应回退到高亮该行本身", scope)
        assertEquals("应恰为该行内容", "k: 1", textOf(scope!!))
    }

    fun testBlankLineHasNoScope() {
        myFixture.configureByText("a.yaml", "k: 1\n<caret>\nj: 2")
        assertNull("空行无内容，应不高亮", resolveAtCaret())
    }
}
