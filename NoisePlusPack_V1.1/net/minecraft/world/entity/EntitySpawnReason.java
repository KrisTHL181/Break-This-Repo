package net.minecraft.world.entity;

public enum EntitySpawnReason {
   NATURAL,
   CHUNK_GENERATION,
   SPAWNER,
   STRUCTURE,
   BREEDING,
   MOB_SUMMONED,
   JOCKEY,
   EVENT,
   CONVERSION,
   REINFORCEMENT,
   TRIGGERED,
   BUCKET,
   SPAWN_ITEM_USE,
   COMMAND,
   DISPENSER,
   PATROL,
   TRIAL_SPAWNER,
   LOAD,
   DIMENSION_TRAVEL;

   public static boolean isSpawner(EntitySpawnReason p_370217_) {
      return p_370217_ == SPAWNER || p_370217_ == TRIAL_SPAWNER;
   }

   public static boolean ignoresLightRequirements(EntitySpawnReason p_370141_) {
      return p_370141_ == TRIAL_SPAWNER;
   }
}
