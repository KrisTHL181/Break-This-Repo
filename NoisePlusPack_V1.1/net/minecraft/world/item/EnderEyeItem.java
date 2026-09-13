package net.minecraft.world.item;

import net.minecraft.advancements.CriteriaTriggers;
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
   public EnderEyeItem(Item.Properties p_41180_) {
      super(p_41180_);
   }

   @Override
   public InteractionResult useOn(UseOnContext p_41182_) {
      Level level = p_41182_.getLevel();
      BlockPos blockpos = p_41182_.getClickedPos();
      BlockState blockstate = level.getBlockState(blockpos);
      if (!blockstate.is(Blocks.END_PORTAL_FRAME) || blockstate.getValue(EndPortalFrameBlock.HAS_EYE)) {
         return InteractionResult.PASS;
      }

      if (level.isClientSide()) {
         return InteractionResult.SUCCESS;
      }

      BlockState blockstate1 = blockstate.setValue(EndPortalFrameBlock.HAS_EYE, true);
      Block.pushEntitiesUp(blockstate, blockstate1, level, blockpos);
      level.setBlock(blockpos, blockstate1, 2);
      level.updateNeighbourForOutputSignal(blockpos, Blocks.END_PORTAL_FRAME);
      p_41182_.getItemInHand().shrink(1);
      level.levelEvent(1503, blockpos, 0);
      BlockPattern.BlockPatternMatch blockpattern$blockpatternmatch = EndPortalFrameBlock.getOrCreatePortalShape().find(level, blockpos);
      if (blockpattern$blockpatternmatch != null) {
         BlockPos blockpos1 = blockpattern$blockpatternmatch.getFrontTopLeft().offset(-3, 0, -3);

         for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
               BlockPos blockpos2 = blockpos1.offset(i, 0, j);
               level.destroyBlock(blockpos2, true, null);
               level.setBlock(blockpos2, Blocks.END_PORTAL.defaultBlockState(), 2);
            }
         }

         level.globalLevelEvent(1038, blockpos1.offset(1, 0, 1), 0);
      }

      return InteractionResult.SUCCESS;
   }

   @Override
   public int getUseDuration(ItemStack p_331297_, LivingEntity p_343235_) {
      return 0;
   }

   @Override
   public InteractionResult use(Level p_41184_, Player p_41185_, InteractionHand p_41186_) {
      ItemStack itemstack = p_41185_.getItemInHand(p_41186_);
      BlockHitResult blockhitresult = getPlayerPOVHitResult(p_41184_, p_41185_, ClipContext.Fluid.NONE);
      if (blockhitresult.getType() == HitResult.Type.BLOCK && p_41184_.getBlockState(blockhitresult.getBlockPos()).is(Blocks.END_PORTAL_FRAME)) {
         return InteractionResult.PASS;
      }

      p_41185_.startUsingItem(p_41186_);
      if (p_41184_ instanceof ServerLevel serverlevel) {
         BlockPos blockpos = serverlevel.findNearestMapStructure(StructureTags.EYE_OF_ENDER_LOCATED, p_41185_.blockPosition(), 100, false);
         if (blockpos == null) {
            return InteractionResult.CONSUME;
         }

         EyeOfEnder eyeofender = new EyeOfEnder(p_41184_, p_41185_.getX(), p_41185_.getY(0.5), p_41185_.getZ());
         eyeofender.setItem(itemstack);
         eyeofender.signalTo(Vec3.atLowerCornerOf(blockpos));
         p_41184_.gameEvent(GameEvent.PROJECTILE_SHOOT, eyeofender.position(), GameEvent.Context.of(p_41185_));
         p_41184_.addFreshEntity(eyeofender);
         if (p_41185_ instanceof ServerPlayer serverplayer) {
            CriteriaTriggers.USED_ENDER_EYE.trigger(serverplayer, blockpos);
         }

         float f = Mth.lerp(p_41184_.random.nextFloat(), 0.33F, 0.5F);
         p_41184_.playSound(null, p_41185_.getX(), p_41185_.getY(), p_41185_.getZ(), SoundEvents.ENDER_EYE_LAUNCH, SoundSource.NEUTRAL, 1.0F, f);
         itemstack.consume(1, p_41185_);
         p_41185_.awardStat(Stats.ITEM_USED.get(this));
      }

      return InteractionResult.SUCCESS_SERVER;
   }
}
