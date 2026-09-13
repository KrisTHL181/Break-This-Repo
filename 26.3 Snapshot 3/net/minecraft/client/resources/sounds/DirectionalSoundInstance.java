package net.minecraft.client.resources.sounds;

import net.minecraft.client.Camera;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

public class DirectionalSoundInstance extends AbstractTickableSoundInstance {
   private final Camera camera;
   private final float xAngle;
   private final float yAngle;

   public DirectionalSoundInstance(
      final SoundEvent event, final SoundSource source, final RandomSource random, final Camera camera, final float xAngle, final float yAngle
   ) {
      super(event, source, random);
      this.camera = camera;
      this.xAngle = xAngle;
      this.yAngle = yAngle;
      this.setPosition();
   }

   private void setPosition() {
      Vec3 direction = Vec3.directionFromRotation(this.xAngle, this.yAngle).scale(10.0);
      this.x = this.camera.position().x + direction.x;
      this.y = this.camera.position().y + direction.y;
      this.z = this.camera.position().z + direction.z;
      this.attenuation = SoundInstance.Attenuation.NONE;
   }

   @Override
   public void tick() {
      this.setPosition();
   }
}
