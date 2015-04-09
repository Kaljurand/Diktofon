Diktofon
========

Dictaphone with Estonian speech-to-text (an Android app).
See more at the

  - general website: <http://diktofon.googlecode.com>
  - Google Play page: <https://play.google.com/store/apps/details?id=kaljurand_at_gmail_dot_com.diktofon>


Compilation
-----------

Go into the app-directory and execute

	ant clean release

Note that you need to have these files that are not part of this
repository:

  - local.properties (pointer to the Android SDK)
  - diktofon.keystore (release keys)

Read the Android developer docs for instructions on how to generate them.

Also, you need to have these jar-files in the `app/libs`-directory:

	apache-mime4j-0.6.jar
	commons-io-2.0.1.jar
	guava-11.0.1.jar
	httpmime-4.1.1.jar
	json_simple-1.1.jar
	net-speech-api-0.1.8.jar

Net Speech API is available from <https://github.com/Kaljurand/net-speech-api>.
