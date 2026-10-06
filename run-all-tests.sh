#!/bin/bash
# ==============================================================================
# Run All Unit and Integration Tests Across the Banking Platform
# ==============================================================================

if [ -z "$JAVA_HOME" ] && [ -d "/usr/local/opt/openjdk" ]; then
    export JAVA_HOME="/usr/local/opt/openjdk"
fi
if [ -n "$JAVA_HOME" ]; then
    export PATH="$JAVA_HOME/bin:$PATH"
fi

echo "======================================================"
echo " Running All Banking Platform Tests with Maven..."
echo " Java Home: $JAVA_HOME"
echo "======================================================"

mvn test
