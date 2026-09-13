package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.InsideBlockEffectType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class PowderSnowBlock extends Block implements BucketPickup {
   public static final MapCodec<PowderSnowBlock> CODEC = simpleCodec(PowderSnowBlock::new);
   private static final float HORIZONTAL_PARTICLE_MOMENTUM_FACTOR = 0.083333336F;
   private static final float IN_BLOCK_HORIZONTAL_SPEED_MULTIPLIER = 0.9F;
   private static final float IN_BLOCK_VERTICAL_SPEED_MULTIPLIER = 1.5F;
   private static final float NUM_BLOCKS_TO_FALL_INTO_BLOCK = 2.5F;
   private static final VoxelShape FALLING_COLLISION_SHAPE = Shapes.box(0.0, 0.0, 0.0, 1.0, 0.9F, 1.0);
   private static final double MINIMUM_FALL_DISTANCE_FOR_SOUND = 4.0;
   private static final double MINIMUM_FALL_DISTANCE_FOR_BIG_SOUND = 7.0;

   @Override
   public MapCodec<PowderSnowBlock> codec() {
      return CODEC;
   }

   public PowderSnowBlock(BlockBehaviour.Properties p_154253_) {
      super(p_154253_);
   }

   @Override
   protected boolean skipRendering(BlockState p_154268_, BlockState p_154269_, Direction p_154270_) {
      return p_154269_.is(this) ? true : super.skipRendering(p_154268_, p_154269_, p_154270_);
   }

   @Override
   protected void entityInside(
      BlockState p_154263_, Level p_154264_, BlockPos p_154265_, Entity p_154266_, InsideBlockEffectApplier p_397760_, boolean p_432055_
   ) {
      if (!(p_154266_ instanceof LivingEntity) || p_154266_.getInBlockState().is(this)) {
         p_154266_.makeStuckInBlock(p_154263_, new Vec3(0.9F, 1.5, 0.9F));
         if (p_154264_.isClientSide()) {
            RandomSource randomsource = p_154264_.getRandom();
            boolean flag = p_154266_.xOld != p_154266_.getX() || p_154266_.zOld != p_154266_.getZ();
            if (flag && randomsource.nextBoolean()) {
               p_154264_.addParticle(
                  ParticleTypes.SNOWFLAKE,
                  p_154266_.getX(),
                  p_154265_.getY() + 1,
                  p_154266_.getZ(),
                  Mth.randomBetween(randomsource, -1.0F, 1.0F) * 0.083333336F,
                  0.05F,
                  Mth.randomBetween(randomsource, -1.0F, 1.0F) * 0.083333336F
               );
            }
         }
      }

      BlockPos blockpos = p_154265_.immutable();
      p_397760_.runBefore(
         InsideBlockEffectType.EXTINGUISH,
         p_449906_ -> {
            if (p_154264_ instanceof ServerLevel serverlevel
               && p_449906_.isOnFire()
               && (serverlevel.getGameRules().get(GameRules.MOB_GRIEFING) || p_449906_ instanceof Player)
               && p_449906_.mayInteract(serverlevel, blockpos)) {
               p_154264_.destroyBlock(blockpos, false);
            }
         }
      );
      p_397760_.apply(InsideBlockEffectType.FREEZE);
      p_397760_.apply(InsideBlockEffectType.EXTINGUISH);
   }

   @Override
   public void fallOn(Level p_196695_, BlockState p_196696_, BlockPos p_196697_, Entity p_196698_, double p_397482_) {
      if (!(p_397482_ < 4.0) && p_196698_ instanceof LivingEntity livingentity) {
         LivingEntity.Fallsounds $$7 = livingentity.getFallSounds();
         SoundEvent soundevent = p_397482_ < 7.0 ? $$7.small() : $$7.big();
         p_196698_.playSound(soundevent, 1.0F, 1.0F);
      }
   }

   @Override
   protected VoxelShape getEntityInsideCollisionShape(BlockState p_395473_, BlockGetter p_393747_, BlockPos p_392572_, Entity p_396104_) {
      VoxelShape voxelshape = this.getCollisionShape(p_395473_, p_393747_, p_392572_, CollisionContext.of(p_396104_));
      return voxelshape.isEmpty() ? Shapes.block() : voxelshape;
   }

   @Override
   protected VoxelShape getCollisionShape(BlockState p_154285_, BlockGetter p_154286_, BlockPos p_154287_, CollisionContext p_154288_) {
      if (!p_154288_.isPlacement() && p_154288_ instanceof EntityCollisionContext entitycollisioncontext) {
         Entity entity = entitycollisioncontext.getEntity();
         if (entity != null) {
            if (entity.fallDistance > 2.5) {
               return FALLING_COLLISION_SHAPE;
            }

            boolean flag = entity instanceof FallingBlockEntity;
            if (flag || canEntityWalkOnPowderSnow(entity) && p_154288_.isAbove(Shapes.block(), p_154287_, false) && !p_154288_.isDescending()) {
               return super.getCollisionShape(p_154285_, p_154286_, p_154287_, p_154288_);
            }
         }
      }

      return Shapes.empty();
   }

   @Override
   protected VoxelShape getVisualShape(BlockState p_154276_, BlockGetter p_154277_, BlockPos p_154278_, CollisionContext p_154279_) {
      return Shapes.empty();
   }

   public static boolean canEntityWalkOnPowderSnow(Entity p_154256_) {
      if (p_154256_.getType().is(EntityTypeTags.POWDER_SNOW_WALKABLE_MOBS)) {
         return true;
      } else {
         return p_154256_ instanceof LivingEntity ? ((LivingEntity)p_154256_).getItemBySlot(EquipmentSlot.FEET).is(Items.LEATHER_BOOTS) : false;
      }
   }

   @Override
   public ItemStack pickupBlock(@Nullable LivingEntity p_397782_, LevelAccessor p_154281_, BlockPos p_154282_, BlockState p_154283_) {
      p_154281_.setBlock(p_154282_, Blocks.AIR.defaultBlockState(), 11);
      if (!p_154281_.isClientSide()) {
         p_154281_.levelEvent(2001, p_154282_, Block.getId(p_154283_));
      }

      return new ItemStack(Items.POWDER_SNOW_BUCKET);
   }

   @Override
   public Optional<SoundEvent> getPickupSound() {
      return Optional.of(SoundEvents.BUCKET_FILL_POWDER_SNOW);
   }

   @Override
   protected boolean isPathfindable(BlockState p_154258_, PathComputationType p_154261_) {
      return true;
   }
}
