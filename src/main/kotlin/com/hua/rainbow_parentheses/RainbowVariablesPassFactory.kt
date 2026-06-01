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
 * 注册 [RainbowVariablesPass]。文件类型 / 语言被排除、或属于大文件时返回 null。
 *
 * 不在此处按 [RainbowParenthesesSettings.enableRainbowVariables] 拦截：pass 始终创建，
 * 在 [RainbowVariablesPass] 内部按该开关产出空高亮，从而在关闭时能清除既有着色。
 *
 * @author Hua
 */
class RainbowVariablesPassFactory :
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

        // 已由作用域感知的 PSI 实现接管的语言，词法版让位
        if (PsiScopeRainbowLanguages.handles(file.language)) return null

        if (settings.doNotRainbowifyBigFiles &&
            editor.document.lineCount > settings.bigFilesLineThreshold
        ) {
            return null
        }

        return RainbowVariablesPass(file, editor)
    }
}
