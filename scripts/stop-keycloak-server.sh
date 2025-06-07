#!/bin/bash

# Find Keycloak process - checking for both potential process names
KC_PID=$(ps aux | grep -E 'quarkus|kc.sh' | grep -v grep | awk '{print $2}')

if [ -z "$KC_PID" ]; then
    echo "No Keycloak server process found"
    exit 0
fi

echo "Found Keycloak processes: $KC_PID"
echo "Stopping Keycloak server..."

# Kill each found process
for pid in $KC_PID; do
    echo "Stopping PID: $pid"
    kill $pid
    
    # Wait for this process to stop
    while kill -0 $pid 2>/dev/null; do
        echo -n "."
        sleep 1
    done
    echo " Done"
done

echo "Keycloak server stopped"

# Verify no processes are left
remaining=$(ps aux | grep -E 'quarkus|kc.sh' | grep -v grep)
if [ ! -z "$remaining" ]; then
    echo "Warning: Some processes might still be running:"
    echo "$remaining"
    echo "You may need to force kill them with: kill -9 <PID>"
fi