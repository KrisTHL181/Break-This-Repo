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
   public Marker(EntityType<?> p_147250_, Level p_147251_) {
      super(p_147250_, p_147251_);
      this.noPhysics = true;
   }

   @Override
   public void tick() {
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder p_334647_) {
   }

   @Override
   protected void readAdditionalSaveData(ValueInput p_406678_) {
   }

   @Override
   protected void addAdditionalSaveData(ValueOutput p_407620_) {
   }

   @Override
   public Packet<ClientGamePacketListener> getAddEntityPacket(ServerEntity p_345137_) {
      throw new IllegalStateException("Markers should never be sent");
   }

   @Override
   protected boolean canAddPassenger(Entity p_265289_) {
      return false;
   }

   @Override
   protected boolean couldAcceptPassenger() {
      return false;
   }

   @Override
   protected void addPassenger(Entity p_270306_) {
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
   public final boolean hurtServer(ServerLevel p_366204_, DamageSource p_366965_, float p_363917_) {
      return false;
   }
}
