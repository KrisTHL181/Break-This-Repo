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
import net.minecraft.network.protocol.game.VecDeltaCodec;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.decoration.ItemFrame;
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

   public ServerEntity(ServerLevel p_8528_, Entity p_8529_, int p_8530_, boolean p_8531_, ServerEntity.Synchronizer p_429094_) {
      this.level = p_8528_;
      this.synchronizer = p_429094_;
      this.entity = p_8529_;
      this.updateInterval = p_8530_;
      this.trackDelta = p_8531_;
      this.positionCodec.setBase(p_8529_.trackingPosition());
      this.lastSentMovement = p_8529_.getDeltaMovement();
      this.lastSentYRot = Mth.packDegrees(p_8529_.getYRot());
      this.lastSentXRot = Mth.packDegrees(p_8529_.getXRot());
      this.lastSentYHeadRot = Mth.packDegrees(p_8529_.getYHeadRot());
      this.wasOnGround = p_8529_.onGround();
      this.trackedDataValues = p_8529_.getEntityData().getNonDefaultValues();
   }

   public void sendChanges() {
      this.entity.updateDataBeforeSync();
      List<Entity> list = this.entity.getPassengers();
      if (!list.equals(this.lastPassengers)) {
         this.synchronizer
            .sendToTrackingPlayersFiltered(
               new ClientboundSetPassengersPacket(this.entity), p_421458_ -> list.contains(p_421458_) == this.lastPassengers.contains(p_421458_)
            );
         this.lastPassengers = list;
      }

      if (this.entity instanceof ItemFrame itemframe && this.tickCount % 10 == 0) {
         ItemStack itemstack = itemframe.getItem();
         if (itemstack.getItem() instanceof MapItem) {
            MapId mapid = itemstack.get(DataComponents.MAP_ID);
            MapItemSavedData mapitemsaveddata = MapItem.getSavedData(mapid, this.level);
            if (mapitemsaveddata != null) {
               for (ServerPlayer serverplayer : this.level.players()) {
                  mapitemsaveddata.tickCarriedBy(serverplayer, itemstack);
                  Packet<?> packet = mapitemsaveddata.getUpdatePacket(mapid, serverplayer);
                  if (packet != null) {
                     serverplayer.connection.send(packet);
                  }
               }
            }
         }

         this.sendDirtyEntityData();
      }

      if (this.tickCount % this.updateInterval == 0 || this.entity.needsSync || this.entity.getEntityData().isDirty()) {
         byte b0 = Mth.packDegrees(this.entity.getYRot());
         byte b1 = Mth.packDegrees(this.entity.getXRot());
         boolean flag4 = Math.abs(b0 - this.lastSentYRot) >= 1 || Math.abs(b1 - this.lastSentXRot) >= 1;
         if (this.entity.isPassenger()) {
            if (flag4) {
               this.synchronizer.sendToTrackingPlayers(new ClientboundMoveEntityPacket.Rot(this.entity.getId(), b0, b1, this.entity.onGround()));
               this.lastSentYRot = b0;
               this.lastSentXRot = b1;
            }

            this.positionCodec.setBase(this.entity.trackingPosition());
            this.sendDirtyEntityData();
            this.wasRiding = true;
         } else if (this.entity instanceof AbstractMinecart abstractminecart
            && abstractminecart.getBehavior() instanceof NewMinecartBehavior newminecartbehavior) {
            this.handleMinecartPosRot(newminecartbehavior, b0, b1, flag4);
         } else {
            this.teleportDelay++;
            Vec3 vec31 = this.entity.trackingPosition();
            boolean flag5 = this.positionCodec.delta(vec31).lengthSqr() >= 7.6293945E-6F;
            Packet<ClientGamePacketListener> packet1 = null;
            boolean flag = flag5 || this.tickCount % 60 == 0;
            boolean flag1 = false;
            boolean flag2 = false;
            long i = this.positionCodec.encodeX(vec31);
            long j = this.positionCodec.encodeY(vec31);
            long k = this.positionCodec.encodeZ(vec31);
            boolean flag3 = i < -32768L || i > 32767L || j < -32768L || j > 32767L || k < -32768L || k > 32767L;
            if (this.entity.getRequiresPrecisePosition() || flag3 || this.teleportDelay > 400 || this.wasRiding || this.wasOnGround != this.entity.onGround()) {
               this.wasOnGround = this.entity.onGround();
               this.teleportDelay = 0;
               packet1 = ClientboundEntityPositionSyncPacket.of(this.entity);
               flag1 = true;
               flag2 = true;
            } else if ((!flag || !flag4) && !(this.entity instanceof AbstractArrow)) {
               if (flag) {
                  packet1 = new ClientboundMoveEntityPacket.Pos(this.entity.getId(), (short)i, (short)j, (short)k, this.entity.onGround());
                  flag1 = true;
               } else if (flag4) {
                  packet1 = new ClientboundMoveEntityPacket.Rot(this.entity.getId(), b0, b1, this.entity.onGround());
                  flag2 = true;
               }
            } else {
               packet1 = new ClientboundMoveEntityPacket.PosRot(this.entity.getId(), (short)i, (short)j, (short)k, b0, b1, this.entity.onGround());
               flag1 = true;
               flag2 = true;
            }

            if (this.entity.needsSync || this.trackDelta || this.entity instanceof LivingEntity && ((LivingEntity)this.entity).isFallFlying()) {
               Vec3 vec3 = this.entity.getDeltaMovement();
               double d0 = vec3.distanceToSqr(this.lastSentMovement);
               if (d0 > 1.0E-7 || d0 > 0.0 && vec3.lengthSqr() == 0.0) {
                  this.lastSentMovement = vec3;
                  if (this.entity instanceof AbstractHurtingProjectile abstracthurtingprojectile) {
                     this.synchronizer
                        .sendToTrackingPlayers(
                           new ClientboundBundlePacket(
                              List.of(
                                 new ClientboundSetEntityMotionPacket(this.entity.getId(), this.lastSentMovement),
                                 new ClientboundProjectilePowerPacket(abstracthurtingprojectile.getId(), abstracthurtingprojectile.accelerationPower)
                              )
                           )
                        );
                  } else {
                     this.synchronizer.sendToTrackingPlayers(new ClientboundSetEntityMotionPacket(this.entity.getId(), this.lastSentMovement));
                  }
               }
            }

            if (packet1 != null) {
               this.synchronizer.sendToTrackingPlayers(packet1);
            }

            this.sendDirtyEntityData();
            if (flag1) {
               this.positionCodec.setBase(vec31);
            }

            if (flag2) {
               this.lastSentYRot = b0;
               this.lastSentXRot = b1;
            }

            this.wasRiding = false;
         }

         byte b2 = Mth.packDegrees(this.entity.getYHeadRot());
         if (Math.abs(b2 - this.lastSentYHeadRot) >= 1) {
            this.synchronizer.sendToTrackingPlayers(new ClientboundRotateHeadPacket(this.entity, b2));
            this.lastSentYHeadRot = b2;
         }

         this.entity.needsSync = false;
      }

      this.tickCount++;
      if (this.entity.hurtMarked) {
         this.entity.hurtMarked = false;
         this.synchronizer.sendToTrackingPlayersAndSelf(new ClientboundSetEntityMotionPacket(this.entity));
      }
   }

   private void handleMinecartPosRot(NewMinecartBehavior p_451605_, byte p_363280_, byte p_367403_, boolean p_369265_) {
      this.sendDirtyEntityData();
      if (p_451605_.lerpSteps.isEmpty()) {
         Vec3 vec3 = this.entity.getDeltaMovement();
         double d0 = vec3.distanceToSqr(this.lastSentMovement);
         Vec3 vec31 = this.entity.trackingPosition();
         boolean flag = this.positionCodec.delta(vec31).lengthSqr() >= 7.6293945E-6F;
         boolean flag1 = flag || this.tickCount % 60 == 0;
         if (flag1 || p_369265_ || d0 > 1.0E-7) {
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
         this.synchronizer.sendToTrackingPlayers(new ClientboundMoveMinecartPacket(this.entity.getId(), List.copyOf(p_451605_.lerpSteps)));
         p_451605_.lerpSteps.clear();
      }

      this.lastSentYRot = p_363280_;
      this.lastSentXRot = p_367403_;
      this.positionCodec.setBase(this.entity.position());
   }

   public void removePairing(ServerPlayer p_8535_) {
      this.entity.stopSeenByPlayer(p_8535_);
      p_8535_.connection.send(new ClientboundRemoveEntitiesPacket(this.entity.getId()));
   }

   public void addPairing(ServerPlayer p_8542_) {
      List<Packet<? super ClientGamePacketListener>> list = new ArrayList<>();
      this.sendPairingData(p_8542_, list::add);
      p_8542_.connection.send(new ClientboundBundlePacket(list));
      this.entity.startSeenByPlayer(p_8542_);
   }

   public void sendPairingData(ServerPlayer p_289562_, Consumer<Packet<ClientGamePacketListener>> p_289563_) {
      this.entity.updateDataBeforeSync();
      if (this.entity.isRemoved()) {
         LOGGER.warn("Fetching packet for removed entity {}", this.entity);
      }

      Packet<ClientGamePacketListener> packet = this.entity.getAddEntityPacket(this);
      p_289563_.accept(packet);
      if (this.trackedDataValues != null) {
         p_289563_.accept(new ClientboundSetEntityDataPacket(this.entity.getId(), this.trackedDataValues));
      }

      if (this.entity instanceof LivingEntity livingentity) {
         Collection<AttributeInstance> collection = livingentity.getAttributes().getSyncableAttributes();
         if (!collection.isEmpty()) {
            p_289563_.accept(new ClientboundUpdateAttributesPacket(this.entity.getId(), collection));
         }
      }

      if (this.entity instanceof LivingEntity livingentity1) {
         List<Pair<EquipmentSlot, ItemStack>> list = Lists.newArrayList();

         for (EquipmentSlot equipmentslot : EquipmentSlot.VALUES) {
            ItemStack itemstack = livingentity1.getItemBySlot(equipmentslot);
            if (!itemstack.isEmpty()) {
               list.add(Pair.of(equipmentslot, itemstack.copy()));
            }
         }

         if (!list.isEmpty()) {
            p_289563_.accept(new ClientboundSetEquipmentPacket(this.entity.getId(), list));
         }
      }

      if (!this.entity.getPassengers().isEmpty()) {
         p_289563_.accept(new ClientboundSetPassengersPacket(this.entity));
      }

      if (this.entity.isPassenger()) {
         p_289563_.accept(new ClientboundSetPassengersPacket(this.entity.getVehicle()));
      }

      if (this.entity instanceof Leashable leashable && leashable.isLeashed()) {
         p_289563_.accept(new ClientboundSetEntityLinkPacket(this.entity, leashable.getLeashHolder()));
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
      SynchedEntityData synchedentitydata = this.entity.getEntityData();
      List<SynchedEntityData.DataValue<?>> list = synchedentitydata.packDirty();
      if (list != null) {
         this.trackedDataValues = synchedentitydata.getNonDefaultValues();
         this.synchronizer.sendToTrackingPlayersAndSelf(new ClientboundSetEntityDataPacket(this.entity.getId(), list));
      }

      if (this.entity instanceof LivingEntity) {
         Set<AttributeInstance> set = ((LivingEntity)this.entity).getAttributes().getAttributesToSync();
         if (!set.isEmpty()) {
            this.synchronizer.sendToTrackingPlayersAndSelf(new ClientboundUpdateAttributesPacket(this.entity.getId(), set));
         }

         set.clear();
      }
   }

   public interface Synchronizer {
      void sendToTrackingPlayers(Packet<? super ClientGamePacketListener> var1);

      void sendToTrackingPlayersAndSelf(Packet<? super ClientGamePacketListener> var1);

      void sendToTrackingPlayersFiltered(Packet<? super ClientGamePacketListener> var1, Predicate<ServerPlayer> var2);
   }
}
