package net.minecraft.client;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.function.BooleanSupplier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ToggleKeyMapping extends KeyMapping {
   private final BooleanSupplier needsToggle;
   private boolean releasedByScreenWhenDown;
   private final boolean shouldRestore;

   public ToggleKeyMapping(String p_92529_, int p_92530_, KeyMapping.Category p_429407_, BooleanSupplier p_92532_, boolean p_428841_) {
      this(p_92529_, InputConstants.Type.KEYSYM, p_92530_, p_429407_, p_92532_, p_428841_);
   }

   public ToggleKeyMapping(
      String p_426167_, InputConstants.Type p_427837_, int p_425923_, KeyMapping.Category p_422745_, BooleanSupplier p_431674_, boolean p_430720_
   ) {
      super(p_426167_, p_427837_, p_425923_, p_422745_);
      this.needsToggle = p_431674_;
      this.shouldRestore = p_430720_;
   }

   @Override
   protected boolean shouldSetOnIngameFocus() {
      return super.shouldSetOnIngameFocus() && !this.needsToggle.getAsBoolean();
   }

   @Override
   public void setDown(boolean p_92534_) {
      if (this.needsToggle.getAsBoolean()) {
         if (p_92534_) {
            super.setDown(!this.isDown());
         }
      } else {
         super.setDown(p_92534_);
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
      boolean flag = this.shouldRestore && this.needsToggle.getAsBoolean() && this.key.getType() == InputConstants.Type.KEYSYM && this.releasedByScreenWhenDown;
      this.releasedByScreenWhenDown = false;
      return flag;
   }

   protected void reset() {
      super.setDown(false);
   }
}
