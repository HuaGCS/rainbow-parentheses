package com.hua.rainbow_parentheses

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.EditorFactory
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
            val editors = EditorFactory.getInstance().allEditors
            editors.filter { it.project == project }.forEach { editor ->
                setupEditorListeners(editor)
            }
        }
    }

    private fun setupEditorListeners(editor: com.intellij.openapi.editor.Editor) {
        // 设置编辑器特定的监听器
        // 例如：鼠标悬停、点击事件等
    }
}