#!/bin/bash

# Read configuration from application-dev.conf
CONFIG_FILE="../src/main/resources/application-dev.conf"

# Default values
KC_HTTP_PORT=8082
REALM_NAME="eagle-bank"
CLIENT_ID="eagle-bank-api"
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

# Define the roles to create
ROLES=("user" "admin" "account-manager")

# Create roles if they don't exist
for ROLE in "${ROLES[@]}"; do
    echo "Checking if role '$ROLE' already exists..."
    ROLE_EXISTS=$(curl -s \
        -H "Authorization: Bearer $TOKEN" \
        "http://localhost:$KC_HTTP_PORT/auth/admin/realms/$REALM_NAME/roles" | grep -c "\"name\":\"$ROLE\"")

    if [ "$ROLE_EXISTS" -gt "0" ]; then
        echo "Role '$ROLE' already exists."
    else
        echo "Creating role '$ROLE'..."
        ROLE_CREATION=$(curl -s -o /dev/null -w "%{http_code}" -X POST "http://localhost:$KC_HTTP_PORT/auth/admin/realms/$REALM_NAME/roles" \
            -H "Authorization: Bearer $TOKEN" \
            -H "Content-Type: application/json" \
            -d "{
                \"name\": \"$ROLE\",
                \"description\": \"$ROLE role for Eagle Bank API\"
            }")

        if [ "$ROLE_CREATION" == "201" ] || [ "$ROLE_CREATION" == "204" ]; then
            echo "Role '$ROLE' created successfully."
        else
            echo "Failed to create role '$ROLE'. HTTP status: $ROLE_CREATION"
        fi
    fi
done

# Get client ID
echo "Getting client ID..."
CLIENTS_JSON=$(curl -s \
    -H "Authorization: Bearer $TOKEN" \
    "http://localhost:$KC_HTTP_PORT/auth/admin/realms/$REALM_NAME/clients")

# Extract the ID for the client with the matching clientId
CLIENT_DB_ID=$(echo "$CLIENTS_JSON" | grep -o "{[^}]*\"clientId\":\"$CLIENT_ID\"[^}]*}" | grep -o '"id":"[^"]*"' | cut -d'"' -f4)

if [ -z "$CLIENT_DB_ID" ]; then
    echo "Failed to get client ID for '$CLIENT_ID'. Role mapper will not be added."
else
    echo "Client ID for '$CLIENT_ID' is: $CLIENT_DB_ID"

    # Check if realm roles mapper already exists
    echo "Checking if realm roles mapper already exists..."
    MAPPER_EXISTS=$(curl -s \
        -H "Authorization: Bearer $TOKEN" \
        "http://localhost:$KC_HTTP_PORT/auth/admin/realms/$REALM_NAME/clients/$CLIENT_DB_ID/protocol-mappers/models" | grep -c "\"name\":\"realm-roles-mapper\"")

    if [ "$MAPPER_EXISTS" -gt "0" ]; then
        echo "Realm roles mapper already exists."
    else
        echo "Adding realm roles mapper..."
        MAPPER_CREATION=$(curl -s -o /dev/null -w "%{http_code}" -X POST "http://localhost:$KC_HTTP_PORT/auth/admin/realms/$REALM_NAME/clients/$CLIENT_DB_ID/protocol-mappers/models" \
            -H "Authorization: Bearer $TOKEN" \
            -H "Content-Type: application/json" \
            -d "{
                \"name\": \"realm-roles-mapper\",
                \"protocol\": \"openid-connect\",
                \"protocolMapper\": \"oidc-usermodel-realm-role-mapper\",
                \"consentRequired\": false,
                \"config\": {
                    \"multivalued\": \"true\",
                    \"userinfo.token.claim\": \"true\",
                    \"id.token.claim\": \"true\",
                    \"access.token.claim\": \"true\",
                    \"claim.name\": \"realm_access.roles\",
                    \"jsonType.label\": \"String\"
                }
            }")

        if [ "$MAPPER_CREATION" == "201" ] || [ "$MAPPER_CREATION" == "204" ]; then
            echo "Realm roles mapper created successfully."
        else
            echo "Failed to create realm roles mapper. HTTP status: $MAPPER_CREATION"
        fi
    fi
fi

# Get test user ID
echo "Getting test user ID..."
USERS_JSON=$(curl -s \
    -H "Authorization: Bearer $TOKEN" \
    "http://localhost:$KC_HTTP_PORT/auth/admin/realms/$REALM_NAME/users?username=test-user")

# Extract the ID for the user with the matching username
USER_ID=$(echo "$USERS_JSON" | grep -o "{[^}]*\"username\":\"test-user\"[^}]*}" | grep -o '"id":"[^"]*"' | cut -d'"' -f4)

if [ -z "$USER_ID" ]; then
    echo "Failed to get user ID for 'test-user'. Roles will not be assigned."
else
    echo "User ID for 'test-user' is: $USER_ID"

    # Assign roles to the test user
    for ROLE in "${ROLES[@]}"; do
        echo "Getting role ID for '$ROLE'..."
        ROLE_JSON=$(curl -s \
            -H "Authorization: Bearer $TOKEN" \
            "http://localhost:$KC_HTTP_PORT/auth/admin/realms/$REALM_NAME/roles/$ROLE")
        
        ROLE_ID=$(echo "$ROLE_JSON" | grep -o '"id":"[^"]*"' | cut -d'"' -f4)
        
        if [ -z "$ROLE_ID" ]; then
            echo "Failed to get role ID for '$ROLE'. Role will not be assigned."
        else
            echo "Assigning role '$ROLE' to user 'test-user'..."
            ROLE_ASSIGNMENT=$(curl -s -o /dev/null -w "%{http_code}" -X POST "http://localhost:$KC_HTTP_PORT/auth/admin/realms/$REALM_NAME/users/$USER_ID/role-mappings/realm" \
                -H "Authorization: Bearer $TOKEN" \
                -H "Content-Type: application/json" \
                -d "[{
                    \"id\": \"$ROLE_ID\",
                    \"name\": \"$ROLE\"
                }]")

            if [ "$ROLE_ASSIGNMENT" == "204" ]; then
                echo "Role '$ROLE' assigned successfully to user 'test-user'."
            else
                echo "Failed to assign role '$ROLE' to user 'test-user'. HTTP status: $ROLE_ASSIGNMENT"
            fi
        fi
    done
fi

echo ""
echo "=== Keycloak Roles Setup Complete! ==="
echo "Created roles: ${ROLES[*]}"
echo "Added realm roles mapper to client: $CLIENT_ID"
echo "Assigned roles to test user: test-user"
echo ""
echo "You can now use these roles in your application for role-based access control."
echo "=== End of Setup ==="