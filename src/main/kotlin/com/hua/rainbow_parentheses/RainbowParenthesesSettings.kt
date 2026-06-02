package com.hua.rainbow_parentheses

import com.intellij.lang.Language
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.fileTypes.FileType
import com.intellij.util.xmlb.XmlSerializerUtil

/**
 * @author Hua
 * @since 2025/9/2 15:31
 */
@State(
    name = "RainbowParenthesesSettings",
    storages = [Storage("rainbowParentheses.xml")]
)
class RainbowParenthesesSettings : PersistentStateComponent<RainbowParenthesesSettings> {

    var enabled: Boolean = true
    var enableRoundParentheses: Boolean = true
    var enableSquareParentheses: Boolean = true
    var enableCurlyParentheses: Boolean = true
    var enableAngleParentheses: Boolean = false
    var numberOfColors: Int = 10
    var excludedFileTypes: MutableSet<String> = mutableSetOf()
    var excludedLanguages: MutableSet<String> = mutableSetOf()
    var doNotRainbowifyBigFiles: Boolean = true
    var bigFilesLineThreshold: Int = 1000
    var enableScopeHighlighting: Boolean = true
    var showIndentGuides: Boolean = true
    var enableRainbowVariables: Boolean = false
    var enableRainbowTags: Boolean = false

    companion object {
        fun getInstance(): RainbowParenthesesSettings =
            ApplicationManager.getApplication().getService(RainbowParenthesesSettings::class.java)
    }

    override fun getState(): RainbowParenthesesSettings = this

    override fun loadState(state: RainbowParenthesesSettings) {
        XmlSerializerUtil.copyBean(state, this)
    }

    fun isEnabledForFileType(fileType: FileType): Boolean =
        enabled && !excludedFileTypes.contains(fileType.name.lowercase())

    fun isEnabledForLanguage(language: Language): Boolean =
        enabled && !excludedLanguages.contains(language.id.lowercase())

    fun isEnabledForBracketType(type: ParenthesesType): Boolean = when (type) {
        ParenthesesType.ROUND -> enableRoundParentheses
        ParenthesesType.SQUARE -> enableSquareParentheses
        ParenthesesType.CURLY -> enableCurlyParentheses
        ParenthesesType.ANGLE -> enableAngleParentheses
    }
}
