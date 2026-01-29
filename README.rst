About Social Butterfly
======================

This is a work in progress.

My goal was to have a desktop app that allowed one to post to as many
common social networks as possible.  Unfortunately, that idea was not
practical, mainly due to how social media sites handle grant
applications like this access to their API's (typically OAuth, whose
design assumes a web-based application; it is possible to work around
this, but the result is decidedly user-unfriendly).

Anyhow, now I am rethinking this as a Web-based service. Planning to do
this incrementally and to focus on doing the basics (making posts,
handling retries and failures), and to have the non-basics (initially,
everything else) be done via the command line on the server.

Initially I plan to support Mastodon, Twitter, and Bluesky. Others may come
later. Plans to support Facebook and other Meta platforms are currently on
hold because their API is broken by design and their process for getting an
application approved for meaningful API access is needlessly onerous.

My motive for writing this is that while there are existing services that
allow one to do this (e.g. Hootsuite, Buffer), these typically cost an arm
and a leg (like, $100 *per month*), plus they contain all sorts of
supposedly “value-added” extra features that I just don’t need. (If there
was a way to delete those features and get a basic service for around
$100/year, I’d probably spring for it.)
