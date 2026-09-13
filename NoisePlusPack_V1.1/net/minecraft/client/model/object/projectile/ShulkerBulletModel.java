package net.minecraft.client.model.object.projectile;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.ShulkerBulletRenderState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ShulkerBulletModel extends EntityModel<ShulkerBulletRenderState> {
   private static final String MAIN = "main";
   private final ModelPart main;

   public ShulkerBulletModel(ModelPart p_454476_) {
      super(p_454476_);
      this.main = p_454476_.getChild("main");
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition meshdefinition = new MeshDefinition();
      PartDefinition partdefinition = meshdefinition.getRoot();
      partdefinition.addOrReplaceChild(
         "main",
         CubeListBuilder.create()
            .texOffs(0, 0)
            .addBox(-4.0F, -4.0F, -1.0F, 8.0F, 8.0F, 2.0F)
            .texOffs(0, 10)
            .addBox(-1.0F, -4.0F, -4.0F, 2.0F, 8.0F, 8.0F)
            .texOffs(20, 0)
            .addBox(-4.0F, -1.0F, -4.0F, 8.0F, 2.0F, 8.0F),
         PartPose.ZERO
      );
      return LayerDefinition.create(meshdefinition, 64, 32);
   }

   public void setupAnim(ShulkerBulletRenderState p_451317_) {
      super.setupAnim(p_451317_);
      this.main.yRot = p_451317_.yRot * (float) (Math.PI / 180.0);
      this.main.xRot = p_451317_.xRot * (float) (Math.PI / 180.0);
   }
}
