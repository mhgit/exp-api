#!/bin/bash

# Base installation directory
BASE_DIR="$HOME/dev"
KC_VERSION="26.2.5"
KC_HOME="$BASE_DIR/keycloak-26.2.5"

# Configuration variables
KC_HTTP_PORT="${KC_HTTP_PORT:-8082}"
KC_MGMT_PORT="${KC_MGMT_PORT:-9002}"

# Check Java version
java -version

# Create base directory if it doesn't exist
mkdir -p "$BASE_DIR"

# Download and install Keycloak if not already installed
if [ ! -d "$KC_HOME" ]; then
    echo "Downloading Keycloak $KC_VERSION..."
    cd "$BASE_DIR"
    curl -LO "https://github.com/keycloak/keycloak/releases/download/$KC_VERSION/keycloak-$KC_VERSION.tar.gz"
    
    echo "Extracting Keycloak..."
    tar xzf "keycloak-$KC_VERSION.tar.gz"
    rm "keycloak-$KC_VERSION.tar.gz"
    
    echo "Keycloak installed at $KC_HOME"
fi

echo "Using Keycloak installation at: $KC_HOME"

# Start Keycloak
echo "Starting Keycloak..."
JAVA_OPTS="-Xms64m -Xmx512m -XX:MetaspaceSize=96M -XX:MaxMetaspaceSize=256m" \
$KC_HOME/bin/kc.sh start-dev \
    --http-port=$KC_HTTP_PORT \
    --http-relative-path=/auth \
    --health-enabled=true \
    --metrics-enabled=true \
    --features=preview \
    --http-management-port=$KC_MGMT_PORT \
    --hostname-strict=false \
    --cache=local &

# Store the PID
KC_PID=$!

# Wait for Keycloak to be ready
echo "Waiting for Keycloak to start..."
echo "Checking Keycloak health at http://localhost:$KC_MGMT_PORT/auth/health/ready"
max_attempts=20
attempt=1

while [ $attempt -le $max_attempts ]; do
    echo -n "Attempt $attempt of $max_attempts: "
    HEALTH_RESPONSE=$(curl -s -w "\n%{http_code}" "http://localhost:$KC_MGMT_PORT/auth/health/ready")
    HTTP_CODE=$(echo "$HEALTH_RESPONSE" | tail -n 1)
    RESPONSE_BODY=$(echo "$HEALTH_RESPONSE" | sed '$d')
    
    if [ "$HTTP_CODE" = "200" ]; then
        CLEANED_RESPONSE=$(echo "$RESPONSE_BODY" | tr -d '[:space:]')
        if [[ "$CLEANED_RESPONSE" == *'"status":"UP"'* ]]; then
            echo "Keycloak started successfully!"
            echo "Health check response: $RESPONSE_BODY"
            break
        else
            echo "Got response but waiting for status UP: $RESPONSE_BODY"
        fi
    else
        echo "Waiting... (HTTP Code: $HTTP_CODE)"
    fi
    
    if [ $attempt -eq $max_attempts ]; then
        echo "Failed to detect Keycloak startup after $max_attempts attempts"
        echo "Last response: $RESPONSE_BODY"
        exit 1
    fi
    
    sleep 2
    attempt=$((attempt + 1))
done

# Save PID to file for later use
echo $KC_PID > keycloak.pid

echo "Keycloak is running with PID: $KC_PID"
echo "Management interface: http://localhost:$KC_MGMT_PORT"
echo "Application interface: http://localhost:$KC_HTTP_PORT/auth"