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
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

import static org.jackhuang.hmcl.setting.SettingsManager.settings;

/// The instances of one folder, as the interface sees them.
///
/// This is the HDSL counterpart of HMCL's `HMCLGameRepository`, and it is
/// the reason the interface never has to go looking for changes itself: it owns
/// a snapshot that is republished whenever the folder is re-read, and a
/// selection that re-resolves against that snapshot. A page subscribes to the
/// two and is therefore always showing what is on disk, without a refresh call
/// anywhere.
///
/// Instances are still files, so a read is a directory listing; the snapshot
/// exists to give that listing an identity the interface can observe, not to
/// cache it.
///
/// The desktop build published through toolkit properties and moved the
/// publication onto the toolkit thread; this copy notifies listeners on the
/// thread that made the change, which is the shape a web server's event bus
/// forwards to browsers anyway.
@NotNullByDefault
public final class DshInstanceRepository {
    /// One read of a folder: the instances it held at that moment.
    ///
    /// The revision is what makes one read different from the next. It is not
    /// decoration: a record compares structurally, so reading a folder again and
    /// finding exactly what was there before would produce a value equal to the
    /// one already published, and a listener — and everything hanging off it —
    /// treats an equal value as no change at all. The pages would then be
    /// showing the folder that was selected before, which is precisely the state
    /// this class exists to keep them out of.
    ///
    /// @param directory the folder the snapshot describes
    /// @param instances the instances inside it, newest first
    /// @param revision  which read this is
    public record Snapshot(GameDirectory directory, List<DshInstance> instances, long revision) {
        /// Finds an instance by id.
        ///
        /// @param id the instance id
        /// @return the instance, or `null` when the snapshot does not hold it
        public @Nullable DshInstance findInstance(String id) {
            for (DshInstance instance : instances) {
                if (instance.id().equals(id)) {
                    return instance;
                }
            }
            return null;
        }
    }

    /// The folder this repository describes.
    private final GameDirectory directory;

    /// The last read of the folder.
    ///
    /// It starts empty rather than unset: a page may subscribe to the selection
    /// before the first read has finished, and an unread folder holds nothing.
    private Snapshot snapshot;

    /// The instance selected in this folder, resolved against the current snapshot.
    private @Nullable DshInstance selectedInstance;

    /// Whether the folder has been read at least once.
    private boolean loaded;

    /// Counts the reads, so that two reads are two snapshots.
    private final AtomicLong revisions = new AtomicLong();

    /// Who is told when a new snapshot is published.
    private final List<ChangeListener<Snapshot>> snapshotListeners = new CopyOnWriteArrayList<>();

    /// Who is told when the selected instance changes.
    private final List<ChangeListener<@Nullable DshInstance>> selectedInstanceListeners = new CopyOnWriteArrayList<>();

    /// Creates a repository for a folder.
    ///
    /// @param directory the folder to describe
    DshInstanceRepository(GameDirectory directory) {
        this.directory = directory;
        this.snapshot = new Snapshot(directory, List.of(), 0);
    }

    /// Returns the folder this repository describes.
    ///
    /// @return the folder
    public GameDirectory getDirectory() {
        return directory;
    }

    /// Returns the last read of the folder.
    ///
    /// @return the snapshot
    public Snapshot getSnapshot() {
        return snapshot;
    }

    /// Registers a listener called with the old and the new snapshot on every
    /// read — even one that found exactly what the previous one did.
    ///
    /// @param listener the listener
    public void addSnapshotListener(ChangeListener<Snapshot> listener) {
        snapshotListeners.add(listener);
    }

    /// Removes a snapshot listener.
    ///
    /// @param listener the listener
    public void removeSnapshotListener(ChangeListener<Snapshot> listener) {
        snapshotListeners.remove(listener);
    }

    /// Returns the instances the folder holds, newest first.
    ///
    /// @return the instances
    public List<DshInstance> getInstances() {
        return snapshot.instances();
    }

    /// Reports whether the folder has been read.
    ///
    /// A listener registered before the first read is called by it, so this is
    /// only asked to decide whether a registration has to be answered
    /// immediately.
    ///
    /// @return whether a read has completed
    public boolean isLoaded() {
        return loaded;
    }

    /// Returns the instance selected in this folder.
    ///
    /// @return the instance, or `null` when the folder holds none
    public @Nullable DshInstance getSelectedInstance() {
        return selectedInstance;
    }

    /// Registers a listener called with the old and the new selected instance
    /// whenever the selection changes.
    ///
    /// @param listener the listener
    public void addSelectedInstanceListener(ChangeListener<@Nullable DshInstance> listener) {
        selectedInstanceListeners.add(listener);
    }

    /// Removes a selected-instance listener.
    ///
    /// @param listener the listener
    public void removeSelectedInstanceListener(ChangeListener<@Nullable DshInstance> listener) {
        selectedInstanceListeners.remove(listener);
    }

    /// Records an instance as this folder's selection.
    ///
    /// The value is only written down; the field follows from it, so passing
    /// an instance of another folder cannot desynchronise the two.
    ///
    /// @param instance the instance to select, or `null` to select nothing
    public void setSelectedInstance(@Nullable DshInstance instance) {
        settings().setSelectedInstance(directory.id(), instance == null ? null : instance.id());
        refreshSelectedInstance();
        SettingsManager.save();
    }

    /// Corrects the remembered selection against the current snapshot.
    ///
    /// A selection naming an instance that is gone is no selection: the home
    /// page would otherwise offer to start something that is not there, while
    /// the list says the folder holds nothing. An empty folder clears it, and a
    /// folder that holds something but has nothing chosen chooses its first
    /// instance — which is what makes a newly created instance the one the home
    /// page acts on without anyone having to click it.
    public void refreshSelectedInstance() {
        @Nullable String persistedId = settings().getSelectedInstance(directory.id());
        @Nullable DshInstance refreshed = persistedId != null ? getSnapshot().findInstance(persistedId) : null;
        if (refreshed == null) {
            refreshed = getInstances().stream().findFirst().orElse(null);
        }

        @Nullable String refreshedId = refreshed == null ? null : refreshed.id();
        if (!Objects.equals(persistedId, refreshedId)) {
            settings().setSelectedInstance(directory.id(), refreshedId);
        }
        setSelected(refreshed);
    }

    /// Re-reads the folder and publishes what it holds.
    ///
    /// Safe to call from any thread; the listeners run on the caller's thread.
    public void refresh() {
        List<DshInstance> instances = DshInstanceManager.listIn(directory.directory());
        publish(new Snapshot(directory, instances, revisions.incrementAndGet()));
    }

    /// Publishes a snapshot and re-resolves the selection against it.
    ///
    /// @param next the snapshot to publish
    private void publish(Snapshot next) {
        loaded = true;
        Snapshot previous = snapshot;
        snapshot = next;
        for (ChangeListener<Snapshot> listener : snapshotListeners) {
            listener.onChange(previous, next);
        }
        refreshSelectedInstance();
    }

    /// Publishes a new selected instance when it differs from the current one.
    ///
    /// @param next the instance to publish
    private void setSelected(@Nullable DshInstance next) {
        if (Objects.equals(selectedInstance == null ? null : selectedInstance.id(),
                next == null ? null : next.id())) {
            return;
        }
        @Nullable DshInstance previous = selectedInstance;
        selectedInstance = next;
        for (ChangeListener<@Nullable DshInstance> listener : selectedInstanceListeners) {
            listener.onChange(previous, next);
        }
    }
}
