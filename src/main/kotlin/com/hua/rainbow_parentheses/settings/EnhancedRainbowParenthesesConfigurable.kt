package com.hua.rainbow_parentheses.settings

import com.hua.rainbow_parentheses.RainbowParenthesesSettings
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.ProjectManager
import com.intellij.ui.IdeBorderFactory
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.FlowLayout
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JSpinner
import javax.swing.SpinnerNumberModel

/**
 * Settings → Tools → Rainbow Parentheses 设置页。
 *
 * 所有开关与设置项分块呈现：主开关、括号类型、缩进线与作用域、颜色、性能、排除。
 *
 * @author Hua
 * @since 2025/9/2 15:47
 */
class EnhancedRainbowParenthesesConfigurable : Configurable {

    private lateinit var enabledCheckBox: JBCheckBox
    private lateinit var roundParenthesesCheckBox: JBCheckBox
    private lateinit var squareParenthesesCheckBox: JBCheckBox
    private lateinit var curlyParenthesesCheckBox: JBCheckBox
    private lateinit var angleParenthesesCheckBox: JBCheckBox

    private lateinit var numberOfColorsSpinner: JSpinner

    private lateinit var doNotRainbowifyBigFilesCheckBox: JBCheckBox
    private lateinit var bigFilesLineThresholdSpinner: JSpinner

    private lateinit var enableScopeHighlightingCheckBox: JBCheckBox
    private lateinit var showIndentGuidesCheckBox: JBCheckBox
    private lateinit var enableRainbowVariablesCheckBox: JBCheckBox
    private lateinit var enableRainbowTagsCheckBox: JBCheckBox

    private lateinit var excludedFileTypesField: JBTextField
    private lateinit var excludedLanguagesField: JBTextField

    private var myMainPanel: JPanel? = null

    override fun getDisplayName(): String = "Rainbow Parentheses"

    override fun createComponent(): JComponent? {
        if (myMainPanel == null) {
            myMainPanel = createMainPanel()
        }
        return myMainPanel
    }

    private fun createMainPanel(): JPanel {
        initControls()

        // 用 FormBuilder 统一排版：所有行强制左对齐、宽度撑满
        val form = FormBuilder.createFormBuilder()
            .addComponent(enabledCheckBox)
            .addVerticalGap(8)
            .addComponent(section("括号类型（按嵌套层级着色）", bracketsPanel()))
            .addComponent(section("标识符", identifiersPanel()))
            .addComponent(section("缩进线与作用域", indentScopePanel()))
            .addComponent(section("颜色", colorsPanel()))
            .addComponent(section("性能", performancePanel()))
            .addComponent(section("排除", exclusionsPanel()))
            .addComponentFillVertically(JPanel(), 0)
            .panel

        val root = JPanel(BorderLayout())
        root.border = JBUI.Borders.empty(8)
        root.add(form, BorderLayout.CENTER)
        return root
    }

    private fun initControls() {
        enabledCheckBox = JBCheckBox("启用 Rainbow Parentheses（总开关）")

        roundParenthesesCheckBox = JBCheckBox("圆括号 ()")
        squareParenthesesCheckBox = JBCheckBox("方括号 []")
        curlyParenthesesCheckBox = JBCheckBox("花括号 {}")
        angleParenthesesCheckBox = JBCheckBox("尖括号 <>（基于文本匹配，对泛型/比较运算符可能误判，默认关闭）")

        numberOfColorsSpinner = JSpinner(SpinnerNumberModel(10, 1, 10, 1))

        doNotRainbowifyBigFilesCheckBox = JBCheckBox("不对大文件着色")
        bigFilesLineThresholdSpinner = JSpinner(SpinnerNumberModel(1000, 100, 100000, 100))

        enableScopeHighlightingCheckBox = JBCheckBox("启用作用域高亮（Ctrl+鼠标右键 点击括号内）")
        showIndentGuidesCheckBox = JBCheckBox("显示彩虹缩进线（替换原生缩进线，光标所在块加亮）")
        enableRainbowVariablesCheckBox =
            JBCheckBox("按名字给标识符着色（同名同色；基于词法，类型/方法名也会着色，默认关闭）")
        enableRainbowTagsCheckBox =
            JBCheckBox("按嵌套深度给 XML / HTML 标签名着色（默认关闭）")

        excludedFileTypesField = JBTextField()
        excludedLanguagesField = JBTextField()
    }

    private fun section(title: String, content: JComponent): JPanel {
        val panel = JPanel(BorderLayout())
        panel.border = IdeBorderFactory.createTitledBorder(title, false)
        panel.add(content, BorderLayout.CENTER)
        return panel
    }

    private fun bracketsPanel(): JComponent {
        val row = JPanel(FlowLayout(FlowLayout.LEFT, 12, 0))
        row.add(roundParenthesesCheckBox)
        row.add(squareParenthesesCheckBox)
        row.add(curlyParenthesesCheckBox)
        val angleRow = JPanel(FlowLayout(FlowLayout.LEFT, 0, 0))
        angleRow.add(angleParenthesesCheckBox)
        return FormBuilder.createFormBuilder()
            .addComponent(row)
            .addComponent(angleRow)
            .panel
    }

    private fun identifiersPanel(): JComponent =
        FormBuilder.createFormBuilder()
            .addComponent(enableRainbowVariablesCheckBox)
            .addComponent(enableRainbowTagsCheckBox)
            .panel

    private fun indentScopePanel(): JComponent =
        FormBuilder.createFormBuilder()
            .addComponent(showIndentGuidesCheckBox)
            .addComponent(enableScopeHighlightingCheckBox)
            .panel

    private fun colorsPanel(): JComponent =
        FormBuilder.createFormBuilder()
            .addLabeledComponent("颜色层数：", numberOfColorsSpinner)
            .addComponent(JBLabel("具体颜色可在 Settings → Editor → Color Scheme → Rainbow Parentheses 中调整"))
            .panel

    private fun performancePanel(): JComponent =
        FormBuilder.createFormBuilder()
            .addComponent(doNotRainbowifyBigFilesCheckBox)
            .addLabeledComponent("    行数阈值：", bigFilesLineThresholdSpinner)
            .panel

    private fun exclusionsPanel(): JComponent =
        FormBuilder.createFormBuilder()
            .addLabeledComponent("排除的文件类型（逗号分隔）：", excludedFileTypesField)
            .addLabeledComponent("排除的语言（逗号分隔，填语言 ID）：", excludedLanguagesField)
            .panel

    override fun isModified(): Boolean {
        val settings = RainbowParenthesesSettings.getInstance()

        return enabledCheckBox.isSelected != settings.enabled
                || roundParenthesesCheckBox.isSelected != settings.enableRoundParentheses
                || squareParenthesesCheckBox.isSelected != settings.enableSquareParentheses
                || curlyParenthesesCheckBox.isSelected != settings.enableCurlyParentheses
                || angleParenthesesCheckBox.isSelected != settings.enableAngleParentheses
                || (numberOfColorsSpinner.value as Int) != settings.numberOfColors
                || doNotRainbowifyBigFilesCheckBox.isSelected != settings.doNotRainbowifyBigFiles
                || (bigFilesLineThresholdSpinner.value as Int) != settings.bigFilesLineThreshold
                || enableScopeHighlightingCheckBox.isSelected != settings.enableScopeHighlighting
                || showIndentGuidesCheckBox.isSelected != settings.showIndentGuides
                || enableRainbowVariablesCheckBox.isSelected != settings.enableRainbowVariables
                || enableRainbowTagsCheckBox.isSelected != settings.enableRainbowTags
                || excludedFileTypesField.text != settings.excludedFileTypes.joinToString(",")
                || excludedLanguagesField.text != settings.excludedLanguages.joinToString(",")
    }

    override fun apply() {
        val settings = RainbowParenthesesSettings.getInstance()

        settings.enabled = enabledCheckBox.isSelected
        settings.enableRoundParentheses = roundParenthesesCheckBox.isSelected
        settings.enableSquareParentheses = squareParenthesesCheckBox.isSelected
        settings.enableCurlyParentheses = curlyParenthesesCheckBox.isSelected
        settings.enableAngleParentheses = angleParenthesesCheckBox.isSelected
        settings.numberOfColors = numberOfColorsSpinner.value as Int
        settings.doNotRainbowifyBigFiles = doNotRainbowifyBigFilesCheckBox.isSelected
        settings.bigFilesLineThreshold = bigFilesLineThresholdSpinner.value as Int
        settings.enableScopeHighlighting = enableScopeHighlightingCheckBox.isSelected
        settings.showIndentGuides = showIndentGuidesCheckBox.isSelected
        settings.enableRainbowVariables = enableRainbowVariablesCheckBox.isSelected
        settings.enableRainbowTags = enableRainbowTagsCheckBox.isSelected

        settings.excludedFileTypes.clear()
        excludedFileTypesField.text.trim().split(",")
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }
            .forEach { settings.excludedFileTypes.add(it) }

        settings.excludedLanguages.clear()
        excludedLanguagesField.text.trim().split(",")
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }
            .forEach { settings.excludedLanguages.add(it) }

        ApplicationManager.getApplication().invokeLater {
            ProjectManager.getInstance().openProjects.forEach { project ->
                DaemonCodeAnalyzer.getInstance(project).restart()
            }
        }
    }

    override fun reset() {
        val settings = RainbowParenthesesSettings.getInstance()

        enabledCheckBox.isSelected = settings.enabled
        roundParenthesesCheckBox.isSelected = settings.enableRoundParentheses
        squareParenthesesCheckBox.isSelected = settings.enableSquareParentheses
        curlyParenthesesCheckBox.isSelected = settings.enableCurlyParentheses
        angleParenthesesCheckBox.isSelected = settings.enableAngleParentheses
        numberOfColorsSpinner.value = settings.numberOfColors
        doNotRainbowifyBigFilesCheckBox.isSelected = settings.doNotRainbowifyBigFiles
        bigFilesLineThresholdSpinner.value = settings.bigFilesLineThreshold
        enableScopeHighlightingCheckBox.isSelected = settings.enableScopeHighlighting
        showIndentGuidesCheckBox.isSelected = settings.showIndentGuides
        enableRainbowVariablesCheckBox.isSelected = settings.enableRainbowVariables
        enableRainbowTagsCheckBox.isSelected = settings.enableRainbowTags
        excludedFileTypesField.text = settings.excludedFileTypes.joinToString(",")
        excludedLanguagesField.text = settings.excludedLanguages.joinToString(",")
    }
}
