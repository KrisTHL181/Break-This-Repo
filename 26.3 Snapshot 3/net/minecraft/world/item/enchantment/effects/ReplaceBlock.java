package net.minecraft.world.item.enchantment.effects;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.phys.Vec3;

public record ReplaceBlock(Vec3i offset, Optional<BlockPredicate> predicate, BlockStateProvider blockState, Optional<Holder<GameEvent>> triggerGameEvent)
   implements EnchantmentEntityEffect {
   public static final MapCodec<ReplaceBlock> CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(
            Vec3i.CODEC.optionalFieldOf("offset", Vec3i.ZERO).forGetter(ReplaceBlock::offset),
            BlockPredicate.CODEC.optionalFieldOf("predicate").forGetter(ReplaceBlock::predicate),
            BlockStateProvider.CODEC.fieldOf("block_state").forGetter(ReplaceBlock::blockState),
            GameEvent.CODEC.optionalFieldOf("trigger_game_event").forGetter(ReplaceBlock::triggerGameEvent)
         )
         .apply(i, ReplaceBlock::new)
   );

   @Override
   public void apply(final ServerLevel serverLevel, final int enchantmentLevel, final EnchantedItemInUse item, final Entity entity, final Vec3 position) {
      BlockPos pos = BlockPos.containing(position).offset(this.offset);
      if (this.predicate.map(p -> p.test(serverLevel, pos)).orElse(true)
         && serverLevel.setBlockAndUpdate(pos, this.blockState.getState(serverLevel, entity.getRandom(), pos))) {
         this.triggerGameEvent.ifPresent(event -> serverLevel.gameEvent(entity, (Holder<GameEvent>)event, pos));
      }
   }

   @Override
   public MapCodec<ReplaceBlock> codec() {
      return CODEC;
   }
}
