package net.minecraft.world.item;

import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
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
   protected final Fluid content;

   public BucketItem(final Fluid content, final Item.Properties properties) {
      super(properties);
      this.content = content;
   }

   @Override
   public InteractionResult use(final Level level, final Player player, final InteractionHand hand) {
      ItemStack itemStack = player.getItemInHand(hand);
      BlockHitResult hitResult = getPlayerPOVHitResult(level, player, this.getFluidContext());
      if (hitResult.getType() == HitResult.Type.MISS) {
         return InteractionResult.PASS;
      }

      if (hitResult.getType() != HitResult.Type.BLOCK) {
         return InteractionResult.PASS;
      }

      BlockPos pos = hitResult.getBlockPos();
      Direction direction = hitResult.getDirection();
      BlockPos directionOffsetPos = pos.relative(direction);
      if (level.mayInteract(player, pos) && player.mayUseItemAt(directionOffsetPos, direction, itemStack)) {
         BlockState clicked = level.getBlockState(pos);
         BlockPos placePos = clicked.getBlock() instanceof LiquidBlockContainer && this.content == Fluids.WATER ? pos : directionOffsetPos;
         if (this.emptyContents(player, level, placePos, hitResult)) {
            this.checkExtraContent(player, level, itemStack, placePos);
            if (player instanceof ServerPlayer serverPlayer && this.content != Fluids.EMPTY) {
               CriteriaTriggers.PLACED_BLOCK.trigger(serverPlayer, placePos, itemStack);
            }

            player.awardStat(Stats.ITEM_USED.get(this));
            ItemStack emptyResult = ItemUtils.createFilledResult(itemStack, player, getEmptySuccessItem(itemStack, player));
            return InteractionResult.SUCCESS.heldItemTransformedTo(emptyResult);
         } else {
            if (this.content == Fluids.EMPTY) {
               BlockState blockState = level.getBlockState(pos);
               if (blockState.getBlock() instanceof BucketPickup bucketPickupBlock) {
                  ItemStack taken = bucketPickupBlock.pickupBlock(player, level, pos, blockState);
                  if (!taken.isEmpty()) {
                     player.awardStat(Stats.ITEM_USED.get(this));
                     bucketPickupBlock.getPickupSound().ifPresent(soundEvent -> player.playSound(soundEvent, 1.0F, 1.0F));
                     level.gameEvent(player, GameEvent.FLUID_PICKUP, pos);
                     ItemStack result = ItemUtils.createFilledResult(itemStack, player, taken);
                     if (!level.isClientSide()) {
                        CriteriaTriggers.FILLED_BUCKET.trigger((ServerPlayer)player, taken);
                     }

                     return InteractionResult.SUCCESS.heldItemTransformedTo(result);
                  }
               }
            }

            return InteractionResult.FAIL;
         }
      } else {
         return InteractionResult.FAIL;
      }
   }

   public ClipContext.Fluid getFluidContext() {
      return this.content == Fluids.EMPTY ? ClipContext.Fluid.SOURCE_ONLY : ClipContext.Fluid.NONE;
   }

   public static ItemStack getEmptySuccessItem(final ItemStack itemStack, final Player player) {
      return !player.hasInfiniteMaterials() ? new ItemStack(Items.BUCKET) : itemStack;
   }

   @Override
   public void checkExtraContent(final @Nullable LivingEntity user, final Level level, final ItemStack itemStack, final BlockPos pos) {
   }

   @Override
   public boolean emptyContents(final @Nullable LivingEntity user, final Level level, final BlockPos pos, final @Nullable BlockHitResult hitResult) {
      if (!(this.content instanceof FlowingFluid flowingFluid)) {
         return false;
      } else {
         BlockState blockState = level.getBlockState(pos);
         Block block = blockState.getBlock();
         boolean mayReplace = blockState.canBeReplaced(this.content);
         boolean shiftKeyDown = user != null && user.isShiftKeyDown();
         boolean placeLiquid = mayReplace
            || block instanceof LiquidBlockContainer container && container.canPlaceLiquid(user, level, pos, blockState, this.content);
         boolean canPlaceFluidInsideBlock = blockState.isAir() || placeLiquid && (!shiftKeyDown || hitResult == null);
         if (!canPlaceFluidInsideBlock) {
            return hitResult != null && this.emptyContents(user, level, hitResult.getBlockPos().relative(hitResult.getDirection()), null);
         }

         if (level.environmentAttributes().getValue(EnvironmentAttributes.WATER_EVAPORATES, pos) && this.content.is(FluidTags.WATER)) {
            int x = pos.getX();
            int y = pos.getY();
            int z = pos.getZ();
            RandomSource random = level.getRandom();
            level.playSound(user, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 2.6F + (random.nextFloat() - random.nextFloat()) * 0.8F);

            for (int i = 0; i < 8; i++) {
               level.addParticle(ParticleTypes.LARGE_SMOKE, x + random.nextFloat(), y + random.nextFloat(), z + random.nextFloat(), 0.0, 0.0, 0.0);
            }

            return true;
         } else if (block instanceof LiquidBlockContainer container && this.content == Fluids.WATER) {
            container.placeLiquid(level, pos, blockState, flowingFluid.getSource(false));
            this.playEmptySound(user, level, pos);
            return true;
         } else {
            if (!level.isClientSide() && mayReplace && !blockState.liquid()) {
               level.destroyBlock(pos, true);
            }

            if (!level.setBlock(pos, this.content.defaultFluidState().createLegacyBlock(), 11) && !blockState.getFluidState().isSource()) {
               return false;
            }

            this.playEmptySound(user, level, pos);
            return true;
         }
      }
   }

   protected void playEmptySound(final @Nullable LivingEntity user, final LevelAccessor level, final BlockPos pos) {
      SoundEvent soundEvent = this.content.is(FluidTags.LAVA) ? SoundEvents.BUCKET_EMPTY_LAVA : SoundEvents.BUCKET_EMPTY;
      level.playSound(user, pos, soundEvent, SoundSource.BLOCKS, 1.0F, 1.0F);
      level.gameEvent(user, GameEvent.FLUID_PLACE, pos);
   }

   public Fluid getContent() {
      return this.content;
   }
}
