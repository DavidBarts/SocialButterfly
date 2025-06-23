package name.blackcap.socialbutterfly

import io.ktor.utils.io.errors.*
import name.blackcap.socialbutterfly.gui.*
import name.blackcap.socialbutterfly.jschema.*
import name.blackcap.socialbutterfly.lib.*
import java.awt.Desktop
import java.awt.Dimension
import java.awt.desktop.QuitStrategy
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.awt.font.TextAttribute
import java.security.GeneralSecurityException
import java.time.Instant
import java.util.logging.Level
import javax.swing.*
import javax.swing.plaf.FontUIResource
import kotlin.system.exitProcess

var config: Config by setOnce()
var state: State by setOnce()

object Application {
    const val MYNAME = "Social Butterfly"
    const val PREFERRED_WIDTH = 640
    const val PREFERRED_HEIGHT = 480

    var frame: JFrame by setOnce()
    var platformsConnector: MapConnector<Platform, String> by setOnce()
    var platformsScroller: StdListScroller<MapConnector<Platform, String>.ListModelEntry> by setOnce()
    var channelsConnector: MapConnector<Channel, String> by setOnce()
    var channelsScroller: StdListScroller<MapConnector<Channel, String>.ListModelEntry> by setOnce()
    var distsConnector: MapConnector<Distribution, String> by setOnce()
    var distsScroller: StdListScroller<MapConnector<Distribution, String>.ListModelEntry> by setOnce()
    var postsConnector: MapConnector<Post, Instant> by setOnce()
    var postsScroller: StdListScroller<MapConnector<Post, Instant>.ListModelEntry> by setOnce()
    var failuresConnector: SetConnector<Failure, Instant> by setOnce()
    var failuresScroller: StdListScroller<SetConnector<Failure, Instant>.ListModelEntry> by setOnce()
    var decryptionKey: CharArray by setOnce()

    fun initialize() {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
        if (OS.type == OS.MAC) {
            setUpMacFonts()
            setUpMacMenus()
        }

        /* start by making an empty frame, which we need in order to show a dialog */
        frame = JFrame(MYNAME).apply {
            jMenuBar = MainMenuBar()
            preferredSize = Dimension(PREFERRED_WIDTH, PREFERRED_HEIGHT)
            defaultCloseOperation = WindowConstants.DISPOSE_ON_CLOSE
            isVisible = true
        }

        /* obtain the needed decryption/encryption key and config data */
        while (true) {
            decryptionKey = getKey()
            try {
                loadConfigState(decryptionKey).run { config = first; state = second }
            } catch (e: GeneralSecurityException) {
                errorDialog("Unable to decrypt config. Please reenter key.")
                continue
            } catch (e: IOException) {
                val fatal = "Fatal error reading config and/or state files."
                LOGGER.log(Level.SEVERE, fatal, e)
                errorDialog(fatal)
                Application.exit(1)
            }
            break
        }

        /* try and make sure key gets destroyed on exit */
        frame.addWindowListener(object : WindowAdapter() {
            override fun windowClosed(e: WindowEvent?) {
                Application.exit()
            }
        })

        /* now that we have the saved data, use it to create the GUI objects */
        platformsConnector = MapConnector(config.platforms) { _, p -> getPlatformName(p::class).lowercase() }
        platformsScroller = StdListScroller.forListModel(platformsConnector.listModel)
            .withCellRenderer(PlatformRenderer())
        channelsConnector = MapConnector(config.channels) { _, c -> c.credentials.username.lowercase() }
        channelsScroller = StdListScroller.forListModel(channelsConnector.listModel)
            .withCellRenderer(ChannelRenderer(config.platforms))
        distsConnector = MapConnector(config.distributions) { _, d -> d.name.lowercase() }
        distsScroller = StdListScroller.forListModel(distsConnector.listModel)
            .withCellRenderer(DistributionRenderer())
        postsConnector = MapConnector(state.posts) { _, p -> p.created }
        postsScroller = StdListScroller.forListModel(postsConnector.listModel)
            .withCellRenderer(PostRenderer())
        failuresConnector = SetConnector(state.failures) { it.created }
        failuresScroller = StdListScroller.forListModel(failuresConnector.listModel)
            .withCellRenderer(FailureRenderer(state.posts))

        /* then display them */
        frame.contentPane.add(
            JTabbedPane().apply {
                addTab("Platforms", tabbedPanePanelFor(platformsScroller))
                addTab("Channels", tabbedPanePanelFor(channelsScroller))
                addTab("Distributions", tabbedPanePanelFor(distsScroller))
                addTab("Posts", tabbedPanePanelFor(postsScroller))
                addTab("Failures", tabbedPanePanelFor(failuresScroller))
            }
        )

        frame.apply {
            revalidate()
            pack()
            repaint()
        }
    }

    fun errorDialog(message: String) =
        JOptionPane.showMessageDialog(frame, message, "Error", JOptionPane.ERROR_MESSAGE)

    private fun getKey(): CharArray {
        val configExists = CONFIG_FILE.exists()
        val keyDialog = KeyDialog(frame, askTwice = !configExists)
        while (true) {
            keyDialog.isVisible = true
            if (configExists) {
                if (keyDialog.key.isEmpty()) {
                    errorDialog("Please enter a key.")
                } else {
                    break
                }
            } else {
                val keysMatch = keyDialog.key.contentEquals(keyDialog.otherKey)
                if (keyDialog.key.isNotEmpty() && keysMatch) {
                    break
                } else if (!keysMatch) {
                    errorDialog("Keys do not match. Please reenter.")
                } else {
                    errorDialog("Please enter a key.")
                }
            }
        }
        val ret = keyDialog.key.clone()
        keyDialog.dispose()
        return ret
    }

    fun exit(status: Int = 0) {
        decryptionKey.clear()
        if (status == 0) {
            LOGGER.log(Level.INFO, "execution complete")
        } else {
            LOGGER.log(Level.INFO, "aborting with status $status")
        }
        exitProcess(status)
    }

/* Note that this is incomplete; there is no good, simple way to do this
   for all fonts in all UI element types. We just do it for labels (and
   anything else needed for this application) and don't worry about the
   rest. */
private fun setUpMacFonts() {
    // https://stackoverflow.com/questions/79506573/is-there-a-way-to-use-the-macos-system-font-with-fallback-for-missing-glyphs-su
    val STD_MAC_FONT = mapOf<TextAttribute, Any>(TextAttribute.FAMILY to ".AppleSystemUIFont")
    listOf<String>("Button.font", "Label.font", "TabbedPane.font").forEach {
        val oldFont = UIManager.getDefaults().getFont(it)
        if (oldFont != null) {
            val newFont = FontUIResource(oldFont.deriveFont(STD_MAC_FONT))
            UIManager.put(it, newFont)
        }
    }
}

private fun setUpMacMenus() {
    Desktop.getDesktop().run {
        setAboutHandler { showAboutDialog() }
        /* setPreferencesHandler(NOT FINISHED) */
        setQuitStrategy(QuitStrategy.CLOSE_ALL_WINDOWS)
    }
}

private fun tabbedPanePanelFor(scroller: StdListScroller<*>) =
    JPanel().apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        add(scroller)
    }
}

fun main() {
    setUpErrors()
    LOGGER.log(Level.INFO, "beginning execution")
    if (OS.type == OS.MAC) {
        System.setProperty("apple.laf.useScreenMenuBar", "true")
        System.setProperty("apple.awt.application.name", Application.MYNAME)
    }
    SwingUtilities.invokeLater {
        try {
            Application.initialize()
        } catch(e: Exception) {
            LOGGER.log(Level.SEVERE, "Unexpected fatal error", e)
            exitProcess(1)
        }
    }
}

private fun setUpErrors() {
    val ps = java.io.PrintStream(
        java.io.FileOutputStream(ERR_FILE), true, CHARSET
    )
    System.setOut(ps)
    System.setErr(ps)
}

