package com.hua.rainbow_parentheses.json

import com.hua.rainbow_parentheses.RainbowParenthesesSettings
import com.intellij.codeHighlighting.Pass
import com.intellij.codeHighlighting.TextEditorHighlightingPass
import com.intellij.codeHighlighting.TextEditorHighlightingPassFactory
import com.intellij.codeHighlighting.TextEditorHighlightingPassFactoryRegistrar
import com.intellij.codeHighlighting.TextEditorHighlightingPassRegistrar
import com.intellij.json.psi.JsonFile
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile

/**
 * 注册 [JsonRainbowKeysPass]。仅对 JSON 文件生效。
 *
 * @author Hua
 */
class JsonRainbowKeysPassFactory :
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
        if (file !is JsonFile) return null

        val settings = RainbowParenthesesSettings.getInstance()
        if (!settings.isEnabledForFileType(file.fileType)) return null
        if (!settings.isEnabledForLanguage(file.language)) return null

        if (settings.doNotRainbowifyBigFiles &&
            editor.document.lineCount > settings.bigFilesLineThreshold
        ) {
            return null
        }

        return JsonRainbowKeysPass(file, editor)
    }
}
