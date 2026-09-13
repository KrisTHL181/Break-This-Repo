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

   public PackRepository(RepositorySource... p_251886_) {
      this.sources = ImmutableSet.copyOf(p_251886_);
   }

   public static String displayPackList(Collection<Pack> p_331712_) {
      return p_331712_.stream()
         .map(p_326476_ -> p_326476_.getId() + (p_326476_.getCompatibility().isCompatible() ? "" : " (incompatible)"))
         .collect(Collectors.joining(", "));
   }

   public void reload() {
      List<String> list = this.selected.stream().map(Pack::getId).collect(ImmutableList.toImmutableList());
      this.available = this.discoverAvailable();
      this.selected = this.rebuildSelected(list);
   }

   private Map<String, Pack> discoverAvailable() {
      Map<String, Pack> map = Maps.newTreeMap();

      for (RepositorySource repositorysource : this.sources) {
         repositorysource.loadPacks(p_143903_ -> map.put(p_143903_.getId(), p_143903_));
      }

      return ImmutableMap.copyOf(map);
   }

   public boolean isAbleToClearAnyPack() {
      List<Pack> list = this.rebuildSelected(List.of());
      return !this.selected.equals(list);
   }

   public void setSelected(Collection<String> p_10510_) {
      this.selected = this.rebuildSelected(p_10510_);
   }

   public boolean addPack(String p_276042_) {
      Pack pack = this.available.get(p_276042_);
      if (pack != null && !this.selected.contains(pack)) {
         List<Pack> list = Lists.newArrayList(this.selected);
         list.add(pack);
         this.selected = list;
         return true;
      } else {
         return false;
      }
   }

   public boolean removePack(String p_276065_) {
      Pack pack = this.available.get(p_276065_);
      if (pack != null && this.selected.contains(pack)) {
         List<Pack> list = Lists.newArrayList(this.selected);
         list.remove(pack);
         this.selected = list;
         return true;
      } else {
         return false;
      }
   }

   private List<Pack> rebuildSelected(Collection<String> p_10518_) {
      List<Pack> list = this.getAvailablePacks(p_10518_).collect(Util.toMutableList());

      for (Pack pack : this.available.values()) {
         if (pack.isRequired() && !list.contains(pack)) {
            pack.getDefaultPosition().insert(list, pack, Pack::selectionConfig, false);
         }
      }

      return ImmutableList.copyOf(list);
   }

   private Stream<Pack> getAvailablePacks(Collection<String> p_10521_) {
      return p_10521_.stream().map(this.available::get).filter(Objects::nonNull);
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

   public @Nullable Pack getPack(String p_10508_) {
      return this.available.get(p_10508_);
   }

   public boolean isAvailable(String p_10516_) {
      return this.available.containsKey(p_10516_);
   }

   public List<PackResources> openAllSelected() {
      return this.selected.stream().map(Pack::open).collect(ImmutableList.toImmutableList());
   }
}
