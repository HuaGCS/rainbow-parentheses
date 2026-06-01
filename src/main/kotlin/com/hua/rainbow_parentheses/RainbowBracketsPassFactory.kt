package com.hua.rainbow_parentheses

import com.intellij.codeHighlighting.Pass
import com.intellij.codeHighlighting.TextEditorHighlightingPass
import com.intellij.codeHighlighting.TextEditorHighlightingPassFactory
import com.intellij.codeHighlighting.TextEditorHighlightingPassFactoryRegistrar
import com.intellij.codeHighlighting.TextEditorHighlightingPassRegistrar
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile

/**
 * 注册 [RainbowBracketsPass]。文件类型 / 语言被排除、或属于大文件时返回 null，不参与高亮。
 *
 * @author Hua
 */
class RainbowBracketsPassFactory :
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
        val settings = RainbowParenthesesSettings.getInstance()
        if (!settings.isEnabledForFileType(file.fileType)) return null
        if (!settings.isEnabledForLanguage(file.language)) return null

        if (settings.doNotRainbowifyBigFiles &&
            editor.document.lineCount > settings.bigFilesLineThreshold
        ) {
            return null
        }

        return RainbowBracketsPass(file, editor)
    }
}
