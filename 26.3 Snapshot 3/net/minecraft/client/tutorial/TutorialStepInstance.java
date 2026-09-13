package net.minecraft.client.tutorial;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.ClientInput;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;

public interface TutorialStepInstance {
   default void clear() {
   }

   default void tick() {
   }

   default void onInput(final ClientInput input) {
   }

   default void onMouse(final double xd, final double yd) {
   }

   default void onLookAt(final ClientLevel level, final HitResult hit) {
   }

   default void onDestroyBlock(final ClientLevel level, final BlockPos pos, final BlockState state, final float percent) {
   }

   default void onOpenInventory() {
   }

   default void onGetItem(final ItemStack itemStack) {
   }
}
