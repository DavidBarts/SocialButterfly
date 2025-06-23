package name.blackcap.socialbutterfly.gui

import javax.swing.BorderFactory
import javax.swing.BoxLayout
import javax.swing.JLabel
import javax.swing.JPanel

class TwoLineElement(titleString: String, subTitleString: String) : JPanel() {
    val title = JLabel(titleString)
    val subTitle = JLabel(subTitleString)

    init {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        border = BorderFactory.createEmptyBorder(BW, 0, BW, 0)
        title.font = title.font.deriveFont(title.font.size2D * 1.5f)
        add(title)
        add(subTitle)
    }

    companion object {
        const val BW = 4
    }

}
