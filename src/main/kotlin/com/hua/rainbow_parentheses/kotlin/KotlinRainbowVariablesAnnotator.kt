package com.hua.rainbow_parentheses.kotlin

import com.hua.rainbow_parentheses.RainbowColorsManager
import com.hua.rainbow_parentheses.RainbowParenthesesSettings
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.psi.KtDestructuringDeclarationEntry
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtSimpleNameExpression

/**
 * 作用域感知的 Kotlin 变量彩虹着色：只给局部变量、函数 / lambda 参数、解构声明项着色；
 * 成员属性、`val/var` 构造参数（实为属性）、类型 / 函数名一律不染。
 *
 * 声明处直接着色其名字标识符；引用处通过 [mainReference] 解析到声明，再判定是否可着色。
 * 颜色种子取"名字 + 声明处偏移"，故同一变量恒同色、异作用域同名异色。
 *
 * 由 `META-INF/rainbow-kotlin.xml` 仅在 Kotlin 插件存在时注册（optional depends）。
 *
 * @author Hua
 */
class KotlinRainbowVariablesAnnotator : Annotator {

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        val settings = RainbowParenthesesSettings.getInstance()
        if (!settings.enabled || !settings.enableRainbowVariables) return

        val file = element.containingFile ?: return
        if (!settings.isEnabledForFileType(file.fileType)) return
        if (!settings.isEnabledForLanguage(file.language)) return
        if (settings.doNotRainbowifyBigFiles) {
            val document = file.viewProvider.document
            if (document != null && document.lineCount > settings.bigFilesLineThreshold) return
        }

        // 确定"着色范围"与"所指声明"：声明本身用其名字标识符，引用先解析
        val declaration: KtNamedDeclaration
        val range: PsiElement
        when (element) {
            is KtParameter -> {
                if (element.hasValOrVar()) return // val/var 构造参数实为成员属性
                declaration = element
                range = element.nameIdentifier ?: return
            }
            is KtProperty -> {
                if (!element.isLocal) return // 成员属性不染
                declaration = element
                range = element.nameIdentifier ?: return
            }
            is KtDestructuringDeclarationEntry -> {
                declaration = element
                range = element.nameIdentifier ?: return
            }
            is KtSimpleNameExpression -> {
                val resolved = element.mainReference.resolve()
                if (!isColorable(resolved)) return
                declaration = resolved as KtNamedDeclaration
                range = element.getReferencedNameElement()
            }
            else -> return
        }

        val name = declaration.name ?: return
        val scopeSeed = declaration.nameIdentifier?.textRange?.startOffset ?: declaration.textOffset
        val colorKey = RainbowColorsManager.getVariableColorKey(name, scopeSeed)

        holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
            .range(range)
            .textAttributes(colorKey)
            .create()
    }

    /** 只有局部变量、非 `val/var` 参数、解构项才着色（与字段语义的成员属性区分开）。 */
    private fun isColorable(declaration: PsiElement?): Boolean = when (declaration) {
        is KtParameter -> !declaration.hasValOrVar()
        is KtProperty -> declaration.isLocal
        is KtDestructuringDeclarationEntry -> true
        else -> false
    }
}
