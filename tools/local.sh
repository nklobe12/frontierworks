#!/usr/bin/env bash
# Local offline build helpers (needs the devkit: Gradle caches in ~/.gradle, JDK 17 and the Forge server in ../devkit).
#   tools/local.sh compile   generate + compile (fast type check)
#   tools/local.sh jars      generate + build per-mod jars
#   tools/local.sh server    run the server self-test with the built jars (log: run-local/server.log)
#   tools/local.sh client    boot the dev client to the title screen under xvfb (log: run-local/client.log)
set -u
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
DEVKIT="${DEVKIT:-$ROOT/../devkit}"
export JAVA_HOME="$DEVKIT/jdk17"
export PATH="$JAVA_HOME/bin:$PATH"
cd "$ROOT"
case "${1:-compile}" in
  compile)
    python3 tools/build.py | tail -1 || exit 1
    ./gradlew --offline compileJava -q 2>&1 | grep -v '^Picked up\|^Note:' ;;
  jars)
    python3 tools/build.py | tail -1 || exit 1
    ./gradlew --offline modJars -q 2>&1 | grep -v '^Picked up\|^Note:'
    ls build/modjars | wc -l ;;
  server)
    rm -rf run-local/server && mkdir -p run-local && cp -r "$DEVKIT/server-cache" run-local/server
    cd run-local/server
    echo "eula=true" > eula.txt
    printf 'online-mode=false\nlevel-seed=lodestar\nspawn-protection=0\nmax-tick-time=-1\nview-distance=4\nsimulation-distance=4\n' > server.properties
    mkdir -p mods && cp "$ROOT"/build/modjars/*.jar mods/
    echo "-Xmx3G -Dlodestar.selftest=server" > user_jvm_args.txt
    chmod +x run.sh
    timeout "${TIMEOUT:-1500}" ./run.sh nogui > ../server.log 2>&1
    echo "exit $?"
    grep -E "LSSELFTEST (DONE|.*FAIL)" ../server.log | tail -40 ;;
  client)
    mkdir -p run
    printf 'version:3465\nonboardAccessibility:false\nskipMultiplayerWarning:true\ntutorialStep:none\njoinedFirstServer:true\nnarrator:0\nsoundCategory_master:0.0\nrenderDistance:2\n' > run/options.txt
    mkdir -p run-local
    LIBGL_ALWAYS_SOFTWARE=1 timeout "${TIMEOUT:-1200}" xvfb-run -a -s "-screen 0 1280x720x24" ./gradlew --offline runClient -PlsSelftest > run-local/client.log 2>&1
    echo "exit $?"
    grep -E "LSSELFTEST|Exception|ERROR" run-local/client.log | head -40 ;;
esac
