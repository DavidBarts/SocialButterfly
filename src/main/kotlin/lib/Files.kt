package name.blackcap.socialbutterfly.lib

import name.blackcap.socialbutterfly.Application
import java.io.File
import java.util.logging.FileHandler
import java.util.logging.Level
import java.util.logging.Logger
import java.util.logging.SimpleFormatter

private fun joinPath(base: String, vararg rest: String) = rest.fold(File(base), ::File)
private fun File.makeIfNeeded() = if (exists()) { true } else { mkdirs() }

private val SHORTNAME = Application.MYNAME.replace(" ", "").lowercase()
private val LONGNAME = "name.blackcap.$SHORTNAME"
private val HOME = System.getenv("HOME")
private val PF_DIR = when (OS.type) {
    OS.MAC -> joinPath(HOME, "Library", "Application Support", LONGNAME)
    OS.WINDOWS -> joinPath(System.getenv("APPDATA"), LONGNAME)
    else -> joinPath(HOME, ".$SHORTNAME")
}.apply {
    makeIfNeeded()
}
private val LF_DIR = when (OS.type) {
    OS.MAC -> joinPath(HOME, "Library", "Application Support", LONGNAME)
    OS.WINDOWS -> joinPath(System.getenv("LOCALAPPDATA"), LONGNAME)
    else -> joinPath(HOME, ".$SHORTNAME")
}.apply {
    makeIfNeeded()
}

val CONFIG_FILE = File(PF_DIR,"config.json.aes")
val STATE_FILE = File(PF_DIR,"state.json")
private val LOG_FILE = File(LF_DIR, "$SHORTNAME.log")
val ERR_FILE = File(LF_DIR, "$SHORTNAME.err")

val LOGGER = run {
    System.setProperty("java.util.logging.SimpleFormatter.format",
        "%1\$tFT%1\$tT%1\$tz %2\$s %4\$s: %5\$s%6\$s%n")
    Logger.getLogger(LONGNAME).apply {
        addHandler(FileHandler(LOG_FILE.toString()).apply {
            formatter = SimpleFormatter() })
        level = Level.CONFIG
        useParentHandlers = false
    }
}
