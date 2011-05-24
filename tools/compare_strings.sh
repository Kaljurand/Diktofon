# BUG: There is probably a better way to do it

default_strings=$DIKTOFON_SRC/app/res/values/strings.xml
et_strings=$DIKTOFON_SRC/app/res/values-et/strings.xml

cat $default_strings | grep 'name="' | sed 's/^.*name="\([^"]*\)".*$/\1/' > /tmp/default_strings.txt
cat $et_strings | grep 'name="' | sed 's/^.*name="\([^"]*\)".*$/\1/' > /tmp/et_strings.txt

fgrep -vf /tmp/et_strings.txt /tmp/default_strings.txt
