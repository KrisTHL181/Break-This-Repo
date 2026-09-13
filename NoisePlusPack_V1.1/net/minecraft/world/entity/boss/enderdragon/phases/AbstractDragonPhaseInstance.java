package net.minecraft.world.entity.boss.enderdragon.phases;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public abstract class AbstractDragonPhaseInstance implements DragonPhaseInstance {
   protected final EnderDragon dragon;

   public AbstractDragonPhaseInstance(EnderDragon p_31178_) {
      this.dragon = p_31178_;
   }

   @Override
   public boolean isSitting() {
      return false;
   }

   @Override
   public void doClientTick() {
   }

   @Override
   public void doServerTick(ServerLevel p_366147_) {
   }

   @Override
   public void onCrystalDestroyed(EndCrystal p_31184_, BlockPos p_31185_, DamageSource p_31186_, @Nullable Player p_31187_) {
   }

   @Override
   public void begin() {
   }

   @Override
   public void end() {
   }

   @Override
   public float getFlySpeed() {
      return 0.6F;
   }

   @Override
   public @Nullable Vec3 getFlyTargetLocation() {
      return null;
   }

   @Override
   public float onHurt(DamageSource p_31181_, float p_31182_) {
      return p_31182_;
   }

   @Override
   public float getTurnSpeed() {
      float f = (float)this.dragon.getDeltaMovement().horizontalDistance() + 1.0F;
      float f1 = Math.min(f, 40.0F);
      return 0.7F / f1 / f;
   }
}
