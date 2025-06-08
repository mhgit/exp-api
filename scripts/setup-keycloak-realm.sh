#!/bin/bash

# Read configuration from application-dev.conf
CONFIG_FILE="../src/main/resources/application-dev.conf"

# Default values
KC_HTTP_PORT=8082
REALM_NAME="eagle-bank"
CLIENT_ID="eagle-bank-api"
CLIENT_SECRET="netmm3Bp2kC9RzzDOOeHyJ4IX67bRwVp"
ADMIN_USERNAME="admin"
ADMIN_PASSWORD="admin"

# Extract values from config file if it exists
if [ -f "$CONFIG_FILE" ]; then
    echo "Reading configuration from $CONFIG_FILE..."

    # Extract server URL and port
    SERVER_URL=$(grep -A 10 "keycloak {" "$CONFIG_FILE" | grep "serverUrl" | cut -d'"' -f2)
    if [ ! -z "$SERVER_URL" ]; then
        KC_HTTP_PORT=$(echo "$SERVER_URL" | grep -o ':[0-9]*' | cut -d':' -f2)
    fi

    # Extract realm
    CONFIG_REALM=$(grep -A 10 "keycloak {" "$CONFIG_FILE" | grep "realm" | head -1 | cut -d'"' -f2)
    if [ ! -z "$CONFIG_REALM" ]; then
        REALM_NAME="$CONFIG_REALM"
    fi

    # Extract client ID
    CONFIG_CLIENT_ID=$(grep -A 10 "keycloak {" "$CONFIG_FILE" | grep "clientId" | cut -d'"' -f2)
    if [ ! -z "$CONFIG_CLIENT_ID" ]; then
        CLIENT_ID="$CONFIG_CLIENT_ID"
    fi

    # Extract client secret
    CONFIG_CLIENT_SECRET=$(grep -A 10 "keycloak {" "$CONFIG_FILE" | grep "clientSecret" | cut -d'"' -f2)
    if [ ! -z "$CONFIG_CLIENT_SECRET" ]; then
        CLIENT_SECRET="$CONFIG_CLIENT_SECRET"
    fi

    # Extract admin credentials
    CONFIG_ADMIN_USERNAME=$(grep -A 10 "keycloak {" "$CONFIG_FILE" | grep "adminUsername" | cut -d'"' -f2)
    if [ ! -z "$CONFIG_ADMIN_USERNAME" ]; then
        ADMIN_USERNAME="$CONFIG_ADMIN_USERNAME"
    fi

    CONFIG_ADMIN_PASSWORD=$(grep -A 10 "keycloak {" "$CONFIG_FILE" | grep "adminPassword" | cut -d'"' -f2)
    if [ ! -z "$CONFIG_ADMIN_PASSWORD" ]; then
        ADMIN_PASSWORD="$CONFIG_ADMIN_PASSWORD"
    fi
fi

echo "Using configuration:"
echo "Server port: $KC_HTTP_PORT"
echo "Realm: $REALM_NAME"
echo "Client ID: $CLIENT_ID"
echo "Admin username: $ADMIN_USERNAME"

# Wait for Keycloak to be available
echo "Checking Keycloak availability..."
while ! curl -s "http://localhost:$KC_HTTP_PORT/auth" > /dev/null; do
    sleep 1
done

# Get admin token
echo "Getting admin token..."
TOKEN=$(curl -s -X POST "http://localhost:$KC_HTTP_PORT/auth/realms/master/protocol/openid-connect/token" \
    -H "Content-Type: application/x-www-form-urlencoded" \
    -d "username=$ADMIN_USERNAME" \
    -d "password=$ADMIN_PASSWORD" \
    -d "grant_type=password" \
    -d "client_id=admin-cli" | grep -o '"access_token":"[^"]*"' | cut -d'"' -f4)

# Check if token was obtained
if [ -z "$TOKEN" ]; then
    echo "Failed to obtain admin token. Check admin credentials and Keycloak availability."
    exit 1
fi
echo "Admin token obtained successfully."

# Check if realm already exists
echo "Checking if realm already exists..."
REALM_EXISTS=$(curl -s -o /dev/null -w "%{http_code}" \
    -H "Authorization: Bearer $TOKEN" \
    "http://localhost:$KC_HTTP_PORT/auth/admin/realms/$REALM_NAME")

if [ "$REALM_EXISTS" == "200" ]; then
    echo "Realm '$REALM_NAME' already exists."
else
    # Create realm
    echo "Creating realm '$REALM_NAME'..."
    REALM_CREATION=$(curl -s -o /dev/null -w "%{http_code}" -X POST "http://localhost:$KC_HTTP_PORT/auth/admin/realms" \
        -H "Authorization: Bearer $TOKEN" \
        -H "Content-Type: application/json" \
        -d "{
            \"realm\": \"$REALM_NAME\",
            \"enabled\": true,
            \"accessTokenLifespan\": 300
        }")

    if [ "$REALM_CREATION" == "201" ] || [ "$REALM_CREATION" == "204" ]; then
        echo "Realm '$REALM_NAME' created successfully."
    else
        echo "Failed to create realm. HTTP status: $REALM_CREATION"
        # Continue anyway as the realm might already exist but with a different response code
    fi
fi

# Check if client already exists
echo "Checking if client already exists..."
CLIENT_EXISTS=$(curl -s \
    -H "Authorization: Bearer $TOKEN" \
    "http://localhost:$KC_HTTP_PORT/auth/admin/realms/$REALM_NAME/clients" | grep -c "\"clientId\":\"$CLIENT_ID\"")

if [ "$CLIENT_EXISTS" -gt "0" ]; then
    echo "Client '$CLIENT_ID' already exists."
else
    # Create client
    echo "Creating client '$CLIENT_ID'..."
    CLIENT_CREATION=$(curl -s -o /dev/null -w "%{http_code}" -X POST "http://localhost:$KC_HTTP_PORT/auth/admin/realms/$REALM_NAME/clients" \
        -H "Authorization: Bearer $TOKEN" \
        -H "Content-Type: application/json" \
        -d "{
            \"clientId\": \"$CLIENT_ID\",
            \"secret\": \"$CLIENT_SECRET\",
            \"enabled\": true,
            \"protocol\": \"openid-connect\",
            \"publicClient\": false,
            \"standardFlowEnabled\": true,
            \"directAccessGrantsEnabled\": true,
            \"serviceAccountsEnabled\": true,
            \"redirectUris\": [\"http://localhost:8080/*\"],
            \"webOrigins\": [\"http://localhost:8080\"]
        }")

    if [ "$CLIENT_CREATION" == "201" ] || [ "$CLIENT_CREATION" == "204" ]; then
        echo "Client '$CLIENT_ID' created successfully."
    else
        echo "Failed to create client. HTTP status: $CLIENT_CREATION"
        # Continue anyway as the client might already exist but with a different response code
    fi
fi

# Get client ID
echo "Getting client ID..."
CLIENTS_JSON=$(curl -s \
    -H "Authorization: Bearer $TOKEN" \
    "http://localhost:$KC_HTTP_PORT/auth/admin/realms/$REALM_NAME/clients")

# Extract the ID for the client with the matching clientId
CLIENT_DB_ID=$(echo "$CLIENTS_JSON" | grep -o "{[^}]*\"clientId\":\"$CLIENT_ID\"[^}]*}" | grep -o '"id":"[^"]*"' | cut -d'"' -f4)

if [ -z "$CLIENT_DB_ID" ]; then
    echo "Failed to get client ID for '$CLIENT_ID'. Audience mapper will not be added."
else
    echo "Client ID for '$CLIENT_ID' is: $CLIENT_DB_ID"

    # Check if audience mapper already exists
    echo "Checking if audience mapper already exists..."
    MAPPER_EXISTS=$(curl -s \
        -H "Authorization: Bearer $TOKEN" \
        "http://localhost:$KC_HTTP_PORT/auth/admin/realms/$REALM_NAME/clients/$CLIENT_DB_ID/protocol-mappers/models" | grep -c "\"name\":\"audience-mapper\"")

    if [ "$MAPPER_EXISTS" -gt "0" ]; then
        echo "Audience mapper already exists."
    else
        echo "Adding audience mapper..."
        MAPPER_CREATION=$(curl -s -o /dev/null -w "%{http_code}" -X POST "http://localhost:$KC_HTTP_PORT/auth/admin/realms/$REALM_NAME/clients/$CLIENT_DB_ID/protocol-mappers/models" \
            -H "Authorization: Bearer $TOKEN" \
            -H "Content-Type: application/json" \
            -d "{
                \"name\": \"audience-mapper\",
                \"protocol\": \"openid-connect\",
                \"protocolMapper\": \"oidc-audience-mapper\",
                \"consentRequired\": false,
                \"config\": {
                    \"included.client.audience\": \"$CLIENT_ID\",
                    \"id.token.claim\": \"false\",
                    \"access.token.claim\": \"true\"
                }
            }")

        if [ "$MAPPER_CREATION" == "201" ] || [ "$MAPPER_CREATION" == "204" ]; then
            echo "Audience mapper created successfully."
        else
            echo "Failed to create audience mapper. HTTP status: $MAPPER_CREATION"
        fi
    fi
fi

# Check if test user already exists
echo "Checking if test user already exists..."
USER_EXISTS=$(curl -s \
    -H "Authorization: Bearer $TOKEN" \
    "http://localhost:$KC_HTTP_PORT/auth/admin/realms/$REALM_NAME/users?username=test-user" | grep -c "\"username\":\"test-user\"")

if [ "$USER_EXISTS" -gt "0" ]; then
    echo "Test user 'test-user' already exists."
else
    # Create test user
    echo "Creating test user 'test-user'..."
    USER_CREATION=$(curl -s -o /dev/null -w "%{http_code}" -X POST "http://localhost:$KC_HTTP_PORT/auth/admin/realms/$REALM_NAME/users" \
        -H "Authorization: Bearer $TOKEN" \
        -H "Content-Type: application/json" \
        -d "{
            \"username\": \"test-user\",
            \"enabled\": true,
            \"credentials\": [{
                \"type\": \"password\",
                \"value\": \"test123\",
                \"temporary\": false
            }]
        }")

    if [ "$USER_CREATION" == "201" ] || [ "$USER_CREATION" == "204" ]; then
        echo "Test user 'test-user' created successfully."
    else
        echo "Failed to create test user. HTTP status: $USER_CREATION"
    fi
fi

echo ""
echo "=== Keycloak Setup Complete! ==="
echo "Keycloak Admin Console: http://localhost:$KC_HTTP_PORT/auth/admin/"
echo "Realm: $REALM_NAME"
echo "Client ID: $CLIENT_ID"
echo "Client Secret: $CLIENT_SECRET"
echo "Test user: test-user"
echo "Test password: test123"
echo ""
echo "To use these credentials in your application, ensure the following settings in application-dev.conf:"
echo "keycloak {"
echo "    serverUrl = \"http://localhost:$KC_HTTP_PORT/auth\""
echo "    realm = \"$REALM_NAME\""
echo "    clientId = \"$CLIENT_ID\""
echo "    clientSecret = \"$CLIENT_SECRET\""
echo "    adminUsername = \"$ADMIN_USERNAME\""
echo "    adminPassword = \"$ADMIN_PASSWORD\""
echo "}"
echo ""
echo "JWT issuer URL: http://localhost:$KC_HTTP_PORT/auth/realms/$REALM_NAME"
echo "=== End of Setup ==="
