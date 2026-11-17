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
package org.jackhuang.hmcl.web.minecraft;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class McInstanceManagerTest {

    private static McInstanceManager manager(Path dir) {
        return new McInstanceManager(dir.resolve("minecraft").resolve("instances"));
    }

    @Test
    void createsManifestPropertiesAndLists(@TempDir Path dir) throws Exception {
        McInstanceManager manager = manager(dir);
        McInstance instance = manager.create("生存服", "1.21.4", 25565, 25575, 2048, "world", "12345");

        assertEquals("mc", instance.id());
        assertNotNull(manager.find("mc"));
        assertEquals(1, manager.list().size());

        String properties = Files.readString(manager.propertiesFile(instance));
        assertTrue(properties.contains("server-port=25565"));
        assertTrue(properties.contains("rcon.port=25575"));
        assertTrue(properties.contains("level-name=world"));
        assertTrue(properties.contains("level-seed=12345"));
        assertTrue(properties.contains("online-mode=true"));
        assertTrue(properties.contains("enable-rcon=false"));
    }

    @Test
    void aSecondInstanceWithTheSameNameGetsItsOwnId(@TempDir Path dir) throws Exception {
        McInstanceManager manager = manager(dir);
        McInstance first = manager.create("生存服", "1.21.4", 25565, 25575, 2048, "world", null);
        McInstance second = manager.create("生存服", "1.21.4", 25566, 25576, 2048, "world-2", null);
        assertEquals("mc", first.id());
        assertEquals("mc-2", second.id());
        assertEquals(2, manager.list().size());
    }

    @Test
    void aPortAlreadyUsedByAnotherInstanceIsRejected(@TempDir Path dir) throws Exception {
        McInstanceManager manager = manager(dir);
        manager.create("a", "1.21.4", 25565, 25575, 2048, "world", null);
        McException gamePort = assertThrows(McException.class,
                () -> manager.create("b", "1.21.4", 25565, 25576, 2048, "world", null));
        assertTrue(gamePort.getMessage().contains("25565"));
        McException rconPort = assertThrows(McException.class,
                () -> manager.create("c", "1.21.4", 25567, 25575, 2048, "world", null));
        assertTrue(rconPort.getMessage().contains("25575"));
    }

    @Test
    void updatePersistsANewName(@TempDir Path dir) throws Exception {
        McInstanceManager manager = manager(dir);
        McInstance instance = manager.create("旧名字", "1.21.4", 25565, 25575, 2048, "world", null);
        manager.update(instance.withName("新名字"));
        assertEquals("新名字", manager.find(instance.id()).name());
    }

    @Test
    void deleteRemovesTheWholeDirectory(@TempDir Path dir) throws Exception {
        McInstanceManager manager = manager(dir);
        McInstance instance = manager.create("doomed", "1.21.4", 25565, 25575, 2048, "world", null);
        Path directory = manager.instanceDirectory(instance.id());
        assertTrue(Files.isDirectory(directory));
        manager.delete(instance.id());
        assertFalse(Files.exists(directory));
        assertNull(manager.find(instance.id()));
    }

    @Test
    void theEulaMarkerIsOnlyWrittenWhenAsked(@TempDir Path dir) throws Exception {
        McInstanceManager manager = manager(dir);
        McInstance instance = manager.create("eula", "1.21.4", 25565, 25575, 2048, "world", null);
        assertFalse(Files.exists(manager.eulaFile(instance)));
        manager.acceptEula(instance);
        assertEquals("eula=true\n", Files.readString(manager.eulaFile(instance)));
    }

    @Test
    void propertiesRoundTripKeepsHandWrittenComments(@TempDir Path dir) throws Exception {
        McInstanceManager manager = manager(dir);
        McInstance instance = manager.create("props", "1.21.4", 25565, 25575, 2048, "world", null);
        Files.writeString(manager.propertiesFile(instance),
                "# keep me\nmotd=old\nunknown-key=1\n");

        McProperties properties = manager.properties(instance);
        assertEquals("old", properties.get("motd"));
        properties.set("motd", "new");
        manager.saveProperties(instance, properties);

        String text = Files.readString(manager.propertiesFile(instance));
        assertTrue(text.contains("# keep me"));
        assertTrue(text.contains("unknown-key=1"));
        assertTrue(text.contains("motd=new"));
    }

    @Test
    void tailLogReturnsTheLastLines(@TempDir Path dir) throws Exception {
        McInstanceManager manager = manager(dir);
        McInstance instance = manager.create("logs", "1.21.4", 25565, 25575, 2048, "world", null);
        Files.writeString(manager.logFile(instance), "a\nb\nc\nd\n");
        assertEquals(java.util.List.of("c", "d"), manager.tailLog(instance, 2));
        assertEquals(java.util.List.of(), manager.tailLog(manager.create(
                "empty", "1.21.4", 25566, 25576, 2048, "world", null), 5));
    }

    // ------------------------------------------------- 专用目录 + 旧位置兼容 --

    @Test
    void newInstancesGoToTheDedicatedRootAndLegacyOnesAreStillListed(@TempDir Path dir) throws Exception {
        Path external = dir.resolve("home").resolve("mc");
        Path legacy = dir.resolve("data").resolve("minecraft").resolve("instances");
        McInstanceManager manager = new McInstanceManager(external, legacy);

        McInstance old = manager.create("old", "1.21.4", 25565, 25575, 2048, "world", null, true);
        McInstance fresh = manager.create("new", "1.21.4", 25566, 25576, 2048, "world", null);

        assertTrue(Files.isDirectory(legacy.resolve(old.id())));
        assertTrue(Files.isDirectory(external.resolve(fresh.id())));
        assertEquals("data", manager.storageOf(old.id()));
        assertEquals("external", manager.storageOf(fresh.id()));
        assertEquals(2, manager.list().size());
        assertNotNull(manager.find(old.id()));
        assertTrue(manager.propertiesFile(old).startsWith(legacy));
        assertTrue(manager.propertiesFile(fresh).startsWith(external));
    }

    @Test
    void idsStayUniqueAcrossBothRoots(@TempDir Path dir) throws Exception {
        Path external = dir.resolve("home").resolve("mc");
        Path legacy = dir.resolve("data").resolve("minecraft").resolve("instances");
        McInstanceManager manager = new McInstanceManager(external, legacy);

        McInstance old = manager.create("survival", "1.21.4", 25565, 25575, 2048, "world", null, true);
        McInstance fresh = manager.create("survival", "1.21.4", 25566, 25576, 2048, "world", null);

        assertEquals("survival", old.id());
        assertEquals("survival-2", fresh.id());
    }

    @Test
    void editingPortsIgnoresTheInstanceItself(@TempDir Path dir) throws Exception {
        McInstanceManager manager = manager(dir);
        McInstance first = manager.create("a", "1.21.4", 25565, 25575, 2048, "world", null);
        McInstance second = manager.create("b", "1.21.4", 25566, 25576, 2048, "world", null);

        // 它自己的两个端口不算冲突，别的实例的算。
        assertFalse(manager.portTaken(25565, first.id()));
        assertTrue(manager.portTaken(25565, second.id()));
        assertTrue(manager.portTaken(25565));
    }

    @Test
    void newPortsLandInBothTheManifestAndServerProperties(@TempDir Path dir) throws Exception {
        McInstanceManager manager = manager(dir);
        McInstance instance = manager.create("ports", "1.21.4", 25565, 25575, 2048, "world", null);

        McInstance updated = manager.updatePorts(instance, 26000, 26001);

        assertEquals(26000, manager.find("ports").port());
        assertEquals(26001, manager.find("ports").rconPort());
        String properties = Files.readString(manager.propertiesFile(updated));
        assertTrue(properties.contains("server-port=26000"));
        assertTrue(properties.contains("rcon.port=26001"));
        assertEquals(26000, updated.port());
    }
}