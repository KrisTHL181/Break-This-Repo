package net.minecraft.client.tutorial;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.ClientInput;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import org.jspecify.annotations.Nullable;

public class Tutorial {
   private final Minecraft minecraft;
   private @Nullable TutorialStepInstance instance;

   public Tutorial(final Minecraft minecraft, final Options options) {
      this.minecraft = minecraft;
   }

   public void onInput(final ClientInput input) {
      if (this.instance != null) {
         this.instance.onInput(input);
      }
   }

   public void onMouse(final double xd, final double yd) {
      if (this.instance != null) {
         this.instance.onMouse(xd, yd);
      }
   }

   public void onLookAt(final @Nullable ClientLevel level, final @Nullable HitResult hit) {
      if (this.instance != null && hit != null && level != null) {
         this.instance.onLookAt(level, hit);
      }
   }

   public void onDestroyBlock(final ClientLevel level, final BlockPos pos, final BlockState state, final float percent) {
      if (this.instance != null) {
         this.instance.onDestroyBlock(level, pos, state, percent);
      }
   }

   public void onOpenInventory() {
      if (this.instance != null) {
         this.instance.onOpenInventory();
      }
   }

   public void onGetItem(final ItemStack itemStack) {
      if (this.instance != null) {
         this.instance.onGetItem(itemStack);
      }
   }

   public void stop() {
      if (this.instance != null) {
         this.instance.clear();
         this.instance = null;
      }
   }

   public void start() {
      if (this.instance != null) {
         this.stop();
      }

      this.instance = this.minecraft.options.tutorialStep.create(this);
   }

   public void tick() {
      if (this.instance != null) {
         if (this.minecraft.level != null) {
            this.instance.tick();
         } else {
            this.stop();
         }
      } else if (this.minecraft.level != null) {
         this.start();
      }
   }

   public void setStep(final TutorialSteps step) {
      this.minecraft.options.tutorialStep = step;
      this.minecraft.options.save();
      if (this.instance != null) {
         this.instance.clear();
         this.instance = step.create(this);
      }
   }

   public Minecraft getMinecraft() {
      return this.minecraft;
   }

   public boolean isSurvival() {
      return this.minecraft.gameMode == null ? false : this.minecraft.gameMode.getPlayerMode() == GameType.SURVIVAL;
   }

   public static Component key(final String name) {
      return Component.keybind("key." + name).withStyle(ChatFormatting.BOLD);
   }

   public void onInventoryAction(final ItemStack itemCarried, final ItemStack itemInSlot, final ClickAction clickAction) {
   }
}
