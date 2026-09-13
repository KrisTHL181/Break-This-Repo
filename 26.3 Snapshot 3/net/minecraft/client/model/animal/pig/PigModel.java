package net.minecraft.client.model.animal.pig;

import java.util.Set;
import net.minecraft.client.model.BabyModelTransform;
import net.minecraft.client.model.QuadrupedModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.MeshTransformer;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

public class PigModel extends QuadrupedModel<LivingEntityRenderState> {
   public static final MeshTransformer BABY_TRANSFORMER = new BabyModelTransform(false, 4.0F, 4.0F, Set.of("head"));

   public PigModel(final ModelPart root) {
      super(root);
   }

   public static LayerDefinition createBodyLayer(final CubeDeformation g) {
      return LayerDefinition.create(createBasePigModel(g), 64, 64);
   }

   protected static MeshDefinition createBasePigModel(final CubeDeformation g) {
      MeshDefinition mesh = QuadrupedModel.createBodyMesh(6, true, false, g);
      PartDefinition root = mesh.getRoot();
      root.addOrReplaceChild(
         "head",
         CubeListBuilder.create()
            .texOffs(0, 0)
            .addBox(-4.0F, -4.0F, -8.0F, 8.0F, 8.0F, 8.0F, g)
            .texOffs(16, 16)
            .addBox(-2.0F, 0.0F, -9.0F, 4.0F, 3.0F, 1.0F, g),
         PartPose.offset(0.0F, 12.0F, -6.0F)
      );
      return mesh;
   }
}
