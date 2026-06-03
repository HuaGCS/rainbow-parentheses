package com.hua.rainbow_parentheses.yaml

import com.hua.rainbow_parentheses.RainbowColorsManager
import com.hua.rainbow_parentheses.RainbowParenthesesSettings
import com.hua.rainbow_parentheses.common.DepthHighlightPass
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiRecursiveElementVisitor
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.YAMLMapping

/**
 * YAML 键名按嵌套深度着色 pass。深度 = 该键外层 [YAMLMapping] 的数量 − 1（顶层键为 0）。
 *
 * @author Hua
 */
class YamlRainbowKeysPass(file: PsiFile, editor: Editor) :
    DepthHighlightPass(file, editor, KEY_HIGHLIGHTERS_KEY) {

    override fun collectHighlights(): List<Highlight> {
        val settings = RainbowParenthesesSettings.getInstance()
        if (!settings.enabled || !settings.enableRainbowKeys) return emptyList()

        val result = ArrayList<Highlight>()
        file.accept(object : PsiRecursiveElementVisitor() {
            override fun visitElement(element: PsiElement) {
                if (element is YAMLKeyValue) {
                    val depth = (ancestorDepth(element, YAMLMapping::class.java) - 1).coerceAtLeast(0)
                    highlightFor(element.key, RainbowColorsManager.getKeyColorKey(depth))
                        ?.let { result.add(it) }
                }
                super.visitElement(element)
            }
        })
        return result
    }

    private companion object {
        val KEY_HIGHLIGHTERS_KEY =
            Key.create<MutableList<RangeHighlighter>>("RAINBOW_YAML_KEY_HIGHLIGHTERS")
    }
}
