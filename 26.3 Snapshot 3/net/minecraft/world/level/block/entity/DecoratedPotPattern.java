package net.minecraft.world.level.block.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;

public record DecoratedPotPattern(Identifier assetId) {
   public static final Codec<DecoratedPotPattern> CODEC = RecordCodecBuilder.create(
      i -> i.group(Identifier.CODEC.fieldOf("asset_id").forGetter(DecoratedPotPattern::assetId)).apply(i, DecoratedPotPattern::new)
   );
}
