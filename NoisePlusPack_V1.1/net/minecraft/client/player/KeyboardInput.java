package net.minecraft.client.player;

import net.minecraft.client.Options;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class KeyboardInput extends ClientInput {
   private final Options options;

   public KeyboardInput(Options p_108580_) {
      this.options = p_108580_;
   }

   private static float calculateImpulse(boolean p_205578_, boolean p_205579_) {
      if (p_205578_ == p_205579_) {
         return 0.0F;
      } else {
         return p_205578_ ? 1.0F : -1.0F;
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
      float f = calculateImpulse(this.keyPresses.forward(), this.keyPresses.backward());
      float f1 = calculateImpulse(this.keyPresses.left(), this.keyPresses.right());
      this.moveVector = new Vec2(f1, f).normalized();
   }
}
