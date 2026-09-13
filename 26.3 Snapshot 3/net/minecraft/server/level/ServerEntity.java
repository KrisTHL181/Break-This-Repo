package net.minecraft.server.level;

import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundEntityPositionSyncPacket;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.network.protocol.game.ClientboundMoveMinecartPacket;
import net.minecraft.network.protocol.game.ClientboundProjectilePowerPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundRotateHeadPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityLinkPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket;
import net.minecraft.network.protocol.game.MovementPacket;
import net.minecraft.network.protocol.game.VecDelta;
import net.minecraft.network.protocol.game.VecDeltaCodec;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.entity.InterpolationTracker;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PositionPath;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.hurtingprojectile.AbstractHurtingProjectile;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.entity.vehicle.minecart.NewMinecartBehavior;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class ServerEntity {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int TOLERANCE_LEVEL_ROTATION = 1;
   private static final double TOLERANCE_LEVEL_POSITION = 7.6293945E-6F;
   public static final int FORCED_POS_UPDATE_PERIOD = 60;
   private static final int FORCED_TELEPORT_PERIOD = 400;
   private final ServerLevel level;
   private final Entity entity;
   private final int updateInterval;
   private final boolean trackDelta;
   private final ServerEntity.Synchronizer synchronizer;
   private final VecDeltaCodec positionCodec = new VecDeltaCodec();
   private byte lastSentYRot;
   private byte lastSentXRot;
   private byte lastSentYHeadRot;
   private Vec3 lastSentMovement;
   private int tickCount;
   private int teleportDelay;
   private List<Entity> lastPassengers = Collections.emptyList();
   private boolean wasRiding;
   private boolean wasOnGround;
   private @Nullable List<SynchedEntityData.DataValue<?>> trackedDataValues;

   public ServerEntity(
      final ServerLevel level, final Entity entity, final int updateInterval, final boolean trackDelta, final ServerEntity.Synchronizer synchronizer
   ) {
      this.level = level;
      this.synchronizer = synchronizer;
      this.entity = entity;
      this.updateInterval = updateInterval;
      this.trackDelta = trackDelta;
      this.positionCodec.setBase(entity.trackingPosition());
      this.lastSentMovement = entity.getDeltaMovement();
      this.lastSentYRot = Mth.packDegrees(entity.getYRot());
      this.lastSentXRot = Mth.packDegrees(entity.getXRot());
      this.lastSentYHeadRot = Mth.packDegrees(entity.getYHeadRot());
      this.wasOnGround = entity.onGround();
      this.trackedDataValues = entity.getEntityData().getNonDefaultValues();
   }

   public void sendChanges() {
      this.entity.updateDataBeforeSync();
      List<Entity> passengers = this.entity.getPassengers();
      if (!passengers.equals(this.lastPassengers)) {
         this.synchronizer
            .sendToTrackingPlayersFiltered(
               new ClientboundSetPassengersPacket(this.entity), player -> passengers.contains(player) == this.lastPassengers.contains(player)
            );
         this.lastPassengers = passengers;
      }

      if (this.entity instanceof ItemFrame frame && this.tickCount % 10 == 0) {
         ItemStack itemStack = frame.getItem();
         if (itemStack.getItem() instanceof MapItem) {
            MapId id = itemStack.get(DataComponents.MAP_ID);
            MapItemSavedData data = MapItem.getSavedData(id, this.level);
            if (data != null) {
               for (ServerPlayer player : this.level.players()) {
                  data.tickCarriedBy(player, itemStack, frame);
                  Packet<?> packet = data.getUpdatePacket(id, player);
                  if (packet != null) {
                     player.connection.send(packet);
                  }
               }
            }
         }

         this.sendDirtyEntityData();
      }

      InterpolationHandler interpolation = this.entity.getInterpolation();
      InterpolationTracker interpolationTracker = interpolation != null ? interpolation.interpolationTracker() : null;
      Vec3 currentPosition = this.entity.trackingPosition();
      if (interpolationTracker != null) {
         interpolationTracker.updateTracking(currentPosition);
      }

      if (this.entity.syncPosition) {
         this.tickCount = this.tickCount / this.updateInterval * this.updateInterval + this.updateInterval;
         this.entity.syncPosition = false;
      }

      if (this.tickCount % this.updateInterval == 0 || this.entity.needsSync || this.entity.getEntityData().isDirty()) {
         byte yRotn = Mth.packDegrees(this.entity.getYRot());
         byte xRotn = Mth.packDegrees(this.entity.getXRot());
         boolean shouldSendRotation = Math.abs(yRotn - this.lastSentYRot) >= 1 || Math.abs(xRotn - this.lastSentXRot) >= 1;
         if (this.entity.isPassenger()) {
            if (shouldSendRotation) {
               this.synchronizer.sendToTrackingPlayers(new ClientboundMoveEntityPacket.Rot(this.entity.getId(), yRotn, xRotn, this.entity.onGround()));
               this.lastSentYRot = yRotn;
               this.lastSentXRot = xRotn;
            }

            if (interpolationTracker != null) {
               interpolationTracker.clear();
            }

            this.positionCodec.setBase(currentPosition);
            this.sendDirtyEntityData();
            this.wasRiding = true;
         } else if (this.entity instanceof AbstractMinecart minecart && minecart.getBehavior() instanceof NewMinecartBehavior newMinecartBehavior) {
            this.handleMinecartPosRot(newMinecartBehavior, yRotn, xRotn, shouldSendRotation);
         } else {
            this.teleportDelay++;
            PositionPath position;
            if (interpolationTracker != null) {
               position = interpolationTracker.getPositionPath(currentPosition);
               interpolationTracker.clear();
            } else {
               position = PositionPath.of(currentPosition);
            }

            MovementPacket<ClientGamePacketListener> packet;
            if (this.entity instanceof AbstractArrow) {
               packet = this.createMovePacket(position, yRotn, xRotn, true, true);
            } else {
               boolean positionChanged = this.positionCodec.delta(currentPosition).lengthSqr() >= 7.6293945E-6F;
               boolean shouldSendPosition = positionChanged || this.tickCount % 60 == 0;
               packet = this.createMovePacket(position, yRotn, xRotn, shouldSendPosition, shouldSendRotation);
            }

            if (this.entity.needsSync || this.trackDelta || this.entity instanceof LivingEntity livingEntity && livingEntity.isFallFlying()) {
               Vec3 movement = this.entity.getDeltaMovement();
               double diff = movement.distanceToSqr(this.lastSentMovement);
               if (diff > 1.0E-7 || diff > 0.0 && movement.lengthSqr() == 0.0) {
                  this.lastSentMovement = movement;
                  if (this.entity instanceof AbstractHurtingProjectile projectile) {
                     this.synchronizer
                        .sendToTrackingPlayers(
                           new ClientboundBundlePacket(
                              List.of(
                                 new ClientboundSetEntityMotionPacket(this.entity.getId(), this.lastSentMovement),
                                 new ClientboundProjectilePowerPacket(projectile.getId(), projectile.accelerationPower)
                              )
                           )
                        );
                  } else {
                     this.synchronizer.sendToTrackingPlayers(new ClientboundSetEntityMotionPacket(this.entity.getId(), this.lastSentMovement));
                  }
               }
            }

            if (packet != null) {
               this.synchronizer.sendToTrackingPlayers(packet);
               this.sendDirtyEntityData();
               if (packet.hasPosition()) {
                  this.positionCodec.setBase(currentPosition);
               }

               if (packet.hasRotation()) {
                  this.lastSentYRot = yRotn;
                  this.lastSentXRot = xRotn;
               }
            } else {
               this.sendDirtyEntityData();
            }

            this.wasRiding = false;
         }

         byte yHeadRot = Mth.packDegrees(this.entity.getYHeadRot());
         if (Math.abs(yHeadRot - this.lastSentYHeadRot) >= 1) {
            this.synchronizer.sendToTrackingPlayers(new ClientboundRotateHeadPacket(this.entity, yHeadRot));
            this.lastSentYHeadRot = yHeadRot;
         }

         this.entity.needsSync = false;
      }

      this.tickCount++;
      if (this.entity.syncVelocity) {
         this.entity.syncVelocity = false;
         this.synchronizer.sendToTrackingPlayersAndSelf(new ClientboundSetEntityMotionPacket(this.entity));
      }
   }

   private @Nullable MovementPacket<ClientGamePacketListener> createMovePacket(
      final PositionPath position, final byte yRotn, final byte xRotn, final boolean shouldSendPosition, final boolean shouldSendRotation
   ) {
      if (this.entity.getRequiresPrecisePosition() || this.teleportDelay > 400 || this.wasRiding || this.wasOnGround != this.entity.onGround()) {
         this.wasOnGround = this.entity.onGround();
         this.teleportDelay = 0;
         return ClientboundEntityPositionSyncPacket.of(this.entity, position);
      }

      if (shouldSendPosition) {
         VecDelta encodedDelta = this.positionCodec.tryEncode(position);
         if (encodedDelta == null || this.isFullPrecisionEncodingRequired(position.endPosition(), encodedDelta)) {
            return ClientboundEntityPositionSyncPacket.of(this.entity, position);
         } else {
            return shouldSendRotation
               ? new ClientboundMoveEntityPacket.PosRot(this.entity.getId(), encodedDelta, yRotn, xRotn, this.entity.onGround())
               : new ClientboundMoveEntityPacket.Pos(this.entity.getId(), encodedDelta, this.entity.onGround());
         }
      } else {
         return shouldSendRotation ? new ClientboundMoveEntityPacket.Rot(this.entity.getId(), yRotn, xRotn, this.entity.onGround()) : null;
      }
   }

   private boolean isFullPrecisionEncodingRequired(final Vec3 pos, final VecDelta delta) {
      if (this.entity instanceof ItemEntity) {
         if (this.entity.verticalCollision
            && (
               delta.hasDeltaX() && VecDeltaCodec.encodingPrecisionLoss(pos.x) != 0.0 || delta.hasDeltaZ() && VecDeltaCodec.encodingPrecisionLoss(pos.z) != 0.0
            )) {
            return true;
         }

         if (this.entity.horizontalCollision
            && (
               delta.hasDeltaX() && VecDeltaCodec.encodingPrecisionLoss(pos.z) != 0.0 || delta.hasDeltaZ() && VecDeltaCodec.encodingPrecisionLoss(pos.x) != 0.0
            )) {
            return true;
         }
      }

      return false;
   }

   private void handleMinecartPosRot(final NewMinecartBehavior newMinecartBehavior, final byte yRotn, final byte xRotn, final boolean shouldSendRotation) {
      this.sendDirtyEntityData();
      if (newMinecartBehavior.lerpSteps.isEmpty()) {
         Vec3 movement = this.entity.getDeltaMovement();
         double diff = movement.distanceToSqr(this.lastSentMovement);
         Vec3 currentPosition = this.entity.trackingPosition();
         boolean positionChanged = this.positionCodec.delta(currentPosition).lengthSqr() >= 7.6293945E-6F;
         boolean shouldSendPosition = positionChanged || this.tickCount % 60 == 0;
         if (shouldSendPosition || shouldSendRotation || diff > 1.0E-7) {
            this.synchronizer
               .sendToTrackingPlayers(
                  new ClientboundMoveMinecartPacket(
                     this.entity.getId(),
                     List.of(
                        new NewMinecartBehavior.MinecartStep(
                           this.entity.position(), this.entity.getDeltaMovement(), this.entity.getYRot(), this.entity.getXRot(), 1.0F
                        )
                     )
                  )
               );
         }
      } else {
         this.synchronizer.sendToTrackingPlayers(new ClientboundMoveMinecartPacket(this.entity.getId(), List.copyOf(newMinecartBehavior.lerpSteps)));
         newMinecartBehavior.lerpSteps.clear();
      }

      this.lastSentYRot = yRotn;
      this.lastSentXRot = xRotn;
      this.positionCodec.setBase(this.entity.position());
   }

   public void removePairing(final ServerPlayer player) {
      this.entity.stopSeenByPlayer(player);
      player.connection.send(new ClientboundRemoveEntitiesPacket(this.entity.getId()));
   }

   public void addPairing(final ServerPlayer player) {
      List<Packet<? super ClientGamePacketListener>> packets = new ArrayList<>();
      this.sendPairingData(player, packets::add);
      player.connection.send(new ClientboundBundlePacket(packets));
      this.entity.startSeenByPlayer(player);
   }

   public void sendPairingData(final ServerPlayer player, final Consumer<Packet<ClientGamePacketListener>> broadcast) {
      this.entity.updateDataBeforeSync();
      if (this.entity.isRemoved()) {
         LOGGER.warn("Fetching packet for removed entity {}", this.entity);
      }

      Packet<ClientGamePacketListener> packet = this.entity.getAddEntityPacket(this);
      broadcast.accept(packet);
      if (this.trackedDataValues != null) {
         broadcast.accept(new ClientboundSetEntityDataPacket(this.entity.getId(), this.trackedDataValues));
      }

      if (this.entity instanceof LivingEntity livingEntity) {
         Collection<AttributeInstance> attributes = livingEntity.getAttributes().getSyncableAttributes();
         if (!attributes.isEmpty()) {
            broadcast.accept(new ClientboundUpdateAttributesPacket(this.entity.getId(), attributes));
         }
      }

      if (this.entity instanceof LivingEntity livingEntity) {
         List<Pair<EquipmentSlot, ItemStack>> slots = Lists.newArrayList();

         for (EquipmentSlot slot : EquipmentSlot.VALUES) {
            ItemStack itemStack = livingEntity.getItemBySlot(slot);
            if (!itemStack.isEmpty()) {
               slots.add(Pair.of(slot, itemStack.copy()));
            }
         }

         if (!slots.isEmpty()) {
            broadcast.accept(new ClientboundSetEquipmentPacket(this.entity.getId(), slots));
         }
      }

      if (!this.entity.getPassengers().isEmpty()) {
         broadcast.accept(new ClientboundSetPassengersPacket(this.entity));
      }

      if (this.entity.isPassenger()) {
         broadcast.accept(new ClientboundSetPassengersPacket(this.entity.getVehicle()));
      }

      if (this.entity instanceof Leashable leashable && leashable.isLeashed()) {
         broadcast.accept(new ClientboundSetEntityLinkPacket(this.entity, leashable.getLeashHolder()));
      }
   }

   public Vec3 getPositionBase() {
      return this.positionCodec.getBase();
   }

   public Vec3 getLastSentMovement() {
      return this.lastSentMovement;
   }

   public float getLastSentXRot() {
      return Mth.unpackDegrees(this.lastSentXRot);
   }

   public float getLastSentYRot() {
      return Mth.unpackDegrees(this.lastSentYRot);
   }

   public float getLastSentYHeadRot() {
      return Mth.unpackDegrees(this.lastSentYHeadRot);
   }

   private void sendDirtyEntityData() {
      SynchedEntityData entityData = this.entity.getEntityData();
      List<SynchedEntityData.DataValue<?>> packedValues = entityData.packDirty();
      if (packedValues != null) {
         this.trackedDataValues = entityData.getNonDefaultValues();
         this.synchronizer.sendToTrackingPlayersAndSelf(new ClientboundSetEntityDataPacket(this.entity.getId(), packedValues));
      }

      if (this.entity instanceof LivingEntity livingEntity) {
         Set<AttributeInstance> attributes = livingEntity.getAttributes().getAttributesToSync();
         if (!attributes.isEmpty()) {
            this.synchronizer.sendToTrackingPlayersAndSelf(new ClientboundUpdateAttributesPacket(this.entity.getId(), attributes));
         }

         attributes.clear();
      }
   }

   public interface Synchronizer {
      void sendToTrackingPlayers(Packet<? super ClientGamePacketListener> packet);

      void sendToTrackingPlayersAndSelf(Packet<? super ClientGamePacketListener> packet);

      void sendToTrackingPlayersFiltered(Packet<? super ClientGamePacketListener> packet, Predicate<ServerPlayer> predicate);
   }
}
