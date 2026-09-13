package net.minecraft.world.level.block;

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
   private static final float HORIZONTAL_PARTICLE_MOMENTUM_FACTOR = 0.083333336F;
   private static final float IN_BLOCK_HORIZONTAL_SPEED_MULTIPLIER = 0.9F;
   private static final float IN_BLOCK_VERTICAL_SPEED_MULTIPLIER = 1.5F;
   private static final float NUM_BLOCKS_TO_FALL_INTO_BLOCK = 2.5F;
   private static final VoxelShape FALLING_COLLISION_SHAPE = Shapes.box(0.0, 0.0, 0.0, 1.0, 0.9F, 1.0);
   private static final double MINIMUM_FALL_DISTANCE_FOR_SOUND = 4.0;
   private static final double MINIMUM_FALL_DISTANCE_FOR_BIG_SOUND = 7.0;

   public PowderSnowBlock(final BlockBehaviour.Properties properties) {
      super(properties);
   }

   @Override
   protected boolean skipRendering(final BlockState state, final BlockState neighborState, final Direction direction) {
      return neighborState.is(this) ? true : super.skipRendering(state, neighborState, direction);
   }

   @Override
   protected void entityInside(
      final BlockState state, final Level level, final BlockPos pos, final Entity entity, final InsideBlockEffectApplier effectApplier, final boolean isPrecise
   ) {
      if (!(entity instanceof LivingEntity) || entity.getInBlockState().is(this)) {
         entity.makeStuckInBlock(state, new Vec3(0.9F, 1.5, 0.9F));
         if (level.isClientSide()) {
            RandomSource random = level.getRandom();
            boolean isMoving = entity.xOld != entity.getX() || entity.zOld != entity.getZ();
            if (isMoving && random.nextBoolean()) {
               level.addParticle(
                  ParticleTypes.SNOWFLAKE,
                  entity.getX(),
                  pos.getY() + 1,
                  entity.getZ(),
                  Mth.randomBetween(random, -1.0F, 1.0F) * 0.083333336F,
                  0.05F,
                  Mth.randomBetween(random, -1.0F, 1.0F) * 0.083333336F
               );
            }
         }
      }

      BlockPos position = pos.immutable();
      effectApplier.runBefore(
         InsideBlockEffectType.EXTINGUISH,
         e -> {
            if (level instanceof ServerLevel serverLevel
               && e.isOnFire()
               && (serverLevel.getGameRules().get(GameRules.MOB_GRIEFING) || e instanceof Player)
               && e.mayInteract(serverLevel, position)) {
               level.destroyBlock(position, false);
            }
         }
      );
      effectApplier.apply(InsideBlockEffectType.FREEZE);
      effectApplier.apply(InsideBlockEffectType.EXTINGUISH);
   }

   @Override
   public void fallOn(final Level level, final BlockState state, final BlockPos pos, final Entity entity, final double fallDistance) {
      if (!(fallDistance < 4.0) && entity instanceof LivingEntity livingEntity) {
         LivingEntity.Fallsounds entityFallsounds = livingEntity.getFallSounds();
         SoundEvent fallSound = fallDistance < 7.0 ? entityFallsounds.small() : entityFallsounds.big();
         entity.playSound(fallSound, 1.0F, 1.0F);
      }
   }

   @Override
   protected VoxelShape getEntityInsideCollisionShape(final BlockState state, final BlockGetter level, final BlockPos pos, final Entity entity) {
      VoxelShape collisionShape = this.getCollisionShape(state, level, pos, CollisionContext.of(entity));
      return collisionShape.isEmpty() ? Shapes.block() : collisionShape;
   }

   @Override
   protected VoxelShape getCollisionShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context) {
      if (!context.isPlacement() && context instanceof EntityCollisionContext entityCollisionContext) {
         Entity entity = entityCollisionContext.getEntity();
         if (entity != null) {
            if (entity.fallDistance > 2.5) {
               return FALLING_COLLISION_SHAPE;
            }

            boolean isFallingBlock = entity instanceof FallingBlockEntity;
            if (isFallingBlock || canEntityWalkOnPowderSnow(entity) && context.isAbove(Shapes.block(), pos, false) && !context.isDescending()) {
               return super.getCollisionShape(state, level, pos, context);
            }
         }
      }

      return Shapes.empty();
   }

   @Override
   protected VoxelShape getVisualShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context) {
      return Shapes.empty();
   }

   public static boolean canEntityWalkOnPowderSnow(final Entity entity) {
      if (entity.is(EntityTypeTags.POWDER_SNOW_WALKABLE_MOBS)) {
         return true;
      } else {
         return entity instanceof LivingEntity livingEntity ? livingEntity.getItemBySlot(EquipmentSlot.FEET).is(Items.LEATHER_BOOTS) : false;
      }
   }

   @Override
   public ItemStack pickupBlock(final @Nullable LivingEntity user, final LevelAccessor level, final BlockPos pos, final BlockState state) {
      level.setBlock(pos, Blocks.AIR.defaultBlockState(), 11);
      if (!level.isClientSide()) {
         level.levelEvent(2001, pos, Block.getId(state));
      }

      return new ItemStack(Items.POWDER_SNOW_BUCKET);
   }

   @Override
   public Optional<SoundEvent> getPickupSound() {
      return Optional.of(SoundEvents.BUCKET_FILL_POWDER_SNOW);
   }

   @Override
   protected boolean isPathfindable(final BlockState state, final PathComputationType type) {
      return true;
   }
}
