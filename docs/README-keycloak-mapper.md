# How to Find and Check the Audience Mapper in Keycloak Admin UI (Version 26.2.5)

This guide will help you locate and verify the audience mapper that was created by the `setup-keycloak-realm.sh` script.
These instructions are specific to Keycloak version 26.2.5.

## What is the Audience Mapper?

The audience mapper is a protocol mapper in Keycloak that adds the client ID as an audience claim (`aud`) in the JWT
access token. This is important for security as it ensures that the token is intended for a specific client application.
When your application validates the JWT token, it should check that the audience claim matches its own client ID.

## Accessing the Keycloak Admin Console

1. Open your browser and navigate to the Keycloak Admin Console URL:
   ```
   http://localhost:8082/auth/admin/
   ```

2. Log in with the admin credentials:
    - Username: `admin`
    - Password: `admin`
      (Or use the credentials specified in your application-dev.conf file)

## Finding the Audience Mapper

1. **Select the Realm**:
    - In the top-left corner of the admin console, you'll see a dropdown menu.
    - Select the `eagle-bank` realm (or the realm name you configured).

2. **Navigate to Clients**:
    - In the left sidebar menu, click on `Clients`.
    - Find and click on the `eagle-bank-api` client in the list.

3. **Access the Client Details**:
   - After selecting the client, you'll see the client details page.

4. **Navigate to Dedicated Scopes**:
   - In the client details page, find and click on the `Dedicated scopes` section.
   - Select `eagle-bank-api-dedicated` from the list.

5. **Find the Audience Mapper**:
   - In the dedicated scopes page, look for the mapper details.
   - Find the entry named `audience-mapper`.
    - This is the mapper that was created by the setup script.

## Verifying the Audience Mapper Configuration

When you click on the `audience-mapper` entry, you should see its configuration details. Verify the following settings:

1. **Mapper Type**: Should be `Audience`
2. **Name**: Should be `audience-mapper`
3. **Included Client Audience**: Should be set to `eagle-bank-api` (your client ID)
4. **Included Custom Audience**: Should be empty
5. **Add to ID token**: Should be `OFF`
6. **Add to access token**: Should be `ON`
7. **Add to lightweight access token**: Should be `OFF`
8. **Add to token introspection**: Should be `ON`

These settings ensure that:

- The client ID is added as an audience claim in the access token and token introspection
- The audience claim is not added to the ID token or lightweight access token

## Testing the Audience Mapper

To verify that the audience mapper is working correctly:

1. **Get a Token**:
    - You can use the test user created by the script (username: `test-user`, password: `test123`)
    - Use a tool like Postman or curl to request a token from Keycloak

2. **Decode the Token**:
    - Use a JWT decoder (like [jwt.io](https://jwt.io/)) to decode the access token
    - Check that the `aud` claim in the payload contains your client ID (`eagle-bank-api`)

## Troubleshooting

If the audience mapper is not present or not working as expected:

1. **Check the Script Output**:
    - Run the setup script again and check for any error messages
    - Verify that the script reports "Audience mapper created successfully" or "Audience mapper already exists"

2. **Manual Creation**:
    - If needed, you can manually create the mapper in the Keycloak admin UI:
       - Navigate to Clients > eagle-bank-api > Dedicated scopes
       - Select "eagle-bank-api-dedicated"
       - Click "Add mapper" button
       - Select "By configuration" tab
        - Select "Audience" from the mapper type dropdown
        - Set the name to "audience-mapper"
       - Set "Included Client Audience" to "eagle-bank-api"
       - Leave "Included Custom Audience" empty
       - Set "Add to ID token" to OFF
       - Set "Add to access token" to ON
       - Set "Add to lightweight access token" to OFF
       - Set "Add to token introspection" to ON
       - Click "Save" button

## Why the Audience Mapper is Important

The audience mapper ensures that your tokens are only valid for your specific application. This is a security best
practice that prevents token misuse if they are somehow intercepted. Your application should validate that the `aud`
claim in incoming JWTs matches its own client ID.
