package net.minecraft.client.model.geom.builders;

import java.util.Set;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.core.Direction;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public final class CubeDefinition {
   private final @Nullable String comment;
   private final Vector3fc origin;
   private final Vector3fc dimensions;
   private final CubeDeformation grow;
   private final boolean mirror;
   private final UVPair texCoord;
   private final UVPair texScale;
   private final Set<Direction> visibleFaces;

   protected CubeDefinition(
      @Nullable String p_273024_,
      float p_273620_,
      float p_273436_,
      float p_273139_,
      float p_273013_,
      float p_272874_,
      float p_273100_,
      float p_273756_,
      float p_273105_,
      CubeDeformation p_272818_,
      boolean p_273585_,
      float p_272829_,
      float p_273119_,
      Set<Direction> p_273201_
   ) {
      this.comment = p_273024_;
      this.texCoord = new UVPair(p_273620_, p_273436_);
      this.origin = new Vector3f(p_273139_, p_273013_, p_272874_);
      this.dimensions = new Vector3f(p_273100_, p_273756_, p_273105_);
      this.grow = p_272818_;
      this.mirror = p_273585_;
      this.texScale = new UVPair(p_272829_, p_273119_);
      this.visibleFaces = p_273201_;
   }

   public ModelPart.Cube bake(int p_171456_, int p_171457_) {
      return new ModelPart.Cube(
         (int)this.texCoord.u(),
         (int)this.texCoord.v(),
         this.origin.x(),
         this.origin.y(),
         this.origin.z(),
         this.dimensions.x(),
         this.dimensions.y(),
         this.dimensions.z(),
         this.grow.growX,
         this.grow.growY,
         this.grow.growZ,
         this.mirror,
         p_171456_ * this.texScale.u(),
         p_171457_ * this.texScale.v(),
         this.visibleFaces
      );
   }
}
