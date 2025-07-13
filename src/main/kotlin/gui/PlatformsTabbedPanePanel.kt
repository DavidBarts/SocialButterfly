package name.blackcap.socialbutterfly.gui

import name.blackcap.socialbutterfly.Application
import name.blackcap.socialbutterfly.driver.DRIVERS
import name.blackcap.socialbutterfly.driver.DriverDispatcher
import name.blackcap.socialbutterfly.driver.getPlatformName
import name.blackcap.socialbutterfly.jschema.Platform
import java.awt.event.ActionEvent
import javax.swing.JMenuItem
import javax.swing.JOptionPane
import javax.swing.JPopupMenu
import javax.swing.ListSelectionModel

class PlatformsTabbedPanePanel(mapping: MutableMap<String, Platform>): MapTabbedPanePanel<Platform>(mapping) {
    override val scroller = StdListScroller.forListModel(connector.listModel)
        .makeNarrow()
        .withCellRenderer(PlatformRenderer())
        .withDoubleClickHandler { doEdit(it.value) }
        .withPopupMenu(JPopupMenu().apply {
            add(deleteItem)
            add(editItem)
        })
    private val deleteItem = JMenuItem().apply { isEnabled = false }
    private val editItem = JMenuItem().apply { isEnabled = false }

    init {
        deleteItem.addActionListener { doDelete(Application.platformsConnector, scroller.jList) }
        editItem.addActionListener {
            val jList = scroller.jList
            val indices = jList.selectedIndices
            for (index in indices) {
                doEdit(jList.model.getElementAt(index).value)
            }
        }
        scroller.jList.addListSelectionListener {
            if (!deleteItem.isEnabled) {
                deleteItem.isEnabled = true
            }
            if (!editItem.isEnabled) {
                editItem.isEnabled = true
            }
        }
    }

    override fun doPlus(event: ActionEvent?) {
        val allDriverNames = DRIVERS.keys.sorted().toTypedArray()
        val selectedDriverName = JOptionPane.showInputDialog(
            Application.frame,
            "Select a platform type:",
            "Select platform type",
            JOptionPane.PLAIN_MESSAGE,
            null,
            allDriverNames,
            null)
        if (selectedDriverName != null) {
            val driver = DRIVERS[selectedDriverName]!!
            driver.createPlatform()
        }
    }

    override fun sortKeyGenerator(key: String, value: Platform): String {
        return getPlatformName(value).lowercase()
    }

    private fun doEdit(platform: Platform) {
        DriverDispatcher.editPlatform(platform)
    }
}
