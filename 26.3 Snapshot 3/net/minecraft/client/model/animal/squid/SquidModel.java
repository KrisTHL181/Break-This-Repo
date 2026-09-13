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

public class SquidModel extends EntityModel<SquidRenderState> {
   public static final MeshTransformer BABY_TRANSFORMER = MeshTransformer.scaling(0.5F);
   private final ModelPart[] tentacles = new ModelPart[8];

   public SquidModel(final ModelPart root) {
      super(root);
      Arrays.setAll(this.tentacles, i -> root.getChild(createTentacleName(i)));
   }

   protected static String createTentacleName(final int i) {
      return "tentacle" + i;
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition mesh = new MeshDefinition();
      PartDefinition root = mesh.getRoot();
      CubeDeformation g = new CubeDeformation(0.02F);
      int yoffs = -16;
      root.addOrReplaceChild(
         "body", CubeListBuilder.create().texOffs(0, 0).addBox(-6.0F, -8.0F, -6.0F, 12.0F, 16.0F, 12.0F, g), PartPose.offset(0.0F, 8.0F, 0.0F)
      );
      int tentacleCount = 8;
      CubeListBuilder tentacle = CubeListBuilder.create().texOffs(48, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 18.0F, 2.0F);

      for (int i = 0; i < 8; i++) {
         double angle = i * Math.PI * 2.0 / 8.0;
         float x = (float)Math.cos(angle) * 5.0F;
         float y = 15.0F;
         float z = (float)Math.sin(angle) * 5.0F;
         angle = i * Math.PI * -2.0 / 8.0 + (Math.PI / 2);
         float yRot = (float)angle;
         root.addOrReplaceChild(createTentacleName(i), tentacle, PartPose.offsetAndRotation(x, 15.0F, z, 0.0F, yRot, 0.0F));
      }

      return LayerDefinition.create(mesh, 64, 32);
   }

   public void setupAnim(final SquidRenderState state) {
      super.setupAnim(state);

      for (ModelPart tentacle : this.tentacles) {
         tentacle.xRot = state.tentacleAngle;
      }
   }
}
