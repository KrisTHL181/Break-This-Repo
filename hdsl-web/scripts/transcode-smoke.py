#!/usr/bin/env python3
"""End-to-end smoke for the transcode module.

Starts a throwaway panel with auth disabled (loopback only), a temporary data dir and
media root, then exercises the real REST surface with the real ffmpeg:

  capabilities -> media list -> preview -> submit -> start queue -> poll -> output -> log

Requires a release jar (./gradlew shadowJar) and ffmpeg/ffprobe on PATH. The script
does not touch the operator's data dir; everything lives under a TemporaryDirectory.

Usage:
    python3 scripts/transcode-smoke.py [path/to/hdsl-web.jar]
"""

from __future__ import annotations

import json
import os
import shutil
import socket
import subprocess
import sys
import tempfile
import time
import urllib.error
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
DEFAULT_JAR_GLOB = "build/libs/hdsl-web-*.jar"


def fail(message: str) -> None:
    print(f"FAIL: {message}", file=sys.stderr)
    raise SystemExit(1)


def check(condition: bool, message: str) -> None:
    if not condition:
        fail(message)


def free_port() -> int:
    with socket.socket() as sock:
        sock.bind(("127.0.0.1", 0))
        return sock.getsockname()[1]


def jar_path() -> Path:
    if len(sys.argv) > 1:
        path = Path(sys.argv[1]).resolve()
        check(path.is_file(), f"jar not found: {path}")
        return path
    candidates = sorted(ROOT.glob(DEFAULT_JAR_GLOB), key=lambda path: path.stat().st_mtime)
    check(bool(candidates), "no build/libs/hdsl-web-*.jar; run ./gradlew shadowJar first")
    return candidates[-1]


def request(base: str, method: str, path: str, body: dict | None = None) -> tuple[int, dict | str]:
    data = None
    headers = {}
    if body is not None:
        data = json.dumps(body).encode()
        headers["Content-Type"] = "application/json"
    req = urllib.request.Request(base + path, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=30) as response:
            raw = response.read().decode()
            try:
                return response.status, json.loads(raw)
            except json.JSONDecodeError:
                return response.status, raw
    except urllib.error.HTTPError as error:
        raw = error.read().decode()
        try:
            return error.code, json.loads(raw)
        except json.JSONDecodeError:
            return error.code, raw


def wait_health(base: str, process: subprocess.Popen) -> None:
    for _ in range(120):
        if process.poll() is not None:
            output = process.stdout.read().decode(errors="replace") if process.stdout else ""
            fail(f"panel exited during startup:\n{output[-4000:]}")
        try:
            status, body = request(base, "GET", "/api/health")
            if status == 200:
                return
        except Exception:
            pass
        time.sleep(0.5)
    fail("panel did not become healthy in 60s")


def main() -> None:
    ffmpeg = shutil.which("ffmpeg")
    ffprobe = shutil.which("ffprobe")
    check(ffmpeg is not None and ffprobe is not None, "ffmpeg/ffprobe are not on PATH")
    java = shutil.which("java")
    check(java is not None, "java is not on PATH")
    jar = jar_path()

    with tempfile.TemporaryDirectory(prefix="hdsl-transcode-smoke-") as temp:
        temp_dir = Path(temp)
        data = temp_dir / "data"
        media = temp_dir / "media"
        data.mkdir()
        media.mkdir()
        (data / "transcode").mkdir()
        (data / "transcode" / "feature.json").write_text(
            json.dumps({
                "state": "ENABLED",
                "consent": {
                    "termsVersion": 1,
                    "termsTextHash": "sha256:smoke",
                    "acceptedAt": "2026-11-10T00:00:00Z",
                    "acceptedBy": "smoke",
                    "acceptedFrom": "127.0.0.1",
                    "ackMethod": "checkbox",
                    "uiVersion": "smoke",
                    "buildCommit": "smoke",
                    "sawDeclineOption": True,
                },
            }),
            encoding="utf-8",
        )
        (data / "server.yaml").write_text(
            "auth:\n  disabled: true\n"
            "bind_host: 127.0.0.1\n"
            "transcode:\n"
            f"  media_root: {media}\n"
            f"  ffmpeg_path: {ffmpeg}\n"
            "  max_concurrent: 1\n"
            "  max_queue: 10\n",
            encoding="utf-8",
        )

        input_file = media / "input.mp4"
        subprocess.run(
            [
                ffmpeg, "-hide_banner", "-loglevel", "error", "-y",
                "-f", "lavfi", "-i", "testsrc=duration=1:size=128x72:rate=10",
                "-f", "lavfi", "-i", "sine=frequency=1000:duration=1",
                "-c:v", "libx264", "-pix_fmt", "yuv420p", "-c:a", "aac", "-shortest",
                str(input_file),
            ],
            check=True,
            timeout=60,
        )

        port = free_port()
        base = f"http://127.0.0.1:{port}"
        env = os.environ.copy()
        env["HDSL_DATA"] = str(data)
        env["HDSL_PORT"] = str(port)
        env["HDSL_BIND_HOST"] = "127.0.0.1"
        env.setdefault("JAVA_OPTS", "-Xmx512m")
        process = subprocess.Popen(
            [java, "-jar", str(jar)],
            cwd=str(ROOT),
            env=env,
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
        )
        try:
            wait_health(base, process)
            print("PASS: health")

            status, capabilities = request(base, "GET", "/api/transcode/capabilities")
            check(status == 200, f"capabilities -> {status}")
            assert isinstance(capabilities, dict)
            check(capabilities.get("available") is True, f"ffmpeg unavailable: {capabilities}")
            check("libx264" in capabilities.get("encoders", []), "libx264 missing from capability matrix")
            print("PASS: capabilities")

            status, listing = request(base, "GET", "/api/transcode/media/list?path=")
            check(status == 200, f"media list -> {status}")
            assert isinstance(listing, dict)
            check(any(entry["name"] == "input.mp4" for entry in listing.get("entries", [])),
                  "input.mp4 missing from media listing")
            print("PASS: media listing")

            status, presets = request(base, "GET", "/api/transcode/presets")
            check(status == 200, f"presets -> {status}")
            assert isinstance(presets, dict)
            check(any(item["id"] == "h264-quality" for item in presets.get("presets", [])),
                  "built-in h264-quality preset missing")
            print("PASS: presets")

            status, preview = request(base, "POST", "/api/transcode/preview",
                                      {"presetId": "h264-quality", "input": "input.mp4"})
            check(status == 200, f"preview -> {status}: {preview}")
            assert isinstance(preview, dict)
            check("-c:v libx264" in preview.get("command", ""), "preview command has no libx264")
            print("PASS: preview")

            status, job = request(base, "POST", "/api/transcode/queue/jobs",
                                   {"presetId": "h264-quality", "input": "input.mp4"})
            check(status == 200, f"submit -> {status}: {job}")
            assert isinstance(job, dict)
            job_id = job["id"]
            print(f"PASS: submit {job_id}")

            status, snapshot = request(base, "POST", "/api/transcode/queue/start", {})
            check(status == 200, f"start queue -> {status}")

            final = None
            for _ in range(120):
                status, snapshot = request(base, "GET", "/api/transcode/queue")
                check(status == 200, f"queue -> {status}")
                assert isinstance(snapshot, dict)
                final = next((item for item in snapshot.get("jobs", []) if item["id"] == job_id), None)
                if final and final["state"] in ("done", "failed", "cancelled"):
                    break
                time.sleep(0.5)
            check(final is not None, "job disappeared from the queue")
            check(final["state"] == "done", f"job ended as {final['state']}: {final.get('error')}")
            output = media / final["output"]
            check(output.is_file() and output.stat().st_size > 0, f"output missing: {output}")
            print("PASS: encode")

            status, log = request(base, "GET", f"/api/transcode/queue/jobs/{job_id}/log?tail=100")
            check(status == 200, f"job log -> {status}")
            assert isinstance(log, dict)
            check("ffmpeg" in log.get("log", ""), "job log does not contain the command")
            print("PASS: log")

            status, extract = request(base, "POST", "/api/transcode/tools/extract",
                                       {"input": "input.mp4", "type": "video", "index": 0,
                                        "output": "extracted.mkv"})
            check(status == 200, f"extract -> {status}: {extract}")
            assert isinstance(extract, dict)
            extract_id = extract["id"]
            extract_final = None
            for _ in range(120):
                status, snapshot = request(base, "GET", "/api/transcode/queue")
                check(status == 200, f"queue -> {status}")
                assert isinstance(snapshot, dict)
                extract_final = next(
                    (item for item in snapshot.get("jobs", []) if item["id"] == extract_id), None)
                if extract_final and extract_final["state"] in ("done", "failed", "cancelled"):
                    break
                time.sleep(0.5)
            check(extract_final is not None, "extract job disappeared")
            check(extract_final["state"] == "done",
                  f"extract ended as {extract_final['state']}: {extract_final.get('error')}")
            check((media / "extracted.mkv").is_file(), "extracted.mkv missing")
            print("PASS: extract tool")

            status, quality = request(base, "POST", "/api/transcode/tools/quality",
                                      {"reference": "input.mp4", "encoded": final["output"],
                                       "metrics": ["psnr", "ssim"]})
            check(status == 200, f"quality start -> {status}: {quality}")
            assert isinstance(quality, dict)
            quality_id = quality["id"]
            quality_final = None
            for _ in range(120):
                status, quality_final = request(
                    base, "GET", f"/api/transcode/tools/quality/{quality_id}")
                check(status == 200, f"quality -> {status}")
                assert isinstance(quality_final, dict)
                if quality_final["state"] in ("done", "failed", "cancelled"):
                    break
                time.sleep(0.5)
            check(quality_final is not None and quality_final["state"] == "done",
                  f"quality ended as {quality_final and quality_final['state']}: "
                  f"{quality_final and quality_final.get('error')}")
            for metric in ("psnr", "ssim"):
                result = quality_final["results"].get(metric)
                check(result is not None, f"quality result missing: {metric}")
                check(result["frames"], f"quality frames missing: {metric}")
            print("PASS: quality metrics")

            # P0 regression: two submissions of the same input/preset must not share
            # an output path while both are still queued.
            request(base, "POST", "/api/transcode/queue/pause", {})
            outputs = []
            ids = []
            for _ in range(2):
                status, extra = request(base, "POST", "/api/transcode/queue/jobs",
                                        {"presetId": "h264-quality", "input": "input.mp4"})
                check(status == 200, f"duplicate submit -> {status}: {extra}")
                assert isinstance(extra, dict)
                outputs.append(extra["output"])
                ids.append(extra["id"])
            check(len(set(outputs)) == 2, f"duplicate submissions shared output paths: {outputs}")
            for job_id in ids:
                request(base, "POST", f"/api/transcode/queue/jobs/{job_id}/cancel", {})
            request(base, "POST", "/api/transcode/queue/start", {})
            print("PASS: output reservation")

            # P1 regression: a video-only source must not fail an audio-copy preset.
            video_only = media / "video_only.mp4"
            subprocess.run(
                [ffmpeg, "-hide_banner", "-loglevel", "error", "-y",
                 "-f", "lavfi", "-i", "testsrc=duration=1:size=128x72:rate=10",
                 "-c:v", "libx264", "-pix_fmt", "yuv420p", str(video_only)],
                check=True, timeout=60,
            )
            status, vo_job = request(base, "POST", "/api/transcode/queue/jobs",
                                     {"presetId": "h264-quality", "input": "video_only.mp4"})
            check(status == 200, f"video-only submit -> {status}: {vo_job}")
            assert isinstance(vo_job, dict)
            vo_final = None
            for _ in range(120):
                status, snapshot = request(base, "GET", "/api/transcode/queue")
                check(status == 200, f"queue -> {status}")
                assert isinstance(snapshot, dict)
                vo_final = next((item for item in snapshot.get("jobs", []) if item["id"] == vo_job["id"]), None)
                if vo_final and vo_final["state"] in ("done", "failed", "cancelled"):
                    break
                time.sleep(0.5)
            check(vo_final is not None and vo_final["state"] == "done",
                  f"video-only job ended as {vo_final and vo_final['state']}: "
                  f"{vo_final and vo_final.get('error')}")
            print("PASS: video-only source")

            # Queue housekeeping: stop -> edit/save a preset -> reset this job onto
            # the new snapshot, then remove / clear records without touching files.
            request(base, "POST", "/api/transcode/queue/pause", {})
            other_preset = next(
                (item["id"] for item in presets.get("presets", []) if item["id"] != "h264-quality"),
                None,
            )
            check(other_preset is not None, "no second preset available for reset-preset")
            target = ids[0]
            status, reset = request(base, "POST", f"/api/transcode/queue/jobs/{target}/reset-preset",
                                    {"presetId": other_preset})
            check(status == 200, f"reset-preset -> {status}: {reset}")
            assert isinstance(reset, dict)
            refreshed = next((item for item in reset.get("jobs", []) if item["id"] == target), None)
            check(refreshed is not None, "reset-preset dropped the job")
            check(refreshed["state"] == "queued", f"reset-preset state -> {refreshed['state']}")
            check(refreshed["presetId"] == other_preset,
                  f"reset-preset kept the old snapshot: {refreshed['presetId']}")
            print("PASS: reset-preset")

            status, removed = request(base, "DELETE", f"/api/transcode/queue/jobs/{target}")
            check(status == 200, f"remove job -> {status}: {removed}")
            assert isinstance(removed, dict)
            check(all(item["id"] != target for item in removed.get("jobs", [])),
                  "removed job is still in the queue")
            print("PASS: remove job")

            status, cleared = request(base, "POST", "/api/transcode/queue/clear", {})
            check(status == 200, f"clear finished -> {status}: {cleared}")
            assert isinstance(cleared, dict)
            check(not any(item["state"] in ("done", "failed", "cancelled")
                          for item in cleared.get("jobs", [])),
                  "clear left terminal records behind")
            check((media / "extracted.mkv").is_file(), "clear deleted a produced file")
            print("PASS: clear finished")

            status, _ = request(base, "GET", "/api/transcode/queue/jobs/unknown")
            check(status == 404, f"unknown job -> {status} (expected 404)")
            print("PASS: 404 contract")
        finally:
            process.terminate()
            try:
                process.wait(timeout=15)
            except subprocess.TimeoutExpired:
                process.kill()
                process.wait(timeout=10)


if __name__ == "__main__":
    main()