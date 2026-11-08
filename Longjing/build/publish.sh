#!/bin/bash

try_upload() {
  RETRY='0'
  SUCCESS=''
  while [ $RETRY -lt 5 ]; do
    aws s3 cp --endpoint-url "$S3_ENDPOINT" $1 $2 && { SUCCESS='yes'; break; }
    RETRY=$[$RETRY+1]
    echo "Retry $RETRY time(s)..."
  done
  if [ -z "$SUCCESS" ]; then
    aws s3 cp --endpoint-url "$S3_ENDPOINT" $1 $2 || { echo 'Failed to upload file to TeaCon Archive Service'; exit -1; }
  fi
}

echo Artifact file name is $ARTIFACT_NAME, located at $ARTIFACT_LOCAL_PATH

# Full path in the bucket
ARTIFACT_PATH="2026/ci/build/team-$TEAM_ID/$TEAM_ID-$GITHUB_RUN_NUMBER.jar"

# Test the archive first. If fails, then we terminates the process early
zip -T $ARTIFACT_LOCAL_PATH || { echo "Integrity check failed for $ARTIFACT_LOCAL_PATH, not a valid zip file"; exit 1; }

# Caculate SHA-256 checksum, keep only the checksum part, output to a file
shasum -a 256 $ARTIFACT_LOCAL_PATH | sed -E 's/ +.+//g' > sha256

# Do the actual upload
try_upload $ARTIFACT_LOCAL_PATH s3://$ARTIFACT_PATH
try_upload sha256 s3://$ARTIFACT_PATH.sha256

# Update the latest build info 

# Switch to git repo root, so that we can gather related info
cd repo

# Assemble the json
jq -n \
  --arg ln "$GITHUB_RUN_NUMBER" \
  --arg ch "$(git show --format='%H' --no-patch HEAD)" \
  --arg cm "$(git show --format='%s' --no-patch HEAD)" \
  --arg ct "$(git show --format='%cI' --no-patch HEAD)" \
  --arg lan "$(python3 -c 'import urllib.parse; print(urllib.parse.quote(input()))' <<< $ARTIFACT_NAME)" \
  '.commit_time = $ct | .commit_hash = $ch | .commit_message = $cm | .longjing_artifact_name = $lan | .longjing_number = ($ln | tonumber)' | tee latest.json

# Do upload, retry 5 times.
try_upload latest.json s3://2026/ci/build/team-$TEAM_ID/latest.json

curl -s -X POST -d @latest.json -H "Authorization: Bearer $BILUOCHUN_TOKEN" $BILUOCHUN_URL/api/v2/teams/$CONTEST_ID/$TEAM_SEQ/version/dev

# Pass out the escaped URL for other steps in the workflow to use
ARTIFACT_PATH_ESCAPED=$(python3 -c 'import urllib.parse; print(urllib.parse.quote(input()))' <<< $ARTIFACT_PATH)

echo "::notice title=Manual Download::You may download the build result at: https://archive.teacon.cn/$ARTIFACT_PATH_ESCAPED"
