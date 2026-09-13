package net.minecraft.world.item;

import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.StructureTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.EyeOfEnder;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EndPortalFrameBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockPattern;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class EnderEyeItem extends Item {
   public EnderEyeItem(final Item.Properties properties) {
      super(properties);
   }

   @Override
   public InteractionResult useOn(final UseOnContext context) {
      Level level = context.getLevel();
      BlockPos pos = context.getClickedPos();
      BlockState targetState = level.getBlockState(pos);
      if (!targetState.is(Blocks.END_PORTAL_FRAME) || targetState.getValue(EndPortalFrameBlock.HAS_EYE)) {
         return InteractionResult.PASS;
      }

      if (level.isClientSide()) {
         return InteractionResult.SUCCESS;
      }

      BlockState newState = targetState.setValue(EndPortalFrameBlock.HAS_EYE, true);
      Block.pushEntitiesUp(targetState, newState, level, pos);
      level.setBlock(pos, newState, 2);
      level.updateNeighbourForOutputSignal(pos, Blocks.END_PORTAL_FRAME);
      context.getItemInHand().shrink(1);
      level.levelEvent(1503, pos, 0);
      BlockPattern.BlockPatternMatch match = EndPortalFrameBlock.getOrCreatePortalShape().find(level, pos);
      if (match != null) {
         BlockPos blockPos = match.getFrontTopLeft().offset(-3, 0, -3);

         for (int x = 0; x < 3; x++) {
            for (int z = 0; z < 3; z++) {
               BlockPos portalBlockPos = blockPos.offset(x, 0, z);
               level.destroyBlock(portalBlockPos, true, null);
               level.setBlock(portalBlockPos, Blocks.END_PORTAL.defaultBlockState(), 2);
            }
         }

         level.globalLevelEvent(1038, blockPos.offset(1, 0, 1), 0);
      }

      return InteractionResult.SUCCESS;
   }

   @Override
   public int getUseDuration(final ItemStack itemStack, final LivingEntity user) {
      return 0;
   }

   @Override
   public InteractionResult use(final Level level, final Player player, final InteractionHand hand) {
      ItemStack itemStack = player.getItemInHand(hand);
      BlockHitResult hitResult = getPlayerPOVHitResult(level, player, ClipContext.Fluid.NONE);
      if (hitResult.getType() == HitResult.Type.BLOCK && level.getBlockState(hitResult.getBlockPos()).is(Blocks.END_PORTAL_FRAME)) {
         return InteractionResult.PASS;
      }

      player.startUsingItem(hand);
      if (level instanceof ServerLevel serverLevel) {
         BlockPos nearestMapFeature = serverLevel.findNearestMapStructure(StructureTags.EYE_OF_ENDER_LOCATED, player.blockPosition(), 100, false);
         if (nearestMapFeature == null) {
            return InteractionResult.CONSUME;
         }

         EyeOfEnder eyeOfEnder = new EyeOfEnder(level, player.getX(), player.getY(0.5), player.getZ());
         eyeOfEnder.setItem(itemStack);
         eyeOfEnder.signalTo(Vec3.atLowerCornerOf(nearestMapFeature));
         level.gameEvent(GameEvent.PROJECTILE_SHOOT, eyeOfEnder.position(), GameEvent.Context.of(player));
         level.addFreshEntity(eyeOfEnder);
         if (player instanceof ServerPlayer serverPlayer) {
            CriteriaTriggers.USED_ENDER_EYE.trigger(serverPlayer, nearestMapFeature);
         }

         float pitch = Mth.lerp(level.getRandom().nextFloat(), 0.33F, 0.5F);
         level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENDER_EYE_LAUNCH, SoundSource.NEUTRAL, 1.0F, pitch);
         itemStack.consume(1, player);
         player.awardStat(Stats.ITEM_USED.get(this));
      }

      return InteractionResult.SUCCESS_SERVER;
   }
}
