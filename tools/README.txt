
== Set up ==

To run the scripts in this directory first set two environment variables.

export DIKTOFON_SRC=${HOME}/path/to/Diktofon/
export ANDROID_SDK=${HOME}/path/to/android-sdk-linux_x86/


== Controlling the phone over USB ==


1. On a 64bit Ubuntu, install first the libraries:

sudo apt-get install ia32-libs


2. Controlling the phone over USB (e.g. to take screenshots), see:

http://developer.android.com/guide/developing/device.html

(1) The app should be debuggable and 

(2) On the device, go to the home screen, press MENU,
select Applications > Development, then enable USB debugging.


3. Create a rule file:

sudo cp 51-android.rules /etc/udev/rules.d/
sudo chmod a+r /etc/udev/rules.d/51-android.rules


4. Verify if it works:

$ ./adb devices
List of devices attached 
HTxxxxxxx	device

or

$ ./adb logcat


5. Make sure that the laptop is not controlling the SD card,
i.e. set the phone to "Charge only"


6.

$ ./adb install -r ~/mywork/Diktofon/bin/Diktofon-release.apk
