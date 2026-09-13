package net.minecraft.server;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Lifecycle;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Stream;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.TagLoader;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.loot.LootDataType;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.ValidationContextSource;
import org.slf4j.Logger;

public class ReloadableServerRegistries {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final RegistrationInfo DEFAULT_REGISTRATION_INFO = new RegistrationInfo(Optional.empty(), Lifecycle.experimental());

   public static CompletableFuture<ReloadableServerRegistries.LoadResult> reload(
      final LayeredRegistryAccess<RegistryLayer> context,
      final List<Registry.PendingTags<?>> updatedContextTags,
      final ResourceManager manager,
      final Executor executor
   ) {
      List<HolderLookup.RegistryLookup<?>> contextRegistriesWithTags = TagLoader.buildUpdatedLookups(
         context.getAccessForLoading(RegistryLayer.RELOADABLE), updatedContextTags
      );
      HolderLookup.Provider loadingContextWithTags = HolderLookup.Provider.create(contextRegistriesWithTags.stream());
      return RegistryDataLoader.load(manager, contextRegistriesWithTags, RegistryDataLoader.RELOADABLE_REGISTRIES, executor)
         .thenApplyAsync(newlyLoadedRegistries -> createAndValidateFullContext(context, loadingContextWithTags, newlyLoadedRegistries), executor);
   }

   private static ReloadableServerRegistries.LoadResult createAndValidateFullContext(
      final LayeredRegistryAccess<RegistryLayer> contextLayers,
      final HolderLookup.Provider contextLookupWithUpdatedTags,
      final RegistryAccess.Frozen reloadableRegistries
   ) {
      LayeredRegistryAccess<RegistryLayer> fullLayers = contextLayers.replaceFrom(RegistryLayer.RELOADABLE, reloadableRegistries);
      HolderLookup.Provider fullLookupWithUpdatedTags = concatenateLookups(contextLookupWithUpdatedTags, reloadableRegistries);
      validateLootRegistries(fullLookupWithUpdatedTags);
      return new ReloadableServerRegistries.LoadResult(fullLayers, fullLookupWithUpdatedTags);
   }

   private static HolderLookup.Provider concatenateLookups(final HolderLookup.Provider first, final HolderLookup.Provider second) {
      return HolderLookup.Provider.create(Stream.concat(first.listRegistries(), second.listRegistries()));
   }

   private static void validateLootRegistries(final HolderLookup.Provider fullContextWithNewTags) {
      ProblemReporter.Collector problems = new ProblemReporter.Collector();
      ValidationContextSource contextSource = new ValidationContextSource(problems, fullContextWithNewTags);
      LootDataType.values().forEach(lootDataType -> lootDataType.runValidation(contextSource, fullContextWithNewTags));
      problems.forEach((id, problem) -> LOGGER.warn("Found loot table element validation problem in {}: {}", id, problem.description()));
   }

   public static class Holder {
      private final HolderLookup.Provider registries;

      public Holder(final HolderLookup.Provider registries) {
         this.registries = registries;
      }

      public HolderLookup.Provider lookup() {
         return this.registries;
      }

      public LootTable getLootTable(final ResourceKey<LootTable> id) {
         return this.registries.lookup(Registries.LOOT_TABLE).flatMap(r -> r.get(id)).map(net.minecraft.core.Holder::value).orElse(LootTable.EMPTY);
      }
   }

   public record LoadResult(LayeredRegistryAccess<RegistryLayer> layers, HolderLookup.Provider lookupWithUpdatedTags) {
   }
}
