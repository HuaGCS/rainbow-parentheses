package com.hua.rainbow_parentheses.actions

import com.hua.rainbow_parentheses.RainbowParenthesesSettings
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages

/**
 * @author Hua
 * @since 2025/9/2 15:35
 */
class ToggleRainbowParenthesesAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        val settings = RainbowParenthesesSettings.Companion.getInstance()
        val newState = !settings.enabled
        settings.enabled = newState

        // 刷新代码高亮
        DaemonCodeAnalyzer.getInstance(project).restart()

        Messages.showInfoMessage(
            project,
            "Rainbow Parentheses ${if (newState) "已启用" else "已禁用"}",
            "Rainbow Parentheses"
        )
    }

    override fun update(e: AnActionEvent) {
        val project = e.project
        e.presentation.isEnabledAndVisible = project != null

        val enabled = RainbowParenthesesSettings.Companion.getInstance().enabled
        e.presentation.text = if (enabled) "禁用 Rainbow Parentheses" else "启用 Rainbow Parentheses"
    }
}