# Bash script that creates Android icons (png) of different sizes
# from a single input SVG file.
#
# Icon name prefixes determine the type (and the size) of the icon.
#
# Icons: ic: ic_star.png
# Launcher icons: ic_launcher: ic_launcher_calendar.png
# Menu icons: ic_menu: ic_menu_archive.png
# Status bar icons: ic_stat_notify: ic_stat_notify_msg.png
# Tab icons: ic_tab: ic_tab_recent.png
# Dialog icons: ic_dialog: ic_dialog_info.png
#
# @author Kaarel Kaljurand
# @version 2011-05-17
# @work_in_progress

dir_svg=${DIKTOFON_SRC}/images/
dir_png=${DIKTOFON_SRC}/app/res/

dir_png_hdpi=${dir_png}/drawable-hdpi/
dir_png_mdpi=${dir_png}/drawable-mdpi/
dir_png_ldpi=${dir_png}/drawable-ldpi/

# Launcher icon
launcher_logo_w=512
launcher_logo_h=512

launcher_hdpi_w=72
launcher_hdpi_h=72

launcher_mdpi_w=48
launcher_mdpi_h=48

launcher_ldpi_w=36
launcher_ldpi_h=36

# Status bar icons (Android 2.2-)
stat_hdpi_w=38
stat_hdpi_h=38

stat_mdpi_w=25
stat_mdpi_h=25

stat_ldpi_w=19
stat_ldpi_h=19

# Status bar icons (Android 2.3+)
stat_hdpi_v9_w=24
stat_hdpi_v9_h=38

stat_mdpi_v9_w=16
stat_mdpi_v9_h=25

stat_ldpi_v9_w=12
stat_ldpi_v9_h=19

echo "Generating status bar notification icons:"
for path in ${dir_svg}ic_stat_notify_*.svg
do
	filename=$(basename $path)
	file=${filename%.*}
	echo "$file"
	rsvg-convert -f png -w ${stat_hdpi_w} -h ${stat_hdpi_h} -o ${dir_png_hdpi}/$file.png $path
	rsvg-convert -f png -w ${stat_mdpi_w} -h ${stat_mdpi_h} -o ${dir_png_mdpi}/$file.png $path
	rsvg-convert -f png -w ${stat_ldpi_w} -h ${stat_ldpi_h} -o ${dir_png_ldpi}/$file.png $path
done

echo "Generating launcher icon:"
for path in ${dir_svg}ic_launcher.svg
do
	filename=$(basename $path)
	file=${filename%.*}
	echo "$file"
	rsvg-convert -f png -w ${launcher_logo_w} -h ${launcher_logo_h} -o ${dir_svg}/$file.png $path
	rsvg-convert -f png -w ${launcher_hdpi_w} -h ${launcher_hdpi_h} -o ${dir_png_hdpi}/$file.png $path
	rsvg-convert -f png -w ${launcher_mdpi_w} -h ${launcher_mdpi_h} -o ${dir_png_mdpi}/$file.png $path
	rsvg-convert -f png -w ${launcher_ldpi_w} -h ${launcher_ldpi_h} -o ${dir_png_ldpi}/$file.png $path
done
