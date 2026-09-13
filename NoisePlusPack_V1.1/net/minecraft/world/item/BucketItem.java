package net.minecraft.world.item;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jspecify.annotations.Nullable;

public class BucketItem extends Item implements DispensibleContainerItem {
   private final Fluid content;

   public BucketItem(Fluid p_40689_, Item.Properties p_40690_) {
      super(p_40690_);
      this.content = p_40689_;
   }

   @Override
   public InteractionResult use(Level p_40703_, Player p_40704_, InteractionHand p_40705_) {
      ItemStack itemstack = p_40704_.getItemInHand(p_40705_);
      BlockHitResult blockhitresult = getPlayerPOVHitResult(
         p_40703_, p_40704_, this.content == Fluids.EMPTY ? ClipContext.Fluid.SOURCE_ONLY : ClipContext.Fluid.NONE
      );
      if (blockhitresult.getType() == HitResult.Type.MISS) {
         return InteractionResult.PASS;
      }

      if (blockhitresult.getType() != HitResult.Type.BLOCK) {
         return InteractionResult.PASS;
      }

      BlockPos blockpos = blockhitresult.getBlockPos();
      Direction direction = blockhitresult.getDirection();
      BlockPos blockpos1 = blockpos.relative(direction);
      if (!p_40703_.mayInteract(p_40704_, blockpos) || !p_40704_.mayUseItemAt(blockpos1, direction, itemstack)) {
         return InteractionResult.FAIL;
      }

      if (this.content == Fluids.EMPTY) {
         BlockState blockstate1 = p_40703_.getBlockState(blockpos);
         if (blockstate1.getBlock() instanceof BucketPickup bucketpickup) {
            ItemStack itemstack3 = bucketpickup.pickupBlock(p_40704_, p_40703_, blockpos, blockstate1);
            if (!itemstack3.isEmpty()) {
               p_40704_.awardStat(Stats.ITEM_USED.get(this));
               bucketpickup.getPickupSound().ifPresent(p_150709_ -> p_40704_.playSound(p_150709_, 1.0F, 1.0F));
               p_40703_.gameEvent(p_40704_, GameEvent.FLUID_PICKUP, blockpos);
               ItemStack itemstack2 = ItemUtils.createFilledResult(itemstack, p_40704_, itemstack3);
               if (!p_40703_.isClientSide()) {
                  CriteriaTriggers.FILLED_BUCKET.trigger((ServerPlayer)p_40704_, itemstack3);
               }

               return InteractionResult.SUCCESS.heldItemTransformedTo(itemstack2);
            }
         }

         return InteractionResult.FAIL;
      } else {
         BlockState blockstate = p_40703_.getBlockState(blockpos);
         BlockPos blockpos2 = blockstate.getBlock() instanceof LiquidBlockContainer && this.content == Fluids.WATER ? blockpos : blockpos1;
         if (this.emptyContents(p_40704_, p_40703_, blockpos2, blockhitresult)) {
            this.checkExtraContent(p_40704_, p_40703_, itemstack, blockpos2);
            if (p_40704_ instanceof ServerPlayer) {
               CriteriaTriggers.PLACED_BLOCK.trigger((ServerPlayer)p_40704_, blockpos2, itemstack);
            }

            p_40704_.awardStat(Stats.ITEM_USED.get(this));
            ItemStack itemstack1 = ItemUtils.createFilledResult(itemstack, p_40704_, getEmptySuccessItem(itemstack, p_40704_));
            return InteractionResult.SUCCESS.heldItemTransformedTo(itemstack1);
         } else {
            return InteractionResult.FAIL;
         }
      }
   }

   public static ItemStack getEmptySuccessItem(ItemStack p_40700_, Player p_40701_) {
      return !p_40701_.hasInfiniteMaterials() ? new ItemStack(Items.BUCKET) : p_40700_;
   }

   @Override
   public void checkExtraContent(@Nullable LivingEntity p_397951_, Level p_150712_, ItemStack p_150713_, BlockPos p_150714_) {
   }

   @Override
   public boolean emptyContents(@Nullable LivingEntity p_392717_, Level p_150717_, BlockPos p_150718_, @Nullable BlockHitResult p_150719_) {
      if (!(this.content instanceof FlowingFluid flowingfluid)) {
         return false;
      } else {
         BlockState blockstate = p_150717_.getBlockState(p_150718_);
         Block $$7 = blockstate.getBlock();
         boolean $$8 = blockstate.canBeReplaced(this.content);
         boolean flag1 = p_392717_ != null && p_392717_.isShiftKeyDown();
         boolean flag2 = $$8
            || $$7 instanceof LiquidBlockContainer liquidblockcontainer
               && liquidblockcontainer.canPlaceLiquid(p_392717_, p_150717_, p_150718_, blockstate, this.content);
         boolean flag3 = blockstate.isAir() || flag2 && (!flag1 || p_150719_ == null);
         if (!flag3) {
            return p_150719_ != null && this.emptyContents(p_392717_, p_150717_, p_150719_.getBlockPos().relative(p_150719_.getDirection()), null);
         }

         if (p_150717_.environmentAttributes().getValue(EnvironmentAttributes.WATER_EVAPORATES, p_150718_) && this.content.is(FluidTags.WATER)) {
            int l = p_150718_.getX();
            int i = p_150718_.getY();
            int j = p_150718_.getZ();
            p_150717_.playSound(
               p_392717_,
               p_150718_,
               SoundEvents.FIRE_EXTINGUISH,
               SoundSource.BLOCKS,
               0.5F,
               2.6F + (p_150717_.random.nextFloat() - p_150717_.random.nextFloat()) * 0.8F
            );

            for (int k = 0; k < 8; k++) {
               p_150717_.addParticle(
                  ParticleTypes.LARGE_SMOKE,
                  l + p_150717_.random.nextFloat(),
                  i + p_150717_.random.nextFloat(),
                  j + p_150717_.random.nextFloat(),
                  0.0,
                  0.0,
                  0.0
               );
            }

            return true;
         } else if ($$7 instanceof LiquidBlockContainer liquidblockcontainer1 && this.content == Fluids.WATER) {
            liquidblockcontainer1.placeLiquid(p_150717_, p_150718_, blockstate, flowingfluid.getSource(false));
            this.playEmptySound(p_392717_, p_150717_, p_150718_);
            return true;
         } else {
            if (!p_150717_.isClientSide() && $$8 && !blockstate.liquid()) {
               p_150717_.destroyBlock(p_150718_, true);
            }

            if (!p_150717_.setBlock(p_150718_, this.content.defaultFluidState().createLegacyBlock(), 11) && !blockstate.getFluidState().isSource()) {
               return false;
            }

            this.playEmptySound(p_392717_, p_150717_, p_150718_);
            return true;
         }
      }
   }

   protected void playEmptySound(@Nullable LivingEntity p_393933_, LevelAccessor p_40697_, BlockPos p_40698_) {
      SoundEvent soundevent = this.content.is(FluidTags.LAVA) ? SoundEvents.BUCKET_EMPTY_LAVA : SoundEvents.BUCKET_EMPTY;
      p_40697_.playSound(p_393933_, p_40698_, soundevent, SoundSource.BLOCKS, 1.0F, 1.0F);
      p_40697_.gameEvent(p_393933_, GameEvent.FLUID_PLACE, p_40698_);
   }

   public Fluid getContent() {
      return this.content;
   }
}
