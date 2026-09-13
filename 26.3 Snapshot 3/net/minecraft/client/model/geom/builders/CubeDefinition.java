package net.minecraft.client.model.geom.builders;

import java.util.Set;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.core.Direction;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

public final class CubeDefinition {
   private final @Nullable String comment;
   private final Vector3fc origin;
   private final Vector3fc dimensions;
   private final CubeDeformation grow;
   private final boolean mirror;
   private final UVPair texCoord;
   private final UVPair texScale;
   private final Set<Direction> visibleFaces;

   public CubeDefinition(
      final @Nullable String comment,
      final float xTexOffs,
      final float yTexOffs,
      final float minX,
      final float minY,
      final float minZ,
      final float width,
      final float height,
      final float depth,
      final CubeDeformation grow,
      final boolean mirror,
      final float xTexScale,
      final float yTexScale,
      final Set<Direction> visibleFaces
   ) {
      this.comment = comment;
      this.texCoord = new UVPair(xTexOffs, yTexOffs);
      this.origin = new Vector3f(minX, minY, minZ);
      this.dimensions = new Vector3f(width, height, depth);
      this.grow = grow;
      this.mirror = mirror;
      this.texScale = new UVPair(xTexScale, yTexScale);
      this.visibleFaces = visibleFaces;
   }

   public ModelPart.Cube bake(final int texScaleX, final int texScaleY) {
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
         texScaleX * this.texScale.u(),
         texScaleY * this.texScale.v(),
         this.visibleFaces
      );
   }
}
