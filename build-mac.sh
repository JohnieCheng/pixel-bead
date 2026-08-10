#!/bin/bash
# Build a macOS app-image (.app) or .dmg for Pixel Bead (Java 21 + JavaFX 21).
# Uses jpackage's non-modular mode (--input + --main-jar) because PDFBox 3.x
# ships as an automatic module, which jlink refuses to bundle.
# Prerequisites: JDK 21, Xcode command line tools (for dmg/hdiutil).
set -euo pipefail
cd "$(dirname "$0")"

TYPE="${1:-app-image}"
JAVA_HOME="${JAVA_HOME:-$HOME/Documents/Environments/Java/jdk-21.0.9.jdk/Contents/Home}"
export JAVA_HOME
MVN_REPO="$HOME/.m2/repository"

echo "==> Building jar..."
./mvnw -q clean package -DskipTests

# Single version source: pom.xml. Optionally overridable via the second arg.
VERSION="${2:-$(./mvnw -q help:evaluate -Dexpression=project.version -DforceStdout 2>/dev/null | tail -1)}"
echo "==> Version: $VERSION"

echo "==> Assembling runtime libs..."
rm -rf target/libs
mkdir -p target/libs
APP_JAR=$(ls target/*.jar | head -1)
cp "$APP_JAR" target/libs/
cp "$MVN_REPO/org/openjfx/javafx-base/21.0.6/javafx-base-21.0.6-mac-aarch64.jar" target/libs/
cp "$MVN_REPO/org/openjfx/javafx-controls/21.0.6/javafx-controls-21.0.6-mac-aarch64.jar" target/libs/
cp "$MVN_REPO/org/openjfx/javafx-fxml/21.0.6/javafx-fxml-21.0.6-mac-aarch64.jar" target/libs/
cp "$MVN_REPO/org/openjfx/javafx-graphics/21.0.6/javafx-graphics-21.0.6-mac-aarch64.jar" target/libs/
cp "$MVN_REPO/org/controlsfx/controlsfx/11.2.1/controlsfx-11.2.1.jar" target/libs/
cp "$MVN_REPO/org/kordamp/ikonli/ikonli-core/12.3.1/ikonli-core-12.3.1.jar" target/libs/
cp "$MVN_REPO/org/kordamp/ikonli/ikonli-javafx/12.3.1/ikonli-javafx-12.3.1.jar" target/libs/
cp "$MVN_REPO/com/fasterxml/jackson/core/jackson-databind/2.18.2/jackson-databind-2.18.2.jar" target/libs/
cp "$MVN_REPO/com/fasterxml/jackson/core/jackson-core/2.18.2/jackson-core-2.18.2.jar" target/libs/
cp "$MVN_REPO/com/fasterxml/jackson/core/jackson-annotations/2.18.2/jackson-annotations-2.18.2.jar" target/libs/
cp "$MVN_REPO/org/apache/pdfbox/pdfbox/3.0.3/pdfbox-3.0.3.jar" target/libs/
cp "$MVN_REPO/org/apache/pdfbox/fontbox/3.0.3/fontbox-3.0.3.jar" target/libs/
cp "$MVN_REPO/commons-logging/commons-logging/1.3.3/commons-logging-1.3.3.jar" target/libs/

ICON_ARGS=()
[ -f src/main/resources/icons/pixel-bead.icns ] && ICON_ARGS+=(--icon "src/main/resources/icons/pixel-bead.icns")

echo "==> Running jpackage ($TYPE)..."
"$JAVA_HOME/bin/jpackage" \
  --type "$TYPE" \
  --name "Pixel Bead" \
  --app-version "$VERSION" \
  --input target/libs \
  --main-jar "$(basename "$APP_JAR")" \
  --main-class com.johnie.pixelbead.Launcher \
  --mac-package-identifier com.johnie.pixelbead \
  --mac-package-name "Pixel Bead" \
  "${ICON_ARGS[@]}" \
  --dest target/dist

echo "==> Done: target/dist"
