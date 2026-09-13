package net.minecraft.world.level.levelgen.feature.rootplacers;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

public record AboveRootPlacement(BlockStateProvider aboveRootProvider, float aboveRootPlacementChance) {
   public static final Codec<AboveRootPlacement> CODEC = RecordCodecBuilder.create(
      p_225762_ -> p_225762_.group(
            BlockStateProvider.CODEC.fieldOf("above_root_provider").forGetter(p_225767_ -> p_225767_.aboveRootProvider),
            Codec.floatRange(0.0F, 1.0F).fieldOf("above_root_placement_chance").forGetter(p_225764_ -> p_225764_.aboveRootPlacementChance)
         )
         .apply(p_225762_, AboveRootPlacement::new)
   );
}
