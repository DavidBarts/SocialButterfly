package name.blackcap.socialbutterfly.driver

import name.blackcap.socialbutterfly.jschema.Credentials
import name.blackcap.socialbutterfly.jschema.Platform
import name.blackcap.socialbutterfly.jschema.Post

/* Driver is not the best name for these, but is the best I have been able
   to come up with. These are not normal driver routines, in that they don't
   usually run behind the scenes. Many of them will contain user interaction
   code! */
interface Driver {
    val NAME: String
    fun createPlatform(): Unit
    fun editPlatform(platform: Platform): Unit
    fun createCredentials(platform: Platform): Unit
    fun editCredentials(platform: Platform, credentials: Credentials): Unit
    fun createPost(platform: Platform, credentials: Credentials, post: Post): Unit
}
