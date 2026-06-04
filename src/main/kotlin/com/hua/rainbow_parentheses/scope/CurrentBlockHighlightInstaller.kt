package com.hua.rainbow_parentheses.scope

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.CaretEvent
import com.intellij.openapi.editor.event.CaretListener
import com.intellij.openapi.editor.event.EditorFactoryEvent
import com.intellij.openapi.editor.event.EditorFactoryListener
import java.util.concurrent.ConcurrentHashMap

/**
 * 应用级单例：给所有编辑器（含之后新建的）挂上光标监听，驱动 [CurrentBlockHighlighter]。
 *
 * 用应用级 [Service] 的生命周期作为 [EditorFactory] 监听器的 disposable，确保只注册一次、随应用释放。
 * 由启动活动触发实例化（[ensureInstalled]）。
 *
 * @author Hua
 */
@Service(Service.Level.APP)
class CurrentBlockHighlightInstaller : Disposable {

    private val caretListeners = ConcurrentHashMap<Editor, CaretListener>()

    init {
        val factory = EditorFactory.getInstance()
        factory.allEditors.forEach { attach(it) }
        factory.addEditorFactoryListener(object : EditorFactoryListener {
            override fun editorCreated(event: EditorFactoryEvent) = attach(event.editor)
            override fun editorReleased(event: EditorFactoryEvent) = detach(event.editor)
        }, this)
    }

    private fun attach(editor: Editor) {
        if (caretListeners.containsKey(editor)) return
        val listener = object : CaretListener {
            override fun caretPositionChanged(event: CaretEvent) = CurrentBlockHighlighter.refresh(editor)
            override fun caretAdded(event: CaretEvent) = CurrentBlockHighlighter.refresh(editor)
            override fun caretRemoved(event: CaretEvent) = CurrentBlockHighlighter.refresh(editor)
        }
        editor.caretModel.addCaretListener(listener)
        caretListeners[editor] = listener
        CurrentBlockHighlighter.refresh(editor)
    }

    private fun detach(editor: Editor) {
        caretListeners.remove(editor)?.let { editor.caretModel.removeCaretListener(it) }
        CurrentBlockHighlighter.clear(editor)
    }

    override fun dispose() {
        caretListeners.forEach { (editor, listener) -> editor.caretModel.removeCaretListener(listener) }
        caretListeners.clear()
    }

    companion object {
        /** 触发服务实例化以完成监听器注册（幂等）。 */
        fun ensureInstalled() {
            service<CurrentBlockHighlightInstaller>()
        }

        /** 设置变化后立即对所有编辑器重算（开 → 立刻显示；关 → 立刻清除）。 */
        fun refreshAllEditors() {
            EditorFactory.getInstance().allEditors.forEach { CurrentBlockHighlighter.refresh(it) }
        }
    }
}
