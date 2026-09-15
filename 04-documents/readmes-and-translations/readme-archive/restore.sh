#!/usr/bin/env bash
set -euo pipefail

archive="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
root="$(git -C "$archive" rev-parse --show-toplevel 2>/dev/null || dirname -- "$archive")"
paths=(README.md README.MD README.markdown)

rm -f "$archive"/README-*.md

count=0
while read -r line; do
  if [[ "$line" == @* ]]; then
    sha="${line%% *}"; sha="${sha#@}"; ts="${line##* }"
  elif [[ -n "$line" ]]; then
    git -C "$root" cat-file -e "$sha:$line" 2>/dev/null || continue
    out="$archive/README-$ts.md"
    [[ -e "$out" ]] && out="$archive/README-$ts-${sha:0:7}.md"
    git -C "$root" show "$sha:$line" > "$out"
    count=$((count + 1))
  fi
done < <(git -C "$root" log --all --reverse --pretty=format:'@%H %ad' --date=format:'%Y%m%d-%H%M%S' --name-only -- "${paths[@]}")

echo "restored $count readme versions into $archive"
