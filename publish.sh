#!/bin/bash

# Gradle Lighthouse Secure Publisher
# This script allows you to publish without hardcoding secrets in gradle.properties.

echo "🏗️  Gradle Lighthouse: Secure Publishing Sequence"
echo "------------------------------------------------"

# Prompt for keys if not set in environment
if [ -z "$GRADLE_PUBLISH_KEY" ]; then
    read -p "Enter Gradle Publish Key: " GRADLE_PUBLISH_KEY
fi

if [ -z "$GRADLE_PUBLISH_SECRET" ]; then
    read -sp "Enter Gradle Publish Secret: " GRADLE_PUBLISH_SECRET
    echo ""
fi

if [ -z "$GRADLE_PUBLISH_KEY" ] || [ -z "$GRADLE_PUBLISH_SECRET" ]; then
    echo "❌ Error: Keys are required."
    exit 1
fi

echo "🚀 Starting publication for Version $(grep 'version=' gradle.properties | cut -d'=' -f2)..."

./gradlew publishPlugins \
    -Pgradle.publish.key="$GRADLE_PUBLISH_KEY" \
    -Pgradle.publish.secret="$GRADLE_PUBLISH_SECRET" \
    --no-daemon --stacktrace

if [ $? -eq 0 ]; then
    echo "✅ Success! Your plugin has been submitted/updated."
else
    echo "❌ Build failed. Check the logs above."
fi
