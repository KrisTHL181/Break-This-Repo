package net.minecraft.world.item.enchantment.effects;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.phys.Vec3;

public record ReplaceDisk(
   LevelBasedValue radius,
   LevelBasedValue height,
   Vec3i offset,
   Optional<BlockPredicate> predicate,
   BlockStateProvider blockState,
   Optional<Holder<GameEvent>> triggerGameEvent
) implements EnchantmentEntityEffect {
   public static final MapCodec<ReplaceDisk> CODEC = RecordCodecBuilder.mapCodec(
      p_345029_ -> p_345029_.group(
            LevelBasedValue.CODEC.fieldOf("radius").forGetter(ReplaceDisk::radius),
            LevelBasedValue.CODEC.fieldOf("height").forGetter(ReplaceDisk::height),
            Vec3i.CODEC.optionalFieldOf("offset", Vec3i.ZERO).forGetter(ReplaceDisk::offset),
            BlockPredicate.CODEC.optionalFieldOf("predicate").forGetter(ReplaceDisk::predicate),
            BlockStateProvider.CODEC.fieldOf("block_state").forGetter(ReplaceDisk::blockState),
            GameEvent.CODEC.optionalFieldOf("trigger_game_event").forGetter(ReplaceDisk::triggerGameEvent)
         )
         .apply(p_345029_, ReplaceDisk::new)
   );

   @Override
   public void apply(ServerLevel p_343394_, int p_343207_, EnchantedItemInUse p_342691_, Entity p_343742_, Vec3 p_342913_) {
      BlockPos blockpos = BlockPos.containing(p_342913_).offset(this.offset);
      RandomSource randomsource = p_343742_.getRandom();
      int i = (int)this.radius.calculate(p_343207_);
      int j = (int)this.height.calculate(p_343207_);

      for (BlockPos blockpos1 : BlockPos.betweenClosed(blockpos.offset(-i, 0, -i), blockpos.offset(i, Math.min(j - 1, 0), i))) {
         if (blockpos1.distToCenterSqr(p_342913_.x(), blockpos1.getY() + 0.5, p_342913_.z()) < Mth.square(i)
            && this.predicate.map(p_343365_ -> p_343365_.test(p_343394_, blockpos1)).orElse(true)
            && p_343394_.setBlockAndUpdate(blockpos1, this.blockState.getState(randomsource, blockpos1))) {
            this.triggerGameEvent.ifPresent(p_344749_ -> p_343394_.gameEvent(p_343742_, (Holder<GameEvent>)p_344749_, blockpos1));
         }
      }
   }

   @Override
   public MapCodec<ReplaceDisk> codec() {
      return CODEC;
   }
}
