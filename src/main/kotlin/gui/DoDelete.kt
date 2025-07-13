package name.blackcap.socialbutterfly.gui

import name.blackcap.socialbutterfly.Application
import name.blackcap.socialbutterfly.lib.IndexRemovable
import javax.swing.JList
import javax.swing.JOptionPane

fun doDelete(removable: IndexRemovable, jList: JList<*>) {
    val indices = jList.selectedIndices
    val count = indices.size
    val delete = if (count > 1) {
        JOptionPane.showConfirmDialog(
            Application.frame,
            "Really delete $count items?",
            "Multiple delete",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE
        ) == JOptionPane.YES_OPTION
    } else {
        count == 1
    }
    if (delete) {
        indices.sortDescending()
        for (index in indices) {
            removable.remove(index)
        }
        jList.validate()
    }
}
