#!/usr/bin/env bash
# Installs a real Forge server, loads every built mod jar and runs the ls_core self-test
# (places all blocks, structures, generates dimensions, spawns entities, then stops).
set -u
ROOT="$(pwd)"
mkdir -p server-cache
if [ ! -f server-cache/run.sh ]; then
  cd server-cache
  curl -sSL -o installer.jar "https://maven.minecraftforge.net/net/minecraftforge/forge/${FORGE}/forge-${FORGE}-installer.jar"
  java -jar installer.jar --installServer > install.log 2>&1 || { tail -50 install.log; exit 1; }
  rm -f installer.jar
  cd "$ROOT"
fi
rm -rf server && cp -r server-cache server
cd server
echo "eula=true" > eula.txt
cat > server.properties <<EOF
online-mode=false
level-seed=lodestar
spawn-protection=0
max-tick-time=-1
view-distance=4
simulation-distance=4
allow-nether=true
EOF
mkdir -p mods
cp "$ROOT"/build/modjars/*.jar mods/
echo "-Xmx4G -Dlodestar.selftest=server" > user_jvm_args.txt
chmod +x run.sh
timeout 1500 ./run.sh nogui > server.log 2>&1
code=$?
echo "server exit code: $code" | tee -a server.log
grep -q "LSSELFTEST DONE failures=0" server.log
