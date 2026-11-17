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
import org.jackhuang.hmcl.dsh.DshInstanceManager;
import org.jackhuang.hmcl.dsh.DshPaths;
import org.jackhuang.hmcl.util.io.FileUtils;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.jackhuang.hmcl.setting.SettingsManager.settings;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies that making an instance reaches the interface.
///
/// This is the reported defect, pinned: an instance created by the wizard was
/// on disk but the list still said the folder held nothing, and the home page
/// still said nothing was selected, until somebody pressed refresh. Both are
/// consequences of a page having to go and look; here the page — represented by
/// the repository and the selected-instance property it publishes — is told.
class DshInstancesWiringTest {
    /// Creates an instance the way the install wizard does.
    ///
    /// @param id the instance id
    /// @return the instance
    private static DshInstance createAsWizard(String id) throws Exception {
        DshInstance instance = DshInstanceManager.create(id, "1.0.0", DshInstance.DEFAULT_PROFILE,
                Path.of(System.getProperty("java.io.tmpdir")), DshHomeMode.ISOLATED, null,
                List.of(), Map.of());
        assertNotNull(instance);
        return instance;
    }

    /// Removes an instance and everything it owns.
    ///
    /// @param id the instance id
    private static void remove(String id) throws Exception {
        DshInstanceManager.delete(id);
    }

    @Test
    void anInstanceMadeByTheWizardIsListedWithoutBeingAskedFor() throws Exception {
        DshInstanceRepository repository = GameDirectoryManager.getSelectedRepository();
        repository.refresh();
        int before = repository.getInstances().size();

        DshInstance created = createAsWizard("wiring-made-by-the-wizard");
        try {
            assertEquals(before + 1, repository.getInstances().size(),
                    "the folder has to publish what was written to it, without a refresh");
            assertTrue(repository.getInstances().stream().anyMatch(i -> i.id().equals(created.id())));
        } finally {
            remove(created.id());
        }

        assertEquals(before, repository.getInstances().size(),
                "removing one has to reach the interface the same way");
    }

    @Test
    void aFolderWithNothingSelectedChoosesWhatTheWizardMade() throws Exception {
        // The launcher is started with an empty folder, which is the state the
        // report starts from: nothing is selected anywhere.
        settings().getSelectedInstance().clear();

        DshInstance created = createAsWizard("wiring-first-instance");
        try {
            DshInstance selected = GameDirectoryManager.getSelectedInstance();
            assertNotNull(selected, "the home page must have something to act on");
            assertEquals(created.id(), selected.id(),
                    "the instance just made is the one the launch button targets");
        } finally {
            remove(created.id());
        }

        assertNull(GameDirectoryManager.getSelectedInstance(),
                "an empty folder leaves nothing selected");
    }

    @Test
    void aSelectionMadeAnywhereReachesTheHomePage() throws Exception {
        DshInstance first = createAsWizard("wiring-selection-a");
        DshInstance second = createAsWizard("wiring-selection-b");
        try {
            // Something is chosen already — the newest instance, chosen by the
            // folder itself. What the list's radio button then does is choose the
            // other one, and the home page is expected to follow without being
            // navigated to again.
            DshInstance current = GameDirectoryManager.getSelectedInstance();
            assertNotNull(current);
            DshInstance other = current.id().equals(first.id()) ? second : first;

            List<String> seen = new java.util.ArrayList<>();
            GameDirectoryManager.addSelectedInstanceListener(
                    (was, now) -> seen.add(now == null ? null : now.id()));

            GameDirectoryManager.setSelectedInstance(DshInstanceManager.find(other.id()));

            assertEquals(List.of(other.id()), seen);
        } finally {
            remove(first.id());
            remove(second.id());
        }
    }

    @Test
    void theFolderTheLauncherOwnsIsTheDefaultOne() {
        assertEquals(DshPaths.INSTANCES.toAbsolutePath().normalize(),
                GameDirectoryManager.selected().directory(),
                "with nothing configured, the launcher's own folder is what is shown");
    }

    @Test
    void theSelectionWrittenBeforeSelectionsWerePerFolderStillCounts() {
        LauncherSettings settings = new LauncherSettings();
        SettingsManager.applyLegacySelection(settings, "an-instance-from-an-older-launcher");

        assertEquals("an-instance-from-an-older-launcher",
                settings.getSelectedInstance(GameDirectory.DEFAULT_ID),
                "the one selection an older launcher kept belonged to the folder it owns");

        // A launcher that has moved on must not have the old value written back
        // over a choice made since.
        LauncherSettings migrated = new LauncherSettings();
        migrated.setSelectedInstance(GameDirectory.DEFAULT_ID, "chosen-since");
        SettingsManager.applyLegacySelection(migrated, "an-instance-from-an-older-launcher");
        assertEquals("chosen-since", migrated.getSelectedInstance(GameDirectory.DEFAULT_ID));
    }

    @Test
    void theDefaultFolderIsWhereAnInstanceGoes() throws Exception {
        assertTrue(Files.isDirectory(DshPaths.INSTANCES) || Files.createDirectories(DshPaths.INSTANCES) != null);
        Path instanceDirectory = DshPaths.instanceDirectory("wiring-path-check");
        assertEquals(DshPaths.INSTANCES.resolve("wiring-path-check"), instanceDirectory);
        FileUtils.deleteDirectory(instanceDirectory);
    }
}
