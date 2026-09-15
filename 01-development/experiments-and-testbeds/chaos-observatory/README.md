# Chaos Observatory / 混沌观测站

This is a tiny field station for observing a repository that refuses to be one
thing. It treats filenames as fossils, extensions as species, and directory
depth as geological pressure.

这是一个观察仓库生态的微型站点：文件名是化石，扩展名是物种，目录深度是地质压力。

The observatory is deliberately read-only. It does not open file contents,
follow symbolic links, import code from the observed repository, or use the
network. It only examines directory entries and file metadata.

## Take an observation

From this directory:

```sh
python3 observe.py ..
python3 weather.py ..
```

Scanning stops after 100,000 files by default, because an observatory should
study chaos rather than become part of it. Change the limit explicitly when
you mean it:

```sh
python3 observe.py .. --max-files 250000
```

Every reading ends with the same scientifically defensible conclusion:

> The site remains in an active state of formation.

## Safety boundary

- read names and metadata only;
- never follow symlinks;
- never enter `.git` or common dependency/cache directories;
- never execute, import, parse, or upload observed files;
- collect no personal information and make no network requests.

Future researchers may append observations to `FIELD_NOTES.md`. Please record
what you saw, not who you think caused it.

## 喵化足迹 / Meow footprint museum

`meow_museum.py` is a separate Git-history exhibit. Unlike the metadata survey
above, it reads local Git tree and commit objects through Git. It does not read
working-tree file contents, execute repository code, collect author information,
follow submodules, or request the network (lazy fetching is disabled).

From this directory, with Python 3.9+ and Git installed:

```sh
python3 meow_museum.py .. --output /tmp/meow-museum.html
python3 -m unittest discover -v
```

Open the generated HTML in a browser. It includes search and status filters,
works offline, and remains readable with JavaScript disabled. The output path
must not already exist. No server, packages, or CI changes are required.

An inaugural snapshot is included as `index.html`: download/open it locally
to visit immediately, or regenerate a new file to observe a later commit.

The default baseline is commit `ac421f58da0ec26c820a87ed7d0a0f63df8d2221`
(PR #508). Samples are the top-level directories where that commit added a
`MEOW.md`. A directory is retained if its latest path-history commit is still
the baseline, revisited if it has a different latest commit, or removed if its
original path is no longer a Git tree. Renames count as removal at the old path.
This measures the latest directory change, not whether the note's text survives.

`--revision COMMIT` selects a reproducible snapshot; `--baseline COMMIT` permits
another compatible event. The baseline must be an ancestor of the snapshot.
Shallow history, missing objects, and unknown revisions fail explicitly rather
than producing misleading counts. A complete-history partial clone is usable
when the needed objects are already local. The page records the snapshot SHA
and commit time; it is not live and may differ from GitHub's cached presentation.

## 猫爪消退日报 / Interval diary

Compare two snapshots after the meow event. The start must be an ancestor of
the end; the original PR #508 side-branch commit is also a valid start:

```sh
python3 meow_diary.py .. --from START_SHA --to END_SHA --output /tmp/diary.md
python3 meow_diary.py .. --from START_SHA --to END_SHA --format html --output /tmp/diary.html
```

The Markdown diary and HTML recent-events panel count added, modified, deleted,
exactly renamed, and type-changed files between the endpoints. Exact renames
are separate from additions/deletions. Transient changes canceled before the
end do not appear in these net counts. Mainline events follow the first-parent
chain, so a branch and its merge are not counted twice. `--limit` (1–1000,
default 100) limits only the displayed events; totals still cover the interval.
Events follow the end's first-parent chain and exclude commits already reachable
from the start. A side-branch start can therefore show its later integration as
an event even when the endpoint file diff is empty; the two metrics are distinct.
As with the museum, output must be a new file and shallow or incomplete history
fails explicitly.

This is a manually generated interval report, not a scheduled job. The included
`diary.md` / `diary.html` record the inaugural museum merge: the observatory
itself became an exhibit when it covered its own latest meow footprint.

## 仓库地图 / Repository atlas

```sh
python3 repo_atlas.py .. --revision HEAD --limit 100000 --output /tmp/atlas.html
```

The atlas groups tracked paths by top-level directory and shows ordinary file
counts, extension distributions, locally known logical byte sizes, and separate
symlink/submodule counts. Search regions, sort by count/known size/name, and
expand a region's type table. It never traverses checkout paths or submodules.
Duplicate blobs count once per path, so these sizes are not disk usage. Missing
partial-clone blobs are explicitly unknown, not zero. No objects are downloaded.

The NUL-delimited tree stream is capped at `--limit` entries (1–1000000). A
truncated sample is labeled prominently and is not a whole-repository ranking.
The supplied `atlas.html` records its revision and coverage. Regenerate to a new
file to obtain a later snapshot. The original metadata survey remains unchanged.

## 奇葩路径展览 / Path portability gallery

```sh
python3 path_gallery.py .. --limit 1000000 --exhibits 100 --output /tmp/paths.html
python3 path_gallery.py .. --format json --output /tmp/path-specimens.json
```

Inspect paths stored in Git without creating, opening, or checking them out.
The gallery flags long UTF-8 paths/components, deep paths, Windows device names,
invalid Windows characters, trailing spaces/dots, control/format characters,
undecodable UTF-8 bytes, and NFC/casefold collisions (including parent directory
collisions such as `A/x` versus `a/y`). These are heuristics, not a platform
compatibility certification. See Microsoft's [file naming rules](https://learn.microsoft.com/en-us/windows/win32/fileio/naming-a-file).
The 240-byte warning threshold is an exhibit rule, not Windows' UTF-16 limit.

Counts cover the scanned sample and can overlap. `--exhibits` (1–1000) caps only
displayed specimens, with a representative for each category where space permits.
Search and category filters operate on those displayed specimens. Scanning and
exhibit truncation are both reported. Controls and bidirectional formatting are
escaped visibly; HTML is escaped and never interpreted. JSON exports preserve
the full selected paths through escapes. `paths.html` is the inaugural snapshot.
