package name.blackcap.socialbutterfly.gui

import name.blackcap.socialbutterfly.Application
import name.blackcap.socialbutterfly.lib.OS
import name.blackcap.socialbutterfly.lib.makeShortcut
import java.awt.event.KeyEvent
import javax.swing.JMenu
import javax.swing.JMenuBar
import javax.swing.JMenuItem
import javax.swing.JOptionPane

class MainMenuBar: JMenuBar() {
    init {
        add(JMenu("File").apply {
            if (OS.type != OS.MAC) {
                add(JMenuItem("Quit").apply {
                    addActionListener { Application.exit() }
                    makeShortcut(KeyEvent.VK_Q)
                })
            }
            add(JMenuItem("Import Configuration…").apply {
                /* NOT FINISHED */
            })
            add(JMenuItem("Export Configuration…").apply {
                /* NOT FINISHED */
            })
        })
        if (OS.type != OS.MAC) {
            add(JMenu("Help").apply {
                add(JMenuItem("About ${Application.MYNAME}…").apply {
                    addActionListener { showAboutDialog() }
                })
            })
        }
    }
}

fun showAboutDialog() {
    JOptionPane.showMessageDialog(
        Application.frame,
        "Social Butterfly social media manager.\n"
                + "© MMXXV, David W. Barts",
        "About Social Butterfly",
        JOptionPane.PLAIN_MESSAGE)
}
