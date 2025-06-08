#!/bin/bash

# I currently don't have a local python setup.
# This should be migrated perhaps a container based test
# Perhaps written in test containers.  Needs thought as this kind of test while valid, can be very slow.

# Configuration
API_URL="http://localhost:8080/v1"
KEYCLOAK_URL="http://localhost:8082/auth"
KEYCLOAK_REALM="eagle-bank"
CLIENT_ID="eagle-bank-api"
CLIENT_SECRET="netmm3Bp2kC9RzzDOOeHyJ4IX67bRwVp"
DEFAULT_USERNAME="test-user"
DEFAULT_PASSWORD="test123"

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Function to print test results
print_result() {
    local test_name=$1
    local status=$2
    if [ $status -eq 0 ]; then
        echo -e "${GREEN}✓ $test_name passed${NC}"
    else
        echo -e "${RED}✗ $test_name failed${NC}"
    fi
}

# Function to get authentication token
get_auth_token() {
    echo -e "🔑 Requesting authentication token..."

    local token_response=$(curl -s -X POST \
        "$KEYCLOAK_URL/realms/$KEYCLOAK_REALM/protocol/openid-connect/token" \
        -H "Content-Type: application/x-www-form-urlencoded" \
        -d "client_id=$CLIENT_ID" \
        -d "client_secret=$CLIENT_SECRET" \
        -d "grant_type=password" \
        -d "username=$DEFAULT_USERNAME" \
        -d "password=$DEFAULT_PASSWORD")

    # Extract access token using grep and sed
    BTOKEN=$(echo "$token_response" | grep -o '"access_token":"[^"]*"' | sed 's/"access_token":"\(.*\)"/\1/')

    if [ -n "$BTOKEN" ]; then
        echo -e "${GREEN}✓ Successfully obtained authentication token${NC}"
        return 0
    else
        echo -e "${RED}✗ Failed to obtain authentication token${NC}"
        echo "Error response: $token_response"
        return 1
    fi
}

# Function to make API calls and check response
make_request() {
    local method=$1
    local endpoint=$2
    local expected_status=$3
    local data=$4
    local auth_header=$5

    echo "Testing $method $endpoint"

    if [ -n "$data" ]; then
        response=$(curl -s -w "\n%{http_code}" -X $method "$API_URL$endpoint" \
            -H "Content-Type: application/json" \
            -H "Accept: application/json" \
            ${auth_header:+-H "$auth_header"} \
            -d "$data")
    else
        response=$(curl -s -w "\n%{http_code}" -X $method "$API_URL$endpoint" \
            -H "Accept: application/json" \
            ${auth_header:+-H "$auth_header"})
    fi

    status_code=$(echo "$response" | tail -n1)
    response_body=$(echo "$response" | sed \$d)

    if [ "$status_code" -eq "$expected_status" ]; then
        print_result "$method $endpoint" 0
        echo "Response: $response_body"
        echo "$response_body" # Return response body for parsing
    else
        print_result "$method $endpoint" 1
        echo "Expected status $expected_status, got $status_code"
        echo "Response: $response_body"
        return 1
    fi
}

echo "🚀 Starting API test suite..."

# Step 1: Get authentication token
get_auth_token
if [ $? -ne 0 ]; then
    echo "Failed to obtain authentication token. Exiting..."
    exit 1
fi

# Set auth header with obtained token
AUTH_HEADER="Authorization: Bearer $BTOKEN"

# Step 2: Create a new user
echo -e "\n📝 Creating new user..."
create_user_data='{
    "name": "John Doe",
    "email": "john.doe@example.com",
    "address": {
        "line1": "123 High Street",
        "line2": "Apartment 4B",
        "town": "London",
        "county": "Greater London",
        "postcode": "SW1A 1AA"
    },
    "phoneNumber": "+447911123456"
}'

create_response=$(make_request "POST" "/users" 201 "$create_user_data")
user_id=$(echo "$create_response" | grep -o '"id":"[^"]*"' | head -n1 | sed 's/"id":"\([^"]*\)".*/\1/')

if [ -z "$user_id" ]; then
    echo "Failed to get user ID from creation response"
    echo "Response was: $create_response"
    exit 1
fi

echo "Created user with ID: $user_id"

# Test authenticated endpoints
echo -e "\n🔒 Testing authenticated endpoints..."

# Get all users
echo -e "\nGetting all users..."
make_request "GET" "/users" 200 "" "$AUTH_HEADER"

# Get specific user
echo -e "\nGetting specific user..."
make_request "GET" "/users/$user_id" 200 "" "$AUTH_HEADER"

# Update user
echo -e "\nUpdating user..."
update_user_data='{
    "name": "John Updated Doe",
    "address": {
        "line1": "456 New Street",
        "line2": "Suite 7C",
        "town": "London",
        "county": "Greater London",
        "postcode": "SW1A 2BB"
    },
    "phoneNumber": "+447911999999"
}'
make_request "PUT" "/users/$user_id" 200 "$update_user_data" "$AUTH_HEADER"

# Delete user
echo -e "\nDeleting user..."
make_request "DELETE" "/users/$user_id" 204 "" "$AUTH_HEADER"

# Verify deletion
echo -e "\nVerifying deletion..."
make_request "GET" "/users/$user_id" 404 "" "$AUTH_HEADER"

echo -e "\n✨ Test suite completed!"
