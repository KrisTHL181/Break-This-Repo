package net.minecraft.client.model.object.chest;

import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ChestModel extends Model<Float> {
   private static final String BOTTOM = "bottom";
   private static final String LID = "lid";
   private static final String LOCK = "lock";
   private final ModelPart lid;
   private final ModelPart lock;

   public ChestModel(ModelPart p_456702_) {
      super(p_456702_, RenderTypes::entitySolid);
      this.lid = p_456702_.getChild("lid");
      this.lock = p_456702_.getChild("lock");
   }

   public static LayerDefinition createSingleBodyLayer() {
      MeshDefinition meshdefinition = new MeshDefinition();
      PartDefinition partdefinition = meshdefinition.getRoot();
      partdefinition.addOrReplaceChild("bottom", CubeListBuilder.create().texOffs(0, 19).addBox(1.0F, 0.0F, 1.0F, 14.0F, 10.0F, 14.0F), PartPose.ZERO);
      partdefinition.addOrReplaceChild(
         "lid", CubeListBuilder.create().texOffs(0, 0).addBox(1.0F, 0.0F, 0.0F, 14.0F, 5.0F, 14.0F), PartPose.offset(0.0F, 9.0F, 1.0F)
      );
      partdefinition.addOrReplaceChild(
         "lock", CubeListBuilder.create().texOffs(0, 0).addBox(7.0F, -2.0F, 14.0F, 2.0F, 4.0F, 1.0F), PartPose.offset(0.0F, 9.0F, 1.0F)
      );
      return LayerDefinition.create(meshdefinition, 64, 64);
   }

   public static LayerDefinition createDoubleBodyRightLayer() {
      MeshDefinition meshdefinition = new MeshDefinition();
      PartDefinition partdefinition = meshdefinition.getRoot();
      partdefinition.addOrReplaceChild("bottom", CubeListBuilder.create().texOffs(0, 19).addBox(1.0F, 0.0F, 1.0F, 15.0F, 10.0F, 14.0F), PartPose.ZERO);
      partdefinition.addOrReplaceChild(
         "lid", CubeListBuilder.create().texOffs(0, 0).addBox(1.0F, 0.0F, 0.0F, 15.0F, 5.0F, 14.0F), PartPose.offset(0.0F, 9.0F, 1.0F)
      );
      partdefinition.addOrReplaceChild(
         "lock", CubeListBuilder.create().texOffs(0, 0).addBox(15.0F, -2.0F, 14.0F, 1.0F, 4.0F, 1.0F), PartPose.offset(0.0F, 9.0F, 1.0F)
      );
      return LayerDefinition.create(meshdefinition, 64, 64);
   }

   public static LayerDefinition createDoubleBodyLeftLayer() {
      MeshDefinition meshdefinition = new MeshDefinition();
      PartDefinition partdefinition = meshdefinition.getRoot();
      partdefinition.addOrReplaceChild("bottom", CubeListBuilder.create().texOffs(0, 19).addBox(0.0F, 0.0F, 1.0F, 15.0F, 10.0F, 14.0F), PartPose.ZERO);
      partdefinition.addOrReplaceChild(
         "lid", CubeListBuilder.create().texOffs(0, 0).addBox(0.0F, 0.0F, 0.0F, 15.0F, 5.0F, 14.0F), PartPose.offset(0.0F, 9.0F, 1.0F)
      );
      partdefinition.addOrReplaceChild(
         "lock", CubeListBuilder.create().texOffs(0, 0).addBox(0.0F, -2.0F, 14.0F, 1.0F, 4.0F, 1.0F), PartPose.offset(0.0F, 9.0F, 1.0F)
      );
      return LayerDefinition.create(meshdefinition, 64, 64);
   }

   public void setupAnim(Float p_458703_) {
      super.setupAnim(p_458703_);
      this.lid.xRot = -(p_458703_ * (float) (Math.PI / 2));
      this.lock.xRot = this.lid.xRot;
   }
}
