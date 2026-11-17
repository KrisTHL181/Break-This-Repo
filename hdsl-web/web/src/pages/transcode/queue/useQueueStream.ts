import { useCallback, useEffect, useRef, useState } from "react";
import { api, type TranscodeJob, type TranscodeQueueSnapshot } from "../../../api";
import { ws } from "../../../ws";
import { errMsg } from "../../../utils";

/// A dropped socket cannot replay what it missed, so a slow poll stays behind the push.
const FALLBACK_MS = 30_000;

type QueueEvent =
  | { type: "transcode-queue"; queue: TranscodeQueueSnapshot }
  | { type: "transcode-job"; job: TranscodeJob };

/// Queue state over the panel's event channel.
///
/// The server already publishes `transcode:queue` (full snapshot on every state change)
/// and `transcode-job` (one job, throttled to 500ms while ffmpeg prints progress), so the
/// page does not need a 1s poll anymore.
///
/// `ws.addTopics` unions on the client — `store.tsx` narrowed the shared connection to
/// `instances`/`tasks`, so without this the queue frames never arrive. Never send a
/// `subscribe` frame by hand: the gateway *replaces* the subscription set with it.
export function useQueueStream() {
  const [snapshot, setSnapshot] = useState<TranscodeQueueSnapshot | null>(null);
  const [error, setError] = useState<string | null>(null);
  const alive = useRef(true);

  const refresh = useCallback(async () => {
    try {
      const next = await api.transcode.queue();
      if (alive.current) {
        setSnapshot(next);
        setError(null);
      }
    } catch (e) {
      if (alive.current) setError(errMsg(e));
    }
  }, []);

  useEffect(() => {
    alive.current = true;
    void refresh();
    ws.addTopics(["transcode:queue"]);
    const off = ws.on((raw) => {
      const event = raw as unknown as QueueEvent;
      if (event.type === "transcode-queue") {
        setSnapshot(event.queue);
        setError(null);
        return;
      }
      if (event.type === "transcode-job") {
        const job = event.job;
        setSnapshot((current) => {
          if (!current) return current;
          const known = current.jobs.some((item) => item.id === job.id);
          return {
            ...current,
            jobs: known
              ? current.jobs.map((item) => (item.id === job.id ? job : item))
              : [...current.jobs, job],
          };
        });
      }
    });
    const fallback = window.setInterval(() => void refresh(), FALLBACK_MS);
    return () => {
      alive.current = false;
      off();
      ws.removeTopics(["transcode:queue"]);
      window.clearInterval(fallback);
    };
  }, [refresh]);

  return { snapshot, error, refresh, setSnapshot };
}
