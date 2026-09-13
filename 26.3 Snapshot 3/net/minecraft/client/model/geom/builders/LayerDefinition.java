package net.minecraft.client.model.geom.builders;

import net.minecraft.client.model.geom.ModelPart;

public class LayerDefinition {
   private final MeshDefinition mesh;
   private final MaterialDefinition material;

   private LayerDefinition(final MeshDefinition mesh, final MaterialDefinition material) {
      this.mesh = mesh;
      this.material = material;
   }

   public LayerDefinition apply(final MeshTransformer transformer) {
      return new LayerDefinition(transformer.apply(this.mesh), this.material);
   }

   public ModelPart bakeRoot() {
      return this.mesh.getRoot().bake(this.material.xTexSize, this.material.yTexSize);
   }

   public static LayerDefinition create(final MeshDefinition mesh, final int xTexSize, final int yTexSize) {
      return new LayerDefinition(mesh, new MaterialDefinition(xTexSize, yTexSize));
   }
}
