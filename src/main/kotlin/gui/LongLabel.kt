package name.blackcap.socialbutterfly.gui

import javax.swing.JTextArea
import javax.swing.UIManager

class LongLabel : JTextArea() {
    init {
        wrapStyleWord = true
        lineWrap = true
        isOpaque = false
        isEditable = false
        isFocusable = false
        background = UIManager.getColor("Label.background")
        font = UIManager.getFont("Label.font")
        border = UIManager.getBorder("Label.border")
    }
}
