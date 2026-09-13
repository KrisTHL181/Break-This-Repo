package net.minecraft.client.model.monster.ghast;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartNames;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.MeshTransformer;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.GhastRenderState;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

public class GhastModel extends EntityModel<GhastRenderState> {
   private final ModelPart[] tentacles = new ModelPart[9];

   public GhastModel(final ModelPart root) {
      super(root);

      for (int i = 0; i < this.tentacles.length; i++) {
         this.tentacles[i] = root.getChild(PartNames.tentacle(i));
      }
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition mesh = new MeshDefinition();
      PartDefinition root = mesh.getRoot();
      root.addOrReplaceChild(
         "body", CubeListBuilder.create().texOffs(0, 0).addBox(-8.0F, -8.0F, -8.0F, 16.0F, 16.0F, 16.0F), PartPose.offset(0.0F, 17.6F, 0.0F)
      );
      RandomSource random = RandomSource.createThreadLocalInstance(1660L);

      for (int i = 0; i < 9; i++) {
         float xo = ((i % 3 - i / 3 % 2 * 0.5F + 0.25F) / 2.0F * 2.0F - 1.0F) * 5.0F;
         float yo = (i / 3 / 2.0F * 2.0F - 1.0F) * 5.0F;
         int len = random.nextInt(7) + 8;
         root.addOrReplaceChild(
            PartNames.tentacle(i), CubeListBuilder.create().texOffs(0, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, len, 2.0F), PartPose.offset(xo, 24.6F, yo)
         );
      }

      return LayerDefinition.create(mesh, 64, 32).apply(MeshTransformer.scaling(4.5F));
   }

   public void setupAnim(final GhastRenderState state) {
      super.setupAnim(state);
      animateTentacles(state, this.tentacles);
   }

   public static void animateTentacles(final EntityRenderState state, final ModelPart[] tentacles) {
      for (int i = 0; i < tentacles.length; i++) {
         tentacles[i].xRot = 0.2F * Mth.sin(state.ageInTicks * 0.3F + i) + 0.4F;
      }
   }
}
