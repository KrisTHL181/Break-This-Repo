package net.minecraft.world.entity;

import io.netty.buffer.ByteBuf;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public enum Relative {
   X(0),
   Y(1),
   Z(2),
   Y_ROT(3),
   X_ROT(4),
   DELTA_X(5),
   DELTA_Y(6),
   DELTA_Z(7),
   ROTATE_DELTA(8);

   public static final Set<Relative> ALL = Set.of(values());
   public static final Set<Relative> ROTATION = Set.of(X_ROT, Y_ROT);
   public static final Set<Relative> DELTA = Set.of(DELTA_X, DELTA_Y, DELTA_Z, ROTATE_DELTA);
   public static final StreamCodec<ByteBuf, Set<Relative>> SET_STREAM_CODEC = ByteBufCodecs.INT.map(Relative::unpack, Relative::pack);
   private final int bit;

   @SafeVarargs
   public static Set<Relative> union(Set<Relative>... p_360861_) {
      HashSet<Relative> hashset = new HashSet<>();

      for (Set<Relative> set : p_360861_) {
         hashset.addAll(set);
      }

      return hashset;
   }

   public static Set<Relative> rotation(boolean p_423486_, boolean p_428813_) {
      Set<Relative> set = EnumSet.noneOf(Relative.class);
      if (p_423486_) {
         set.add(Y_ROT);
      }

      if (p_428813_) {
         set.add(X_ROT);
      }

      return set;
   }

   public static Set<Relative> position(boolean p_429389_, boolean p_426820_, boolean p_431699_) {
      Set<Relative> set = EnumSet.noneOf(Relative.class);
      if (p_429389_) {
         set.add(X);
      }

      if (p_426820_) {
         set.add(Y);
      }

      if (p_431699_) {
         set.add(Z);
      }

      return set;
   }

   public static Set<Relative> direction(boolean p_428176_, boolean p_425808_, boolean p_429880_) {
      Set<Relative> set = EnumSet.noneOf(Relative.class);
      if (p_428176_) {
         set.add(DELTA_X);
      }

      if (p_425808_) {
         set.add(DELTA_Y);
      }

      if (p_429880_) {
         set.add(DELTA_Z);
      }

      return set;
   }

   Relative(final int p_367418_) {
      this.bit = p_367418_;
   }

   private int getMask() {
      return 1 << this.bit;
   }

   private boolean isSet(int p_364108_) {
      return (p_364108_ & this.getMask()) == this.getMask();
   }

   public static Set<Relative> unpack(int p_366469_) {
      Set<Relative> set = EnumSet.noneOf(Relative.class);

      for (Relative relative : values()) {
         if (relative.isSet(p_366469_)) {
            set.add(relative);
         }
      }

      return set;
   }

   public static int pack(Set<Relative> p_370231_) {
      int i = 0;

      for (Relative relative : p_370231_) {
         i |= relative.getMask();
      }

      return i;
   }
}
