package name.blackcap.socialbutterfly.lib

import java.awt.Toolkit
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import javax.swing.JMenuItem
import javax.swing.KeyStroke

fun JMenuItem.makeShortcut(key: Int): Unit {
    val SC_KEY_MASK = Toolkit.getDefaultToolkit().menuShortcutKeyMaskEx
    setAccelerator(KeyStroke.getKeyStroke(key, SC_KEY_MASK))
}

private val FORMATTER = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.LONG)
private val ZONE_ID = ZoneId.systemDefault()
fun Instant.toLocalizedString() : String =
    FORMATTER.format(ZonedDateTime.ofInstant(this, ZONE_ID))
