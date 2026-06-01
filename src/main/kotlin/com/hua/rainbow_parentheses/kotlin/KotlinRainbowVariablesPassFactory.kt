package com.hua.rainbow_parentheses.kotlin

import com.hua.rainbow_parentheses.RainbowParenthesesSettings
import com.intellij.codeHighlighting.Pass
import com.intellij.codeHighlighting.TextEditorHighlightingPass
import com.intellij.codeHighlighting.TextEditorHighlightingPassFactory
import com.intellij.codeHighlighting.TextEditorHighlightingPassFactoryRegistrar
import com.intellij.codeHighlighting.TextEditorHighlightingPassRegistrar
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.psi.KtFile

/**
 * 注册 [KotlinRainbowVariablesPass]。仅对 Kotlin 文件生效；文件类型 / 语言被排除或属于大文件时返回 null。
 *
 * 锚点 LAST：在常规高亮（含 Kotlin 语义高亮）之后运行，再叠加我们的彩虹色。
 *
 * @author Hua
 */
class KotlinRainbowVariablesPassFactory :
    TextEditorHighlightingPassFactory, TextEditorHighlightingPassFactoryRegistrar {

    override fun registerHighlightingPassFactory(
        registrar: TextEditorHighlightingPassRegistrar,
        project: Project
    ) {
        registrar.registerTextEditorHighlightingPass(
            this,
            TextEditorHighlightingPassRegistrar.Anchor.LAST,
            Pass.LAST_PASS,
            false,
            false
        )
    }

    override fun createHighlightingPass(file: PsiFile, editor: Editor): TextEditorHighlightingPass? {
        if (file !is KtFile) return null

        val settings = RainbowParenthesesSettings.getInstance()
        if (!settings.isEnabledForFileType(file.fileType)) return null
        if (!settings.isEnabledForLanguage(file.language)) return null

        if (settings.doNotRainbowifyBigFiles &&
            editor.document.lineCount > settings.bigFilesLineThreshold
        ) {
            return null
        }

        return KotlinRainbowVariablesPass(file, editor)
    }
}
