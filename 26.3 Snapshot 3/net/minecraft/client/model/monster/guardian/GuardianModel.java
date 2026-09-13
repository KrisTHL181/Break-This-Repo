package net.minecraft.client.model.monster.guardian;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.MeshTransformer;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.GuardianRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class GuardianModel extends EntityModel<GuardianRenderState> {
   public static final MeshTransformer ELDER_GUARDIAN_SCALE = MeshTransformer.scaling(2.35F);
   private static final float[] SPIKE_X_ROT = new float[]{1.75F, 0.25F, 0.0F, 0.0F, 0.5F, 0.5F, 0.5F, 0.5F, 1.25F, 0.75F, 0.0F, 0.0F};
   private static final float[] SPIKE_Y_ROT = new float[]{0.0F, 0.0F, 0.0F, 0.0F, 0.25F, 1.75F, 1.25F, 0.75F, 0.0F, 0.0F, 0.0F, 0.0F};
   private static final float[] SPIKE_Z_ROT = new float[]{0.0F, 0.0F, 0.25F, 1.75F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.75F, 1.25F};
   private static final float[] SPIKE_X = new float[]{0.0F, 0.0F, 8.0F, -8.0F, -8.0F, 8.0F, 8.0F, -8.0F, 0.0F, 0.0F, 8.0F, -8.0F};
   private static final float[] SPIKE_Y = new float[]{-8.0F, -8.0F, -8.0F, -8.0F, 0.0F, 0.0F, 0.0F, 0.0F, 8.0F, 8.0F, 8.0F, 8.0F};
   private static final float[] SPIKE_Z = new float[]{8.0F, -8.0F, 0.0F, 0.0F, -8.0F, -8.0F, 8.0F, 8.0F, 8.0F, -8.0F, 0.0F, 0.0F};
   private static final String EYE = "eye";
   private static final String TAIL_0 = "tail0";
   private static final String TAIL_1 = "tail1";
   private static final String TAIL_2 = "tail2";
   private final ModelPart head;
   private final ModelPart eye;
   private final ModelPart[] spikeParts = new ModelPart[12];
   private final ModelPart[] tailParts;

   public GuardianModel(final ModelPart root) {
      super(root);
      this.head = root.getChild("head");

      for (int i = 0; i < this.spikeParts.length; i++) {
         this.spikeParts[i] = this.head.getChild(createSpikeName(i));
      }

      this.eye = this.head.getChild("eye");
      this.tailParts = new ModelPart[3];
      this.tailParts[0] = this.head.getChild("tail0");
      this.tailParts[1] = this.tailParts[0].getChild("tail1");
      this.tailParts[2] = this.tailParts[1].getChild("tail2");
   }

   private static String createSpikeName(final int i) {
      return "spike" + i;
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition mesh = new MeshDefinition();
      PartDefinition root = mesh.getRoot();
      PartDefinition head = root.addOrReplaceChild(
         "head",
         CubeListBuilder.create()
            .texOffs(0, 0)
            .addBox(-6.0F, 10.0F, -8.0F, 12.0F, 12.0F, 16.0F)
            .texOffs(0, 28)
            .addBox(-8.0F, 10.0F, -6.0F, 2.0F, 12.0F, 12.0F)
            .texOffs(0, 28)
            .addBox(6.0F, 10.0F, -6.0F, 2.0F, 12.0F, 12.0F, true)
            .texOffs(16, 40)
            .addBox(-6.0F, 8.0F, -6.0F, 12.0F, 2.0F, 12.0F)
            .texOffs(16, 40)
            .addBox(-6.0F, 22.0F, -6.0F, 12.0F, 2.0F, 12.0F),
         PartPose.ZERO
      );
      CubeListBuilder spike = CubeListBuilder.create().texOffs(0, 0).addBox(-1.0F, -4.5F, -1.0F, 2.0F, 9.0F, 2.0F);

      for (int i = 0; i < 12; i++) {
         float x = getSpikeX(i, 0.0F, 0.0F);
         float y = getSpikeY(i, 0.0F, 0.0F);
         float z = getSpikeZ(i, 0.0F, 0.0F);
         float xRot = (float) Math.PI * SPIKE_X_ROT[i];
         float yRot = (float) Math.PI * SPIKE_Y_ROT[i];
         float zRot = (float) Math.PI * SPIKE_Z_ROT[i];
         head.addOrReplaceChild(createSpikeName(i), spike, PartPose.offsetAndRotation(x, y, z, xRot, yRot, zRot));
      }

      head.addOrReplaceChild("eye", CubeListBuilder.create().texOffs(8, 0).addBox(-1.0F, 15.0F, 0.0F, 2.0F, 2.0F, 1.0F), PartPose.offset(0.0F, 0.0F, -8.25F));
      PartDefinition tailPart0 = head.addOrReplaceChild(
         "tail0", CubeListBuilder.create().texOffs(40, 0).addBox(-2.0F, 14.0F, 7.0F, 4.0F, 4.0F, 8.0F), PartPose.ZERO
      );
      PartDefinition tailPart1 = tailPart0.addOrReplaceChild(
         "tail1", CubeListBuilder.create().texOffs(0, 54).addBox(0.0F, 14.0F, 0.0F, 3.0F, 3.0F, 7.0F), PartPose.offset(-1.5F, 0.5F, 14.0F)
      );
      tailPart1.addOrReplaceChild(
         "tail2",
         CubeListBuilder.create().texOffs(41, 32).addBox(0.0F, 14.0F, 0.0F, 2.0F, 2.0F, 6.0F).texOffs(25, 19).addBox(1.0F, 10.5F, 3.0F, 1.0F, 9.0F, 9.0F),
         PartPose.offset(0.5F, 0.5F, 6.0F)
      );
      return LayerDefinition.create(mesh, 64, 64);
   }

   public static LayerDefinition createElderGuardianLayer() {
      return createBodyLayer().apply(ELDER_GUARDIAN_SCALE);
   }

   public void setupAnim(final GuardianRenderState state) {
      super.setupAnim(state);
      this.head.yRot = state.yRot * (float) (Math.PI / 180.0);
      this.head.xRot = state.xRot * (float) (Math.PI / 180.0);
      float withdrawal = (1.0F - state.spikesAnimation) * 0.55F;
      this.setupSpikes(state.ageInTicks, withdrawal);
      if (state.lookAtPosition != null && state.lookDirection != null) {
         double dy = state.lookAtPosition.y - state.eyePosition.y;
         if (dy > 0.0) {
            this.eye.y = 0.0F;
         } else {
            this.eye.y = 1.0F;
         }

         Vec3 viewVector = state.lookDirection;
         viewVector = new Vec3(viewVector.x, 0.0, viewVector.z);
         Vec3 delta = new Vec3(state.eyePosition.x - state.lookAtPosition.x, 0.0, state.eyePosition.z - state.lookAtPosition.z)
            .normalize()
            .yRot((float) (Math.PI / 2));
         double dot = viewVector.dot(delta);
         this.eye.x = Mth.sqrt((float)Math.abs(dot)) * 2.0F * (float)Math.signum(dot);
      }

      this.eye.visible = true;
      float swim = state.tailAnimation;
      this.tailParts[0].yRot = Mth.sin(swim) * (float) Math.PI * 0.05F;
      this.tailParts[1].yRot = Mth.sin(swim) * (float) Math.PI * 0.1F;
      this.tailParts[2].yRot = Mth.sin(swim) * (float) Math.PI * 0.15F;
   }

   private void setupSpikes(final float ageInTicks, final float withdrawal) {
      for (int i = 0; i < 12; i++) {
         this.spikeParts[i].x = getSpikeX(i, ageInTicks, withdrawal);
         this.spikeParts[i].y = getSpikeY(i, ageInTicks, withdrawal);
         this.spikeParts[i].z = getSpikeZ(i, ageInTicks, withdrawal);
      }
   }

   private static float getSpikeOffset(final int spike, final float ageInTicks, final float withdrawal) {
      return 1.0F + Mth.cos(ageInTicks * 1.5F + spike) * 0.01F - withdrawal;
   }

   private static float getSpikeX(final int spike, final float ageInTicks, final float withdrawal) {
      return SPIKE_X[spike] * getSpikeOffset(spike, ageInTicks, withdrawal);
   }

   private static float getSpikeY(final int spike, final float ageInTicks, final float withdrawal) {
      return 16.0F + SPIKE_Y[spike] * getSpikeOffset(spike, ageInTicks, withdrawal);
   }

   private static float getSpikeZ(final int spike, final float ageInTicks, final float withdrawal) {
      return SPIKE_Z[spike] * getSpikeOffset(spike, ageInTicks, withdrawal);
   }
}
