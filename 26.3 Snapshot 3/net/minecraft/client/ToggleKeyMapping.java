package net.minecraft.client;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.function.BooleanSupplier;

public class ToggleKeyMapping extends KeyMapping {
   private final BooleanSupplier needsToggle;
   private boolean releasedByScreenWhenDown;
   private final boolean shouldRestore;

   public ToggleKeyMapping(
      final String name, final int value, final KeyMapping.Category category, final BooleanSupplier needsToggle, final boolean shouldRestore
   ) {
      this(name, InputConstants.Type.KEYSYM, value, category, needsToggle, shouldRestore);
   }

   public ToggleKeyMapping(
      final String name,
      final InputConstants.Type type,
      final int value,
      final KeyMapping.Category category,
      final BooleanSupplier needsToggle,
      final boolean shouldRestore
   ) {
      super(name, type, value, category);
      this.needsToggle = needsToggle;
      this.shouldRestore = shouldRestore;
   }

   @Override
   protected boolean shouldSetOnIngameFocus() {
      return super.shouldSetOnIngameFocus() && !this.needsToggle.getAsBoolean();
   }

   @Override
   public void setDown(final boolean down) {
      if (this.needsToggle.getAsBoolean()) {
         if (down) {
            super.setDown(!this.isDown());
         }
      } else {
         super.setDown(down);
      }
   }

   @Override
   protected void release() {
      if (this.needsToggle.getAsBoolean() && this.isDown() || this.releasedByScreenWhenDown) {
         this.releasedByScreenWhenDown = true;
      }

      this.reset();
   }

   public boolean shouldRestoreStateOnScreenClosed() {
      boolean shouldRestore = this.shouldRestore
         && this.needsToggle.getAsBoolean()
         && this.key.getType() == InputConstants.Type.KEYSYM
         && this.releasedByScreenWhenDown;
      this.releasedByScreenWhenDown = false;
      return shouldRestore;
   }

   protected void reset() {
      super.setDown(false);
   }
}
