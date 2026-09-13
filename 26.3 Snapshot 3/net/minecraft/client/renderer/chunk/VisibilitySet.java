package net.minecraft.client.renderer.chunk;

import java.util.BitSet;
import java.util.Locale;
import java.util.Set;
import net.minecraft.core.Direction;

public class VisibilitySet {
   private static final int FACINGS = Direction.values().length;
   private final BitSet data = new BitSet(FACINGS * FACINGS);

   public void add(final Set<Direction> directions) {
      for (Direction direction1 : directions) {
         for (Direction direction2 : directions) {
            this.set(direction1, direction2, true);
         }
      }
   }

   public void set(final Direction direction1, final Direction direction2, final boolean value) {
      this.data.set(direction1.ordinal() + direction2.ordinal() * FACINGS, value);
      this.data.set(direction2.ordinal() + direction1.ordinal() * FACINGS, value);
   }

   public void setAll(final boolean visible) {
      this.data.set(0, this.data.size(), visible);
   }

   public boolean visibilityBetween(final Direction direction1, final Direction direction2) {
      return this.data.get(direction1.ordinal() + direction2.ordinal() * FACINGS);
   }

   @Override
   public String toString() {
      StringBuilder builder = new StringBuilder();
      builder.append(' ');

      for (Direction direction : Direction.values()) {
         builder.append(' ').append(direction.toString().toUpperCase(Locale.ROOT).charAt(0));
      }

      builder.append('\n');

      for (Direction direction1 : Direction.values()) {
         builder.append(direction1.toString().toUpperCase(Locale.ROOT).charAt(0));

         for (Direction direction2 : Direction.values()) {
            if (direction1 == direction2) {
               builder.append("  ");
            } else {
               boolean ok = this.visibilityBetween(direction1, direction2);
               builder.append(' ').append((char)(ok ? 'Y' : 'n'));
            }
         }

         builder.append('\n');
      }

      return builder.toString();
   }
}
