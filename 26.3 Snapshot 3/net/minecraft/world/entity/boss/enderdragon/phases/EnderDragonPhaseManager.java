package net.minecraft.world.entity.boss.enderdragon.phases;

import com.mojang.logging.LogUtils;
import java.util.Objects;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class EnderDragonPhaseManager {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final EnderDragon dragon;
   private final @Nullable DragonPhaseInstance[] phases = new DragonPhaseInstance[EnderDragonPhase.getCount()];
   private @Nullable DragonPhaseInstance currentPhase;

   public EnderDragonPhaseManager(final EnderDragon dragon) {
      this.dragon = dragon;
      this.setPhase(EnderDragonPhase.HOVERING);
   }

   public void setPhase(final EnderDragonPhase<?> target) {
      if (this.currentPhase == null || target != this.currentPhase.getPhase()) {
         if (this.currentPhase != null) {
            this.currentPhase.end();
         }

         this.currentPhase = this.getPhase((EnderDragonPhase<DragonPhaseInstance>)target);
         if (!this.dragon.level().isClientSide()) {
            this.dragon.getEntityData().set(EnderDragon.DATA_PHASE, target.getId());
         }

         LOGGER.debug("Dragon is now in phase {} on the {}", target, this.dragon.level().isClientSide() ? "client" : "server");
         this.currentPhase.begin();
      }
   }

   public DragonPhaseInstance getCurrentPhase() {
      return Objects.requireNonNull(this.currentPhase);
   }

   public <T extends DragonPhaseInstance> T getPhase(final EnderDragonPhase<T> phase) {
      int id = phase.getId();
      DragonPhaseInstance phaseInstance = this.phases[id];
      if (phaseInstance == null) {
         phaseInstance = phase.createInstance(this.dragon);
         this.phases[id] = phaseInstance;
      }

      return (T)phaseInstance;
   }
}
