#!/bin/bash

# Configuration variables
KC_HTTP_PORT="${KC_HTTP_PORT:-8082}"
KC_MGMT_PORT="${KC_MGMT_PORT:-9002}"
REALM_NAME="eagle-bank"
CLIENT_ID="eagle-bank-api"
ADMIN_USER="admin"
ADMIN_PASSWORD="admin"

echo "Checking Keycloak availability..."
HEALTH_CHECK=$(curl -s -w "%{http_code}" "http://localhost:$KC_MGMT_PORT/auth/health/ready" -o /dev/null)
if [ "$HEALTH_CHECK" != "200" ]; then
    echo "Error: Keycloak is not running or not ready. Please start it first."
    exit 1
fi

echo "Getting admin token..."
TOKEN=$(curl -s -X POST "http://localhost:$KC_HTTP_PORT/auth/realms/master/protocol/openid-connect/token" \
    -H "Content-Type: application/x-www-form-urlencoded" \
    -d "username=$ADMIN_USER" \
    -d "password=$ADMIN_PASSWORD" \
    -d "grant_type=password" \
    -d "client_id=admin-cli" | grep -o '"access_token":"[^"]*' | cut -d'"' -f4)

if [ -z "$TOKEN" ]; then
    echo "Error: Failed to get admin token"
    exit 1
fi

# Check if realm exists
echo "Checking if realm exists..."
REALM_CHECK=$(curl -s -w "%{http_code}" -o /dev/null \
    -H "Authorization: Bearer $TOKEN" \
    "http://localhost:$KC_HTTP_PORT/auth/admin/realms/$REALM_NAME")

if [ "$REALM_CHECK" = "404" ]; then
    echo "Creating realm..."
    curl -s -X POST "http://localhost:$KC_HTTP_PORT/auth/admin/realms" \
        -H "Authorization: Bearer $TOKEN" \
        -H "Content-Type: application/json" \
        -d "{
            \"realm\": \"$REALM_NAME\",
            \"enabled\": true,
            \"displayName\": \"Eagle Bank API\"
        }"
else
    echo "Realm already exists"
fi

# Check if client exists
echo "Checking if client exists..."
CLIENT_CHECK=$(curl -s \
    -H "Authorization: Bearer $TOKEN" \
    "http://localhost:$KC_HTTP_PORT/auth/admin/realms/$REALM_NAME/clients" | grep "\"clientId\":\"$CLIENT_ID\"")

if [ -z "$CLIENT_CHECK" ]; then
    echo "Creating client..."
    curl -s -X POST "http://localhost:$KC_HTTP_PORT/auth/admin/realms/$REALM_NAME/clients" \
        -H "Authorization: Bearer $TOKEN" \
        -H "Content-Type: application/json" \
        -d "{
            \"clientId\": \"$CLIENT_ID\",
            \"enabled\": true,
            \"protocol\": \"openid-connect\",
            \"publicClient\": false,
            \"standardFlowEnabled\": true,
            \"directAccessGrantsEnabled\": true,
            \"serviceAccountsEnabled\": true,
            \"redirectUris\": [\"http://localhost:8080/*\"],
            \"webOrigins\": [\"http://localhost:8080\"]
        }"
else
    echo "Client already exists"
fi

# Create roles if they don't exist
for ROLE in "user" "admin" "account-manager"; do
    echo "Checking role: $ROLE"
    ROLE_CHECK=$(curl -s -w "%{http_code}" -o /dev/null \
        -H "Authorization: Bearer $TOKEN" \
        "http://localhost:$KC_HTTP_PORT/auth/admin/realms/$REALM_NAME/roles/$ROLE")
    
    if [ "$ROLE_CHECK" = "404" ]; then
        echo "Creating role: $ROLE"
        curl -s -X POST "http://localhost:$KC_HTTP_PORT/auth/admin/realms/$REALM_NAME/roles" \
            -H "Authorization: Bearer $TOKEN" \
            -H "Content-Type: application/json" \
            -d "{
                \"name\": \"$ROLE\",
                \"description\": \"$ROLE role for Eagle Bank API\"
            }"
    else
        echo "Role $ROLE already exists"
    fi
done

# Check if test user exists
echo "Checking if test user exists..."
USER_CHECK=$(curl -s \
    -H "Authorization: Bearer $TOKEN" \
    "http://localhost:$KC_HTTP_PORT/auth/admin/realms/$REALM_NAME/users?username=test-user")

if [[ "$USER_CHECK" == "[]" ]]; then
    echo "Creating test user..."
    curl -s -X POST "http://localhost:$KC_HTTP_PORT/auth/admin/realms/$REALM_NAME/users" \
        -H "Authorization: Bearer $TOKEN" \
        -H "Content-Type: application/json" \
        -d "{
            \"username\": \"test-user\",
            \"enabled\": true,
            \"emailVerified\": true,
            \"credentials\": [{
                \"type\": \"password\",
                \"value\": \"test123\",
                \"temporary\": false
            }]
        }"

    # Get user ID for role assignment
    USER_ID=$(curl -s \
        -H "Authorization: Bearer $TOKEN" \
        "http://localhost:$KC_HTTP_PORT/auth/admin/realms/$REALM_NAME/users?username=test-user" | grep -o '"id":"[^"]*' | cut -d'"' -f4)

    if [ ! -z "$USER_ID" ]; then
        echo "Assigning roles to user..."
        for ROLE in "user" "admin"; do
            ROLE_ID=$(curl -s \
                -H "Authorization: Bearer $TOKEN" \
                "http://localhost:$KC_HTTP_PORT/auth/admin/realms/$REALM_NAME/roles/$ROLE" | grep -o '"id":"[^"]*' | cut -d'"' -f4)
            
            if [ ! -z "$ROLE_ID" ]; then
                curl -s -X POST \
                    -H "Authorization: Bearer $TOKEN" \
                    -H "Content-Type: application/json" \
                    -d "[{\"id\": \"$ROLE_ID\", \"name\": \"$ROLE\"}]" \
                    "http://localhost:$KC_HTTP_PORT/auth/admin/realms/$REALM_NAME/users/$USER_ID/role-mappings/realm"
                echo "Assigned role $ROLE to test user"
            fi
        done
    fi
else
    echo "Test user already exists"
fi

echo "Setup complete!"
echo "Realm: ${REALM_NAME}"
echo "Client ID: ${CLIENT_ID}"
echo "Test user: test-user"
echo "Test password: test123"