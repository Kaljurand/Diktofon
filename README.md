Diktofon
========

Dictaphone with Estonian speech-to-text (an Android app).
See more at the

  - general website: <https://github.com/Kaljurand/Diktofon>
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

Diktofon requires the storage permission. It is essential
to allow Diktofon to store the audio files and their transcriptions.
