package net.minecraft.client.model.geom.builders;

@FunctionalInterface
public interface MeshTransformer {
   MeshTransformer IDENTITY = mesh -> mesh;

   static MeshTransformer scaling(final float factor) {
      float yOffset = 24.016F * (1.0F - factor);
      return mesh -> mesh.transformed(pose -> pose.scaled(factor).translated(0.0F, yOffset, 0.0F));
   }

   MeshDefinition apply(MeshDefinition mesh);
}
