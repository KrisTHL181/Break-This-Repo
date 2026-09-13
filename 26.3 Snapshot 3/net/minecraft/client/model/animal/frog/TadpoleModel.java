package net.minecraft.client.model.animal.frog;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.Mth;

public class TadpoleModel extends EntityModel<LivingEntityRenderState> {
   private final ModelPart tail;

   public TadpoleModel(final ModelPart root) {
      super(root, RenderTypes::entityCutout);
      this.tail = root.getChild("tail");
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition mesh = new MeshDefinition();
      PartDefinition root = mesh.getRoot();
      float xo = 0.0F;
      float yo = 22.0F;
      float zo = -3.0F;
      root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-1.5F, -1.0F, 0.0F, 3.0F, 2.0F, 3.0F), PartPose.offset(0.0F, 22.0F, -3.0F));
      root.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(0, 0).addBox(0.0F, -1.0F, 0.0F, 0.0F, 2.0F, 7.0F), PartPose.offset(0.0F, 22.0F, 0.0F));
      return LayerDefinition.create(mesh, 16, 16);
   }

   public void setupAnim(final LivingEntityRenderState state) {
      super.setupAnim(state);
      float amplitudeMultiplier = state.isInWater ? 1.0F : 1.5F;
      this.tail.yRot = -amplitudeMultiplier * 0.25F * Mth.sin(0.3F * state.ageInTicks);
   }
}
