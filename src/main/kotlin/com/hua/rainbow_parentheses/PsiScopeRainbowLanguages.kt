package com.hua.rainbow_parentheses

import com.intellij.lang.Language

/**
 * 记录哪些语言的变量着色已由"作用域感知"的 PSI 实现（annotator）接管。
 *
 * 对这些语言，语言无关的词法版 [RainbowVariablesPass] 让位，避免同一标识符被两套逻辑重复着色。
 * 随着后续按语言精修，往 [handledIds] 里添加对应 language id 即可。
 *
 * @author Hua
 */
object PsiScopeRainbowLanguages {

    private val handledIds: Set<String> = hashSetOf("JAVA")

    fun handles(language: Language): Boolean {
        var current: Language? = language
        while (current != null) {
            if (current.id in handledIds) return true
            current = current.baseLanguage
        }
        return false
    }
}
