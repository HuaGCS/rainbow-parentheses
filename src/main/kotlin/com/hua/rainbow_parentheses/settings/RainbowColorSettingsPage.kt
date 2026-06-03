package com.hua.rainbow_parentheses.settings

import com.hua.rainbow_parentheses.ParenthesesType
import com.hua.rainbow_parentheses.RainbowColorsManager
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.PlainSyntaxHighlighter
import com.intellij.openapi.options.colors.AttributesDescriptor
import com.intellij.openapi.options.colors.ColorDescriptor
import com.intellij.openapi.options.colors.ColorSettingsPage
import javax.swing.Icon

class RainbowColorSettingsPage : ColorSettingsPage {

    companion object {
        private val TAG_NAMES = mapOf(
            'r' to "Round Brackets",
            's' to "Square Brackets",
            'c' to "Curly Brackets",
            'a' to "Angle Brackets"
        )

        private val TAG_TO_TYPE: Map<Char, ParenthesesType> = mapOf(
            'r' to ParenthesesType.ROUND,
            's' to ParenthesesType.SQUARE,
            'c' to ParenthesesType.CURLY,
            'a' to ParenthesesType.ANGLE
        )

        private val DEMO_TEXT = """
// Rainbow Parentheses - Color Preview
// Each bracket pair is colored by nesting level (1-10)

// ---------- Round Brackets Level 1-10 ----------
<r0>(</r0><r1>(</r1><r2>(</r2><r3>(</r3><r4>(</r4><r5>(</r5><r6>(</r6><r7>(</r7><r8>(</r8><r9>(</r9>round<r9>)</r9><r8>)</r8><r7>)</r7><r6>)</r6><r5>)</r5><r4>)</r4><r3>)</r3><r2>)</r2><r1>)</r1><r0>)</r0>

// ---------- Square Brackets Level 1-10 ----------
<s0>[</s0><s1>[</s1><s2>[</s2><s3>[</s3><s4>[</s4><s5>[</s5><s6>[</s6><s7>[</s7><s8>[</s8><s9>[</s9>square<s9>]</s9><s8>]</s8><s7>]</s7><s6>]</s6><s5>]</s5><s4>]</s4><s3>]</s3><s2>]</s2><s1>]</s1><s0>]</s0>

// ---------- Curly Brackets Level 1-10 ----------
<c0>{</c0><c1>{</c1><c2>{</c2><c3>{</c3><c4>{</c4><c5>{</c5><c6>{</c6><c7>{</c7><c8>{</c8><c9>{</c9>curly<c9>}</c9><c8>}</c8><c7>}</c7><c6>}</c6><c5>}</c5><c4>}</c4><c3>}</c3><c2>}</c2><c1>}</c1><c0>}</c0>

// ---------- Angle Brackets Level 1-10 ----------
<a0><</a0><a1><</a1><a2><</a2><a3><</a3><a4><</a4><a5><</a5><a6><</a6><a7><</a7><a8><</a8><a9><</a9>angle<a9>></a9><a8>></a8><a7>></a7><a6>></a6><a5>></a5><a4>></a4><a3>></a3><a2>></a2><a1>></a1><a0>></a0>

// ---------- Mixed Brackets ----------
<r0>(</r0><s1>[</s1><c2>{</c2><a3><</a3><r4>(</r4>mixed<r4>)</r4><a3>></a3><c2>}</c2><s1>]</s1><r0>)</r0>

// ---------- Variables (colored by name hash; same name -> same color) ----------
<v0>alpha</v0> <v1>beta</v1> <v2>gamma</v2> <v3>delta</v3> <v4>epsilon</v4> <v5>zeta</v5> <v6>eta</v6> <v7>theta</v7> <v8>iota</v8> <v9>kappa</v9>

// ---------- Tags (XML / HTML tag names colored by nesting depth 1-10) ----------
<<t0>level0</t0>> <<t1>level1</t1>> <<t2>level2</t2>> <<t3>level3</t3>> <<t4>level4</t4>> <<t5>level5</t5>> <<t6>level6</t6>> <<t7>level7</t7>> <<t8>level8</t8>> <<t9>level9</t9>>

// ---------- Keys (JSON / YAML key names colored by nesting depth 1-10) ----------
<k0>depth0</k0>: <k1>depth1</k1>: <k2>depth2</k2>: <k3>depth3</k3>: <k4>depth4</k4>: <k5>depth5</k5>: <k6>depth6</k6>: <k7>depth7</k7>: <k8>depth8</k8>: <k9>depth9</k9>:
        """.trimIndent()

        private val DESCRIPTORS: Array<AttributesDescriptor> by lazy {
            val brackets = TAG_NAMES.flatMap { (tag, groupName) ->
                val type = TAG_TO_TYPE[tag]!!
                RainbowColorsManager.getColorKeys(type).mapIndexed { index, key ->
                    AttributesDescriptor("$groupName//Level ${index + 1}", key)
                }
            }
            val variables = RainbowColorsManager.getVariableColorKeys().mapIndexed { index, key ->
                AttributesDescriptor("Variables//Color ${index + 1}", key)
            }
            val tags = RainbowColorsManager.getTagColorKeys().mapIndexed { index, key ->
                AttributesDescriptor("Tags//Depth ${index + 1}", key)
            }
            val keys = RainbowColorsManager.getKeyColorKeys().mapIndexed { index, key ->
                AttributesDescriptor("Keys//Depth ${index + 1}", key)
            }
            (brackets + variables + tags + keys).toTypedArray()
        }

        private val TAG_DESCRIPTOR_MAP: Map<String, TextAttributesKey> by lazy {
            val brackets = TAG_TO_TYPE.flatMap { (tag, type) ->
                RainbowColorsManager.getColorKeys(type).mapIndexed { index, key ->
                    "$tag$index" to key
                }
            }
            val variables = RainbowColorsManager.getVariableColorKeys().mapIndexed { index, key ->
                "v$index" to key
            }
            val tags = RainbowColorsManager.getTagColorKeys().mapIndexed { index, key ->
                "t$index" to key
            }
            val keys = RainbowColorsManager.getKeyColorKeys().mapIndexed { index, key ->
                "k$index" to key
            }
            (brackets + variables + tags + keys).toMap()
        }
    }

    override fun getDisplayName(): String = "Rainbow Parentheses"

    override fun getIcon(): Icon? = null

    override fun getHighlighter() = PlainSyntaxHighlighter()

    override fun getDemoText(): String = DEMO_TEXT

    override fun getAdditionalHighlightingTagToDescriptorMap(): Map<String, TextAttributesKey> = TAG_DESCRIPTOR_MAP

    override fun getAttributeDescriptors(): Array<AttributesDescriptor> = DESCRIPTORS

    override fun getColorDescriptors(): Array<ColorDescriptor> = ColorDescriptor.EMPTY_ARRAY
}
