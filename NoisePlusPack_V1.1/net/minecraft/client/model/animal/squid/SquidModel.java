package net.minecraft.client.model.animal.squid;

import java.util.Arrays;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.MeshTransformer;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.SquidRenderState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SquidModel extends EntityModel<SquidRenderState> {
   public static final MeshTransformer BABY_TRANSFORMER = MeshTransformer.scaling(0.5F);
   private final ModelPart[] tentacles = new ModelPart[8];

   public SquidModel(ModelPart p_458502_) {
      super(p_458502_);
      Arrays.setAll(this.tentacles, p_455834_ -> p_458502_.getChild(createTentacleName(p_455834_)));
   }

   private static String createTentacleName(int p_451275_) {
      return "tentacle" + p_451275_;
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition meshdefinition = new MeshDefinition();
      PartDefinition partdefinition = meshdefinition.getRoot();
      CubeDeformation cubedeformation = new CubeDeformation(0.02F);
      int i = -16;
      partdefinition.addOrReplaceChild(
         "body", CubeListBuilder.create().texOffs(0, 0).addBox(-6.0F, -8.0F, -6.0F, 12.0F, 16.0F, 12.0F, cubedeformation), PartPose.offset(0.0F, 8.0F, 0.0F)
      );
      int j = 8;
      CubeListBuilder cubelistbuilder = CubeListBuilder.create().texOffs(48, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 18.0F, 2.0F);

      for (int k = 0; k < 8; k++) {
         double d0 = k * Math.PI * 2.0 / 8.0;
         float f = (float)Math.cos(d0) * 5.0F;
         float f1 = 15.0F;
         float f2 = (float)Math.sin(d0) * 5.0F;
         d0 = k * Math.PI * -2.0 / 8.0 + (Math.PI / 2);
         float f3 = (float)d0;
         partdefinition.addOrReplaceChild(createTentacleName(k), cubelistbuilder, PartPose.offsetAndRotation(f, 15.0F, f2, 0.0F, f3, 0.0F));
      }

      return LayerDefinition.create(meshdefinition, 64, 32);
   }

   public void setupAnim(SquidRenderState p_460603_) {
      super.setupAnim(p_460603_);

      for (ModelPart modelpart : this.tentacles) {
         modelpart.xRot = p_460603_.tentacleAngle;
      }
   }
}
