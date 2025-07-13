package name.blackcap.socialbutterfly.gui

import name.blackcap.socialbutterfly.Application
import name.blackcap.socialbutterfly.lib.ConfigState
import name.blackcap.socialbutterfly.lib.LOGGER
import name.blackcap.socialbutterfly.lib.OS
import name.blackcap.socialbutterfly.lib.makeShortcut
import java.awt.desktop.QuitResponse
import java.awt.event.KeyEvent
import java.io.IOException
import java.security.GeneralSecurityException
import java.util.logging.Level
import javax.swing.JMenu
import javax.swing.JMenuBar
import javax.swing.JMenuItem
import javax.swing.JOptionPane

class MainMenuBar: JMenuBar() {
    init {
        add(JMenu("File").apply {
            if (OS.type != OS.MAC) {
                add(JMenuItem("Quit").apply {
                    addActionListener { doQuit() }
                    makeShortcut(KeyEvent.VK_Q)
                })
            }
            add(JMenuItem("Export Configuration…").apply {
                /* NOT FINISHED */
            })
            add(JMenuItem("Import Configuration…").apply {
                /* NOT FINISHED */
            })
            add(JMenuItem("Save Configuration").apply {
                addActionListener { doSave() }
                makeShortcut(KeyEvent.VK_S)
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

private fun doSave() : Boolean {
    var failure: Exception? = null
    lateinit var message: String
    try {
        ConfigState.save(Application.decryptionKey)
    } catch (e: IOException) {
        failure = e
        message = e.message ?: "I/O error"
    } catch (e: GeneralSecurityException) {
        failure = e
        message = e.message ?: "security error"
    }
    if (failure != null)  {
        LOGGER.log(Level.SEVERE, "Unable to save config and/or state.", failure)
        JOptionPane.showMessageDialog(Application.frame, message, "Error", JOptionPane.ERROR_MESSAGE)
    }
    return failure == null
}

fun doQuit(response: QuitResponse? = null) {
    if (ConfigState.dirty) {
        val answer = JOptionPane.showConfirmDialog(
            Application.frame,
            "Unsaved changes to the configuration exist.\nSave them?",
            "Unsaved Configuration Changes",
            JOptionPane.YES_NO_CANCEL_OPTION)
        val cancel = when (answer) {
            JOptionPane.YES_OPTION -> !doSave()
            JOptionPane.CANCEL_OPTION, JOptionPane.CLOSED_OPTION -> true
            else -> false
        }
        if (cancel) {
            response?.cancelQuit()
            return
        }
    }
    Application.exit()
}
