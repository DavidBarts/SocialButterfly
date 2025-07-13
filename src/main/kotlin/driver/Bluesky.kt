package name.blackcap.socialbutterfly.driver

import name.blackcap.socialbutterfly.Application
import name.blackcap.socialbutterfly.jschema.*
import name.blackcap.socialbutterfly.lib.ConfigState
import java.util.*
import javax.swing.JOptionPane

object Bluesky: Driver {
    override val NAME = "Bluesky"

    override fun createPlatform(): Unit {
        val exists = ConfigState.config.platforms.values.firstOrNull { it is BlueskyPlatform } != null
        if (exists) {
            JOptionPane.showMessageDialog(
                Application.frame,
                "Bluesky platform object already exists.",
                "Error",
                JOptionPane.ERROR_MESSAGE)
        } else {
            Application.platformsConnector.add(UUID.randomUUID().toString(), BlueskyPlatform())
            Application.platformsPanel.run {
                revalidate()
                repaint()
            }
        }
    }

    override fun editPlatform(platform: Platform): Unit {
        val blueskyPlatform = platform as BlueskyPlatform
        TODO("Not yet implemented")
    }

    override fun createCredentials(platform: Platform): Unit {
        val blueskyPlatform = platform as BlueskyPlatform
        TODO("Not yet implemented")
    }

    override fun editCredentials(platform: Platform, credentials: Credentials): Unit {
        val blueskyPlatform = platform as BlueskyPlatform
        val blueskyCredentials = credentials as BlueskyCredentials
        TODO("Not yet implemented")
    }

    override fun createPost(platform: Platform, credentials: Credentials, post: Post): Unit {
        val blueskyPlatform = platform as BlueskyPlatform
        val blueskyCredentials = credentials as BlueskyCredentials
        TODO("Not yet implemented")
    }
}