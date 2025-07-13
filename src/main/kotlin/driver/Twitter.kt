package name.blackcap.socialbutterfly.driver

import name.blackcap.socialbutterfly.Application
import name.blackcap.socialbutterfly.jschema.*
import name.blackcap.socialbutterfly.lib.ConfigState
import java.util.*
import javax.swing.JOptionPane

object Twitter : Driver {

    override val NAME = "X (Twitter)"

    override fun createPlatform(): Unit {
        val exists = ConfigState.config.platforms.values.firstOrNull { it is TwitterPlatform } != null
        if (exists) {
            JOptionPane.showMessageDialog(
                Application.frame,
                "Twitter platform object already exists.",
                "Error",
                JOptionPane.ERROR_MESSAGE
            )
            return
        }
        /* TODO: display explanation here, let user chicken out */
        val clientId = JOptionPane.showInputDialog(
            Application.frame,
            "Enter X (Twitter) client ID:",
            "Enter Client ID",
            JOptionPane.PLAIN_MESSAGE)
        Application.platformsConnector.add(UUID.randomUUID().toString(), TwitterPlatform(clientId = clientId))
        Application.platformsPanel.run {
            revalidate()
            repaint()
        }
    }

    override fun editPlatform(platform: Platform): Unit {
        val twitterPlatform = platform as TwitterPlatform
        val newClientId = JOptionPane.showInputDialog(
            Application.frame,
            "Enter new X (Twitter) client ID:",
            "Edit Client ID",
            JOptionPane.PLAIN_MESSAGE,
            null,
            null,
            twitterPlatform.clientId)
        if (newClientId != twitterPlatform.clientId) {
            twitterPlatform.clientId = newClientId as String
        }
    }

    override fun createCredentials(platform: Platform): Unit {
        val twitterPlatform = platform as TwitterPlatform
        TODO("Not yet implemented")
    }

    override fun editCredentials(platform: Platform, credentials: Credentials): Unit {
        val twitterPlatform = platform as TwitterPlatform
        val twitterCredentials = credentials as TwitterCredentials
        TODO("Not yet implemented")
    }

    override fun createPost(platform: Platform, credentials: Credentials, post: Post): Unit {
        val twitterPlatform = platform as TwitterPlatform
        val twitterCredentials = credentials as TwitterCredentials
        TODO("Not yet implemented")
    }
}
