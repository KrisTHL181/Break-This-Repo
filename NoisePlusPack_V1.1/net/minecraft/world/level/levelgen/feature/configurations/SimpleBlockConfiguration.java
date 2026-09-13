package net.minecraft.world.level.levelgen.feature.configurations;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

public record SimpleBlockConfiguration(BlockStateProvider toPlace, boolean scheduleTick) implements FeatureConfiguration {
   public static final Codec<SimpleBlockConfiguration> CODEC = RecordCodecBuilder.create(
      p_375355_ -> p_375355_.group(
            BlockStateProvider.CODEC.fieldOf("to_place").forGetter(p_161168_ -> p_161168_.toPlace),
            Codec.BOOL.optionalFieldOf("schedule_tick", false).forGetter(p_375356_ -> p_375356_.scheduleTick)
         )
         .apply(p_375355_, SimpleBlockConfiguration::new)
   );

   public SimpleBlockConfiguration(BlockStateProvider p_161155_) {
      this(p_161155_, false);
   }
}
