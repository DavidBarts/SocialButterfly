package name.blackcap.socialbutterfly.driver

import name.blackcap.socialbutterfly.jschema.Platform

/* Nasty ugly repetition, but the limitations of JVM reflection force our
   hand here. */
val DRIVERS = mapOf<String, Driver>(
    Twitter.NAME to Twitter,
    Bluesky.NAME to Bluesky,
    Mastodon.NAME to Mastodon
)
