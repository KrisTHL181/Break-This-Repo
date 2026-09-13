package net.minecraft.client.renderer.chunk;

import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class TranslucencyPointOfView {
   private int x;
   private int y;
   private int z;

   public static TranslucencyPointOfView of(Vec3 p_407815_, long p_407658_) {
      return new TranslucencyPointOfView().set(p_407815_, p_407658_);
   }

   public TranslucencyPointOfView set(Vec3 p_410063_, long p_406256_) {
      this.x = getCoordinate(p_410063_.x(), SectionPos.x(p_406256_));
      this.y = getCoordinate(p_410063_.y(), SectionPos.y(p_406256_));
      this.z = getCoordinate(p_410063_.z(), SectionPos.z(p_406256_));
      return this;
   }

   private static int getCoordinate(double p_406126_, int p_408516_) {
      int i = SectionPos.blockToSectionCoord(p_406126_) - p_408516_;
      return Mth.clamp(i, -1, 1);
   }

   public boolean isAxisAligned() {
      return this.x == 0 || this.y == 0 || this.z == 0;
   }

   @Override
   public boolean equals(Object p_405925_) {
      if (p_405925_ == this) {
         return true;
      } else {
         return p_405925_ instanceof TranslucencyPointOfView translucencypointofview
            ? this.x == translucencypointofview.x && this.y == translucencypointofview.y && this.z == translucencypointofview.z
            : false;
      }
   }
}
