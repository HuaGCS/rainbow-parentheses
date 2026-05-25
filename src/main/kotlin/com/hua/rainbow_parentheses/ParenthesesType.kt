package com.hua.rainbow_parentheses

/**
 * @author Hua
 * @since 2025/9/2 15:27
 */
enum class ParenthesesType(val open: String, val close: String) {
    ROUND("(", ")"),
    SQUARE("[", "]"),
    CURLY("{", "}"),
    ANGLE("<", ">");

    fun isOpen(ch: Char): Boolean = ch == open[0]
    fun isClose(ch: Char): Boolean = ch == close[0]

    companion object {
        fun fromChar(ch: Char): ParenthesesType? = when (ch) {
            '(', ')' -> ROUND
            '[', ']' -> SQUARE
            '{', '}' -> CURLY
            '<', '>' -> ANGLE
            else -> null
        }
    }
}