#!/usr/bin/env python3

import json
import os
import os.path
import subprocess
import sys
import uuid

from string import Template
from urllib.error import HTTPError
from urllib.request import Request, urlopen
from typing import List, Optional, TypedDict, Literal, Tuple

# Print the fetch error message
def fetch_error(msg: str, force_exit: bool = True):
    if os.getenv('GITHUB_ACTIONS', False):
        print(f'::error::{msg} Check log for details')
    else:
        print(msg)
    print(sys.exc_info())
    if force_exit:
        exit(-1)

# Get the contest id
def get_contest_id() -> Tuple[int, str, str]:
    try:
        url = 'https://biluochun.teacon.cn/api/v1/contest/current'
        with urlopen(url, timeout=15) as body:
            contest = json.load(body)
            return contest['id'], contest['teekie_domain'], contest['title']    
    except:
        fetch_error("Error occurred while fetching current event information.")

class Team(TypedDict):
    id: int; name: str
    type: Literal[0, 1, 2]
    repo: Optional[str]; branch: Optional[str]
    work_id: str; work_name: str; work_description: str
    ready: bool

# Fetch latest team information from Biluochun (TeaCon Admission Portal) API
# Remark: https://bugs.python.org/issue40321
# See also https://github.com/python/cpython/pull/19588
# That issue exists back when Biluochun was still built on Flask.
def get_teams(contest_id: int) -> List[Team]:
    team_list = []
    try:
        print('Fetching team/mod list')
        url = f'https://biluochun.teacon.cn/api/v1/contest/{contest_id}/teams'
        with urlopen(url, timeout = 15) as f:
            team_list: List[Team] = json.load(f)
            team_list.sort(key=lambda t: t['id'])
            print('Successfully fetched team/mod list')
            return team_list
    except:
        fetch_error('Error occured while fetching participting team list.')

def get_team_dependencies(contest_slug: str, team_id: int):
    try:
        req = Request(f'https://biluochun.teacon.cn/api/v2/teams/{contest_slug}/{team_id}/deps',
            headers = { 'Authorization': f'Bearer {os.environ["BILUOCHUN_TOKEN"]}' })
        with urlopen(req, timeout = 15) as f:
            deps = json.load(f)
            return deps, True
    except:
        fetch_error(f'Error occured while fetching dependencies of team#{team_id} (event "{contest_slug}").', force_exit = False)
        return [], False

# Load GitHub Action Workflow template
# As we only perform simple string subtitution, we uses the built-in string.Template
def load_workflow_template() -> Optional[str]:
    workflow_template: Optional[str] = None
    with open('fetch/workflow_template.yaml') as f:
        workflow_template = Template(f.read())
    return workflow_template

def disable_workflow(team_id: str):
    if not os.environ.get('GITHUB_ACTIONS', None):
        return
    req=Request(f"https://api.github.com/repos/teaconmc/Longjing/actions/workflows/mod-team-{team_id}.yaml/disable", method='PUT', 
        headers={ 'Accept': 'application/vnd.github.v3+json', 'Authorization': 'token ' + os.environ['GITHUB_TOKEN'] })
    try:
        with urlopen(req):
            print(f"Disabled workflow mod-team-{team_id}.yaml")
    except HTTPError as resp:
        if resp.code != 404:
            details=json.load(resp)
            print(f"Error occured while disabling workflow mod-team-{team_id}.yaml (status {resp.code}): {details['message']}")

def enable_workflow(team_id: str):
    if not os.environ.get('GITHUB_ACTIONS', None):
        return
    req=Request(f"https://api.github.com/repos/teaconmc/Longjing/actions/workflows/mod-team-{team_id}.yaml/enable", method='PUT', 
        headers={ 'Accept': 'application/vnd.github.v3+json', 'Authorization': 'token ' + os.environ['GITHUB_TOKEN'] })
    try:
        with urlopen(req):
            print(f"Enabled workflow mod-team-{team_id}.yaml")
    except HTTPError as resp:
        pass # Ignore all the possible errors during workflow enabling

# Write team informations including workflow files and git head references
def write_team_info(team: Team, contest_seq: int, contest_name: str, contest_slug: str, workflow_template: str, force_update: bool = False) -> None:
    # Skip the process entirely if team['ready'] == False
    if not team['ready']:
        print(f"Team #{team['id']} ({team['name']}) is not ready for Longjing build, skipping")
        if os.getenv('GITHUB_ACTIONS', False):
            print(f"::warning::Team #{team['id']} ({team['name']}) is skipped because it is not marked as ready for Longjing build.")
        return

    head_ref = 'HEAD' if team['branch'] is None else team['branch']
    print("Fetching information for team",
          f"#{team['id']} ({team['name']}) from {team['repo']} (ref {head_ref})")

    skip = False

    if not team['repo']:
        print(f"Team #{team['id']} ({team['name']}) did not provide a git repo address, skipping")
        if os.getenv('GITHUB_ACTIONS', False):
            print(f"::warning::Team #{team['id']} ({team['name']}) did not provide a git repo address.")
        skip = True
    
    # Escape ' by doubling it. YAML decides to use this for single-quoted strings.
    team['name'] = team['name'].replace("'", "''")
    team['work_name'] = team['work_name'].replace("'", "''")

    team_id = team['work_id'].replace('_', '-')

    # Each team gets their own directory to storing related information.
    # Internal team id is used because it is permenant.
    info_dir = f"mods/team-{team_id}"

    # Create workflow run, or force update it if already exist
    workflow_file = f".github/workflows/mod-team-{team_id}.yaml"
    with open(workflow_file, 'w') as f:
        f.write(workflow_template.substitute(
            title=f"{contest_name} | {team['work_name']} | {team['name']}",
            contest_seq=contest_seq,
            contest_slug=contest_slug,
            contest_title=contest_name,
            job_title=f"Build {team['work_name']}",
            workflow_file=workflow_file,
            info_dir=info_dir,
            team_id=team_id,
            team_seq=team['id']))

    if skip:
        return

    # Enable their workflow
    enable_workflow(team_id)
    # New team is signalled as absence of their tracking info in our repo. 
    # If a new team is found, we then create a directory for them.
    # Create meta information directory
    os.makedirs(info_dir, exist_ok=True)
    # Skip teams that have declared withdrawal
    #if os.path.exists(f"{info_dir}/withdrawn"):
    if team['type'] == 2:
        print(f"Team #{team['id']} ({team['name']}) has withdrawn, skipping")
        return
    # Write repo address to $info_dir/remote
    # This is always done in case that a team updates their remote repo address.
    with open(f"{info_dir}/remote", 'w') as f:
        f.write(team['repo'])

    # If force_update is set, write a random UUID to the sentinel file to trigger a new update.
    if force_update:
        with open(f"{info_dir}/sentinel", 'w') as f:
            f.write(uuid.uuid4().hex)
    
    # We use `git ls-remote $repo_url HEAD` to get the latest commit and use it to 
    # determine if we should trigger a build.
    # The sole criterion is "current HEAD commit hash is different from our recorded one".
    # We store the HEAD commit hash in `mods/team-${team_id}/HEAD`.
    # A new build is triggered by updating that recorded commit hash.
    previous_head = None
    if os.path.exists(f"{info_dir}/HEAD"):
        with open(f"{info_dir}/HEAD") as f:
            previous_head = f.read()
    try:
        # 感谢 TeaCon 2023 的理想境一号组和打雪仗工具批发商，让我再次想起了这里不是 shell，这里是 exec
        # 如果 URL 前后有空格，那这空格会作为 URL 的一部分给送进 git-ls-remote(1) 里，
        # 进而导致拉不到任何数据。
        # Strip all possible whitespace char from the repo URL to avoid error.
        get_head_process = subprocess.run(['git', 'ls-remote', team['repo'].strip(), head_ref],
            timeout = 10,
            capture_output = True, 
            text = True)
    except subprocess.TimeoutExpired:
        print(f"Timeout while fetching git repo information for team #{team['id']} (upstream {team['repo']})")
        if os.getenv('GITHUB_ACTIONS', False):
            print(f"::warning::Timeout while fetching {team['repo']}",
                  f"for team #{team['id']}. Information about this team will not be updated.")
        return
    if get_head_process.returncode == 0:
        current_head = None
        if get_head_process.stdout:
            print("git ls-remote result: " + get_head_process.stdout)
            current_head = get_head_process.stdout.split()[0]
        else:
            print(f"::warning::git ls-remote result is empty for team #{team['id']}, which is not good!")
        if current_head:
            if current_head != previous_head:
                with open(f"{info_dir}/HEAD", "w") as f:
                    f.write(current_head)
            else:
                print(f"Team #{team['id']}'s repo is up-to-date.")
        else:
            print(f"::warning::Unexpected git-ls-remote output for team #{team['id']}: {str(get_head_process.stdout)}")
    else:
        print(f"::warning::Error occured while fetching git repo information for team #{team['id']}: {str(get_head_process.stdout)}")

def write_readme(contest_title: str, team_list: List[Team]):
    from html import escape

    readme = f'# 「{contest_title}」作品列表\n\n|作品信息|作品简介|注意事项|\n|:------------|:------------|:------------|\n'

    for team in team_list:
        workflow_name=f"mod-team-{team['work_id'].replace('_', '-')}.yaml"
        readme += f"|{escape(team['work_name'])}<br />"
        if team['ready']:
            workflow_url=f"https://github.com/teaconmc/Longjing/actions/workflows/{workflow_name}"
            badge=f"[![{team['work_id']}]({workflow_url}/badge.svg)]({workflow_url})"
            readme += f"{badge}<br />"
        readme += f"<br />"
        readme += f"队伍名：{escape(team['name'])}<br />"
        readme += f"作品 ID：`{team['work_id']}`（模组 ID），`{team['id']}`（数字 ID）<br />"
        readme += f"项目仓库：[{team['repo']}]({team['repo']})|"
        readme += f"{'<br />'.join([escape(line) for line in team['work_description'].splitlines()])}|"
        readme += f"{'<br />'.join([escape(line) for line in team['notice'].splitlines()])}|"
        readme += '\n'

    with open('mods/README.md', 'w+') as f:
        f.write(readme)

def write_batch_git_clone(contest_title: str, team_list: List[Team]):
    script = f'#!/bin/bash\n\n# 「{contest_title}」 submission repositories batch clone script\n\n'
    for team in team_list:
        script += f"# Clone {team['work_name']} (modid = {team['work_id']}) \ngit clone {team['repo']} {team['work_id']}\n\n"
    with open('mods/batch-clone.sh', 'w+') as f:
        f.write(script)

if __name__ == '__main__':
    workflow_template = load_workflow_template()
    contest_id, contest_slug, contest_title = get_contest_id()
    team_list = get_teams(contest_id)

    # If a teams filter is present, try parsing the filter.
    # Filter is a comma-separated list of numeric team IDs.
    teams_filter = set()
    if len(sys.argv) > 1:
        teams_filter = { int(t) for t in sys.argv[1].split(',') if t.isdigit() }

    for team in team_list:
        if teams_filter and team['id'] not in teams_filter:
            print("Skpping " + team['work_id'] + " based on filter")
            continue
        if not team['ready']:
            print("Skipping " + team['work_id'] + " as it is not marked ready")
            disable_workflow(team['work_id'].replace('_', '-'))
            continue
        deps, dep_ok = get_team_dependencies(contest_slug, team['id'])
        # TeaCon 2026：可能是因为 CDN 问题，GitHub Action Runner 访问碧螺春超时的频率有点高。
        # 为避免一个炸全部炸，这里暂时先跳过无法获取前置信息的作品。
        if not dep_ok:
            print(f"Skipping {team['work_id']} because there was an error while fetching its dependency")
            continue
        depends_on_pending_dep = False
        for dep in deps:
            if dep['review_status'] != 1:
                print(f"Mod '{team['work_name']}' (mod ID '{team['work_id']}', from team '{team['name']}') depends on pending/rejected dependency {dep['name']} (mod id {dep['mod_id']})")
                depends_on_pending_dep = True
        if depends_on_pending_dep:
            disable_workflow(team['work_id'].replace('_', '-'))
            continue
        write_team_info(team, contest_id, contest_title, contest_slug, workflow_template, teams_filter and team['id'] in teams_filter)
    write_readme(contest_title, team_list)
    write_batch_git_clone(contest_title, team_list)
