package net.minecraft.client.tutorial;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.TutorialToast;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class OpenInventoryTutorialStep implements TutorialStepInstance {
   private static final int HINT_DELAY = 600;
   private static final Component TITLE = Component.translatable("tutorial.open_inventory.title");
   private static final Component DESCRIPTION = Component.translatable("tutorial.open_inventory.description", Tutorial.key("inventory"));
   private final Tutorial tutorial;
   private @Nullable TutorialToast toast;
   private int timeWaiting;

   public OpenInventoryTutorialStep(Tutorial p_120537_) {
      this.tutorial = p_120537_;
   }

   @Override
   public void tick() {
      this.timeWaiting++;
      if (!this.tutorial.isSurvival()) {
         this.tutorial.setStep(TutorialSteps.NONE);
      } else if (this.timeWaiting >= 600 && this.toast == null) {
         Minecraft minecraft = this.tutorial.getMinecraft();
         this.toast = new TutorialToast(minecraft.font, TutorialToast.Icons.RECIPE_BOOK, TITLE, DESCRIPTION, false);
         minecraft.getToastManager().addToast(this.toast);
      }
   }

   @Override
   public void clear() {
      if (this.toast != null) {
         this.toast.hide();
         this.toast = null;
      }
   }

   @Override
   public void onOpenInventory() {
      this.tutorial.setStep(TutorialSteps.CRAFT_PLANKS);
   }
}
