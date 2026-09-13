package net.minecraft.client.player;

import net.minecraft.client.Options;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;

public class KeyboardInput extends ClientInput {
   private final Options options;

   public KeyboardInput(final Options options) {
      this.options = options;
   }

   private static float calculateImpulse(final boolean positive, final boolean negative) {
      if (positive == negative) {
         return 0.0F;
      } else {
         return positive ? 1.0F : -1.0F;
      }
   }

   @Override
   public void tick() {
      this.keyPresses = new Input(
         this.options.keyUp.isDown(),
         this.options.keyDown.isDown(),
         this.options.keyLeft.isDown(),
         this.options.keyRight.isDown(),
         this.options.keyJump.isDown(),
         this.options.keyShift.isDown(),
         this.options.keySprint.isDown()
      );
      float forwardImpulse = calculateImpulse(this.keyPresses.forward(), this.keyPresses.backward());
      float leftImpulse = calculateImpulse(this.keyPresses.left(), this.keyPresses.right());
      this.moveVector = new Vec2(leftImpulse, forwardImpulse).normalized();
   }
}
