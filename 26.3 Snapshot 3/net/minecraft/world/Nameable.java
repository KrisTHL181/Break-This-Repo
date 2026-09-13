package net.minecraft.world;

import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

public interface Nameable {
   Component getName();

   default String getPlainTextName() {
      return this.getName().getString();
   }

   default boolean hasCustomName() {
      return this.getCustomName() != null;
   }

   default Component getDisplayName() {
      return this.getName();
   }

   default @Nullable Component getCustomName() {
      return null;
   }
}
