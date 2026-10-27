#!/usr/bin/env bash
# Boots the dev client under a virtual display with software OpenGL until the title screen,
# so model/texture/blockstate loading problems show up in the log.
set -u
sudo apt-get install -y -qq xvfb mesa-utils libgl1-mesa-dri > /dev/null 2>&1 || true
mkdir -p run
cat > run/options.txt <<EOF
version:3465
onboardAccessibility:false
skipMultiplayerWarning:true
tutorialStep:none
joinedFirstServer:true
narrator:0
soundCategory_master:0.0
renderDistance:2
EOF
export LIBGL_ALWAYS_SOFTWARE=1
timeout 1500 xvfb-run -a -s "-screen 0 1280x720x24" ./gradlew runClient -PfwSelftest --no-daemon > client.log 2>&1
code=$?
echo "client exit code: $code" >> client.log
grep -q "FWSELFTEST CLIENT reached title screen" client.log
