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
package org.jackhuang.hmcl.web.http;

import org.jackhuang.hmcl.web.event.EventBus;
import org.jackhuang.hmcl.web.task.TaskService;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// A progress line reaches its task through the handle the work is handed
/// ([TaskService.TaskWork]) — never through a slot the submitter fills in
/// afterwards.
///
/// Every path that reports install progress is built this way: it wants the
/// task inside a callback the task body itself triggers, and `submit` hands the
/// work to its executor before it returns the handle. A one-slot holder read
/// from that callback therefore had a window with no recipient in it, and the
/// price of the workaround was a null check that silently dropped the line —
/// exactly the first line of a pnpm run, which is the one a user waits for.
/// Passing the task into the work removes the window rather than guarding it,
/// which is what this pins.
class TaskProgressTest {

    @Test
    void theWorkIsHandedTheTaskItsSubmissionReturned() throws Exception {
        TaskService tasks = new TaskService(new EventBus());
        AtomicReference<TaskService.Task> handed = new AtomicReference<>();
        CountDownLatch reported = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);

        TaskService.Task task = tasks.submit("probe", null, live -> {
            handed.set(live);
            live.update("installing", -1);
            reported.countDown();
            release.await();
            return "done";
        });

        assertTrue(reported.await(30, TimeUnit.SECONDS), "the work must start");
        assertSame(task, handed.get(), "the handle the work gets is the handle submit returns");
        assertEquals("installing", tasks.get(task.id()).orElseThrow().message(),
                "a progress line reported through that handle reaches the task");
        release.countDown();
    }
}