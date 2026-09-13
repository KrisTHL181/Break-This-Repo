package net.minecraft.world.entity;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class Marker extends Entity {
   public Marker(final EntityType<?> type, final Level level) {
      super(type, level);
      this.noPhysics = true;
   }

   @Override
   public void tick() {
   }

   @Override
   protected void defineSynchedData(final SynchedEntityData.Builder entityData) {
   }

   @Override
   protected void readAdditionalSaveData(final ValueInput input) {
   }

   @Override
   protected void addAdditionalSaveData(final ValueOutput output) {
   }

   @Override
   public Packet<ClientGamePacketListener> getAddEntityPacket(final ServerEntity serverEntity) {
      throw new IllegalStateException("Markers should never be sent");
   }

   @Override
   protected boolean canAddPassenger(final Entity passenger) {
      return false;
   }

   @Override
   protected boolean couldAcceptPassenger() {
      return false;
   }

   @Override
   protected void addPassenger(final Entity passenger) {
      throw new IllegalStateException("Should never addPassenger without checking couldAcceptPassenger()");
   }

   @Override
   public PushReaction getPistonPushReaction() {
      return PushReaction.IGNORE;
   }

   @Override
   public boolean isIgnoringBlockTriggers() {
      return true;
   }

   @Override
   public final boolean hurtServer(final ServerLevel level, final DamageSource source, final float damage) {
      return false;
   }
}
