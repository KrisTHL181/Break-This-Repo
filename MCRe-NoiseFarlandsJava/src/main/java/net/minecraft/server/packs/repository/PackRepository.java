package net.minecraft.server.packs.repository;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.server.packs.PackResources;
import net.minecraft.util.Util;
import net.minecraft.world.flag.FeatureFlagSet;
import org.jspecify.annotations.Nullable;

public class PackRepository {
    private final Set<RepositorySource> sources;
    private Map<String, Pack> available = ImmutableMap.of();
    private List<Pack> selected = ImmutableList.of();

    public PackRepository(final RepositorySource... sources) {
        this.sources = ImmutableSet.copyOf(sources);
    }

    public static String displayPackList(final Collection<Pack> packs) {
        return packs.stream().map(pack -> pack.getId() + (pack.getCompatibility().isCompatible() ? "" : " (incompatible)")).collect(Collectors.joining(", "));
    }

    public void reload() {
        List<String> currentlySelectedNames = this.selected.stream().map(Pack::getId).collect(ImmutableList.toImmutableList());
        this.available = this.discoverAvailable();
        this.selected = this.rebuildSelected(currentlySelectedNames);
    }

    private Map<String, Pack> discoverAvailable() {
        Map<String, Pack> discovered = Maps.newTreeMap();

        for (RepositorySource source : this.sources) {
            source.loadPacks(pack -> discovered.put(pack.getId(), pack));
        }

        return ImmutableMap.copyOf(discovered);
    }

    public boolean isAbleToClearAnyPack() {
        List<Pack> newSelected = this.rebuildSelected(List.of());
        return !this.selected.equals(newSelected);
    }

    public void setSelected(final Collection<String> packs) {
        this.selected = this.rebuildSelected(packs);
    }

    public boolean addPack(final String packId) {
        Pack pack = this.available.get(packId);
        if (pack != null && !this.selected.contains(pack)) {
            List<Pack> selectedCopy = Lists.newArrayList(this.selected);
            selectedCopy.add(pack);
            this.selected = selectedCopy;
            return true;
        } else {
            return false;
        }
    }

    public boolean removePack(final String packId) {
        Pack pack = this.available.get(packId);
        if (pack != null && this.selected.contains(pack)) {
            List<Pack> selectedCopy = Lists.newArrayList(this.selected);
            selectedCopy.remove(pack);
            this.selected = selectedCopy;
            return true;
        } else {
            return false;
        }
    }

    private List<Pack> rebuildSelected(final Collection<String> selectedNames) {
        List<Pack> selectedAndPresent = this.getAvailablePacks(selectedNames).collect(Util.toMutableList());

        for (Pack pack : this.available.values()) {
            if (pack.isRequired() && !selectedAndPresent.contains(pack)) {
                pack.getDefaultPosition().insert(selectedAndPresent, pack, Pack::selectionConfig, false);
            }
        }

        return ImmutableList.copyOf(selectedAndPresent);
    }

    private Stream<Pack> getAvailablePacks(final Collection<String> ids) {
        return ids.stream().map(this.available::get).filter(Objects::nonNull);
    }

    public Collection<String> getAvailableIds() {
        return this.available.keySet();
    }

    public Collection<Pack> getAvailablePacks() {
        return this.available.values();
    }

    public Collection<String> getSelectedIds() {
        return this.selected.stream().map(Pack::getId).collect(ImmutableSet.toImmutableSet());
    }

    public FeatureFlagSet getRequestedFeatureFlags() {
        return this.getSelectedPacks().stream().map(Pack::getRequestedFeatures).reduce(FeatureFlagSet::join).orElse(FeatureFlagSet.of());
    }

    public Collection<Pack> getSelectedPacks() {
        return this.selected;
    }

    public @Nullable Pack getPack(final String id) {
        return this.available.get(id);
    }

    public boolean isAvailable(final String id) {
        return this.available.containsKey(id);
    }

    public List<PackResources> openAllSelected() {
        return this.selected.stream().map(Pack::open).collect(ImmutableList.toImmutableList());
    }
}