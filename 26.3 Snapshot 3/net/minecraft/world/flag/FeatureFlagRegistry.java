package net.minecraft.world.flag;

import com.google.common.collect.Sets;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;

public class FeatureFlagRegistry {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final FeatureFlagUniverse universe;
   private final Map<Identifier, FeatureFlag> names;
   private final FeatureFlagSet allFlags;

   private FeatureFlagRegistry(final FeatureFlagUniverse universe, final FeatureFlagSet allFlags, final Map<Identifier, FeatureFlag> names) {
      this.universe = universe;
      this.names = names;
      this.allFlags = allFlags;
   }

   public boolean isSubset(final FeatureFlagSet set) {
      return set.isSubsetOf(this.allFlags);
   }

   public FeatureFlagSet allFlags() {
      return this.allFlags;
   }

   public FeatureFlagSet fromNames(final Iterable<Identifier> flagIds) {
      return this.fromNames(flagIds, flagId -> LOGGER.warn("Unknown feature flag: {}", flagId));
   }

   public FeatureFlagSet subset(final FeatureFlag... flags) {
      return FeatureFlagSet.create(this.universe, Arrays.asList(flags));
   }

   public FeatureFlagSet fromNames(final Iterable<Identifier> flagIds, final Consumer<Identifier> unknownFlags) {
      Set<FeatureFlag> flags = Sets.newIdentityHashSet();

      for (Identifier flagId : flagIds) {
         FeatureFlag flag = this.names.get(flagId);
         if (flag == null) {
            unknownFlags.accept(flagId);
         } else {
            flags.add(flag);
         }
      }

      return FeatureFlagSet.create(this.universe, flags);
   }

   public Set<Identifier> toNames(final FeatureFlagSet set) {
      Set<Identifier> result = new HashSet<>();
      this.names.forEach((id, flag) -> {
         if (set.contains(flag)) {
            result.add(id);
         }
      });
      return result;
   }

   public Codec<FeatureFlagSet> codec() {
      return Identifier.CODEC.listOf().comapFlatMap(ids -> {
         Set<Identifier> unknownIds = new HashSet<>();
         FeatureFlagSet result = this.fromNames(ids, unknownIds::add);
         return !unknownIds.isEmpty() ? DataResult.error(() -> "Unknown feature ids: " + unknownIds, result) : DataResult.success(result);
      }, set -> List.copyOf(this.toNames(set)));
   }

   public static class Builder {
      private final FeatureFlagUniverse universe;
      private int id;
      private final Map<Identifier, FeatureFlag> flags = new LinkedHashMap<>();

      public Builder(final String universeId) {
         this.universe = new FeatureFlagUniverse(universeId);
      }

      public FeatureFlag createVanilla(final String name) {
         return this.create(Identifier.withDefaultNamespace(name));
      }

      public FeatureFlag create(final Identifier name) {
         if (this.id >= 64) {
            throw new IllegalStateException("Too many feature flags");
         } else {
            FeatureFlag result = new FeatureFlag(this.universe, this.id++);
            FeatureFlag previous = this.flags.put(name, result);
            if (previous != null) {
               throw new IllegalStateException("Duplicate feature flag " + name);
            } else {
               return result;
            }
         }
      }

      public FeatureFlagRegistry build() {
         FeatureFlagSet allValues = FeatureFlagSet.create(this.universe, this.flags.values());
         return new FeatureFlagRegistry(this.universe, allValues, Map.copyOf(this.flags));
      }
   }
}
