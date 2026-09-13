package net.minecraft.client.model.monster.blaze;

import java.util.Arrays;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;

public class BlazeModel extends EntityModel<LivingEntityRenderState> {
   private final ModelPart[] upperBodyParts;
   private final ModelPart head;

   public BlazeModel(final ModelPart root) {
      super(root);
      this.head = root.getChild("head");
      this.upperBodyParts = new ModelPart[12];
      Arrays.setAll(this.upperBodyParts, i -> root.getChild(getPartName(i)));
   }

   private static String getPartName(final int i) {
      return "part" + i;
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition mesh = new MeshDefinition();
      PartDefinition root = mesh.getRoot();
      root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -4.0F, -4.0F, 8.0F, 8.0F, 8.0F), PartPose.ZERO);
      float angle = 0.0F;
      CubeListBuilder rod = CubeListBuilder.create().texOffs(0, 16).addBox(0.0F, 0.0F, 0.0F, 2.0F, 8.0F, 2.0F);

      for (int i = 0; i < 4; i++) {
         float x = Mth.cos(angle) * 9.0F;
         float y = -2.0F + Mth.cos(i * 2 * 0.25F);
         float z = Mth.sin(angle) * 9.0F;
         root.addOrReplaceChild(getPartName(i), rod, PartPose.offset(x, y, z));
         angle += (float) (Math.PI / 2);
      }

      angle = (float) (Math.PI / 4);

      for (int i = 4; i < 8; i++) {
         float x = Mth.cos(angle) * 7.0F;
         float y = 2.0F + Mth.cos(i * 2 * 0.25F);
         float z = Mth.sin(angle) * 7.0F;
         root.addOrReplaceChild(getPartName(i), rod, PartPose.offset(x, y, z));
         angle += (float) (Math.PI / 2);
      }

      angle = 0.47123894F;

      for (int i = 8; i < 12; i++) {
         float x = Mth.cos(angle) * 5.0F;
         float y = 11.0F + Mth.cos(i * 1.5F * 0.5F);
         float z = Mth.sin(angle) * 5.0F;
         root.addOrReplaceChild(getPartName(i), rod, PartPose.offset(x, y, z));
         angle += (float) (Math.PI / 2);
      }

      return LayerDefinition.create(mesh, 64, 32);
   }

   public void setupAnim(final LivingEntityRenderState state) {
      super.setupAnim(state);
      float angle = state.ageInTicks * (float) Math.PI * -0.1F;

      for (int i = 0; i < 4; i++) {
         this.upperBodyParts[i].y = -2.0F + Mth.cos((i * 2 + state.ageInTicks) * 0.25F);
         this.upperBodyParts[i].x = Mth.cos(angle) * 9.0F;
         this.upperBodyParts[i].z = Mth.sin(angle) * 9.0F;
         angle += (float) (Math.PI / 2);
      }

      angle = (float) (Math.PI / 4) + state.ageInTicks * (float) Math.PI * 0.03F;

      for (int i = 4; i < 8; i++) {
         this.upperBodyParts[i].y = 2.0F + Mth.cos((i * 2 + state.ageInTicks) * 0.25F);
         this.upperBodyParts[i].x = Mth.cos(angle) * 7.0F;
         this.upperBodyParts[i].z = Mth.sin(angle) * 7.0F;
         angle += (float) (Math.PI / 2);
      }

      angle = 0.47123894F + state.ageInTicks * (float) Math.PI * -0.05F;

      for (int i = 8; i < 12; i++) {
         this.upperBodyParts[i].y = 11.0F + Mth.cos((i * 1.5F + state.ageInTicks) * 0.5F);
         this.upperBodyParts[i].x = Mth.cos(angle) * 5.0F;
         this.upperBodyParts[i].z = Mth.sin(angle) * 5.0F;
         angle += (float) (Math.PI / 2);
      }

      this.head.yRot = state.yRot * (float) (Math.PI / 180.0);
      this.head.xRot = state.xRot * (float) (Math.PI / 180.0);
   }
}
