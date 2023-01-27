#!/usr/bin/env bash
if [ "$1" = "" ] || [ "$2" = "" ]; then
  echo "The module or output path must not be empty."
  exit 1
fi
# Check JAVA_HOME
if [ "${JAVA_HOME}" = "" ]; then
  echo "Please ensure system variable contain JAVA_HOME."
  exit 1
fi

java="${JAVA_HOME}/bin/java"

# Check java version
version=$("$java" -version 2>&1 | awk -F '"' '/version/ {print $2}')

# shellcheck disable=SC2071
if [[ "$version" -lt "17" ]]; then
  echo "Java version must >=17"
  exit 1
fi

echo "Current Java home:${JAVA_HOME} and Java version:${version}"

# shellcheck disable=SC2034
REQUIRE_MODULES="java.logging,javafx.base,javafx.fxml,javafx.graphics,javafx.controls,cn.navclub.nes4j.bin,cn.navclub.nes4j.app"

# shellcheck disable=SC2091
$(rm -rf "$2")

# shellcheck disable=SC2034
# shellcheck disable=SC2091
CMD="${JAVA_HOME}/bin/jlink --module-path $1 --add-modules ${REQUIRE_MODULES} --output $2"

echo "$CMD"
# shellcheck disable=SC2091
$($CMD)

# shellcheck disable=SC2091
$(touch launch.sh)
# shellcheck disable=SC2091
$(chmod 777 launch.sh)

echo "#!/usr/bin/env bash
nohup runtime/bin/java -Dnes4j.log.level=WARN -m cn.navclub.nes4j.app/cn.navclub.nes4j.app.Launcher \$@ > nes4j.log 2>&1 &" >"linux/launch.sh"

cp ../LICENSE linux