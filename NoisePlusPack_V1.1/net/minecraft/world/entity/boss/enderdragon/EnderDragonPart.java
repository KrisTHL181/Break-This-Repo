package net.minecraft.world.entity.boss.enderdragon;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class EnderDragonPart extends Entity {
   public final EnderDragon parentMob;
   public final String name;
   private final EntityDimensions size;

   public EnderDragonPart(EnderDragon p_450227_, String p_453842_, float p_460816_, float p_454700_) {
      super(p_450227_.getType(), p_450227_.level());
      this.size = EntityDimensions.scalable(p_460816_, p_454700_);
      this.refreshDimensions();
      this.parentMob = p_450227_;
      this.name = p_453842_;
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder p_457219_) {
   }

   @Override
   protected void readAdditionalSaveData(ValueInput p_458531_) {
   }

   @Override
   protected void addAdditionalSaveData(ValueOutput p_457885_) {
   }

   @Override
   public boolean isPickable() {
      return true;
   }

   @Override
   public @Nullable ItemStack getPickResult() {
      return this.parentMob.getPickResult();
   }

   @Override
   public final boolean hurtServer(ServerLevel p_456432_, DamageSource p_459379_, float p_457650_) {
      return this.isInvulnerableToBase(p_459379_) ? false : this.parentMob.hurt(p_456432_, this, p_459379_, p_457650_);
   }

   @Override
   public boolean is(Entity p_459764_) {
      return this == p_459764_ || this.parentMob == p_459764_;
   }

   @Override
   public Packet<ClientGamePacketListener> getAddEntityPacket(ServerEntity p_455905_) {
      throw new UnsupportedOperationException();
   }

   @Override
   public EntityDimensions getDimensions(Pose p_452890_) {
      return this.size;
   }

   @Override
   public boolean shouldBeSaved() {
      return false;
   }
}
