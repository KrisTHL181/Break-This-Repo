package net.minecraft.world.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BrushableBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BrushableBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class BrushItem extends Item {
   public static final int ANIMATION_DURATION = 10;
   private static final int USE_DURATION = 200;

   public BrushItem(final Item.Properties properties) {
      super(properties);
   }

   @Override
   public InteractionResult useOn(final UseOnContext context) {
      Player player = context.getPlayer();
      if (player != null && this.calculateHitResult(player).getType() == HitResult.Type.BLOCK) {
         player.startUsingItem(context.getHand());
      }

      return InteractionResult.CONSUME;
   }

   @Override
   public ItemUseAnimation getUseAnimation(final ItemStack itemStack) {
      return ItemUseAnimation.BRUSH;
   }

   @Override
   public int getUseDuration(final ItemStack itemStack, final LivingEntity user) {
      return 200;
   }

   @Override
   public void onUseTick(final Level level, final LivingEntity livingEntity, final ItemStack itemStack, final int ticksRemaining) {
      if (ticksRemaining >= 0 && livingEntity instanceof Player player) {
         HitResult hitResult = this.calculateHitResult(player);
         if (hitResult instanceof BlockHitResult blockHitResult && hitResult.getType() == HitResult.Type.BLOCK) {
            int timeElapsed = this.getUseDuration(itemStack, livingEntity) - ticksRemaining + 1;
            boolean isLastTickBeforeBackswing = timeElapsed % 10 == 5;
            if (isLastTickBeforeBackswing) {
               BlockPos pos = blockHitResult.getBlockPos();
               BlockState state = level.getBlockState(pos);
               HumanoidArm brushingArm = livingEntity.getUsedItemHand() == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
               if (state.shouldSpawnTerrainParticles() && state.getRenderShape() != RenderShape.INVISIBLE) {
                  this.spawnDustParticles(level, blockHitResult, state, livingEntity.getViewVector(0.0F), brushingArm);
               }

               SoundEvent brushSound;
               if (state.getBlock() instanceof BrushableBlock brushableBlock) {
                  brushSound = brushableBlock.getBrushSound();
               } else {
                  brushSound = SoundEvents.BRUSH_GENERIC;
               }

               level.playSound(player, pos, brushSound, SoundSource.BLOCKS);
               if (level instanceof ServerLevel serverLevel && level.getBlockEntity(pos) instanceof BrushableBlockEntity brushableBlockEntity) {
                  boolean brushingUpdatedState = brushableBlockEntity.brush(level.getGameTime(), serverLevel, player, blockHitResult.getDirection(), itemStack);
                  if (brushingUpdatedState) {
                     EquipmentSlot equippedHand = itemStack.equals(player.getItemBySlot(EquipmentSlot.OFFHAND))
                        ? EquipmentSlot.OFFHAND
                        : EquipmentSlot.MAINHAND;
                     itemStack.hurtAndBreak(1, player, equippedHand);
                  }
               }
            }
         } else {
            livingEntity.releaseUsingItem();
         }
      } else {
         livingEntity.releaseUsingItem();
      }
   }

   private HitResult calculateHitResult(final Player player) {
      return ProjectileUtil.getHitResultOnViewVector(player, EntitySelector.CAN_BE_PICKED, player.blockInteractionRange());
   }

   private void spawnDustParticles(
      final Level level, final BlockHitResult hitResult, final BlockState state, final Vec3 viewVector, final HumanoidArm brushingArm
   ) {
      double deltaScale = 3.0;
      int flip = brushingArm == HumanoidArm.RIGHT ? 1 : -1;
      int particles = level.getRandom().nextInt(7, 12);
      BlockParticleOption particle = new BlockParticleOption(ParticleTypes.BLOCK, state);
      Direction hitDirection = hitResult.getDirection();
      BrushItem.DustParticlesDelta dustParticlesDelta = BrushItem.DustParticlesDelta.fromDirection(viewVector, hitDirection);
      Vec3 hitLocation = hitResult.getLocation();

      for (int i = 0; i < particles; i++) {
         level.addParticle(
            particle,
            hitLocation.x - (hitDirection == Direction.WEST ? 1.0E-6F : 0.0F),
            hitLocation.y,
            hitLocation.z - (hitDirection == Direction.NORTH ? 1.0E-6F : 0.0F),
            dustParticlesDelta.xd() * flip * 3.0 * level.getRandom().nextDouble(),
            0.0,
            dustParticlesDelta.zd() * flip * 3.0 * level.getRandom().nextDouble()
         );
      }
   }

   private record DustParticlesDelta(double xd, double yd, double zd) {
      private static final double ALONG_SIDE_DELTA = 1.0;
      private static final double OUT_FROM_SIDE_DELTA = 0.1;

      public static BrushItem.DustParticlesDelta fromDirection(final Vec3 viewVector, final Direction hitDirection) {
         double yd = 0.0;

         return switch (hitDirection) {
            case DOWN, UP -> new BrushItem.DustParticlesDelta(viewVector.z(), 0.0, -viewVector.x());
            case NORTH -> new BrushItem.DustParticlesDelta(1.0, 0.0, -0.1);
            case SOUTH -> new BrushItem.DustParticlesDelta(-1.0, 0.0, 0.1);
            case WEST -> new BrushItem.DustParticlesDelta(-0.1, 0.0, -1.0);
            case EAST -> new BrushItem.DustParticlesDelta(0.1, 0.0, 1.0);
         };
      }
   }
}
