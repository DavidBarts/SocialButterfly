package name.blackcap.socialbutterfly.gui

import java.awt.Component
import java.awt.Dimension
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*

/*
 * What all of our JLists of basic object collections are based around. Note
 * that this is actually a JScrollPane containing a JList.
 */
class StdListScroller<E : Any> private constructor(val jList: JList<E>) : JScrollPane(jList) {

    private var isNarrow: Boolean = false

    inner class HighlightingWrapper(private val wrapped: ListCellRenderer<E>) : ListCellRenderer<E> {
        override fun getListCellRendererComponent(
            list: JList<out E>?,
            value: E,
            index: Int,
            isSelected: Boolean,
            cellHasFocus: Boolean
        ): Component {
            val ret = wrapped.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
            if (isSelected) {
                ret.background = list!!.selectionBackground
                ret.foreground = list.selectionForeground
            } else {
                ret.background = list!!.background
                ret.foreground = list.foreground
            }
            return ret
        }
    }

    inner class DoubleClickListener(private val doubleClickHandler: (E) -> Unit) : MouseAdapter() {
        override fun mouseClicked(e: MouseEvent?) {
            if (e?.clickCount == 2) {
                val index = jList.locationToIndex(e.point)
                if (index >= 0) {
                    val listModel = jList.model as DefaultListModel<E>
                    val obj = listModel.get(index)
                    doubleClickHandler(obj)
                }
            }
        }
    }

    class PopupMenuListener(private val popupMenu: JPopupMenu) : MouseAdapter() {
        override fun mousePressed(e: MouseEvent?) {
            if (e?.isPopupTrigger == true) {
                popupMenu.show(e.component, e.x, e.y)
            }
        }
    }

    fun withDoubleClickHandler(handler: (E) -> Unit) : StdListScroller<E> {
        jList.addMouseListener(DoubleClickListener(handler))
        return this
    }

    init {
        verticalScrollBarPolicy = VERTICAL_SCROLLBAR_ALWAYS
        /* the following is minorly annoying, but majorly simplifies our code */
        jList.selectionMode = ListSelectionModel.SINGLE_SELECTION
    }

    fun withCellRenderer(renderer: ListCellRenderer<E>) : StdListScroller<E> {
        jList.cellRenderer = HighlightingWrapper(renderer)
        return this
    }

    fun withPopupMenu(popupMenu: JPopupMenu) : StdListScroller<E> {
        jList.addMouseListener(PopupMenuListener(popupMenu))
        return this
    }

    override fun getMaximumSize(): Dimension {
        val stdMaxSize = super.getMaximumSize()
        return if (isNarrow) { Dimension(preferredSize.width, stdMaxSize.height) } else { stdMaxSize }
    }

    fun makeNarrow() : StdListScroller<E> {
        preferredSize = Dimension(NARROW_WIDTH, preferredSize.height)
        isNarrow = true
        return this
    }

    companion object {
        const val NARROW_WIDTH = 215  /* approx 1/3 of Application.PREFERRED_WIDTH */

        fun <T: Any>forListModel(model: DefaultListModel<T>) : StdListScroller<T> {
            return StdListScroller(JList(model))
        }
    }
}
