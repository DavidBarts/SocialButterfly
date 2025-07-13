package name.blackcap.socialbutterfly.gui

import io.ktor.client.call.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import name.blackcap.socialbutterfly.Application
import name.blackcap.socialbutterfly.lib.OS
import java.awt.Component
import java.awt.Cursor
import java.awt.Frame
import javax.swing.*

class HttpErrorDialog(owner: Frame, message: String, details: String): JDialog(owner) {

    init {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        title = "HTTP Error"
        val messageLabel = JLabel(message)
        val textArea = JTextArea(details,24, 80).apply {
            isEnabled = true
            if (OS.type == OS.WINDOWS) {
                font = font.deriveFont(messageLabel.font.size2D)
            }
        }
        val detailsPane = JScrollPane(textArea).apply {
            verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS
            horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS
            isVisible = false
        }
        add(messageLabel)
        add(detailsPane)
        add(Box(BoxLayout.X_AXIS).apply {
            add(Box.createHorizontalGlue())
            add(JButton("OK").apply {
                addActionListener { dismiss() }
            })
            add(Box.createHorizontalGlue())
            add(JButton(DETAILS).apply {
                addActionListener {
                    if (text == DETAILS) {
                        detailsPane.isVisible = true
                        text = HIDE
                    } else {
                        detailsPane.isVisible = false
                        text = DETAILS
                    }
                    redraw()
                }
            })
            add(Box.createHorizontalGlue())
        })
    }

    private fun dismiss() {
        isVisible = false
        dispose()
    }

    private fun redraw() {
        revalidate()
        repaint()
    }

    companion object {
        private const val HIDE = "Hide Details"
        private const val DETAILS = "Details…"
    }
}

suspend fun verifyResponse(dubious: HttpResponse): Boolean {
    if (!dubious.status.isSuccess()) {
        HttpErrorDialog(
            Application.frame,
            "Unexpected ${dubious.status.value} ${dubious.status.description} HTTP response.",
            dubious.bodyAsText())
    }
    return dubious.status.isSuccess()
}

fun Component.useWaitCursor() {
    this.cursor = Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR)
}

fun Component.useNormalCursor() {
    this.cursor = Cursor.getDefaultCursor()
}
