package name.blackcap.socialbutterfly.gui

import java.awt.Color
import javax.swing.BorderFactory
import javax.swing.BoxLayout
import javax.swing.JLabel
import javax.swing.JPanel

class TwoLineElement(titleString: String, subTitleString: String) : JPanel() {
    val title = JLabel(titleString)
    val subTitle = JLabel(subTitleString)

    init {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        border = BorderFactory.createMatteBorder(0, 0, 1, 0, Color.GRAY)
        title.font = title.font.deriveFont(title.font.size2D * 1.5f)
        add(title)
        add(subTitle)
    }

    companion object {
        const val BW = 4
    }

}
