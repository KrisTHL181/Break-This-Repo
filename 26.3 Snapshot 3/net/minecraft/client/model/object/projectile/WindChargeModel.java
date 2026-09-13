package net.minecraft.client.model.object.projectile;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;

public class WindChargeModel extends EntityModel<EntityRenderState> {
   private static final int ROTATION_SPEED = 16;
   private final ModelPart bone;
   private final ModelPart windCharge;
   private final ModelPart wind;

   public WindChargeModel(final ModelPart root) {
      super(root, RenderTypes::entityTranslucent);
      this.bone = root.getChild("bone");
      this.wind = this.bone.getChild("wind");
      this.windCharge = this.bone.getChild("wind_charge");
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition meshdefinition = new MeshDefinition();
      PartDefinition partdefinition = meshdefinition.getRoot();
      PartDefinition bone = partdefinition.addOrReplaceChild("bone", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));
      bone.addOrReplaceChild(
         "wind",
         CubeListBuilder.create()
            .texOffs(15, 20)
            .addBox(-4.0F, -1.0F, -4.0F, 8.0F, 2.0F, 8.0F, new CubeDeformation(0.0F))
            .texOffs(0, 9)
            .addBox(-3.0F, -2.0F, -3.0F, 6.0F, 4.0F, 6.0F, new CubeDeformation(0.0F)),
         PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -0.7854F, 0.0F)
      );
      bone.addOrReplaceChild(
         "wind_charge",
         CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -2.0F, -2.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(0.0F)),
         PartPose.offset(0.0F, 0.0F, 0.0F)
      );
      return LayerDefinition.create(meshdefinition, 64, 32);
   }

   public void setupAnim(final EntityRenderState state) {
      super.setupAnim(state);
      this.windCharge.yRot = -state.ageInTicks * 16.0F * (float) (Math.PI / 180.0);
      this.wind.yRot = state.ageInTicks * 16.0F * (float) (Math.PI / 180.0);
   }
}
