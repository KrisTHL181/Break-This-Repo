/*
 * HDSL-web
 * Copyright (C) 2026  HDSL-web contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.jackhuang.hmcl.web.task;

import org.jackhuang.hmcl.web.event.EventBus;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The third install waits in a state of its own.
///
/// It used to call itself `running` with an empty message while it sat on the semaphore behind
/// two long installs. Three brand installs submitted together (the ARM smoke run) then looked
/// like "started and hung", and the only way to tell was a thread dump. `queued` says it, and
/// the wait has to stay cancellable — a queued task that cannot be cancelled holds a place in
/// a queue nobody can shorten.
class TaskServiceQueueTest {

    /// Polls the snapshot until `ready` holds, or gives up loudly.
    private static TaskService.TaskInfo await(TaskService service, String id,
                                              java.util.function.Predicate<TaskService.TaskInfo> ready)
            throws InterruptedException {
        TaskService.TaskInfo last = null;
        for (int i = 0; i < 400; i++) {
            last = service.get(id).orElse(null);
            if (last != null && ready.test(last)) {
                return last;
            }
            Thread.sleep(10);
        }
        throw new AssertionError("timed out; last snapshot: " + last);
    }

    @Test
    void aThirdInstallIsQueuedWithAReasonAndThenRuns() throws Exception {
        TaskService service = new TaskService(new EventBus());
        CountDownLatch release = new CountDownLatch(1);

        TaskService.Task first = service.submit("install", "inst-a", () -> {
            release.await(30, TimeUnit.SECONDS);
            return "a";
        });
        TaskService.Task second = service.submit("install", "inst-b", () -> {
            release.await(30, TimeUnit.SECONDS);
            return "b";
        });
        await(service, first.id(), info -> info.state().equals("running"));
        await(service, second.id(), info -> info.state().equals("running"));

        TaskService.Task third = service.submit("install", "inst-c", () -> "c");

        TaskService.TaskInfo queued = await(service, third.id(),
                info -> info.state().equals("queued"));
        assertTrue(queued.message().contains("安装槽位"),
                "a queued task says why it is not running: " + queued.message());
        assertFalse(queued.message().isBlank(), "an empty message is what made it look hung");
        assertTrue(queued.fraction() < 0, "a queued task has no progress to report");

        // Still queued while both slots are taken.
        assertEquals("queued", service.get(third.id()).orElseThrow().state());

        release.countDown();
        await(service, third.id(), info -> info.state().equals("done"));
        assertEquals("c", service.get(third.id()).orElseThrow().message());
    }

    @Test
    void aQueuedInstallCanBeCancelledWithoutTakingASlot() throws Exception {
        TaskService service = new TaskService(new EventBus());
        CountDownLatch release = new CountDownLatch(1);
        TaskService.Task first = service.submit("install", "inst-a", () -> {
            release.await(30, TimeUnit.SECONDS);
            return "a";
        });
        TaskService.Task second = service.submit("install", "inst-b", () -> {
            release.await(30, TimeUnit.SECONDS);
            return "b";
        });
        await(service, first.id(), info -> info.state().equals("running"));
        await(service, second.id(), info -> info.state().equals("running"));

        TaskService.Task third = service.submit("install", "inst-c", () -> "c");
        await(service, third.id(), info -> info.state().equals("queued"));

        assertTrue(service.cancel(third.id()), "a queued task is cancellable");
        await(service, third.id(), info -> info.state().equals("failed"));
        assertEquals("cancelled", service.get(third.id()).orElseThrow().message());

        // The two running installs still hold their slots; the cancelled one never took one,
        // which is what lets the next install start as soon as a slot frees up.
        assertEquals("running", service.get(first.id()).orElseThrow().state());
        release.countDown();
        await(service, first.id(), info -> info.state().equals("done"));
    }
}
