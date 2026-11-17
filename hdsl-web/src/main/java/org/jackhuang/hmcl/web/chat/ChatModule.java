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
package org.jackhuang.hmcl.web.chat;

import org.jackhuang.hmcl.web.config.ServerConfig;
import org.jackhuang.hmcl.web.event.EventBus;
import org.jetbrains.annotations.NotNullByDefault;

import java.sql.SQLException;

/// 聊天模块的装配与生命周期：一个 SQLite 库、一个服务门面、一个维护线程。
@NotNullByDefault
public final class ChatModule implements AutoCloseable {

    private final ChatStore store;
    private final ChatService service;

    private ChatModule(ChatStore store, ChatService service) {
        this.store = store;
        this.service = service;
    }

    public static ChatModule create(ServerConfig config, EventBus bus) {
        ChatStore store = null;
        try {
            store = new ChatStore(config.dataDir);
            ChatService service = new ChatService(store, bus, config.dataDir);
            return new ChatModule(store, service);
        } catch (SQLException | java.io.IOException e) {
            // A half-open store keeps the WAL file handle; close it before the
            // caller decides to run without chat.
            if (store != null) {
                store.close();
            }
            throw new IllegalStateException("Cannot open the chat store under "
                    + config.dataDir.resolve("chat"), e);
        }
    }

    public ChatService service() {
        return service;
    }

    public ChatStore store() {
        return store;
    }

    @Override
    public void close() {
        service.close();
    }
}