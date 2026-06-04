package com.hua.rainbow_parentheses

import com.hua.rainbow_parentheses.scope.CurrentBlockHighlightInstaller
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

/**
 * @author Hua
 * @since 2025/9/2 15:40
 */
class RainbowParenthesesStartupActivity : ProjectActivity {

    override suspend fun execute(project: Project) {
        initializePlugin(project)
    }

    private fun initializePlugin(project: Project) {
        RainbowColorsManager.initialize()

        ApplicationManager.getApplication().invokeLater {
            // 注册当前块持续高亮的编辑器/光标监听（应用级单例，幂等；在 EDT 上访问编辑器）
            CurrentBlockHighlightInstaller.ensureInstalled()
        }
    }
}