package net.minecraft.server;

import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.Lifecycle;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Stream;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.WritableRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.tags.TagLoader;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.Util;
import net.minecraft.world.level.storage.loot.LootDataType;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import org.slf4j.Logger;

public class ReloadableServerRegistries {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final RegistrationInfo DEFAULT_REGISTRATION_INFO = new RegistrationInfo(Optional.empty(), Lifecycle.experimental());

   public static CompletableFuture<ReloadableServerRegistries.LoadResult> reload(
      LayeredRegistryAccess<RegistryLayer> p_331894_, List<Registry.PendingTags<?>> p_361834_, ResourceManager p_333753_, Executor p_334093_
   ) {
      List<HolderLookup.RegistryLookup<?>> list = TagLoader.buildUpdatedLookups(p_331894_.getAccessForLoading(RegistryLayer.RELOADABLE), p_361834_);
      HolderLookup.Provider holderlookup$provider = HolderLookup.Provider.create(list.stream());
      RegistryOps<JsonElement> registryops = holderlookup$provider.createSerializationContext(JsonOps.INSTANCE);
      List<CompletableFuture<WritableRegistry<?>>> list1 = LootDataType.values()
         .map(p_358525_ -> scheduleRegistryLoad((LootDataType<?>)p_358525_, registryops, p_333753_, p_334093_))
         .toList();
      CompletableFuture<List<WritableRegistry<?>>> completablefuture = Util.sequence(list1);
      return completablefuture.thenApplyAsync(
         p_358521_ -> createAndValidateFullContext(p_331894_, holderlookup$provider, (List<WritableRegistry<?>>)p_358521_), p_334093_
      );
   }

   private static <T> CompletableFuture<WritableRegistry<?>> scheduleRegistryLoad(
      LootDataType<T> p_335755_, RegistryOps<JsonElement> p_328500_, ResourceManager p_330738_, Executor p_327700_
   ) {
      return CompletableFuture.supplyAsync(
         () -> {
            WritableRegistry<T> writableregistry = new MappedRegistry<>(p_335755_.registryKey(), Lifecycle.experimental());
            Map<Identifier, T> map = new HashMap<>();
            SimpleJsonResourceReloadListener.scanDirectory(p_330738_, p_335755_.registryKey(), p_328500_, p_335755_.codec(), map);
            map.forEach(
               (p_448834_, p_448835_) -> writableregistry.register(
                  ResourceKey.create(p_335755_.registryKey(), p_448834_), (T)p_448835_, DEFAULT_REGISTRATION_INFO
               )
            );
            TagLoader.loadTagsForRegistry(p_330738_, writableregistry);
            return writableregistry;
         },
         p_327700_
      );
   }

   private static ReloadableServerRegistries.LoadResult createAndValidateFullContext(
      LayeredRegistryAccess<RegistryLayer> p_368439_, HolderLookup.Provider p_370039_, List<WritableRegistry<?>> p_363778_
   ) {
      LayeredRegistryAccess<RegistryLayer> layeredregistryaccess = createUpdatedRegistries(p_368439_, p_363778_);
      HolderLookup.Provider holderlookup$provider = concatenateLookups(p_370039_, layeredregistryaccess.getLayer(RegistryLayer.RELOADABLE));
      validateLootRegistries(holderlookup$provider);
      return new ReloadableServerRegistries.LoadResult(layeredregistryaccess, holderlookup$provider);
   }

   private static HolderLookup.Provider concatenateLookups(HolderLookup.Provider p_366421_, HolderLookup.Provider p_368061_) {
      return HolderLookup.Provider.create(Stream.concat(p_366421_.listRegistries(), p_368061_.listRegistries()));
   }

   private static void validateLootRegistries(HolderLookup.Provider p_368763_) {
      ProblemReporter.Collector problemreporter$collector = new ProblemReporter.Collector();
      ValidationContext validationcontext = new ValidationContext(problemreporter$collector, LootContextParamSets.ALL_PARAMS, p_368763_);
      LootDataType.values().forEach(p_358528_ -> validateRegistry(validationcontext, (LootDataType<?>)p_358528_, p_368763_));
      problemreporter$collector.forEach(
         (p_405126_, p_405127_) -> LOGGER.warn("Found loot table element validation problem in {}: {}", p_405126_, p_405127_.description())
      );
   }

   private static LayeredRegistryAccess<RegistryLayer> createUpdatedRegistries(
      LayeredRegistryAccess<RegistryLayer> p_334470_, List<WritableRegistry<?>> p_328349_
   ) {
      return p_334470_.replaceFrom(RegistryLayer.RELOADABLE, new RegistryAccess.ImmutableRegistryAccess(p_328349_).freeze());
   }

   private static <T> void validateRegistry(ValidationContext p_335560_, LootDataType<T> p_335486_, HolderLookup.Provider p_365047_) {
      HolderLookup<T> holderlookup = p_365047_.lookupOrThrow(p_335486_.registryKey());
      holderlookup.listElements().forEach(p_334560_ -> p_335486_.runValidation(p_335560_, p_334560_.key(), p_334560_.value()));
   }

   public static class Holder {
      private final HolderLookup.Provider registries;

      public Holder(HolderLookup.Provider p_369437_) {
         this.registries = p_369437_;
      }

      public HolderLookup.Provider lookup() {
         return this.registries;
      }

      public LootTable getLootTable(ResourceKey<LootTable> p_331432_) {
         return this.registries
            .lookup(Registries.LOOT_TABLE)
            .flatMap(p_328118_ -> p_328118_.get(p_331432_))
            .map(net.minecraft.core.Holder::value)
            .orElse(LootTable.EMPTY);
      }
   }

   public record LoadResult(LayeredRegistryAccess<RegistryLayer> layers, HolderLookup.Provider lookupWithUpdatedTags) {
   }
}
