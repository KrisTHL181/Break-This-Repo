package net.minecraft.world.entity.player;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.ExtraCodecs;

public class Abilities {
   private static final boolean DEFAULT_INVULNERABLE = false;
   private static final boolean DEFAULY_FLYING = false;
   private static final boolean DEFAULT_MAY_FLY = false;
   private static final boolean DEFAULT_INSTABUILD = false;
   private static final boolean DEFAULT_MAY_BUILD = true;
   private static final float DEFAULT_FLYING_SPEED = 0.05F;
   private static final float DEFAULT_WALKING_SPEED = 0.1F;
   public boolean invulnerable;
   public boolean flying;
   public boolean mayfly;
   public boolean instabuild;
   public boolean mayBuild = true;
   private float flyingSpeed = 0.05F;
   private float walkingSpeed = 0.1F;

   public float getFlyingSpeed() {
      return this.flyingSpeed;
   }

   public void setFlyingSpeed(final float value) {
      this.flyingSpeed = value;
   }

   public float getWalkingSpeed() {
      return this.walkingSpeed;
   }

   public void setWalkingSpeed(final float value) {
      this.walkingSpeed = value;
   }

   public Abilities.Packed pack() {
      return new Abilities.Packed(this.invulnerable, this.flying, this.mayfly, this.instabuild, this.mayBuild, this.flyingSpeed, this.walkingSpeed);
   }

   public void apply(final Abilities.Packed packed) {
      this.invulnerable = packed.invulnerable;
      this.flying = packed.flying;
      this.mayfly = packed.mayFly;
      this.instabuild = packed.instabuild;
      this.mayBuild = packed.mayBuild;
      this.flyingSpeed = packed.flyingSpeed;
      this.walkingSpeed = packed.walkingSpeed;
   }

   public record Packed(boolean invulnerable, boolean flying, boolean mayFly, boolean instabuild, boolean mayBuild, float flyingSpeed, float walkingSpeed) {
      public static final Codec<Abilities.Packed> CODEC = RecordCodecBuilder.create(
         i -> i.group(
               ExtraCodecs.optionalAlwaysPresentFieldOf(Codec.BOOL, "invulnerable", false).forGetter(Abilities.Packed::invulnerable),
               ExtraCodecs.optionalAlwaysPresentFieldOf(Codec.BOOL, "flying", false).forGetter(Abilities.Packed::flying),
               ExtraCodecs.optionalAlwaysPresentFieldOf(Codec.BOOL, "mayfly", false).forGetter(Abilities.Packed::mayFly),
               ExtraCodecs.optionalAlwaysPresentFieldOf(Codec.BOOL, "instabuild", false).forGetter(Abilities.Packed::instabuild),
               ExtraCodecs.optionalAlwaysPresentFieldOf(Codec.BOOL, "mayBuild", true).forGetter(Abilities.Packed::mayBuild),
               ExtraCodecs.optionalAlwaysPresentFieldOf(Codec.FLOAT, "flySpeed", 0.05F).forGetter(Abilities.Packed::flyingSpeed),
               ExtraCodecs.optionalAlwaysPresentFieldOf(Codec.FLOAT, "walkSpeed", 0.1F).forGetter(Abilities.Packed::walkingSpeed)
            )
            .apply(i, Abilities.Packed::new)
      );
   }
}
