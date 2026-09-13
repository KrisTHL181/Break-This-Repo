package net.minecraft.client.model.animal.ghast;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.MeshTransformer;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.HappyGhastRenderState;

public class HappyGhastHarnessModel extends EntityModel<HappyGhastRenderState> {
   private static final float GOGGLES_Y_OFFSET = 14.0F;
   private final ModelPart goggles;

   public HappyGhastHarnessModel(final ModelPart root) {
      super(root);
      this.goggles = root.getChild("goggles");
   }

   public static LayerDefinition createHarnessLayer(final boolean baby) {
      MeshDefinition meshdefinition = new MeshDefinition();
      PartDefinition root = meshdefinition.getRoot();
      root.addOrReplaceChild(
         "harness", CubeListBuilder.create().texOffs(0, 0).addBox(-8.0F, -16.0F, -8.0F, 16.0F, 16.0F, 16.0F), PartPose.offset(0.0F, 24.0F, 0.0F)
      );
      root.addOrReplaceChild(
         "goggles",
         CubeListBuilder.create().texOffs(0, 32).addBox(-8.0F, -2.5F, -2.5F, 16.0F, 5.0F, 5.0F, new CubeDeformation(0.15F)),
         PartPose.offset(0.0F, 14.0F, -5.5F)
      );
      return LayerDefinition.create(meshdefinition, 64, 64)
         .apply(MeshTransformer.scaling(4.0F))
         .apply(baby ? HappyGhastModel.BABY_TRANSFORMER : MeshTransformer.IDENTITY);
   }

   public void setupAnim(final HappyGhastRenderState state) {
      super.setupAnim(state);
      if (state.isRidden) {
         this.goggles.xRot = 0.0F;
         this.goggles.y = 14.0F;
      } else {
         this.goggles.xRot = -0.7854F;
         this.goggles.y = 9.0F;
      }
   }
}
