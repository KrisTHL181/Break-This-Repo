import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile
import unittest


HERE = Path(__file__).resolve().parent


class ObservatoryIntegrationTest(unittest.TestCase):
    def run_cli(self, script, *args, env=None):
        return subprocess.run(
            [sys.executable, str(HERE / script), *map(str, args)],
            capture_output=True,
            text=True,
            env=env,
            timeout=30,
        )

    def test_observe_compare_and_trend_cli_privacy_and_no_overwrite(self):
        with tempfile.TemporaryDirectory() as directory:
            base = Path(directory)
            repository = base / "private-local-repository"
            repository.mkdir()
            before = base / "secret-before-snapshot.json"
            after = base / "secret-after-snapshot.json"
            trend = base / "private-trend.html"

            first = self.run_cli("observe.py", repository, "--label", "station A")
            self.assertEqual(first.returncode, 0, first.stderr)
            before.write_text(first.stdout, encoding="utf-8")
            (repository / "new.txt").write_text("content stays private", encoding="utf-8")
            second = self.run_cli("observe.py", repository, "--label", "station B")
            self.assertEqual(second.returncode, 0, second.stderr)
            after.write_text(second.stdout, encoding="utf-8")

            compared = self.run_cli("compare_observations.py", before, after, "--format", "json")
            self.assertEqual(compared.returncode, 0, compared.stderr)
            deltas = json.loads(compared.stdout)
            self.assertEqual(deltas["metric_deltas"]["files_observed"], 1)
            self.assertNotIn(str(repository), compared.stdout)

            created = self.run_cli("observation_trend.py", before, after, "--output", trend)
            self.assertEqual(created.returncode, 0, created.stderr)
            page = trend.read_text(encoding="utf-8")
            self.assertNotIn(str(base), page)
            self.assertNotIn("secret-before-snapshot.json", page)
            self.assertNotIn("secret-after-snapshot.json", page)
            self.assertNotIn("content stays private", page)
            self.assertNotIn("<script", page)
            self.assertNotIn("https://", page)

            trend.write_text("keep this page", encoding="utf-8")
            overwrite = self.run_cli("observation_trend.py", before, after, "--output", trend)
            self.assertEqual(overwrite.returncode, 1)
            self.assertEqual(trend.read_text(encoding="utf-8"), "keep this page")

    def test_atlas_and_blob_cli_are_local_metadata_only_and_preserve_outputs(self):
        with tempfile.TemporaryDirectory() as directory:
            base = Path(directory)
            repository = base / "history"
            repository.mkdir()
            real_git = shutil.which("git")
            self.assertIsNotNone(real_git)
            self.git(repository, "init", "-q")
            self.git(repository, "config", "user.name", "Integration test")
            self.git(repository, "config", "user.email", "integration@example.invalid")
            (repository / "base.txt").write_text("base", encoding="utf-8")
            start = self.commit(repository)
            (repository / "literal[1]").mkdir()
            (repository / "literal[1]" / "new.bin").write_bytes(b"unique blob")
            (repository / "literal1").mkdir()
            (repository / "literal1" / "other.bin").write_bytes(b"other")
            end = self.commit(repository)

            wrapper_dir = base / "bin"
            wrapper_dir.mkdir()
            call_log = base / "git-calls.jsonl"
            wrapper = wrapper_dir / "git"
            wrapper.write_text(
                "#!/usr/bin/env python3\n"
                "import json, os, sys\n"
                "with open(os.environ['GIT_CALL_LOG'], 'a', encoding='utf-8') as f:\n"
                "    f.write(json.dumps({'args': sys.argv[1:], 'no_lazy_fetch': os.environ.get('GIT_NO_LAZY_FETCH')}) + '\\n')\n"
                "if any(arg in {'fetch', 'clone', 'pull', 'push', 'ls-remote'} for arg in sys.argv[1:]):\n"
                "    raise SystemExit(97)\n"
                "os.execv(os.environ['REAL_GIT'], [os.environ['REAL_GIT'], *sys.argv[1:]])\n",
                encoding="utf-8",
            )
            wrapper.chmod(0o755)
            env = os.environ.copy()
            env.update({
                "PATH": str(wrapper_dir) + os.pathsep + env["PATH"],
                "REAL_GIT": real_git,
                "GIT_CALL_LOG": str(call_log),
                "GIT_ALLOW_PROTOCOL": "",
            })

            atlas_output = base / "scoped-atlas.json"
            atlas = self.run_cli("repo_atlas.py", repository, "--path-prefix", "literal[1]",
                                 "--format", "json", "--output", atlas_output, env=env)
            self.assertEqual(atlas.returncode, 0, atlas.stderr)
            atlas_report = json.loads(atlas_output.read_text(encoding="utf-8"))
            self.assertEqual(atlas_report["entries"], 1)
            self.assertEqual(atlas_report["path_prefix"], "literal[1]")

            blob_output = base / "blob-report.json"
            blobs = self.run_cli("blob_strata.py", repository, "--from", start, "--to", end,
                                 "--format", "json", "--output", blob_output, env=env)
            self.assertEqual(blobs.returncode, 0, blobs.stderr)
            blob_report = json.loads(blob_output.read_text(encoding="utf-8"))
            self.assertEqual(blob_report["known_unique_blobs"], 2)
            self.assertEqual(blob_report["known_unique_blob_bytes"], len(b"unique blob") + len(b"other"))

            blob_protected = base / "existing-blob-report.json"
            blob_protected.write_text("keep blob report", encoding="utf-8")
            blob_refused = self.run_cli("blob_strata.py", repository, "--from", start, "--to", end,
                                        "--format", "json", "--output", blob_protected, env=env)
            self.assertEqual(blob_refused.returncode, 1)
            self.assertEqual(blob_protected.read_text(encoding="utf-8"), "keep blob report")

            protected_output = base / "existing.json"
            protected_output.write_text("preserve", encoding="utf-8")
            refused = self.run_cli("repo_atlas.py", repository, "--format", "json",
                                   "--output", protected_output, env=env)
            self.assertEqual(refused.returncode, 1)
            self.assertEqual(protected_output.read_text(encoding="utf-8"), "preserve")

            calls = [json.loads(line) for line in call_log.read_text(encoding="utf-8").splitlines()]
            self.assertTrue(calls)
            self.assertTrue(all(call["no_lazy_fetch"] == "1" for call in calls))
            self.assertFalse(any(
                arg in {"fetch", "clone", "pull", "push", "ls-remote"}
                for call in calls for arg in call["args"]
            ))

    @staticmethod
    def git(root, *args):
        return subprocess.check_output(["git", "-C", str(root), *args], text=True).strip()

    def commit(self, root):
        self.git(root, "add", "-A")
        subprocess.run(["git", "-C", str(root), "-c", "core.hooksPath=/dev/null",
                        "commit", "-qm", "integration snapshot"], check=True)
        return self.git(root, "rev-parse", "HEAD")


if __name__ == "__main__":
    unittest.main()
