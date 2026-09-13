package net.minecraft.client.resources.sounds;

import net.minecraft.client.Camera;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class DirectionalSoundInstance extends AbstractTickableSoundInstance {
   private final Camera camera;
   private final float xAngle;
   private final float yAngle;

   public DirectionalSoundInstance(SoundEvent p_427541_, SoundSource p_426514_, RandomSource p_422543_, Camera p_428456_, float p_431137_, float p_427907_) {
      super(p_427541_, p_426514_, p_422543_);
      this.camera = p_428456_;
      this.xAngle = p_431137_;
      this.yAngle = p_427907_;
      this.setPosition();
   }

   private void setPosition() {
      Vec3 vec3 = Vec3.directionFromRotation(this.xAngle, this.yAngle).scale(10.0);
      this.x = this.camera.position().x + vec3.x;
      this.y = this.camera.position().y + vec3.y;
      this.z = this.camera.position().z + vec3.z;
      this.attenuation = SoundInstance.Attenuation.NONE;
   }

   @Override
   public void tick() {
      this.setPosition();
   }
}
