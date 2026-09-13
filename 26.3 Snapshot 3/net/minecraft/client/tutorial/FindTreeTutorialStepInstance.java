package net.minecraft.client.tutorial;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.TutorialToast;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.stats.Stats;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jspecify.annotations.Nullable;

public class FindTreeTutorialStepInstance implements TutorialStepInstance {
   private static final int HINT_DELAY = 6000;
   private static final Component TITLE = Component.translatable("tutorial.find_tree.title");
   private static final Component DESCRIPTION = Component.translatable("tutorial.find_tree.description");
   private final Tutorial tutorial;
   private @Nullable TutorialToast toast;
   private int timeWaiting;

   public FindTreeTutorialStepInstance(final Tutorial tutorial) {
      this.tutorial = tutorial;
   }

   @Override
   public void tick() {
      this.timeWaiting++;
      if (!this.tutorial.isSurvival()) {
         this.tutorial.setStep(TutorialSteps.NONE);
      } else {
         Minecraft minecraft = this.tutorial.getMinecraft();
         if (this.timeWaiting == 1) {
            LocalPlayer player = minecraft.player;
            if (player != null && (hasCollectedTreeItems(player) || hasPunchedTreesPreviously(player))) {
               this.tutorial.setStep(TutorialSteps.CRAFT_PLANKS);
               return;
            }
         }

         if (this.timeWaiting >= 6000 && this.toast == null) {
            this.toast = new TutorialToast(minecraft.font, TutorialToast.Icons.TREE, TITLE, DESCRIPTION, false);
            minecraft.gui.toastManager().addToast(this.toast);
         }
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
   public void onLookAt(final ClientLevel level, final HitResult hit) {
      if (hit.getType() == HitResult.Type.BLOCK) {
         BlockState state = level.getBlockState(((BlockHitResult)hit).getBlockPos());
         if (state.is(BlockTags.COMPLETES_FIND_TREE_TUTORIAL)) {
            this.tutorial.setStep(TutorialSteps.PUNCH_TREE);
         }
      }
   }

   @Override
   public void onGetItem(final ItemStack itemStack) {
      if (itemStack.is(ItemTags.COMPLETES_FIND_TREE_TUTORIAL)) {
         this.tutorial.setStep(TutorialSteps.CRAFT_PLANKS);
      }
   }

   private static boolean hasCollectedTreeItems(final LocalPlayer player) {
      return player.getInventory().hasAnyMatching(item -> item.is(ItemTags.COMPLETES_FIND_TREE_TUTORIAL));
   }

   public static boolean hasPunchedTreesPreviously(final LocalPlayer player) {
      for (Holder<Block> holder : BuiltInRegistries.BLOCK.getTagOrEmpty(BlockTags.COMPLETES_FIND_TREE_TUTORIAL)) {
         Block block = holder.value();
         if (player.getStats().getValue(Stats.BLOCK_MINED.get(block)) > 0) {
            return true;
         }
      }

      return false;
   }
}
