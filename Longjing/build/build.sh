#!/bin/bash

die() {
    echo "$*"
    exit -1
}

LONGJING_CONFIG_URL="$BILUOCHUN_URL/api/v1/team/$TEAM_SEQ/longjing"

# -H specifies header
# -S suppress non-error log
# -f will raise any status code > 400 to non-zero exit status
# -o specifies output destination
CURL_OPTIONS=( --no-progress-meter -S -f -o ./longjing-config.json )

curl ${CURL_OPTIONS[@]} $LONGJING_CONFIG_URL || die Fail to fetch Longjing cnofig

which jq >/dev/null || die Cannot find program jq, build process cannot continue

# Retrieve special build command, fallback to 'build' if not specified
BUILD_COMMAND=$(jq -rM .build_command ./longjing-config.json)
if [ -z "$BUILD_COMMAND" ] || [ "$BUILD_COMMAND" = 'null' ]; then
    BUILD_COMMAND=build
fi
# https://unix.stackexchange.com/questions/459367/using-shell-variables-for-command-options
# tl;dr: DO NOT TRY STUFFING CLI OPTIONS INTO SHELL VARIABLE
BUILD_COMMAND=( $BUILD_COMMAND )

# Retrieve name of the task responsible for producing the jar, fallback to empty string 
# if it is string 'null'.
OUTPUT_JAR_TASK=$(jq -rM .output_task ./longjing-config.json)
if [ -z "$OUTPUT_JAR_TASK" ] || [ "$OUTPUT_JAR_TASK" = 'null' ]; then
    unset OUTPUT_JAR_TASK
fi
echo Target output jar task will be: $OUTPUT_JAR_TASK

# Let later steps know if they need all other submitted works in order to run dedicated server test
echo "LONGJING_REQUIRE_OTHER_WORKS=$(jq -rM .require_other_works ./longjing-config.json)" >> $GITHUB_ENV

cd repo

if [ "$GRADLE_WRAPPER_CHECK" = 'true' ] && [ -f './gradlew' ]; then
    chmod +x ./gradlew
    GRADLE_EXEC=./gradlew 
else
    echo '::warning::Gradle wrapper is not found, which is not recommended. Using system-level gradle instead.'
    GRADLE_EXEC=gradle
fi

# We add empty socks.proxyHost, http.proxyHost and https.proxyHost system properties, so that any pre-existing 
# proxy configurations are void in CI environment. We do not need any proxy on GitHub Action.
#
# We add USERNAME and TOKEN environmental variables for those who use GitHub Package Registry.
# For more information, see the below doc page on GitHub docs:
# https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-gradle-registry

if [ -f ../../$INFO_DIR/maven_coordinate ]; then
  USERNAME=$GITHUB_USERNAME TOKEN=$GITHUB_TOKEN $GRADLE_EXEC -Dsocks.proxyHost= -Dhttp.proxyHost= -Dhttps.proxyHost= --stacktrace publishToMavenLocal
  # 临时覆盖 IFS 为 :，然后借助 read 命令分割 Maven Coordinate，并分别赋值到正确的变量名中 
  IFS=: read -r GROUP ARTIFACT VERSION < ../../$INFO_DIR/maven_coordinate
  # 尝试构建正确的文件路径
  TARGET_FILE=$HOME/.m2/repository/${GROUP//./\/}/$ARTIFACT/$VERSION/$ARTIFACT-$VERSION.jar
  # 检查文件是否存在。若不存在，报错退出
  [ -f $TARGET_FILE ] || die "::error::无法根据输入的 Maven Coordinate $(cat ../../$INFO_DIR/maven_coordinate) 定位到指定文件。推定路径：$TARGET_FILE"
  echo "ARTIFACT_NAME=$ARTIFACT-$VERSION.jar" >> $GITHUB_ENV
  echo "ARTIFACT_LOCAL_PATH=$TARGET_FILE" >> $GITHUB_ENV
  echo "artifact=$TARGET_FILE" >> $GITHUB_OUTPUT
else
TEACON_ARTIFACT_TASK=$OUTPUT_JAR_TASK USERNAME=$GITHUB_USERNAME TOKEN=$GITHUB_TOKEN \
  $GRADLE_EXEC -Dorg.gradle.java.home=$JAVA_HOME -Dsocks.proxyHost= -Dhttp.proxyHost= -Dhttps.proxyHost= --stacktrace -I ../setup.gradle "${BUILD_COMMAND[@]}" teaconLongjingProcessing
fi