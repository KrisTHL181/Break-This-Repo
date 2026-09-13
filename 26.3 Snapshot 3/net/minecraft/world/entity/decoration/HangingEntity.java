package net.minecraft.world.entity.decoration;

import java.util.Objects;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DiodeBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.Validate;

public abstract class HangingEntity extends BlockAttachedEntity {
   private static final EntityDataAccessor<Direction> DATA_DIRECTION = SynchedEntityData.defineId(HangingEntity.class, EntityDataSerializers.DIRECTION);
   private static final Direction DEFAULT_DIRECTION = Direction.SOUTH;

   protected HangingEntity(final EntityType<? extends HangingEntity> type, final Level level) {
      super(type, level);
   }

   protected HangingEntity(final EntityType<? extends HangingEntity> type, final Level level, final BlockPos pos) {
      this(type, level);
      this.pos = pos;
   }

   @Override
   protected void defineSynchedData(final SynchedEntityData.Builder entityData) {
      entityData.define(DATA_DIRECTION, DEFAULT_DIRECTION);
   }

   @Override
   public void onSyncedDataUpdated(final EntityDataAccessor<?> accessor) {
      super.onSyncedDataUpdated(accessor);
      if (accessor.equals(DATA_DIRECTION)) {
         this.setDirection(this.getDirection());
      }
   }

   @Override
   public Direction getDirection() {
      return this.entityData.get(DATA_DIRECTION);
   }

   protected void setDirectionRaw(final Direction direction) {
      this.entityData.set(DATA_DIRECTION, direction);
   }

   protected void setDirection(final Direction direction) {
      Objects.requireNonNull(direction);
      Validate.isTrue(direction.getAxis().isHorizontal());
      this.setDirectionRaw(direction);
      this.setYRot(direction.get2DDataValue() * 90);
      this.yRotO = this.getYRot();
      this.recalculateBoundingBox();
   }

   @Override
   protected void recalculateBoundingBox() {
      if (this.getDirection() != null) {
         AABB aabb = this.calculateBoundingBox(this.pos, this.getDirection());
         Vec3 center = aabb.getCenter();
         this.setPosRaw(center.x, center.y, center.z);
         this.setBoundingBox(aabb);
      }
   }

   protected abstract AABB calculateBoundingBox(BlockPos pos, Direction direction);

   @Override
   public boolean survives() {
      if (this.hasLevelCollision(this.getPopBox())) {
         return false;
      }

      boolean isSupported = this.level().findBlocksIn(this.calculateSupportBox()).filterState(HangingEntity::isSupportingBlock).allMatched();
      return isSupported && this.canCoexist(false);
   }

   private static boolean isSupportingBlock(final BlockState state) {
      return state.isSolid() || DiodeBlock.isDiode(state);
   }

   protected AABB calculateSupportBox() {
      return this.getBoundingBox().move(this.getDirection().step().mul(-0.5F)).deflate(1.0E-7);
   }

   protected boolean canCoexist(final boolean allowIntersectingSameType) {
      Predicate<HangingEntity> nonIntersectable = hangingEntity -> {
         boolean intersectsSameType = !allowIntersectingSameType && hangingEntity.getType() == this.getType();
         boolean isSameDirection = hangingEntity.getDirection() == this.getDirection();
         return hangingEntity != this && (intersectsSameType || isSameDirection);
      };
      return !this.level().hasEntities(EntityTypeTest.forClass(HangingEntity.class), this.getPopBox(), nonIntersectable);
   }

   protected boolean hasLevelCollision(final AABB popBox) {
      Level level = this.level();
      return !level.noBlockCollision(this, popBox) || !level.noBorderCollision(this, popBox);
   }

   protected AABB getPopBox() {
      return this.getBoundingBox();
   }

   public abstract void playPlacementSound();

   @Override
   public ItemEntity spawnAtLocation(final ServerLevel level, final ItemStack itemStack, final float yOffs) {
      ItemEntity entity = new ItemEntity(
         this.level(),
         this.getX() + this.getDirection().getStepX() * 0.15F,
         this.getY() + yOffs,
         this.getZ() + this.getDirection().getStepZ() * 0.15F,
         itemStack
      );
      entity.setDefaultPickUpDelay();
      this.level().addFreshEntity(entity);
      return entity;
   }

   @Override
   public float rotate(final Rotation rotation) {
      Direction direction = this.getDirection();
      if (direction.getAxis() != Direction.Axis.Y) {
         switch (rotation) {
            case CLOCKWISE_180:
               direction = direction.getOpposite();
               break;
            case COUNTERCLOCKWISE_90:
               direction = direction.getCounterClockWise();
               break;
            case CLOCKWISE_90:
               direction = direction.getClockWise();
         }

         this.setDirection(direction);
      }

      float angle = Mth.wrapDegrees(this.getYRot());

      return switch (rotation) {
         case CLOCKWISE_180 -> angle + 180.0F;
         case COUNTERCLOCKWISE_90 -> angle + 90.0F;
         case CLOCKWISE_90 -> angle + 270.0F;
         default -> angle;
      };
   }

   @Override
   public float mirror(final Mirror mirror) {
      return this.rotate(mirror.getRotation(this.getDirection()));
   }
}
