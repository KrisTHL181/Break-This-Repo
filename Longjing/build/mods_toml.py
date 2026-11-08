#!/usr/bin/env python3

'''
This simple sub-routine will do basic check against build artifact to make sure

1. The Mod ID declared in the mod manifest file matches the one we have in Biluochun.
2. The mod size is within the limit, or is allowed to exceed the limit.
'''

import json
import os
import tomllib
import zipfile

if os.environ.get('LICENSE_REPORT', None):
    with open(os.environ.get('LICENSE_REPORT')) as f:
        report = json.load(f)
        for license_obj in report['licenses']:
            print(f"Detected license: {license_obj['spdx_id']}")

# Retrieve mod id for current team, fail if not configured (should not happen)
team_id=os.environ.get('TEAM_ID', None)
if not team_id:
    print('Team id is not found or is empty, which should not happen!')
    exit(-1)

# Check if the mod id declared in Biluochun exists in neoforge.mods.toml
# Fails if not found.
with zipfile.ZipFile(os.environ['ARTIFACT_LOCAL_PATH']) as z:
    with z.open('META-INF/neoforge.mods.toml') as f:
        metadata=tomllib.load(f)
        mods=metadata.get('mods', [])
        if not any(mod.get('modId', '').replace('_', '-') == team_id for mod in mods):
            print(f"::warning::The mod id list {[mod.get('modId', '') for mod in mods]}",
                "defined in neoforge.mods.toml does not contains any mod id matching the team id",
                f"({team_id}) so this build will fail", flush=True)
            exit(-1)
        license=metadata.get('license', None)
        if license:
            print(f"::notice::This jar declared the following license: {license}")
        else:
            print(f"::error::This jar either did not declare license, or declared empty string as license. This will fail in runtime!")

file_size=os.path.getsize(os.environ['ARTIFACT_LOCAL_PATH'])
if file_size > 10 * 1024 * 1024: # 10485760 bytes, aka 10MiB
    if os.path.exists('./mods/team-' + team_id.replace('_', '-') + '/oversize-permit'):
        print(f"::notice::Build output exceeds the 10MiB limit (actual size {file_size} bytes); permission has acquired to exceed this limit")
    else:
        print(f"::error::Build output exceeds the 10MiB limit (actual size {file_size} bytes); however, the mod has not acquired explicit permission for exceeded size")
        exit(-1)

print("Preliminary check has all passed.")