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
   public static Set<Relative> union(final Set<Relative>... sets) {
      HashSet<Relative> set = new HashSet<>();

      for (Set<Relative> s : sets) {
         set.addAll(s);
      }

      return set;
   }

   public static Set<Relative> rotation(final boolean relativeYRot, final boolean relativeXRot) {
      Set<Relative> relatives = EnumSet.noneOf(Relative.class);
      if (relativeYRot) {
         relatives.add(Y_ROT);
      }

      if (relativeXRot) {
         relatives.add(X_ROT);
      }

      return relatives;
   }

   public static Set<Relative> position(final boolean relativeX, final boolean relativeY, final boolean relativeZ) {
      Set<Relative> relatives = EnumSet.noneOf(Relative.class);
      if (relativeX) {
         relatives.add(X);
      }

      if (relativeY) {
         relatives.add(Y);
      }

      if (relativeZ) {
         relatives.add(Z);
      }

      return relatives;
   }

   public static Set<Relative> direction(final boolean relativeX, final boolean relativeY, final boolean relativeZ) {
      Set<Relative> relatives = EnumSet.noneOf(Relative.class);
      if (relativeX) {
         relatives.add(DELTA_X);
      }

      if (relativeY) {
         relatives.add(DELTA_Y);
      }

      if (relativeZ) {
         relatives.add(DELTA_Z);
      }

      return relatives;
   }

   Relative(final int bit) {
      this.bit = bit;
   }

   private int getMask() {
      return 1 << this.bit;
   }

   private boolean isSet(final int value) {
      return (value & this.getMask()) == this.getMask();
   }

   public static Set<Relative> unpack(final int value) {
      Set<Relative> result = EnumSet.noneOf(Relative.class);

      for (Relative argument : values()) {
         if (argument.isSet(value)) {
            result.add(argument);
         }
      }

      return result;
   }

   public static int pack(final Set<Relative> set) {
      int result = 0;

      for (Relative argument : set) {
         result |= argument.getMask();
      }

      return result;
   }
}
