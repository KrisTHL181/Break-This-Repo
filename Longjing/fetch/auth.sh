#!/bin/bash
# 
# This bash script is to authenticate as a GitHub App installation, and then configure 
# git to allow us pushing commits on behalf of the GitHub App.
# Documentation about authenticate as a GitHub App installation may be found at:
# https://docs.github.com/en/developers/apps/authenticating-with-github-apps#authenticating-as-an-installation
#
# Special thanks to this gist on how to sign JWT with OpenSSL command-line interface:
# https://gist.github.com/indrayam/dd47bf6eef849a57c07016c0036f5207

function b64url() {
  echo $1 | openssl enc -base64 -A | tr '+/' '-_' | tr -d '='
}

# Step 1: Generate JWT header.
HEAD='{"alg":"RS256","typ":"JWT"}'

# Step 2: Generate JWT claim.
# Env variable $APP_ID is supplied via GitHub Action.
TIME_NOW=`date +%s`
IAT=`expr $TIME_NOW - 30`
EXP=`expr $TIME_NOW + 60`
PAYLOAD="{\"iat\":$IAT,\"exp\":$EXP,\"iss\":$APP_ID}"

# Step 3: Assemble the payload to sign and produce the signature.
BODY="`b64url $HEAD`.`b64url $PAYLOAD`"
echo "$APP_PRIVATE_KEY" > ./pkey.pem
SIG=`echo -n $BODY | openssl dgst -binary -sha256 -sign ./pkey.pem | openssl enc -base64 -A | tr '+/' '-_' | tr -d '='`
rm -f ./pkey.pem

# Step 4: Assemble the final JWT.
JWT="$BODY.$SIG"

# Step 5: List installations; then get the first one.
# For now, there should be only one installation listed: our organization.
ACCESS_TOKEN_URL=`curl --silent -H "Authorization: Bearer $JWT" -H "Accept: application/vnd.github.v3+json" https://api.github.com/app/installations | jq -r .[0].access_tokens_url`
# Step 6: Authenticate as a GitHub App installation.
TOKEN=`curl --silent -X POST -H "Authorization: Bearer $JWT" -H "Accept: application/vnd.github.v3+json" $ACCESS_TOKEN_URL | jq -r .token`

# Step 7: Passdown token to further steps in our GitHub Action workflow, if detected
if [ $GITHUB_ACTIONS ]
then
  echo "::add-mask::$TOKEN"
  echo "token=$TOKEN" >> $GITHUB_OUTPUT
fi
