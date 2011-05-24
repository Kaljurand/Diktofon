local=$DIKTOFON_SRC/examples/files/
remote=/sdcard/Android/data/kaljurand_at_gmail_dot_com.diktofon/files/

echo "Pushing $local to $remote"

$ANDROID_SDK/platform-tools/adb push $local $remote
