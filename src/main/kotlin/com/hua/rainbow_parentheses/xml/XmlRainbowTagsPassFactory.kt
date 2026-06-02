package com.hua.rainbow_parentheses.xml

import com.hua.rainbow_parentheses.RainbowParenthesesSettings
import com.intellij.codeHighlighting.Pass
import com.intellij.codeHighlighting.TextEditorHighlightingPass
import com.intellij.codeHighlighting.TextEditorHighlightingPassFactory
import com.intellij.codeHighlighting.TextEditorHighlightingPassFactoryRegistrar
import com.intellij.codeHighlighting.TextEditorHighlightingPassRegistrar
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.xml.XmlFile

/**
 * 注册 [XmlRainbowTagsPass]。仅对 XML / HTML 文件生效；文件类型 / 语言被排除或属于大文件时返回 null。
 *
 * @author Hua
 */
class XmlRainbowTagsPassFactory :
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
        if (file !is XmlFile) return null

        val settings = RainbowParenthesesSettings.getInstance()
        if (!settings.isEnabledForFileType(file.fileType)) return null
        if (!settings.isEnabledForLanguage(file.language)) return null

        if (settings.doNotRainbowifyBigFiles &&
            editor.document.lineCount > settings.bigFilesLineThreshold
        ) {
            return null
        }

        return XmlRainbowTagsPass(file, editor)
    }
}
