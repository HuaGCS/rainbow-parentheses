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

    fun testBraceLessLanguageFallsBackToPsiBlock() {
        myFixture.configureByText(
            "a.yaml",
            """
            top:
              nested:
                lea<caret>f: 1
            """.trimIndent()
        )

        val scope = resolveAtCaret()
        assertNotNull("无括号语言应回退到 PSI 块作用域", scope)
        assertTrue(
            "PSI 块作用域应使用缩进色，得到 ${scope!!.colorKey.externalName}",
            scope.colorKey.externalName.startsWith("HUA_RAINBOW_INDENT_")
        )
        assertTrue("作用域应包含光标", scope.startOffset <= myFixture.editor.caretModel.offset)
        val text = textOf(scope)
        assertTrue("作用域应跨多行", text.contains("\n"))
        assertTrue("作用域应包含光标所在键", text.contains("leaf"))
    }

    fun testSingleLineHasNoBlockScope() {
        myFixture.configureByText("a.yaml", "k: <caret>1")
        assertNull("单行无跨行块，应无作用域可高亮", resolveAtCaret())
    }
}
