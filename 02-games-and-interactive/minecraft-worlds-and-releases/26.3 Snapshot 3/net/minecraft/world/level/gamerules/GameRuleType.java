package net.minecraft.world.level.gamerules;

import net.minecraft.util.StringRepresentable;

public enum GameRuleType implements StringRepresentable {
   INT("integer"),
   BOOL("boolean");

   private final String name;

   GameRuleType(final String name) {
      this.name = name;
   }

   @Override
   public String getSerializedName() {
      return this.name;
   }
}
