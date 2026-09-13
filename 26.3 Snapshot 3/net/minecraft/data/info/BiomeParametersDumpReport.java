package net.minecraft.data.info;

import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Encoder;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterList;
import org.slf4j.Logger;

public class BiomeParametersDumpReport implements DataProvider {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final Path topPath;
   private final CompletableFuture<HolderLookup.Provider> registries;
   private static final MapCodec<ResourceKey<Biome>> ENTRY_CODEC = ResourceKey.codec(Registries.BIOME).fieldOf("biome");
   private static final Codec<Climate.ParameterList<ResourceKey<Biome>>> CODEC = Climate.ParameterList.codec(ENTRY_CODEC).fieldOf("biomes").codec();

   public BiomeParametersDumpReport(final PackOutput output, final CompletableFuture<HolderLookup.Provider> registries) {
      this.topPath = output.getOutputFolder(PackOutput.Target.REPORTS).resolve("biome_parameters");
      this.registries = registries;
   }

   @Override
   public CompletableFuture<?> run(final CachedOutput cache) {
      return this.registries
         .thenCompose(
            registryAccess -> {
               DynamicOps<JsonElement> registryOps = registryAccess.createSerializationContext(JsonOps.INSTANCE);
               List<CompletableFuture<?>> result = new ArrayList<>();
               MultiNoiseBiomeSourceParameterList.knownPresets()
                  .forEach((preset, parameterList) -> result.add(dumpValue(this.createPath(preset.id()), cache, registryOps, CODEC, parameterList)));
               return CompletableFuture.allOf(result.toArray(CompletableFuture[]::new));
            }
         );
   }

   private static <E> CompletableFuture<?> dumpValue(
      final Path path, final CachedOutput cache, final DynamicOps<JsonElement> ops, final Encoder<E> codec, final E value
   ) {
      Optional<JsonElement> result = codec.encodeStart(ops, value).resultOrPartial(e -> LOGGER.error("Couldn't serialize element {}: {}", path, e));
      return result.isPresent() ? DataProvider.saveStable(cache, result.get(), path) : CompletableFuture.completedFuture(null);
   }

   private Path createPath(final Identifier element) {
      return element.withSuffix(".json").resolveAgainst(this.topPath);
   }

   @Override
   public final String getName() {
      return "Biome Parameters";
   }
}
