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

   protected ShoulderRidingEntity(EntityType<? extends ShoulderRidingEntity> p_453343_, Level p_455523_) {
      super(p_453343_, p_455523_);
   }

   public boolean setEntityOnShoulder(ServerPlayer p_456816_) {
      try (ProblemReporter.ScopedCollector problemreporter$scopedcollector = new ProblemReporter.ScopedCollector(this.problemPath(), LOGGER)) {
         TagValueOutput tagvalueoutput = TagValueOutput.createWithContext(problemreporter$scopedcollector, this.registryAccess());
         this.saveWithoutId(tagvalueoutput);
         tagvalueoutput.putString("id", this.getEncodeId());
         if (p_456816_.setEntityOnShoulder(tagvalueoutput.buildResult())) {
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
