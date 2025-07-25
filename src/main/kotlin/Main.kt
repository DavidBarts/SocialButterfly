package name.blackcap.socialbutterfly

import io.ktor.utils.io.errors.*
import name.blackcap.socialbutterfly.driver.getPlatformName
import name.blackcap.socialbutterfly.gui.*
import name.blackcap.socialbutterfly.jschema.*
import name.blackcap.socialbutterfly.lib.*
import java.awt.Desktop
import java.awt.Dimension
import java.security.GeneralSecurityException
import java.time.Instant
import java.util.logging.Level
import javax.swing.*
import kotlin.system.exitProcess

object Application {
    const val MYNAME = "Social Butterfly"
    val CLIENTNAME = MYNAME.replace(" ", "")

    private const val NBSP = "\u00a0"

    /* if the following are changed, widths in some of our widgets in gui
       package may also need changing */
    const val PREFERRED_WIDTH = 640
    const val PREFERRED_HEIGHT = 480

    var frame: JFrame by setOnce()
    var platformsConnector: MapConnector<Platform, String> by setOnce()
    var platformsPanel: PlatformsTabbedPanePanel by setOnce()
    var channelsConnector: MapConnector<Channel, String> by setOnce()
    var channelsScroller: StdListScroller<MapConnector<Channel, String>.ListModelEntry> by setOnce()
    var distsConnector: MapConnector<Distribution, String> by setOnce()
    var distsScroller: StdListScroller<MapConnector<Distribution, String>.ListModelEntry> by setOnce()
    var postsConnector: MapConnector<Post, Instant> by setOnce()
    var postsScroller: StdListScroller<MapConnector<Post, Instant>.ListModelEntry> by setOnce()
    var failuresConnector: SetConnector<Failure, Instant> by setOnce()
    var failuresScroller: StdListScroller<SetConnector<Failure, Instant>.ListModelEntry> by setOnce()
    lateinit var decryptionKey: CharArray

    fun initialize() {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
        if (OS.type == OS.MAC) {
            setUpMacMenus()
        }

        /* adding this forces the frame to be preferred size */
        val dummyLabel = JLabel(NBSP).apply {
            horizontalAlignment = JLabel.CENTER
            verticalAlignment = JLabel.CENTER
        }
        /* start by making an empty frame, which we need in order to show a dialog */
        frame = JFrame(MYNAME).apply {
            jMenuBar = MainMenuBar()
            preferredSize = Dimension(PREFERRED_WIDTH, PREFERRED_HEIGHT)
            defaultCloseOperation = WindowConstants.DISPOSE_ON_CLOSE
            contentPane.add(dummyLabel)
            validate()
            pack()
            isVisible = true
        }

        /* obtain the needed decryption/encryption key and config data */
        while (true) {
            decryptionKey = getKey()
            try {
                ConfigState.load(decryptionKey)
            } catch (e: GeneralSecurityException) {
                errorDialog("Unable to decrypt config. Please reenter key.")
                continue
            } catch (e: IOException) {
                val fatal = "Fatal error reading config and/or state files."
                LOGGER.log(Level.SEVERE, fatal, e)
                errorDialog(fatal)
                exit(1)
            }
            break
        }

        /* now that we have the saved data, use it to create the GUI objects */
        platformsConnector = MapConnector(ConfigState.config.platforms) { _, p -> getPlatformName(p).lowercase() }
        platformsPanel = PlatformsTabbedPanePanel(ConfigState.config.platforms)
        channelsConnector = MapConnector(ConfigState.config.channels) { _, c -> c.credentials.username.lowercase() }
        channelsScroller = StdListScroller.forListModel(channelsConnector.listModel)
            .makeNarrow()
            .withCellRenderer(ChannelRenderer(ConfigState.config.platforms))
        distsConnector = MapConnector(ConfigState.config.distributions) { _, d -> d.name.lowercase() }
        distsScroller = StdListScroller.forListModel(distsConnector.listModel)
            .makeNarrow()
            .withCellRenderer(DistributionRenderer())
        postsConnector = MapConnector(ConfigState.state.posts) { _, p -> p.created }
        postsScroller = StdListScroller.forListModel(postsConnector.listModel)
            .withCellRenderer(PostRenderer())
        failuresConnector = SetConnector(ConfigState.state.failures) { it.created }
        failuresScroller = StdListScroller.forListModel(failuresConnector.listModel)
            .withCellRenderer(FailureRenderer(ConfigState.state.posts))

        /* then display them */
        frame.contentPane.apply {
            remove(dummyLabel)
            add(
                JTabbedPane().apply {
                    addTab("Platforms", platformsPanel)
                    addTab("Channels", tabbedPanePanelFor(channelsScroller))
                    addTab("Distributions", tabbedPanePanelFor(distsScroller))
                    addTab("Posts", tabbedPanePanelFor(postsScroller))
                    addTab("Failures", tabbedPanePanelFor(failuresScroller))
                }
            )
        }

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
            keyDialog.reset()
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

    private fun setUpMacMenus() {
    Desktop.getDesktop().run {
        setAboutHandler { showAboutDialog() }
        setQuitHandler { _, response -> doQuit(response) }
        /* setPreferencesHandler(NOT FINISHED) */
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
