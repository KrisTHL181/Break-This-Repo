package net.minecraft.world.entity.variant;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.Biome;

public record BiomeCheck(HolderSet<Biome> requiredBiomes) implements SpawnCondition {
   public static final MapCodec<BiomeCheck> MAP_CODEC = RecordCodecBuilder.mapCodec(
      p_392898_ -> p_392898_.group(RegistryCodecs.homogeneousList(Registries.BIOME).fieldOf("biomes").forGetter(BiomeCheck::requiredBiomes))
         .apply(p_392898_, BiomeCheck::new)
   );

   public boolean test(SpawnContext p_397261_) {
      return this.requiredBiomes.contains(p_397261_.biome());
   }

   @Override
   public MapCodec<BiomeCheck> codec() {
      return MAP_CODEC;
   }
}
