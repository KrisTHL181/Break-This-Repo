package net.minecraft.world.entity;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

public enum MobCategory implements StringRepresentable {
   MONSTER("monster", 70, false, false, 128),
   CREATURE("creature", 10, true, true, 128),
   AMBIENT("ambient", 15, true, false, 128),
   AXOLOTLS("axolotls", 5, true, false, 128),
   UNDERGROUND_WATER_CREATURE("underground_water_creature", 5, true, false, 128),
   WATER_CREATURE("water_creature", 5, true, false, 128),
   WATER_AMBIENT("water_ambient", 20, true, false, 64),
   MISC("misc", -1, true, true, 128);

   public static final Codec<MobCategory> CODEC = StringRepresentable.fromEnum(MobCategory::values);
   private final int max;
   private final boolean isFriendly;
   private final boolean isPersistent;
   private final String name;
   private final int noDespawnDistance = 32;
   private final int despawnDistance;

   MobCategory(final String p_21597_, final int p_21598_, final boolean p_21599_, final boolean p_21600_, final int p_21601_) {
      this.name = p_21597_;
      this.max = p_21598_;
      this.isFriendly = p_21599_;
      this.isPersistent = p_21600_;
      this.despawnDistance = p_21601_;
   }

   public String getName() {
      return this.name;
   }

   @Override
   public String getSerializedName() {
      return this.name;
   }

   public int getMaxInstancesPerChunk() {
      return this.max;
   }

   public boolean isFriendly() {
      return this.isFriendly;
   }

   public boolean isPersistent() {
      return this.isPersistent;
   }

   public int getDespawnDistance() {
      return this.despawnDistance;
   }

   public int getNoDespawnDistance() {
      return 32;
   }
}
