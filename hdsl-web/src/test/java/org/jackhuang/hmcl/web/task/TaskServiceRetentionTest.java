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
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The finished tail of `/api/tasks` is bounded so a long-running server does
/// not grow without limit; running and approval-parked tasks are never pruned.
class TaskServiceRetentionTest {

    @Test
    void finishedTasksAreBounded() throws Exception {
        TaskService service = new TaskService(new EventBus(), 5);
        int submitted = 12;
        CountDownLatch latch = new CountDownLatch(submitted);
        for (int i = 0; i < submitted; i++) {
            service.submit("misc", null, () -> {
                latch.countDown();
                return "ok";
            });
        }
        assertTrue(latch.await(30, TimeUnit.SECONDS));
        for (int i = 0; i < 200 && service.list().size() > 5; i++) {
            Thread.sleep(10);
        }
        assertTrue(service.list().size() <= 5, "kept " + service.list().size());
    }

    @Test
    void runningTasksAreNeverPruned() throws Exception {
        TaskService service = new TaskService(new EventBus(), 1);
        CountDownLatch blocker = new CountDownLatch(1);
        TaskService.Task first = service.submit("misc", null, () -> "ok");
        TaskService.Task second = service.submit("misc", null, () -> "ok");
        // Submitted last: it is the newest, so the prune that runs when it
        // finishes keeps it (the bound keeps the newest terminal task).
        TaskService.Task running = service.submit("misc", null, () -> {
            assertTrue(blocker.await(30, TimeUnit.SECONDS));
            return "ok";
        });

        // Both quick tasks finish; with the bound at 1 the older one gives way.
        // The running task must survive every prune.
        for (int i = 0; i < 200 && (service.get(first.id()).isPresent()
                || !service.get(second.id()).isPresent()); i++) {
            Thread.sleep(10);
        }
        assertTrue(service.get(first.id()).isEmpty(), "oldest finished task should be pruned");
        assertTrue(service.get(second.id()).isPresent(), "newest finished task should stay");
        assertTrue(service.get(running.id()).isPresent(), "running task must never be pruned");

        blocker.countDown();
        for (int i = 0; i < 200 && service.get(running.id()).isPresent()
                && service.get(running.id()).get().state().equals("running"); i++) {
            Thread.sleep(10);
        }
        assertEquals("done", service.get(running.id()).orElseThrow().state());
    }
}
