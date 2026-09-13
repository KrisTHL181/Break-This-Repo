package net.minecraft.client.gui.components;

public interface SelectableEntry {
   default boolean mouseOverIcon(final int relX, final int relY, final int size) {
      return relX >= 0 && relX < size && relY >= 0 && relY < size;
   }

   default boolean mouseOverLeftHalf(final int relX, final int relY, final int size) {
      return relX >= 0 && relX < size / 2 && relY >= 0 && relY < size;
   }

   default boolean mouseOverRightHalf(final int relX, final int relY, final int size) {
      return relX >= size / 2 && relX < size && relY >= 0 && relY < size;
   }

   default boolean mouseOverTopRightQuarter(final int relX, final int relY, final int size) {
      return relX >= size / 2 && relX < size && relY >= 0 && relY < size / 2;
   }

   default boolean mouseOverBottomRightQuarter(final int relX, final int relY, final int size) {
      return relX >= size / 2 && relX < size && relY >= size / 2 && relY < size;
   }

   default boolean mouseOverTopLeftQuarter(final int relX, final int relY, final int size) {
      return relX >= 0 && relX < size / 2 && relY >= 0 && relY < size / 2;
   }

   default boolean mouseOverBottomLeftQuarter(final int relX, final int relY, final int size) {
      return relX >= 0 && relX < size / 2 && relY >= size / 2 && relY < size;
   }
}
