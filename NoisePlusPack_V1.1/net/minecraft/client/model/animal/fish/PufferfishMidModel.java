package net.minecraft.client.model.animal.fish;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class PufferfishMidModel extends EntityModel<EntityRenderState> {
   private final ModelPart leftBlueFin;
   private final ModelPart rightBlueFin;

   public PufferfishMidModel(ModelPart p_459274_) {
      super(p_459274_);
      this.leftBlueFin = p_459274_.getChild("left_blue_fin");
      this.rightBlueFin = p_459274_.getChild("right_blue_fin");
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition meshdefinition = new MeshDefinition();
      PartDefinition partdefinition = meshdefinition.getRoot();
      int i = 22;
      partdefinition.addOrReplaceChild(
         "body", CubeListBuilder.create().texOffs(12, 22).addBox(-2.5F, -5.0F, -2.5F, 5.0F, 5.0F, 5.0F), PartPose.offset(0.0F, 22.0F, 0.0F)
      );
      partdefinition.addOrReplaceChild(
         "right_blue_fin", CubeListBuilder.create().texOffs(24, 0).addBox(-2.0F, 0.0F, 0.0F, 2.0F, 0.0F, 2.0F), PartPose.offset(-2.5F, 18.0F, -1.5F)
      );
      partdefinition.addOrReplaceChild(
         "left_blue_fin", CubeListBuilder.create().texOffs(24, 3).addBox(0.0F, 0.0F, 0.0F, 2.0F, 0.0F, 2.0F), PartPose.offset(2.5F, 18.0F, -1.5F)
      );
      partdefinition.addOrReplaceChild(
         "top_front_fin",
         CubeListBuilder.create().texOffs(19, 17).addBox(-2.5F, -1.0F, 0.0F, 5.0F, 1.0F, 0.0F),
         PartPose.offsetAndRotation(0.0F, 17.0F, -2.5F, (float) (Math.PI / 4), 0.0F, 0.0F)
      );
      partdefinition.addOrReplaceChild(
         "top_back_fin",
         CubeListBuilder.create().texOffs(11, 17).addBox(-2.5F, -1.0F, 0.0F, 5.0F, 1.0F, 0.0F),
         PartPose.offsetAndRotation(0.0F, 17.0F, 2.5F, (float) (-Math.PI / 4), 0.0F, 0.0F)
      );
      partdefinition.addOrReplaceChild(
         "right_front_fin",
         CubeListBuilder.create().texOffs(5, 17).addBox(-1.0F, -5.0F, 0.0F, 1.0F, 5.0F, 0.0F),
         PartPose.offsetAndRotation(-2.5F, 22.0F, -2.5F, 0.0F, (float) (-Math.PI / 4), 0.0F)
      );
      partdefinition.addOrReplaceChild(
         "right_back_fin",
         CubeListBuilder.create().texOffs(9, 17).addBox(-1.0F, -5.0F, 0.0F, 1.0F, 5.0F, 0.0F),
         PartPose.offsetAndRotation(-2.5F, 22.0F, 2.5F, 0.0F, (float) (Math.PI / 4), 0.0F)
      );
      partdefinition.addOrReplaceChild(
         "left_back_fin",
         CubeListBuilder.create().texOffs(1, 17).addBox(0.0F, -5.0F, 0.0F, 1.0F, 5.0F, 0.0F),
         PartPose.offsetAndRotation(2.5F, 22.0F, 2.5F, 0.0F, (float) (-Math.PI / 4), 0.0F)
      );
      partdefinition.addOrReplaceChild(
         "left_front_fin",
         CubeListBuilder.create().texOffs(1, 17).addBox(0.0F, -5.0F, 0.0F, 1.0F, 5.0F, 0.0F),
         PartPose.offsetAndRotation(2.5F, 22.0F, -2.5F, 0.0F, (float) (Math.PI / 4), 0.0F)
      );
      partdefinition.addOrReplaceChild(
         "bottom_back_fin",
         CubeListBuilder.create().texOffs(18, 20).addBox(0.0F, 0.0F, 0.0F, 5.0F, 1.0F, 0.0F),
         PartPose.offsetAndRotation(-2.5F, 22.0F, 2.5F, (float) (Math.PI / 4), 0.0F, 0.0F)
      );
      partdefinition.addOrReplaceChild(
         "bottom_front_fin",
         CubeListBuilder.create().texOffs(17, 19).addBox(-2.5F, 0.0F, 0.0F, 5.0F, 1.0F, 1.0F),
         PartPose.offsetAndRotation(0.0F, 22.0F, -2.5F, (float) (-Math.PI / 4), 0.0F, 0.0F)
      );
      return LayerDefinition.create(meshdefinition, 32, 32);
   }

   public void setupAnim(EntityRenderState p_451326_) {
      super.setupAnim(p_451326_);
      this.rightBlueFin.zRot = -0.2F + 0.4F * Mth.sin(p_451326_.ageInTicks * 0.2F);
      this.leftBlueFin.zRot = 0.2F - 0.4F * Mth.sin(p_451326_.ageInTicks * 0.2F);
   }
}
