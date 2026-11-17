/*
 * HDSL
 * Copyright (C) 2026  HDSL contributors
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
package org.jackhuang.hmcl.setting;

import org.jackhuang.hmcl.dsh.DshHomeMode;
import org.jackhuang.hmcl.dsh.DshInstance;
import org.jackhuang.hmcl.dsh.DshInstanceIcon;
import org.jackhuang.hmcl.dsh.DshInstanceManager;
import org.jackhuang.hmcl.dsh.DshNodeRuntime;
import org.jackhuang.hmcl.dsh.DshPortMode;
import org.jackhuang.hmcl.util.gson.JsonUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.jackhuang.hmcl.setting.SettingsManager.settings;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/// Verifies the selection rules a folder follows.
///
/// These are the rules that decide what the home page acts on, and the ones a
/// page used to have to re-derive for itself — badly, because a page only ran
/// its copy of them when somebody happened to refresh it.
class DshInstanceRepositoryTest {
    /// The folders the tests make instances in.
    @TempDir
    Path root;

    /// A second folder, for the tests that need two.
    @TempDir
    Path otherRoot;

    /// Starts every test with nothing selected anywhere.
    @BeforeEach
    void clearSelection() {
        settings().getSelectedInstance().clear();
    }

    /// Writes an instance manifest into a folder, as the manager would.
    ///
    /// @param folder    the folder acting as an instances directory
    /// @param id        the instance id
    /// @param createdAt when the instance was made, which is what orders the list
    /// @return the instance
    private static DshInstance write(Path folder, String id, long createdAt) throws IOException {
        DshInstance instance = new DshInstance(id, "1.0.0", DshInstance.DEFAULT_PROFILE,
                folder.toString(), DshNodeRuntime.SYSTEM, DshHomeMode.ISOLATED, null,
                List.of(), Map.of(), DshInstanceIcon.DEFAULT.id(), null,
                DshPortMode.AUTO, 0, createdAt);
        Path directory = folder.resolve(id);
        Files.createDirectories(directory);
        JsonUtils.writeToJsonFile(directory.resolve(DshInstanceManager.MANIFEST_NAME), instance);
        return instance;
    }

    /// Builds a repository over a folder.
    ///
    /// @param folder the folder
    /// @return the repository
    private static DshInstanceRepository repository(Path folder) {
        return new DshInstanceRepository(GameDirectory.of(folder));
    }

    @Test
    void aSnapshotHoldsTheInstancesTheFolderHolds() throws Exception {
        write(root, "alpha", 1L);
        write(root, "beta", 2L);

        DshInstanceRepository repository = repository(root);
        repository.refresh();

        assertEquals(List.of("beta", "alpha"),
                repository.getInstances().stream().map(DshInstance::id).toList(),
                "the newest instance leads, as the manager's own listing has it");
    }

    @Test
    void aFolderReadAgainIsPublishedAgain() throws Exception {
        write(root, "alpha", 1L);

        DshInstanceRepository repository = repository(root);
        List<Long> published = new java.util.ArrayList<>();
        repository.addSnapshotListener(
                (was, now) -> published.add(now.revision()));

        repository.refresh();
        repository.refresh();

        // Reading a folder twice without it changing produces two equal reads,
        // and a page that is only told about changes would never hear the
        // second one — which is how the list went on showing the folder that had
        // been selected before.
        assertEquals(2, published.size(),
                "every read has to reach the pages, even when the folder holds exactly what it held");
    }

    @Test
    void aFolderWithNothingChosenChoosesItsFirstInstance() throws Exception {
        write(root, "alpha", 1L);
        write(root, "beta", 2L);

        DshInstanceRepository repository = repository(root);
        repository.refresh();

        // This is what used to be missing: an instance the user just created was
        // listed but not selected, so the home page still said there was none.
        DshInstance selected = repository.getSelectedInstance();
        assertNotNull(selected, "a folder holding instances must have one selected");
        assertEquals("beta", selected.id());
        assertEquals("beta", settings().getSelectedInstance(repository.getDirectory().id()),
                "the choice has to be written down, or the next start forgets it");
    }

    @Test
    void anEmptyFolderSelectsNothing() throws Exception {
        DshInstanceRepository repository = repository(root);
        repository.refresh();

        assertNull(repository.getSelectedInstance());
        assertNull(settings().getSelectedInstance(repository.getDirectory().id()));
    }

    @Test
    void aSelectionThatIsGoneIsReplaced() throws Exception {
        write(root, "alpha", 1L);
        DshInstance beta = write(root, "beta", 2L);

        DshInstanceRepository repository = repository(root);
        repository.refresh();
        repository.setSelectedInstance(repository.getSnapshot().findInstance("alpha"));
        assertEquals("alpha", repository.getSelectedInstance().id());

        // The instance the selection names is deleted, as removing it from the
        // list does; the folder must not keep pointing at something that is gone.
        Files.delete(root.resolve("alpha").resolve(DshInstanceManager.MANIFEST_NAME));
        repository.refresh();

        assertNotNull(repository.getSelectedInstance());
        assertEquals(beta.id(), repository.getSelectedInstance().id());
    }

    @Test
    void everyFolderRemembersItsOwnSelection() throws Exception {
        DshInstance alpha = write(root, "alpha", 1L);
        DshInstance gamma = write(otherRoot, "gamma", 2L);

        DshInstanceRepository first = repository(root);
        first.refresh();
        DshInstanceRepository second = repository(otherRoot);
        second.refresh();

        first.setSelectedInstance(alpha);
        second.setSelectedInstance(gamma);

        assertEquals(alpha.id(), first.getSelectedInstance().id());
        assertEquals(gamma.id(), second.getSelectedInstance().id(),
                "one folder's choice must not be read as the other's");
    }

    @Test
    void choosingAnInstancePublishesIt() throws Exception {
        DshInstance alpha = write(root, "alpha", 1L);
        write(root, "beta", 2L);

        DshInstanceRepository repository = repository(root);
        repository.refresh();
        assertEquals("beta", repository.getSelectedInstance().id(), "the newest one is chosen first");

        List<String> seen = new java.util.ArrayList<>();
        repository.addSelectedInstanceListener(
                (was, now) -> seen.add(now == null ? null : now.id()));

        repository.setSelectedInstance(alpha);

        assertEquals(List.of("alpha"), seen,
                "the selection has to reach whoever is showing it, without being asked for");
    }
}
