#!/usr/bin/env bash
# Loads the self-test world in the dev client (software OpenGL under Xvfb) and lets the fw_guide showcase
# driver build scenes and save screenshots of blocks, creatures, structures, dimensions and guide pages.
set -u
sudo apt-get install -y -qq xvfb mesa-utils libgl1-mesa-dri > /dev/null 2>&1 || true
mkdir -p run/saves
rm -rf run/saves/showcase run/screenshots
if [ ! -d server/world ]; then echo "no server world to load"; exit 1; fi
cp -r server/world run/saves/showcase
cat > run/options.txt <<EOT
version:3465
onboardAccessibility:false
skipMultiplayerWarning:true
tutorialStep:none
joinedFirstServer:true
narrator:0
soundCategory_master:0.0
renderDistance:6
simulationDistance:5
guiScale:2
fov:0.0
renderClouds:"false"
EOT
export LIBGL_ALWAYS_SOFTWARE=1
timeout 2400 xvfb-run -a -s "-screen 0 1280x720x24" ./gradlew runClient -PfwShowcase --no-daemon > showcase.log 2>&1
echo "showcase exit code: $?" >> showcase.log
mkdir -p ci-report/screenshots
cp run/screenshots/fw_*.png ci-report/screenshots/ 2>/dev/null || true
ls ci-report/screenshots | wc -l
grep -q "FWSHOWCASE DONE" showcase.log
