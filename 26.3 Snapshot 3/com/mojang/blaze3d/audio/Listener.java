package com.mojang.blaze3d.audio;

import net.minecraft.world.phys.Vec3;
import org.lwjgl.openal.AL10;

public class Listener {
   private ListenerTransform transform = ListenerTransform.INITIAL;

   public void setTransform(final ListenerTransform transform) {
      this.transform = transform;
      Vec3 position = transform.position();
      Vec3 forward = transform.forward();
      Vec3 up = transform.up();
      AL10.alListener3f(4100, (float)position.x, (float)position.y, (float)position.z);
      AL10.alListenerfv(4111, new float[]{(float)forward.x, (float)forward.y, (float)forward.z, (float)up.x(), (float)up.y(), (float)up.z()});
   }

   public void reset() {
      this.setTransform(ListenerTransform.INITIAL);
   }

   public ListenerTransform getTransform() {
      return this.transform;
   }
}
