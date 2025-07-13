package name.blackcap.socialbutterfly.gui

import name.blackcap.socialbutterfly.jschema.Platform
import name.blackcap.socialbutterfly.lib.MapConnector
import java.awt.event.ActionEvent
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JPanel

abstract class MapTabbedPanePanel<T: Any>(val mapping: MutableMap<String, T>): JPanel() {
    val connector = MapConnector(mapping, ::sortKeyGenerator)
    abstract val scroller: StdListScroller<MapConnector<Platform, String>.ListModelEntry>

    init {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        add(scroller)
        val plusButton = JButton("+").apply {
            addActionListener(::doPlus)
        }
        val minusButton = JButton(MINUS).apply {
            isEnabled = false
            addActionListener { doDelete(connector, scroller.jList) }
        }
        scroller.jList.addListSelectionListener {
            if (!minusButton.isEnabled) {
                minusButton.isEnabled = true
                validate()
            }
        }
        add(Box(BoxLayout.X_AXIS).apply {
            add(plusButton)
            add(minusButton)
            add(Box.createHorizontalGlue())
        })
    }

    protected abstract fun sortKeyGenerator(key: String, value: T): String

    protected abstract fun doPlus(event: ActionEvent?)

    companion object {
        private const val MINUS = "\u2212"
    }
}
