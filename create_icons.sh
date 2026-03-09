#!/bin/bash
DIR="app/src/main/res/drawable"
mkdir -p $DIR

# Gradients
cat << 'XML' > $DIR/bg_header_gradient.xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android">
    <gradient android:startColor="#1A2979FF" android:endColor="#0A0E1A" android:angle="270" />
</shape>
XML

cat << 'XML' > $DIR/bg_auth_gradient.xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android">
    <gradient android:startColor="#802979FF" android:endColor="#0A0E1A" android:angle="270" />
</shape>
XML

cat << 'XML' > $DIR/bg_sos_ring.xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="oval">
    <solid android:color="#33FF1744" />
</shape>
XML

cat << 'XML' > $DIR/bg_splash.xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android">
    <solid android:color="#0A0E1A" />
</shape>
XML

# Placeholder icons
ICONS=("ic_document" "ic_timeline" "ic_map_pin" "ic_ai_chat" "ic_car" "ic_arrow_right" "ic_badge" "ic_visibility" "ic_visibility_off" "ic_email" "ic_lock" "ic_user" "ic_phone" "ic_station")

for icon in "${ICONS[@]}"; do
    cat << 'XML' > $DIR/${icon}.xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp"
    android:viewportWidth="24" android:viewportHeight="24">
    <path android:fillColor="#FFFFFFFF" android:pathData="M12,2A10,10 0 1,0 22,12A10,10 0 0,0 12,2M12,20A8,8 0 1,1 20,12A8,8 0 0,1 12,20Z"/>
</vector>
XML
done

chmod +x create_icons.sh
./create_icons.sh
