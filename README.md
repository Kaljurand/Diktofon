Diktofon
========

Dictaphone with Estonian speech-to-text (an Android app).
See more at the

  - general website: <http://diktofon.googlecode.com>
  - Google Play page: <https://play.google.com/store/apps/details?id=kaljurand_at_gmail_dot_com.diktofon>


Compilation
-----------

Go into the app-directory and execute

> ant clean release

Note that you need to have 3 additional files that are not part of this
repository:

  - app/libs/json_simple-1.1.jar (to keep ProGuard happy, there might be a simpler way)
  - app/local.properties (pointer to the Android SDK)
  - app/diktofon.keystore (release keys)

Read the Android developer docs for instructions on how to generate the
last two files.


Tags
----

Version tags are set by e.g.

> git tag -a v0.9.76 -m 'version 0.9.76'

The last number should be even.


Lint
----

> lint --html report.html app
