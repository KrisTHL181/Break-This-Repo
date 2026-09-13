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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class Tutorial {
   private final Minecraft minecraft;
   private @Nullable TutorialStepInstance instance;

   public Tutorial(Minecraft p_175022_, Options p_175023_) {
      this.minecraft = p_175022_;
   }

   public void onInput(ClientInput p_367719_) {
      if (this.instance != null) {
         this.instance.onInput(p_367719_);
      }
   }

   public void onMouse(double p_120566_, double p_120567_) {
      if (this.instance != null) {
         this.instance.onMouse(p_120566_, p_120567_);
      }
   }

   public void onLookAt(@Nullable ClientLevel p_120579_, @Nullable HitResult p_120580_) {
      if (this.instance != null && p_120580_ != null && p_120579_ != null) {
         this.instance.onLookAt(p_120579_, p_120580_);
      }
   }

   public void onDestroyBlock(ClientLevel p_120582_, BlockPos p_120583_, BlockState p_120584_, float p_120585_) {
      if (this.instance != null) {
         this.instance.onDestroyBlock(p_120582_, p_120583_, p_120584_, p_120585_);
      }
   }

   public void onOpenInventory() {
      if (this.instance != null) {
         this.instance.onOpenInventory();
      }
   }

   public void onGetItem(ItemStack p_120569_) {
      if (this.instance != null) {
         this.instance.onGetItem(p_120569_);
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

   public void setStep(TutorialSteps p_120589_) {
      this.minecraft.options.tutorialStep = p_120589_;
      this.minecraft.options.save();
      if (this.instance != null) {
         this.instance.clear();
         this.instance = p_120589_.create(this);
      }
   }

   public Minecraft getMinecraft() {
      return this.minecraft;
   }

   public boolean isSurvival() {
      return this.minecraft.gameMode == null ? false : this.minecraft.gameMode.getPlayerMode() == GameType.SURVIVAL;
   }

   public static Component key(String p_120593_) {
      return Component.keybind("key." + p_120593_).withStyle(ChatFormatting.BOLD);
   }

   public void onInventoryAction(ItemStack p_175025_, ItemStack p_175026_, ClickAction p_175027_) {
   }
}
