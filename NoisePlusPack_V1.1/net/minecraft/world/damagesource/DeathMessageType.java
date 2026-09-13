package net.minecraft.world.damagesource;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

public enum DeathMessageType implements StringRepresentable {
   DEFAULT("default"),
   FALL_VARIANTS("fall_variants"),
   INTENTIONAL_GAME_DESIGN("intentional_game_design");

   public static final Codec<DeathMessageType> CODEC = StringRepresentable.fromEnum(DeathMessageType::values);
   private final String id;

   DeathMessageType(final String p_270201_) {
      this.id = p_270201_;
   }

   @Override
   public String getSerializedName() {
      return this.id;
   }
}
