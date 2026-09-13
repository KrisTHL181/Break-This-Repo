package net.minecraft.client.model.player;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Quaternionf;

@OnlyIn(Dist.CLIENT)
public class PlayerCapeModel extends PlayerModel {
   private static final String CAPE = "cape";
   private final ModelPart cape = this.body.getChild("cape");

   public PlayerCapeModel(ModelPart p_453424_) {
      super(p_453424_, false);
   }

   public static LayerDefinition createCapeLayer() {
      MeshDefinition meshdefinition = PlayerModel.createMesh(CubeDeformation.NONE, false);
      PartDefinition partdefinition = meshdefinition.getRoot().clearRecursively();
      PartDefinition partdefinition1 = partdefinition.getChild("body");
      partdefinition1.addOrReplaceChild(
         "cape",
         CubeListBuilder.create().texOffs(0, 0).addBox(-5.0F, 0.0F, -1.0F, 10.0F, 16.0F, 1.0F, CubeDeformation.NONE, 1.0F, 0.5F),
         PartPose.offsetAndRotation(0.0F, 0.0F, 2.0F, 0.0F, (float) Math.PI, 0.0F)
      );
      return LayerDefinition.create(meshdefinition, 64, 64);
   }

   @Override
   public void setupAnim(AvatarRenderState p_450931_) {
      super.setupAnim(p_450931_);
      this.cape
         .rotateBy(
            new Quaternionf()
               .rotateY((float) -Math.PI)
               .rotateX((6.0F + p_450931_.capeLean / 2.0F + p_450931_.capeFlap) * (float) (Math.PI / 180.0))
               .rotateZ(p_450931_.capeLean2 / 2.0F * (float) (Math.PI / 180.0))
               .rotateY((180.0F - p_450931_.capeLean2 / 2.0F) * (float) (Math.PI / 180.0))
         );
   }
}
