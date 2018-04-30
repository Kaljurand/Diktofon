Diktofon
========

Dictaphone with Estonian speech-to-text (an Android app).
See more at the

  - general website: <http://diktofon.googlecode.com>
  - Google Play page: <https://play.google.com/store/apps/details?id=kaljurand_at_gmail_dot_com.diktofon>


Compilation
-----------

Go into the app-directory and execute

    gradle installRelease

Note that you need to have these files that are not part of this
repository:

  - local.properties (pointer to the Android SDK)
  - diktofon.keystore (release keys)

Read the Android developer docs for instructions on how to generate them.


Permissions
-----------

Diktofon requires the storage and microphone permissions. The first is essential
to allow Diktofon to store the audio files and their transcriptions. The second is needed
only if Diktofon is used as a recorder. Note that currently Diktofon does not ask for
these permissions, instead they must to be enabled via the general Android Settings menu.
