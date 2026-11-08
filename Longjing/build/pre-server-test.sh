#!/bin/bash

# Fetch the list of dependency from Biluochun, then assemble the mod list to be read by 
# the dedicated-launch-test action.
# Fetching is done by curl.
# List assembling is done by jq. Filter is read as following:
#   - .[] iterates over all elements in JSON array
#   - select(.review_status == 1) selects all approved dependency
#   - select(.type != 1) selects non-client-only dependency (i.e. common and server-only)
#   - { name: ..., file: ... } transforms input JSON object into the needed format
#   - The enclosing [] collects everything into an JSON array 

die() {
    echo "::error::$*"
    exit -1
}

curl -so 'raw-deps.json' -H "Authorization: Bearer $BILUOCHUN_TOKEN" "$BILUOCHUN_URL/api/v2/teams/$CONTEST_SLUG/$TEAM_SEQ/deps"

# Check if any dependencies are rejected / under review.
# If so, the build cannot continue and we have to stop early. 
jq -r '[ .[] | select(.review_status != 1) ] | length | if . != 0 then halt_error(1) else "All dependencies are approved" end' raw-deps.json || die '发现仍在审核或已拒绝的前置库，构建无法继续'

# Check if any dependencies aren't ready
jq -r '[ .[] | select(.ready != true) ] | length | if . != 0 then halt_error(1) else "All dependencies are readt" end' raw-deps.json || die '发现未就绪的前置库，构建无法继续'

jq -M '[ .[] | select(.type != 1) | { name: .filename, file: .download_url } ]' raw-deps.json > main-deps.json

# If requested (see build.sh, line 38), fetch the list of all submitted works from Biluochun, 
# pick all submitted works with successful builds, then assemble the mod list to be read by 
# the dedicated-launch-test action.
# If not requested, we use an empty JSON array as a placeholder.
if [ "$LONGJING_REQUIRE_OTHER_WORKS" = "true" ]; then
  # We also need to download all dependencies in this case.
  curl -s $BILUOCHUN_URL/api/v1/contest/$CONTEST_SEQ/deps \
    | jq -M '[ .[] | select(.review_status == 1) | select(.type != 1) | { name: .filename, file: .download_url } ]' > main-deps.json
  curl -s $BILUOCHUN_URL/api/v1/contest/$CONTEST_SEQ/teams \
    | jq -M '[ .[] | select( .test_version != null ) | select( .ready ) | { mod_id: ( .work_id | gsub("_"; "-") ), build: .test_version.longjing_number, filename: .test_version.longjing_artifact_name }]' \
    | jq -M '[ .[] | { name: .filename, file: "https://archive.teacon.cn/2026/ci/build/team-\(.mod_id)/\(.mod_id)-\(.build).jar" } ]' > extra-deps.json
else
  echo '[]' > extra-deps.json
fi

# Merge two JSON array into one.
# Ref: https://stackoverflow.com/questions/42011086/merge-arrays-of-json
jq '.[]' main-deps.json extra-deps.json | jq -s > all-deps.json
