package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.IntProviders;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SculkBehaviour;
import net.minecraft.world.level.block.SculkShriekerBlock;
import net.minecraft.world.level.block.SculkSpreader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;

public record SculkPatchFeature(
   int chargeCount, int amountPerCharge, int spreadAttempts, int growthRounds, int spreadRounds, IntProvider extraRareGrowths, float catalystChance
) implements Feature {
   public static final MapCodec<SculkPatchFeature> CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(
            Codec.intRange(1, 32).fieldOf("charge_count").forGetter(SculkPatchFeature::chargeCount),
            Codec.intRange(1, 500).fieldOf("amount_per_charge").forGetter(SculkPatchFeature::amountPerCharge),
            Codec.intRange(1, 64).fieldOf("spread_attempts").forGetter(SculkPatchFeature::spreadAttempts),
            Codec.intRange(0, 8).fieldOf("growth_rounds").forGetter(SculkPatchFeature::growthRounds),
            Codec.intRange(0, 8).fieldOf("spread_rounds").forGetter(SculkPatchFeature::spreadRounds),
            IntProviders.CODEC.fieldOf("extra_rare_growths").forGetter(SculkPatchFeature::extraRareGrowths),
            Codec.floatRange(0.0F, 1.0F).fieldOf("catalyst_chance").forGetter(SculkPatchFeature::catalystChance)
         )
         .apply(i, SculkPatchFeature::new)
   );

   @Override
   public MapCodec<SculkPatchFeature> codec() {
      return CODEC;
   }

   @Override
   public boolean place(final WorldGenLevel level, final ChunkGenerator chunkGenerator, final RandomSource random, final BlockPos origin) {
      if (!this.canSpreadFrom(level, origin)) {
         return false;
      }

      SculkSpreader spreader = SculkSpreader.createWorldGenSpreader();
      int totalRounds = this.spreadRounds + this.growthRounds;

      for (int round = 0; round < totalRounds; round++) {
         for (int i = 0; i < this.chargeCount; i++) {
            spreader.addCursors(origin, this.amountPerCharge);
         }

         boolean spreadVeins = round < this.spreadRounds;

         for (int i = 0; i < this.spreadAttempts; i++) {
            spreader.updateCursors(level, origin, random, spreadVeins);
         }

         spreader.clear();
      }

      BlockPos below = origin.below();
      if (random.nextFloat() <= this.catalystChance && level.getBlockState(below).isCollisionShapeFullBlock(level, below)) {
         level.setBlockAndUpdate(origin, Blocks.SCULK_CATALYST.defaultBlockState());
      }

      int extraGrowths = this.extraRareGrowths.sample(random);

      for (int i = 0; i < extraGrowths; i++) {
         BlockPos candidate = origin.offset(random.nextInt(5) - 2, 0, random.nextInt(5) - 2);
         if (level.getBlockState(candidate).isAir() && level.getBlockState(candidate.below()).isFaceSturdy(level, candidate.below(), Direction.UP)) {
            level.setBlockAndUpdate(candidate, Blocks.SCULK_SHRIEKER.defaultBlockState().setValue(SculkShriekerBlock.CAN_SUMMON, true));
         }
      }

      return true;
   }

   private boolean canSpreadFrom(final LevelAccessor level, final BlockPos origin) {
      BlockState start = level.getBlockState(origin);
      if (start.getBlock() instanceof SculkBehaviour) {
         return true;
      } else {
         return !start.isAir() && (!start.is(Blocks.WATER) || !start.getFluidState().isSource())
            ? false
            : Direction.stream().map(origin::relative).anyMatch(pos -> level.getBlockState(pos).isCollisionShapeFullBlock(level, pos));
      }
   }
}
