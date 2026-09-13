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

   public EnderDragonPart(final EnderDragon parentMob, final String name, final float w, final float h) {
      super(parentMob.getType(), parentMob.level());
      this.size = EntityDimensions.scalable(w, h);
      this.refreshDimensions();
      this.parentMob = parentMob;
      this.name = name;
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
   public boolean isPickable() {
      return true;
   }

   @Override
   public @Nullable ItemStack getPickResult() {
      return this.parentMob.getPickResult();
   }

   @Override
   public final boolean hurtServer(final ServerLevel level, final DamageSource source, final float damage) {
      return this.isInvulnerableToBase(source) ? false : this.parentMob.hurt(level, this, source, damage);
   }

   @Override
   public boolean is(final Entity other) {
      return this == other || this.parentMob == other;
   }

   @Override
   public Packet<ClientGamePacketListener> getAddEntityPacket(final ServerEntity serverEntity) {
      throw new UnsupportedOperationException();
   }

   @Override
   public EntityDimensions getDimensions(final Pose pose) {
      return this.size;
   }

   @Override
   public boolean shouldBeSaved() {
      return false;
   }
}
