package com.hua.rainbow_parentheses.kotlin

import com.hua.rainbow_parentheses.RainbowColorsManager
import com.hua.rainbow_parentheses.RainbowParenthesesSettings
import com.intellij.codeHighlighting.TextEditorHighlightingPass
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.psi.KtDestructuringDeclarationEntry
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid

/**
 * 作用域感知的 Kotlin 变量彩虹着色 pass。
 *
 * 之前用 annotator 实现，但 Kotlin 自带的语义高亮在同一图层会盖过 annotator 的颜色（Java 无此竞争，
 * 故 Java 仍用 annotator）。这里改为与彩虹括号一致的 markup 直接着色，并把图层显式抬高到 Kotlin
 * 语义高亮之上，确保彩虹色生效。
 *
 * 只给局部变量、函数 / lambda 参数、解构声明项着色；成员属性、`val/var` 构造参数、类型 / 函数名不染。
 * 颜色种子取"名字 + 源声明名字标识符偏移"，故同一变量恒同色、异作用域同名异色。
 *
 * @author Hua
 */
class KotlinRainbowVariablesPass(
    private val file: KtFile,
    private val editor: Editor
) : TextEditorHighlightingPass(file.project, editor.document, false) {

    private data class VariableHighlight(
        val startOffset: Int,
        val endOffset: Int,
        val colorKey: TextAttributesKey
    )

    private var highlights: List<VariableHighlight> = emptyList()

    override fun doCollectInformation(progress: ProgressIndicator) {
        val settings = RainbowParenthesesSettings.getInstance()
        if (!settings.enabled || !settings.enableRainbowVariables) {
            highlights = emptyList()
            return
        }

        val result = ArrayList<VariableHighlight>()
        file.accept(object : KtTreeVisitorVoid() {
            override fun visitProperty(property: KtProperty) {
                super.visitProperty(property)
                if (property.isLocal) addColored(property.nameIdentifier, property, result)
            }

            override fun visitParameter(parameter: KtParameter) {
                super.visitParameter(parameter)
                if (!parameter.hasValOrVar()) addColored(parameter.nameIdentifier, parameter, result)
            }

            override fun visitDestructuringDeclarationEntry(entry: KtDestructuringDeclarationEntry) {
                super.visitDestructuringDeclarationEntry(entry)
                addColored(entry.nameIdentifier, entry, result)
            }

            override fun visitSimpleNameExpression(expression: KtSimpleNameExpression) {
                super.visitSimpleNameExpression(expression)
                val declaration = colorableSourceDeclaration(expression.mainReference.resolve()) ?: return
                addColored(expression.getReferencedNameElement(), declaration, result)
            }
        })
        highlights = result
    }

    private fun addColored(range: PsiElement?, declaration: KtNamedDeclaration, out: MutableList<VariableHighlight>) {
        if (range == null) return
        val name = declaration.name ?: return
        val nameIdentifier = declaration.nameIdentifier ?: return
        val colorKey = RainbowColorsManager.getVariableColorKey(name, nameIdentifier.textRange.startOffset)
        out.add(VariableHighlight(range.textRange.startOffset, range.textRange.endOffset, colorKey))
    }

    /**
     * 把解析结果规整到"源声明"并判定是否可着色：只接受局部变量、非 `val/var` 参数、解构项。
     */
    private fun colorableSourceDeclaration(resolved: PsiElement?): KtNamedDeclaration? =
        when (val source = resolved?.navigationElement) {
            is KtParameter -> if (source.hasValOrVar()) null else source
            is KtProperty -> if (source.isLocal) source else null
            is KtDestructuringDeclarationEntry -> source
            else -> null
        }

    override fun doApplyInformationToEditor() {
        val markupModel = editor.markupModel

        editor.getUserData(KOTLIN_VARIABLE_HIGHLIGHTERS_KEY)?.let { old ->
            old.forEach { markupModel.removeHighlighter(it) }
            old.clear()
        }

        if (highlights.isEmpty()) {
            editor.putUserData(KOTLIN_VARIABLE_HIGHLIGHTERS_KEY, null)
            return
        }

        val textLength = editor.document.textLength
        val newHighlighters = ArrayList<RangeHighlighter>(highlights.size)
        for (h in highlights) {
            if (h.startOffset < 0 || h.endOffset > textLength || h.startOffset >= h.endOffset) continue
            newHighlighters.add(
                markupModel.addRangeHighlighter(
                    h.colorKey,
                    h.startOffset,
                    h.endOffset,
                    LAYER,
                    HighlighterTargetArea.EXACT_RANGE
                )
            )
        }
        editor.putUserData(KOTLIN_VARIABLE_HIGHLIGHTERS_KEY, newHighlighters)
    }

    private companion object {
        // 抬到 Kotlin 语义高亮（ADDITIONAL_SYNTAX）之上，确保我们的前景色胜出。
        const val LAYER = HighlighterLayer.ADDITIONAL_SYNTAX + 1
        val KOTLIN_VARIABLE_HIGHLIGHTERS_KEY =
            Key.create<MutableList<RangeHighlighter>>("RAINBOW_KOTLIN_VARIABLE_HIGHLIGHTERS")
    }
}
