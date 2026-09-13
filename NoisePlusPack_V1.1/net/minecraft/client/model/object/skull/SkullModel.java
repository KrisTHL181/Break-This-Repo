package net.minecraft.client.model.object.skull;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SkullModel extends SkullModelBase {
   protected final ModelPart head;

   public SkullModel(ModelPart p_461005_) {
      super(p_461005_);
      this.head = p_461005_.getChild("head");
   }

   public static MeshDefinition createHeadModel() {
      MeshDefinition meshdefinition = new MeshDefinition();
      PartDefinition partdefinition = meshdefinition.getRoot();
      partdefinition.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F), PartPose.ZERO);
      return meshdefinition;
   }

   public static LayerDefinition createHumanoidHeadLayer() {
      MeshDefinition meshdefinition = createHeadModel();
      PartDefinition partdefinition = meshdefinition.getRoot();
      partdefinition.getChild("head")
         .addOrReplaceChild(
            "hat", CubeListBuilder.create().texOffs(32, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.25F)), PartPose.ZERO
         );
      return LayerDefinition.create(meshdefinition, 64, 64);
   }

   public static LayerDefinition createMobHeadLayer() {
      MeshDefinition meshdefinition = createHeadModel();
      return LayerDefinition.create(meshdefinition, 64, 32);
   }

   public void setupAnim(SkullModelBase.State p_455338_) {
      super.setupAnim(p_455338_);
      this.head.yRot = p_455338_.yRot * (float) (Math.PI / 180.0);
      this.head.xRot = p_455338_.xRot * (float) (Math.PI / 180.0);
   }
}
