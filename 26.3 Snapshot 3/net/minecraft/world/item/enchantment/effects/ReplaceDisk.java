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
      i -> i.group(
            LevelBasedValue.CODEC.fieldOf("radius").forGetter(ReplaceDisk::radius),
            LevelBasedValue.CODEC.fieldOf("height").forGetter(ReplaceDisk::height),
            Vec3i.CODEC.optionalFieldOf("offset", Vec3i.ZERO).forGetter(ReplaceDisk::offset),
            BlockPredicate.CODEC.optionalFieldOf("predicate").forGetter(ReplaceDisk::predicate),
            BlockStateProvider.CODEC.fieldOf("block_state").forGetter(ReplaceDisk::blockState),
            GameEvent.CODEC.optionalFieldOf("trigger_game_event").forGetter(ReplaceDisk::triggerGameEvent)
         )
         .apply(i, ReplaceDisk::new)
   );

   @Override
   public void apply(final ServerLevel serverLevel, final int enchantmentLevel, final EnchantedItemInUse item, final Entity entity, final Vec3 position) {
      BlockPos centerBlock = BlockPos.containing(position).offset(this.offset);
      RandomSource random = entity.getRandom();
      int dist = (int)this.radius.calculate(enchantmentLevel);
      int height = (int)this.height.calculate(enchantmentLevel);

      for (BlockPos pos : BlockPos.betweenClosed(centerBlock.offset(-dist, 0, -dist), centerBlock.offset(dist, Math.min(height - 1, 0), dist))) {
         if (pos.distToCenterSqr(position.x(), pos.getY() + 0.5, position.z()) < Mth.square(dist)
            && this.predicate.map(p -> p.test(serverLevel, pos)).orElse(true)
            && serverLevel.setBlockAndUpdate(pos, this.blockState.getState(serverLevel, random, pos))) {
            this.triggerGameEvent.ifPresent(event -> serverLevel.gameEvent(entity, (Holder<GameEvent>)event, pos));
         }
      }
   }

   @Override
   public MapCodec<ReplaceDisk> codec() {
      return CODEC;
   }
}
