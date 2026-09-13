package net.minecraft.world.entity.decoration;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class LeashFenceKnotEntity extends BlockAttachedEntity {
   public static final double OFFSET_Y = 0.375;

   public LeashFenceKnotEntity(final EntityType<? extends LeashFenceKnotEntity> type, final Level level) {
      super(type, level);
   }

   public LeashFenceKnotEntity(final Level level, final BlockPos pos) {
      super(EntityTypes.LEASH_KNOT, level, pos);
      this.setPos(pos.getX(), pos.getY(), pos.getZ());
   }

   @Override
   protected void defineSynchedData(final SynchedEntityData.Builder entityData) {
   }

   @Override
   protected void recalculateBoundingBox() {
      this.setPosRaw(this.pos.getX() + 0.5, this.pos.getY() + 0.375, this.pos.getZ() + 0.5);
      double halfWidth = this.getType().getWidth() / 2.0;
      double height = this.getType().getHeight();
      this.setBoundingBox(
         new AABB(this.getX() - halfWidth, this.getY(), this.getZ() - halfWidth, this.getX() + halfWidth, this.getY() + height, this.getZ() + halfWidth)
      );
   }

   @Override
   public boolean shouldRenderAtSqrDistance(final double distance) {
      return distance < 1024.0;
   }

   @Override
   public void dropItem(final ServerLevel level, final @Nullable Entity causedBy) {
      this.playSound(SoundEvents.LEAD_UNTIED, 1.0F, 1.0F);
   }

   @Override
   protected void addAdditionalSaveData(final ValueOutput output) {
   }

   @Override
   protected void readAdditionalSaveData(final ValueInput input) {
   }

   @Override
   public InteractionResult interact(final Player player, final InteractionHand hand, final Vec3 location) {
      if (this.level().isClientSide()) {
         return InteractionResult.SUCCESS;
      }

      if (player.getItemInHand(hand).is(Items.SHEARS)) {
         InteractionResult result = super.interact(player, hand, location);
         if (result instanceof InteractionResult.Success success && success.wasItemInteraction()) {
            return result;
         }
      }

      boolean attachedMob = false;

      for (Leashable leashable : Leashable.leashableLeashedTo(player)) {
         if (leashable.canHaveALeashAttachedTo(this)) {
            leashable.setLeashedTo(this, true);
            attachedMob = true;
         }
      }

      boolean anyDropped = false;
      if (!attachedMob && !player.isSecondaryUseActive()) {
         for (Leashable mob : Leashable.leashableLeashedTo(this)) {
            if (mob.canHaveALeashAttachedTo(player)) {
               mob.setLeashedTo(player, true);
               anyDropped = true;
            }
         }
      }

      if (!attachedMob && !anyDropped) {
         return super.interact(player, hand, location);
      }

      this.gameEvent(GameEvent.BLOCK_ATTACH, player);
      this.playSound(SoundEvents.LEAD_TIED);
      return InteractionResult.SUCCESS;
   }

   @Override
   public void notifyLeasheeRemoved(final Leashable entity) {
      if (Leashable.leashableLeashedTo(this).isEmpty()) {
         this.discard();
      }
   }

   @Override
   public boolean survives() {
      return this.level().getBlockState(this.pos).is(BlockTags.FENCES);
   }

   public static LeashFenceKnotEntity getOrCreateKnot(final Level level, final BlockPos pos) {
      return getKnot(level, pos).orElseGet(() -> createKnot(level, pos));
   }

   public static Optional<LeashFenceKnotEntity> getKnot(final Level level, final BlockPos pos) {
      int x = pos.getX();
      int y = pos.getY();
      int z = pos.getZ();

      for (LeashFenceKnotEntity knot : level.getEntitiesOfClass(LeashFenceKnotEntity.class, new AABB(x - 1.0, y - 1.0, z - 1.0, x + 1.0, y + 1.0, z + 1.0))) {
         if (knot.getPos().equals(pos)) {
            return Optional.of(knot);
         }
      }

      return Optional.empty();
   }

   public static LeashFenceKnotEntity createKnot(final Level level, final BlockPos pos) {
      LeashFenceKnotEntity knot = new LeashFenceKnotEntity(level, pos);
      level.addFreshEntity(knot);
      return knot;
   }

   public void playPlacementSound() {
      this.playSound(SoundEvents.LEAD_TIED, 1.0F, 1.0F);
   }

   @Override
   public Packet<ClientGamePacketListener> getAddEntityPacket(final ServerEntity serverEntity) {
      return new ClientboundAddEntityPacket(this, 0, this.getPos());
   }

   @Override
   public Vec3 getRopeHoldPosition(final float partialTickTime) {
      return this.getPosition(partialTickTime).add(0.0, 0.2, 0.0);
   }

   @Override
   public ItemStack getPickResult() {
      return new ItemStack(Items.LEAD);
   }
}
