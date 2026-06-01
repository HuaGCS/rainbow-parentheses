package com.hua.rainbow_parentheses.java

import com.hua.rainbow_parentheses.RainbowColorsManager
import com.hua.rainbow_parentheses.RainbowParenthesesSettings
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiIdentifier
import com.intellij.psi.PsiLocalVariable
import com.intellij.psi.PsiParameter
import com.intellij.psi.PsiReferenceExpression
import com.intellij.psi.PsiVariable

/**
 * 作用域感知的 Java 变量彩虹着色：只给局部变量与参数（含 for/catch/lambda 参数）着色，
 * 字段 / 类型 / 方法名一律不染。
 *
 * 通过 [PsiReferenceExpression.resolve] 把每处引用解析到其声明，颜色种子取"名字 + 声明处偏移"，
 * 因而同一变量的所有出现恒为同色，而不同作用域中的同名变量得到不同颜色。
 *
 * 由 `META-INF/rainbow-java.xml` 仅在 Java 插件存在时注册（optional depends）。
 *
 * @author Hua
 */
class JavaRainbowVariablesAnnotator : Annotator {

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        // 先用最廉价的判断过滤掉绝大多数元素
        if (element !is PsiIdentifier) return

        val settings = RainbowParenthesesSettings.getInstance()
        if (!settings.enabled || !settings.enableRainbowVariables) return

        val variable = when (val parent = element.parent) {
            is PsiReferenceExpression -> parent.resolve() as? PsiVariable
            is PsiLocalVariable -> parent
            is PsiParameter -> parent
            else -> null
        } ?: return

        // 只着色局部变量与参数，排除字段（PsiField）等其他 PsiVariable
        if (variable !is PsiLocalVariable && variable !is PsiParameter) return

        val name = variable.name ?: return

        val file = element.containingFile ?: return
        if (!settings.isEnabledForFileType(file.fileType)) return
        if (!settings.isEnabledForLanguage(file.language)) return
        if (settings.doNotRainbowifyBigFiles) {
            val document = file.viewProvider.document
            if (document != null && document.lineCount > settings.bigFilesLineThreshold) return
        }

        val scopeSeed = variable.nameIdentifier?.textRange?.startOffset ?: variable.textOffset
        val colorKey = RainbowColorsManager.getVariableColorKey(name, scopeSeed)

        holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
            .range(element)
            .textAttributes(colorKey)
            .create()
    }
}
