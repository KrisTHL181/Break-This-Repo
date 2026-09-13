package net.minecraft.world.entity.animal.parrot;

import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.TagValueOutput;
import org.slf4j.Logger;

public abstract class ShoulderRidingEntity extends TamableAnimal {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int RIDE_COOLDOWN = 100;
   private int rideCooldownCounter;

   protected ShoulderRidingEntity(final EntityType<? extends ShoulderRidingEntity> type, final Level level) {
      super(type, level);
   }

   public boolean setEntityOnShoulder(final ServerPlayer player) {
      try (ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(this.problemPath(), LOGGER)) {
         TagValueOutput output = TagValueOutput.createWithContext(reporter, this.registryAccess());
         this.saveWithoutId(output);
         output.putString("id", this.getEncodeId());
         if (player.setEntityOnShoulder(output.buildResult())) {
            this.discard();
            return true;
         }
      }

      return false;
   }

   @Override
   public void tick() {
      this.rideCooldownCounter++;
      super.tick();
   }

   public boolean canSitOnShoulder() {
      return this.rideCooldownCounter > 100;
   }
}
