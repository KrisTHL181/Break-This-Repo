package net.minecraft.client.model.animal.turtle;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.TurtleRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;

public class AdultTurtleModel extends TurtleModel {
   private static final String EGG_BELLY = "egg_belly";
   private final ModelPart eggBelly;

   public AdultTurtleModel(final ModelPart root) {
      super(root, RenderTypes::entityCutout);
      this.eggBelly = root.getChild("egg_belly");
   }

   @Override
   public void setupAnim(final TurtleRenderState state) {
      super.setupAnim(state);
      this.eggBelly.visible = state.hasEgg;
      if (this.eggBelly.visible) {
         this.root.y--;
      }
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition mesh = new MeshDefinition();
      PartDefinition root = mesh.getRoot();
      root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(3, 0).addBox(-3.0F, -1.0F, -3.0F, 6.0F, 5.0F, 6.0F), PartPose.offset(0.0F, 19.0F, -10.0F));
      root.addOrReplaceChild(
         "body",
         CubeListBuilder.create()
            .texOffs(7, 37)
            .addBox("shell", -9.5F, 3.0F, -10.0F, 19.0F, 20.0F, 6.0F)
            .texOffs(31, 1)
            .addBox("belly", -5.5F, 3.0F, -13.0F, 11.0F, 18.0F, 3.0F),
         PartPose.offsetAndRotation(0.0F, 11.0F, -10.0F, (float) (Math.PI / 2), 0.0F, 0.0F)
      );
      root.addOrReplaceChild(
         "egg_belly",
         CubeListBuilder.create().texOffs(70, 33).addBox(-4.5F, 3.0F, -14.0F, 9.0F, 18.0F, 1.0F),
         PartPose.offsetAndRotation(0.0F, 11.0F, -10.0F, (float) (Math.PI / 2), 0.0F, 0.0F)
      );
      int legHeight = 1;
      root.addOrReplaceChild(
         "right_hind_leg", CubeListBuilder.create().texOffs(1, 23).addBox(-2.0F, 0.0F, 0.0F, 4.0F, 1.0F, 10.0F), PartPose.offset(-3.5F, 22.0F, 11.0F)
      );
      root.addOrReplaceChild(
         "left_hind_leg", CubeListBuilder.create().texOffs(1, 12).addBox(-2.0F, 0.0F, 0.0F, 4.0F, 1.0F, 10.0F), PartPose.offset(3.5F, 22.0F, 11.0F)
      );
      root.addOrReplaceChild(
         "right_front_leg", CubeListBuilder.create().texOffs(27, 30).addBox(-13.0F, 0.0F, -2.0F, 13.0F, 1.0F, 5.0F), PartPose.offset(-5.0F, 21.0F, -4.0F)
      );
      root.addOrReplaceChild(
         "left_front_leg", CubeListBuilder.create().texOffs(27, 24).addBox(0.0F, 0.0F, -2.0F, 13.0F, 1.0F, 5.0F), PartPose.offset(5.0F, 21.0F, -4.0F)
      );
      return LayerDefinition.create(mesh, 128, 64);
   }
}
