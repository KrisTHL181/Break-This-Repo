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

   public BiomeParametersDumpReport(PackOutput p_256322_, CompletableFuture<HolderLookup.Provider> p_256222_) {
      this.topPath = p_256322_.getOutputFolder(PackOutput.Target.REPORTS).resolve("biome_parameters");
      this.registries = p_256222_;
   }

   @Override
   public CompletableFuture<?> run(CachedOutput p_254091_) {
      return this.registries
         .thenCompose(
            p_325848_ -> {
               DynamicOps<JsonElement> dynamicops = p_325848_.createSerializationContext(JsonOps.INSTANCE);
               List<CompletableFuture<?>> list = new ArrayList<>();
               MultiNoiseBiomeSourceParameterList.knownPresets()
                  .forEach((p_448666_, p_448667_) -> list.add(dumpValue(this.createPath(p_448666_.id()), p_254091_, dynamicops, CODEC, p_448667_)));
               return CompletableFuture.allOf(list.toArray(CompletableFuture[]::new));
            }
         );
   }

   private static <E> CompletableFuture<?> dumpValue(
      Path p_254407_, CachedOutput p_254093_, DynamicOps<JsonElement> p_253788_, Encoder<E> p_254276_, E p_254073_
   ) {
      Optional<JsonElement> optional = p_254276_.encodeStart(p_253788_, p_254073_)
         .resultOrPartial(p_236195_ -> LOGGER.error("Couldn't serialize element {}: {}", p_254407_, p_236195_));
      return optional.isPresent() ? DataProvider.saveStable(p_254093_, optional.get(), p_254407_) : CompletableFuture.completedFuture(null);
   }

   private Path createPath(Identifier p_454585_) {
      return this.topPath.resolve(p_454585_.getNamespace()).resolve(p_454585_.getPath() + ".json");
   }

   @Override
   public final String getName() {
      return "Biome Parameters";
   }
}
