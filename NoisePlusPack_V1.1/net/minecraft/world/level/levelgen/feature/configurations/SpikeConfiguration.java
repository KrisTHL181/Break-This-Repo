package net.minecraft.world.level.levelgen.feature.configurations;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.feature.SpikeFeature;
import org.jspecify.annotations.Nullable;

public class SpikeConfiguration implements FeatureConfiguration {
   public static final Codec<SpikeConfiguration> CODEC = RecordCodecBuilder.create(
      p_68115_ -> p_68115_.group(
            Codec.BOOL.fieldOf("crystal_invulnerable").orElse(false).forGetter(p_161195_ -> p_161195_.crystalInvulnerable),
            SpikeFeature.EndSpike.CODEC.listOf().fieldOf("spikes").forGetter(p_161193_ -> p_161193_.spikes),
            BlockPos.CODEC.optionalFieldOf("crystal_beam_target").forGetter(p_161191_ -> Optional.ofNullable(p_161191_.crystalBeamTarget))
         )
         .apply(p_68115_, SpikeConfiguration::new)
   );
   private final boolean crystalInvulnerable;
   private final List<SpikeFeature.EndSpike> spikes;
   private final @Nullable BlockPos crystalBeamTarget;

   public SpikeConfiguration(boolean p_68105_, List<SpikeFeature.EndSpike> p_68106_, @Nullable BlockPos p_68107_) {
      this(p_68105_, p_68106_, Optional.ofNullable(p_68107_));
   }

   private SpikeConfiguration(boolean p_68109_, List<SpikeFeature.EndSpike> p_68110_, Optional<BlockPos> p_68111_) {
      this.crystalInvulnerable = p_68109_;
      this.spikes = p_68110_;
      this.crystalBeamTarget = p_68111_.orElse(null);
   }

   public boolean isCrystalInvulnerable() {
      return this.crystalInvulnerable;
   }

   public List<SpikeFeature.EndSpike> getSpikes() {
      return this.spikes;
   }

   public @Nullable BlockPos getCrystalBeamTarget() {
      return this.crystalBeamTarget;
   }
}
