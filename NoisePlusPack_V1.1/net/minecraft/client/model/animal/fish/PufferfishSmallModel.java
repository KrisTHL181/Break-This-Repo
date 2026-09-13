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
public class PufferfishSmallModel extends EntityModel<EntityRenderState> {
   private final ModelPart leftFin;
   private final ModelPart rightFin;

   public PufferfishSmallModel(ModelPart p_460207_) {
      super(p_460207_);
      this.leftFin = p_460207_.getChild("left_fin");
      this.rightFin = p_460207_.getChild("right_fin");
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition meshdefinition = new MeshDefinition();
      PartDefinition partdefinition = meshdefinition.getRoot();
      int i = 23;
      partdefinition.addOrReplaceChild(
         "body", CubeListBuilder.create().texOffs(0, 27).addBox(-1.5F, -2.0F, -1.5F, 3.0F, 2.0F, 3.0F), PartPose.offset(0.0F, 23.0F, 0.0F)
      );
      partdefinition.addOrReplaceChild(
         "right_eye", CubeListBuilder.create().texOffs(24, 6).addBox(-1.5F, 0.0F, -1.5F, 1.0F, 1.0F, 1.0F), PartPose.offset(0.0F, 20.0F, 0.0F)
      );
      partdefinition.addOrReplaceChild(
         "left_eye", CubeListBuilder.create().texOffs(28, 6).addBox(0.5F, 0.0F, -1.5F, 1.0F, 1.0F, 1.0F), PartPose.offset(0.0F, 20.0F, 0.0F)
      );
      partdefinition.addOrReplaceChild(
         "back_fin", CubeListBuilder.create().texOffs(-3, 0).addBox(-1.5F, 0.0F, 0.0F, 3.0F, 0.0F, 3.0F), PartPose.offset(0.0F, 22.0F, 1.5F)
      );
      partdefinition.addOrReplaceChild(
         "right_fin", CubeListBuilder.create().texOffs(25, 0).addBox(-1.0F, 0.0F, 0.0F, 1.0F, 0.0F, 2.0F), PartPose.offset(-1.5F, 22.0F, -1.5F)
      );
      partdefinition.addOrReplaceChild(
         "left_fin", CubeListBuilder.create().texOffs(25, 0).addBox(0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 2.0F), PartPose.offset(1.5F, 22.0F, -1.5F)
      );
      return LayerDefinition.create(meshdefinition, 32, 32);
   }

   public void setupAnim(EntityRenderState p_458126_) {
      super.setupAnim(p_458126_);
      this.rightFin.zRot = -0.2F + 0.4F * Mth.sin(p_458126_.ageInTicks * 0.2F);
      this.leftFin.zRot = 0.2F - 0.4F * Mth.sin(p_458126_.ageInTicks * 0.2F);
   }
}
