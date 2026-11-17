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

import org.jackhuang.hmcl.dsh.DshInstance;
import org.jackhuang.hmcl.dsh.DshInstanceManager;
import org.jackhuang.hmcl.dsh.DshPaths;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import static org.jackhuang.hmcl.setting.SettingsManager.settings;
import static org.jackhuang.hmcl.util.logging.Logger.LOG;

/// The folders the launcher looks for instances in.
///
/// The launcher owns one directory — the one it created — and can be pointed at
/// others. It never moves instances between them: adding a folder makes the
/// launcher look inside, and that is all. HMCL behaves the same way, and the
/// reasoning holds here: the files are the user's, and relocating them is not
/// what "add this folder" asked for.
///
/// This class is also where the interface gets its instances from. Each folder
/// has a [DshInstanceRepository] that owns that folder's snapshot and its
/// remembered selection, and the one belonging to the selected folder is what
/// every page observes. Pages therefore never ask what changed; they are told.
///
/// The desktop build exposed toolkit observables; this copy exposes plain
/// listener registration, which is the shape the web server's event bus
/// consumes.
@NotNullByDefault
public final class GameDirectoryManager {
    private GameDirectoryManager() {
    }

    /// Whether [#init()] has run.
    private static boolean initialized;

    /// The folders the launcher knows about, in the order they are shown.
    private static List<GameDirectory> mergedGameDirectories = List.of();

    /// The repository of every folder, created on first use.
    private static final Map<String, DshInstanceRepository> repositories = new HashMap<>();

    /// The folder whose instances the interface shows.
    private static @Nullable GameDirectory selectedGameDirectory;

    /// The repository of the selected folder, or `null` before one is resolved.
    private static @Nullable DshInstanceRepository selectedRepository;

    /// The instance selected in the selected folder.
    ///
    /// Projected from the selected repository rather than stored, so a page
    /// subscribes once and the value survives a switch of folder.
    private static @Nullable DshInstance selectedInstance;

    /// Who is told when the selected instance changes.
    private static final List<ChangeListener<@Nullable DshInstance>> selectedInstanceListeners =
            new CopyOnWriteArrayList<>();

    /// Listeners notified whenever the selected folder publishes a snapshot.
    private static final List<Consumer<DshInstanceRepository>> versionsListeners = new ArrayList<>(4);

    /// Initializes the folder list and the selected folder from the settings.
    ///
    /// Called by the launcher at start-up; every other entry point calls it on
    /// first use as well, so a command-line run that never starts the interface
    /// still sees the folders it was given.
    public static synchronized void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        DshInstanceManager.addChangeListener(GameDirectoryManager::refreshRepositories);
        rebuildDirectories();
        selectDirectory(resolveSelectedDirectory());
    }

    /// Returns the folders the launcher looks for instances in.
    ///
    /// @return the folders, the default first
    public static @Unmodifiable List<GameDirectory> getGameDirectories() {
        ensureInitialized();
        return mergedGameDirectories;
    }

    /// Returns the configured directories.
    ///
    /// @return the directories, the default first
    public static List<GameDirectory> directories() {
        return getGameDirectories();
    }

    /// Registers a listener called with the old and the new selected folder.
    ///
    /// @param listener the listener
    public static void addSelectedGameDirectoryListener(ChangeListener<@Nullable GameDirectory> listener) {
        ensureInitialized();
        directoryListeners.add(listener);
    }

    /// Who is told when the selected folder changes.
    private static final List<ChangeListener<@Nullable GameDirectory>> directoryListeners =
            new CopyOnWriteArrayList<>();

    /// Returns the folder whose instances the list shows.
    ///
    /// @return the selected directory, falling back to the first
    public static GameDirectory selected() {
        ensureInitialized();
        GameDirectory directory = selectedGameDirectory;
        return directory != null ? directory : resolveSelectedDirectory();
    }

    /// Selects the directory whose instances the list shows.
    ///
    /// @param id the directory identifier
    public static void select(String id) {
        GameDirectory directory = find(id);
        if (directory != null) {
            ensureInitialized();
            selectDirectory(directory);
        }
    }

    /// Selects the folder whose instances the list shows.
    ///
    /// @param directory the folder, which must be one of [#getGameDirectories()]
    public static void setSelectedGameDirectory(GameDirectory directory) {
        if (!getGameDirectories().contains(directory)) {
            throw new IllegalArgumentException("Unknown game directory: " + directory);
        }
        selectDirectory(directory);
    }

    /// Switches the selected folder, wiring the selected-instance projection to
    /// its repository and re-reading it.
    ///
    /// @param directory the folder to select
    private static void selectDirectory(GameDirectory directory) {
        GameDirectory previousDirectory = selectedGameDirectory;
        DshInstanceRepository previousRepository = selectedRepository;
        selectedGameDirectory = directory;
        settings().setSelectedGameDirectoryId(directory.id());

        DshInstanceRepository repository = getOrCreateRepository(directory);
        selectedRepository = repository;

        if (previousRepository != null) {
            previousRepository.removeSnapshotListener(snapshotListener);
            previousRepository.removeSelectedInstanceListener(selectedInstanceListener);
        }
        repository.addSnapshotListener(snapshotListener);
        repository.addSelectedInstanceListener(selectedInstanceListener);

        for (ChangeListener<@Nullable GameDirectory> listener : directoryListeners) {
            listener.onChange(previousDirectory, directory);
        }

        // The folder is re-read on every switch, as HMCL re-reads a game
        // directory when it becomes the selected one: a folder that was not
        // looked at while it was not selected may well have changed.
        repository.refresh();
        publishSelectedInstance(repository.getSelectedInstance());
    }

    /// Returns the repository of the selected folder.
    ///
    /// @return the repository
    public static DshInstanceRepository getSelectedRepository() {
        ensureInitialized();
        DshInstanceRepository repository = selectedRepository;
        if (repository == null) {
            repository = getOrCreateRepository(selected());
            selectedRepository = repository;
            repository.addSnapshotListener(snapshotListener);
            repository.addSelectedInstanceListener(selectedInstanceListener);
            repository.refresh();
        }
        return repository;
    }

    /// Returns the instance selected in the selected folder.
    ///
    /// @return the instance, or `null` when the folder holds none
    public static @Nullable DshInstance getSelectedInstance() {
        return getSelectedRepository().getSelectedInstance();
    }

    /// Registers a listener called with the old and the new selected instance.
    ///
    /// The value follows a switch of folder, a change of selection, and a change
    /// to the folder's contents, which is what lets a page show the right
    /// instance without ever asking whether it should.
    ///
    /// @param listener the listener
    public static void addSelectedInstanceListener(ChangeListener<@Nullable DshInstance> listener) {
        getSelectedRepository();
        selectedInstanceListeners.add(listener);
    }

    /// Removes a selected-instance listener.
    ///
    /// @param listener the listener
    public static void removeSelectedInstanceListener(ChangeListener<@Nullable DshInstance> listener) {
        selectedInstanceListeners.remove(listener);
    }

    /// Records the instance selected in the selected folder.
    ///
    /// @param instance the instance to select, or `null` to select nothing
    public static void setSelectedInstance(@Nullable DshInstance instance) {
        getSelectedRepository().setSelectedInstance(instance);
    }

    /// Registers a listener notified when the selected folder's instances change.
    ///
    /// A listener registered after the first read is answered immediately, so a
    /// page that is created later is not left waiting for the next change to
    /// learn what is already there.
    ///
    /// @param listener the listener
    public static void registerVersionsListener(Consumer<DshInstanceRepository> listener) {
        DshInstanceRepository repository = getSelectedRepository();
        if (repository.isLoaded()) {
            listener.accept(repository);
        }
        versionsListeners.add(listener);
    }

    /// Re-reads every folder the launcher has looked at.
    ///
    /// Called after a write so that a folder which is not the selected one is
    /// current by the time it is selected.
    public static void refreshRepositories() {
        ensureInitialized();
        for (DshInstanceRepository repository : List.copyOf(repositories.values())) {
            repository.refresh();
        }
    }

    /// Finds a directory by identifier.
    ///
    /// @param id the identifier
    /// @return the directory, or `null` when there is none
    public static @Nullable GameDirectory find(String id) {
        for (GameDirectory directory : getGameDirectories()) {
            if (directory.id().equals(id)) {
                return directory;
            }
        }
        return null;
    }

    /// Adds a folder to the list.
    ///
    /// A folder already present is returned as it stands rather than added
    /// twice: the identity is the path, not the entry.
    ///
    /// @param path the folder to add
    /// @return the directory entry, existing or new
    /// @throws IllegalArgumentException when the path is not a directory
    public static GameDirectory add(Path path) {
        return add(path, null);
    }

    /// Adds a folder, under a name of its own if one is given.
    ///
    /// The path is recorded as given: a folder chosen relative to the launcher
    /// stays relative, and one chosen by its absolute path stays absolute. Both
    /// are resolved the same way when the folder is read back.
    ///
    /// @param path       the folder to add
    /// @param customName the name to show, or `null` to derive one from the path
    /// @return the directory entry, existing or new
    /// @throws IllegalArgumentException when the path is not a directory
    public static GameDirectory add(Path path, @Nullable String customName) {
        ensureInitialized();
        Path normalized = path.toAbsolutePath().normalize();
        if (!Files.isDirectory(normalized)) {
            throw new IllegalArgumentException(normalized + " is not a directory");
        }
        for (GameDirectory directory : directories()) {
            if (directory.directory().equals(normalized)) {
                return directory;
            }
        }

        GameDirectory added = GameDirectory.of(path, customName);
        List<GameDirectory> updated = new ArrayList<>(directories());
        updated.add(added);
        settings().setGameDirectories(List.copyOf(updated));
        rebuildDirectories();
        return added;
    }

    /// Removes a folder from the list.
    ///
    /// Nothing is deleted: the instances inside keep their files and simply stop
    /// being listed. The directory the launcher owns cannot be removed, because
    /// it is where a new instance goes when nowhere else is chosen.
    ///
    /// @param id the directory identifier
    /// @throws IllegalArgumentException when the default directory is named
    public static void remove(String id) {
        ensureInitialized();
        if (GameDirectory.DEFAULT_ID.equals(id)) {
            throw new IllegalArgumentException("The default directory cannot be removed");
        }
        List<GameDirectory> updated = new ArrayList<>(directories());
        updated.removeIf(directory -> directory.id().equals(id));
        settings().setGameDirectories(List.copyOf(updated));
        rebuildDirectories();
        if (id.equals(settings().getSelectedGameDirectoryId())) {
            select(GameDirectory.DEFAULT_ID);
        }
    }

    /// Counts the instances a folder holds.
    ///
    /// Used to tell the user what adding a folder found, which is the whole
    /// point of adding one.
    ///
    /// @param directory the folder
    /// @return how many instances it holds
    public static int countInstances(GameDirectory directory) {
        Path root = directory.directory();
        if (!Files.isDirectory(root)) {
            return 0;
        }
        try (var children = Files.list(root)) {
            return (int) children
                    .filter(Files::isDirectory)
                    .filter(child -> Files.isRegularFile(child.resolve(DshInstanceManager.MANIFEST_NAME)))
                    .count();
        } catch (java.io.IOException e) {
            return 0;
        }
    }

    /// Reports whether this folder is the one the launcher owns.
    ///
    /// @param directory the directory
    /// @return whether the launcher writes its own instances there
    public static boolean isOwned(GameDirectory directory) {
        return DshPaths.INSTANCES.toAbsolutePath().normalize().equals(directory.directory());
    }

    /// Returns the repository of a folder, creating it on first use.
    ///
    /// @param directory the folder
    /// @return the repository
    private static DshInstanceRepository getOrCreateRepository(GameDirectory directory) {
        return repositories.computeIfAbsent(directory.id(), id -> new DshInstanceRepository(directory));
    }

    /// The snapshot listener every selected repository carries.
    ///
    /// Held rather than written inline, because a listener is removed by
    /// identity: a method reference written twice would be two different objects
    /// and the previous folder would keep talking to the interface.
    private static final ChangeListener<DshInstanceRepository.Snapshot> snapshotListener =
            (was, now) -> {
                for (Consumer<DshInstanceRepository> listener : List.copyOf(versionsListeners)) {
                    // Caught one by one: a page that fails to redraw must not
                    // leave the pages registered after it showing what was there
                    // before, which is what letting the exception out would do.
                    try {
                        listener.accept(selectedRepository);
                    } catch (RuntimeException e) {
                        LOG.warning("A listener failed to handle a new snapshot", e);
                    }
                }
            };

    /// The selected-instance listener every selected repository carries.
    private static final ChangeListener<@Nullable DshInstance> selectedInstanceListener =
            (was, now) -> publishSelectedInstance(now);

    /// Publishes the selected instance to every subscriber.
    ///
    /// @param instance the instance to publish
    private static void publishSelectedInstance(@Nullable DshInstance instance) {
        DshInstance previous = selectedInstance;
        boolean same = previous == null ? instance == null
                : instance != null && previous.id().equals(instance.id());
        if (same) {
            return;
        }
        selectedInstance = instance;
        for (ChangeListener<@Nullable DshInstance> listener : selectedInstanceListeners) {
            listener.onChange(previous, instance);
        }
    }

    /// Rebuilds the folder list from the settings.
    ///
    /// A launcher that has never been given a folder reports the one it owns, so
    /// the default needs no entry in the settings and no migration to introduce.
    private static void rebuildDirectories() {
        List<GameDirectory> configured = settings().getGameDirectories();
        List<GameDirectory> resolved = configured == null || configured.isEmpty()
                ? List.of(GameDirectory.defaultDirectory())
                : List.copyOf(configured);
        mergedGameDirectories = resolved;
    }

    /// Resolves the remembered selected folder against the current list.
    ///
    /// @return the selected folder, falling back to the first
    private static GameDirectory resolveSelectedDirectory() {
        String id = settings().getSelectedGameDirectoryId();
        for (GameDirectory directory : mergedGameDirectories) {
            if (directory.id().equals(id)) {
                return directory;
            }
        }
        return mergedGameDirectories.get(0);
    }

    /// Initializes this class on first use.
    private static void ensureInitialized() {
        if (!initialized) {
            init();
        }
    }
}
