timestamp=`date '+%Y%m%d-%H%M'`

tar="/tmp/Diktofon_$timestamp.tar"
dropbox="$HOME/Dropbox/Tmp/"

echo "Creating: $tar"
tar cf $tar -C $DIKTOFON_SRC .
gzip $tar

echo "Copying $tar.gz -> $dropbox"
cp $tar.gz $dropbox

echo "Finished!"
