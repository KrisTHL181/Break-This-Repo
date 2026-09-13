package net.minecraft.world.entity.animal.equine;

import java.util.function.IntFunction;
import net.minecraft.util.ByIdMap;

public enum Markings {
   NONE(0),
   WHITE(1),
   WHITE_FIELD(2),
   WHITE_DOTS(3),
   BLACK_DOTS(4);

   private static final IntFunction<Markings> BY_ID = ByIdMap.continuous(Markings::getId, values(), ByIdMap.OutOfBoundsStrategy.WRAP);
   private final int id;

   Markings(final int id) {
      this.id = id;
   }

   public int getId() {
      return this.id;
   }

   public static Markings byId(final int id) {
      return BY_ID.apply(id);
   }
}
