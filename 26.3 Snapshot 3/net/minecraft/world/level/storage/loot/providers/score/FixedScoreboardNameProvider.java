package net.minecraft.world.level.storage.loot.providers.score;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.scores.ScoreHolder;

public record FixedScoreboardNameProvider(String name) implements ScoreboardNameProvider {
   public static final MapCodec<FixedScoreboardNameProvider> MAP_CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(Codec.STRING.fieldOf("name").forGetter(FixedScoreboardNameProvider::name)).apply(i, FixedScoreboardNameProvider::new)
   );

   public static ScoreboardNameProvider forName(final String name) {
      return new FixedScoreboardNameProvider(name);
   }

   @Override
   public MapCodec<FixedScoreboardNameProvider> codec() {
      return MAP_CODEC;
   }

   @Override
   public ScoreHolder getScoreHolder(final LootContext context) {
      return ScoreHolder.forNameOnly(this.name);
   }
}
