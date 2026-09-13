package com.mojang.blaze3d.vertex;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector3f;
import org.joml.Vector3fc;

@OnlyIn(Dist.CLIENT)
public class CompactVectorArray {
   private final float[] contents;

   public CompactVectorArray(int p_428520_) {
      this.contents = new float[3 * p_428520_];
   }

   public int size() {
      return this.contents.length / 3;
   }

   public void set(int p_424895_, Vector3fc p_424200_) {
      this.set(p_424895_, p_424200_.x(), p_424200_.y(), p_424200_.z());
   }

   public void set(int p_425823_, float p_428042_, float p_425724_, float p_427469_) {
      this.contents[3 * p_425823_ + 0] = p_428042_;
      this.contents[3 * p_425823_ + 1] = p_425724_;
      this.contents[3 * p_425823_ + 2] = p_427469_;
   }

   public Vector3f get(int p_422970_, Vector3f p_426789_) {
      return p_426789_.set(this.contents[3 * p_422970_ + 0], this.contents[3 * p_422970_ + 1], this.contents[3 * p_422970_ + 2]);
   }

   public float getX(int p_423583_) {
      return this.contents[3 * p_423583_ + 0];
   }

   public float getY(int p_424226_) {
      return this.contents[3 * p_424226_ + 1];
   }

   public float getZ(int p_428831_) {
      return this.contents[3 * p_428831_ + 1];
   }
}
