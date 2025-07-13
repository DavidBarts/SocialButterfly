package name.blackcap.socialbutterfly.driver

/* For handling platform-specific calls, generally for dispatching them to
   the appropriate driver based on the name of the leaf object type. I am
   not 100% sure if this is a good idea, but here it is. It does free us from
   having references to a platform driver in each platform-specific data
   type, and it does solve certain issues with dispatching and overloading
   by programmatically forcing them to runtime. */

/* TODO: clean up unneeded functions (if any), make stuff private/internal to DriverDispatcher as needed */

import name.blackcap.socialbutterfly.jschema.Credentials
import name.blackcap.socialbutterfly.jschema.Platform
import name.blackcap.socialbutterfly.jschema.Post
import kotlin.reflect.KClass

fun suffixFor(platform: Platform) = "Platform"

fun suffixFor(credentials: Credentials) = "Credentials"

fun getRawPlatformName(klass: KClass<*>, suffix: String): String {
    var name = klass.simpleName!!
    if (name.endsWith(suffix)) {
        name = name.substring(0, name.length - suffix.length)
    }
    return name
}

fun getRawPlatformName(platform: Platform): String =
    getRawPlatformName(platform::class, suffixFor(platform))

fun getRawPlatformName(credentials: Credentials): String =
    getRawPlatformName(credentials::class, suffixFor(credentials))

fun getPlatformName(klass: KClass<*>, suffix: String): String =
    driverFor(klass, suffix).NAME

fun getPlatformName(platform: Platform): String =
    getPlatformName(platform::class, suffixFor(platform))

fun getPlatformName(credentials: Credentials): String =
    getPlatformName(credentials::class, suffixFor(credentials))

fun driverFor(klass: KClass<*>, suffix: String): Driver {
    val rawName = getRawPlatformName(klass, suffix)
    val driverClass = Class.forName("name.blackcap.socialbutterfly.driver.$rawName").kotlin
    return driverClass.objectInstance as Driver
}

fun driverFor(platform: Platform) =
    driverFor(platform::class, suffixFor(platform))

fun driverFor(credentials: Credentials) =
    driverFor(credentials::class, suffixFor(credentials))

object DriverDispatcher: Driver {
    override val NAME = "virtual platform"

    override fun createPlatform() {
        throw NotImplementedError("Dispatching to createPlatform not supported.")
    }

    override fun editPlatform(platform: Platform) {
        driverFor(platform).editPlatform(platform)
    }

    override fun createCredentials(platform: Platform) {
        driverFor(platform).createCredentials(platform)
    }

    override fun editCredentials(platform: Platform, credentials: Credentials) {
        driverFor(platform).editCredentials(platform, credentials)
    }

    override fun createPost(platform: Platform, credentials: Credentials, post: Post) {
        driverFor(platform).createPost(platform, credentials, post)
    }
}
