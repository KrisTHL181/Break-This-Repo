package net.minecraft.client.resources.sounds;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BubbleColumnBlock;
import net.minecraft.world.level.block.state.BlockState;

public class BubbleColumnAmbientSoundHandler implements AmbientSoundHandler {
   private final LocalPlayer player;
   private boolean wasInBubbleColumn;
   private boolean firstTick = true;

   public BubbleColumnAmbientSoundHandler(final LocalPlayer player) {
      this.player = player;
   }

   @Override
   public void tick() {
      Level level = this.player.level();
      BlockState state = level.getBlockStatesIfLoaded(this.player.getBoundingBox().inflate(0.0, -0.4F, 0.0).deflate(1.0E-6))
         .filter(s -> s.is(Blocks.BUBBLE_COLUMN))
         .findFirst()
         .orElse(null);
      if (state != null) {
         if (!this.wasInBubbleColumn && !this.firstTick && state.is(Blocks.BUBBLE_COLUMN) && !this.player.isSpectator()) {
            boolean dragDown = state.getValue(BubbleColumnBlock.DRAG_DOWN);
            if (dragDown) {
               this.player.playSound(SoundEvents.BUBBLE_COLUMN_WHIRLPOOL_INSIDE, 1.0F, 1.0F);
            } else {
               this.player.playSound(SoundEvents.BUBBLE_COLUMN_UPWARDS_INSIDE, 1.0F, 1.0F);
            }
         }

         this.wasInBubbleColumn = true;
      } else {
         this.wasInBubbleColumn = false;
      }

      this.firstTick = false;
   }
}
