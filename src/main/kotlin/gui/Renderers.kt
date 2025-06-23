package name.blackcap.socialbutterfly.gui

import name.blackcap.socialbutterfly.jschema.*
import name.blackcap.socialbutterfly.lib.MapConnector
import name.blackcap.socialbutterfly.lib.SetConnector
import name.blackcap.socialbutterfly.lib.toLocalizedString
import java.awt.Component
import java.time.Instant
import javax.swing.JList
import javax.swing.ListCellRenderer
import kotlin.reflect.KClass

fun getPlatformName(klass: KClass<out Platform>): String {
    val SUFFIX = "Platform"
    var name = klass.simpleName!!
    if (name.endsWith(SUFFIX)) {
        name = name.substring(0, name.length - SUFFIX.length)
    }
    if (name == "Twitter") {
        name = "X (Twitter)"
    }
    return name
}

private fun annotatedTime(time: Instant, flag: Boolean, annotation: String): String =
    if (flag) {
        time.toLocalizedString() + " ($annotation)"
    } else {
        time.toLocalizedString()
    }

class PlatformRenderer : ListCellRenderer<MapConnector<Platform, String>.ListModelEntry> {
    override fun getListCellRendererComponent(
        list: JList<out MapConnector<Platform, String>.ListModelEntry>?,
        value: MapConnector<Platform, String>.ListModelEntry?,
        index: Int,
        isSelected: Boolean,
        cellHasFocus: Boolean
    ): Component =
        if (value!!.value.isCentralized) {
            TwoLineElement(getPlatformName(value.value::class), "")
        } else {
            TwoLineElement(value.value.host, getPlatformName(value.value::class))
        }
}

class ChannelRenderer(private val platforms: MutableMap<String, Platform>) : ListCellRenderer<MapConnector<Channel, String>.ListModelEntry> {
    override fun getListCellRendererComponent(
        list: JList<out MapConnector<Channel, String>.ListModelEntry>?,
        value: MapConnector<Channel, String>.ListModelEntry?,
        index: Int,
        isSelected: Boolean,
        cellHasFocus: Boolean
    ): Component {
        val platform = platforms[value!!.value.platform]!!
        val platformName = getPlatformName(platform::class)
        return if (platform.isCentralized) {
            TwoLineElement(value.value.credentials.username, platformName)
        } else {
            TwoLineElement(value.value.credentials.username, "${platform.host} ($platformName)")
        }
    }
}

class DistributionRenderer : ListCellRenderer<MapConnector<Distribution, String>.ListModelEntry> {
    override fun getListCellRendererComponent(
        list: JList<out MapConnector<Distribution, String>.ListModelEntry>?,
        value: MapConnector<Distribution, String>.ListModelEntry?,
        index: Int,
        isSelected: Boolean,
        cellHasFocus: Boolean
    ): Component {
        val nChannels = value!!.value.channels.size
        val s = if (nChannels == 1) "" else "s"
        return TwoLineElement(value.value.name, "$nChannels channel$s")
    }
}

/* JLabel should do the … truncation for us */

class PostRenderer : ListCellRenderer<MapConnector<Post, Instant>.ListModelEntry> {
    override fun getListCellRendererComponent(
        list: JList<out MapConnector<Post, Instant>.ListModelEntry>?,
        value: MapConnector<Post, Instant>.ListModelEntry?,
        index: Int,
        isSelected: Boolean,
        cellHasFocus: Boolean
    ): Component =
        TwoLineElement(value!!.value.text,
            annotatedTime(value.value.created, value.value.isEdited, "edited"))
}

class FailureRenderer(private val posts: MutableMap<String, Post>) : ListCellRenderer<SetConnector<Failure, Instant>.ListModelEntry> {
    override fun getListCellRendererComponent(
        list: JList<out SetConnector<Failure, Instant>.ListModelEntry>?,
        value: SetConnector<Failure, Instant>.ListModelEntry?,
        index: Int,
        isSelected: Boolean,
        cellHasFocus: Boolean
    ): Component =
        TwoLineElement(posts[value!!.value.post]!!.text,
            annotatedTime(value.value.created, value.value.isRetried, "retried"))
}
