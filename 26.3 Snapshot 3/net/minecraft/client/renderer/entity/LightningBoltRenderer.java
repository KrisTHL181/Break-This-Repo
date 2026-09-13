package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.LightningBoltRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LightningBolt;
import org.joml.Matrix4fc;

public class LightningBoltRenderer extends EntityRenderer<LightningBolt, LightningBoltRenderState> {
   private static final float BOLT_RED = 0.45F;
   private static final float BOLT_GREEN = 0.45F;
   private static final float BOLT_BLUE = 0.5F;
   private static final int SEGMENT_COUNT = 8;
   private static final int LAYER_COUNT = 4;
   private static final int BRANCH_COUNT = 3;
   private static final int BRANCH_SEGMENT_COUNT = 3;

   public LightningBoltRenderer(final EntityRendererProvider.Context context) {
      super(context);
   }

   public void submit(
      final LightningBoltRenderState state, final PoseStack poseStack, final SubmitNodeCollector submitNodeCollector, final CameraRenderState camera
   ) {
      float[] xOffsets = new float[8];
      float[] zOffsets = new float[8];
      float xOffset = 0.0F;
      float zOffset = 0.0F;
      RandomSource random = RandomSource.createThreadLocalInstance(state.seed);

      for (int heightSegmentIndex = 7; heightSegmentIndex >= 0; heightSegmentIndex--) {
         xOffsets[heightSegmentIndex] = xOffset;
         zOffsets[heightSegmentIndex] = zOffset;
         xOffset += random.nextInt(11) - 5;
         zOffset += random.nextInt(11) - 5;
      }

      float finalXOff = xOffset;
      float finalZOff = zOffset;
      submitNodeCollector.submitCustomGeometry(
         poseStack,
         RenderTypes.lightning(),
         (pose, buffer) -> {
            Matrix4fc poseMatrix = pose.pose();

            for (int layer = 0; layer < 4; layer++) {
               RandomSource randomx = RandomSource.createThreadLocalInstance(state.seed);

               for (int branchNumber = 0; branchNumber < 3; branchNumber++) {
                  boolean isTrunkBranch = branchNumber == 0;
                  int branchStartSegment = 7 - branchNumber;
                  int branchEndSegment = isTrunkBranch ? 0 : branchStartSegment - 3 + 1;
                  float segmentStartX = xOffsets[branchStartSegment] - finalXOff;
                  float segmentStartZ = zOffsets[branchStartSegment] - finalZOff;

                  for (int currentSegment = branchStartSegment; currentSegment >= branchEndSegment; currentSegment--) {
                     float segmentEndX = segmentStartX;
                     float segmentEndZ = segmentStartZ;
                     if (isTrunkBranch) {
                        segmentStartX += randomx.nextInt(11) - 5;
                        segmentStartZ += randomx.nextInt(11) - 5;
                     } else {
                        segmentStartX += randomx.nextInt(31) - 15;
                        segmentStartZ += randomx.nextInt(31) - 15;
                     }

                     float topRadius = 0.1F + layer * 0.2F;
                     float bottomRadius = topRadius;
                     if (isTrunkBranch) {
                        topRadius *= currentSegment * 0.1F + 1.0F;
                        bottomRadius *= (currentSegment - 1) * 0.1F + 1.0F;
                     }

                     quad(
                        poseMatrix,
                        buffer,
                        segmentStartX,
                        segmentStartZ,
                        segmentEndX,
                        segmentEndZ,
                        currentSegment,
                        topRadius,
                        bottomRadius,
                        false,
                        false,
                        true,
                        false
                     );
                     quad(
                        poseMatrix,
                        buffer,
                        segmentStartX,
                        segmentStartZ,
                        segmentEndX,
                        segmentEndZ,
                        currentSegment,
                        topRadius,
                        bottomRadius,
                        true,
                        false,
                        true,
                        true
                     );
                     quad(
                        poseMatrix,
                        buffer,
                        segmentStartX,
                        segmentStartZ,
                        segmentEndX,
                        segmentEndZ,
                        currentSegment,
                        topRadius,
                        bottomRadius,
                        true,
                        true,
                        false,
                        true
                     );
                     quad(
                        poseMatrix,
                        buffer,
                        segmentStartX,
                        segmentStartZ,
                        segmentEndX,
                        segmentEndZ,
                        currentSegment,
                        topRadius,
                        bottomRadius,
                        false,
                        true,
                        false,
                        false
                     );
                  }
               }
            }
         }
      );
   }

   private static void quad(
      final Matrix4fc pose,
      final VertexConsumer buffer,
      final float segmentStartX,
      final float segmentStartZ,
      final float segmentEndX,
      final float segmentEndZ,
      final int currentSegment,
      final float topRadius,
      final float bottomRadius,
      final boolean rightXPositive,
      final boolean rightZPositive,
      final boolean leftXPositive,
      final boolean leftZPositive
   ) {
      buffer.addVertex(
            pose,
            segmentStartX + (rightXPositive ? bottomRadius : -bottomRadius),
            currentSegment * 16,
            segmentStartZ + (rightZPositive ? bottomRadius : -bottomRadius)
         )
         .setColor(0.45F, 0.45F, 0.5F, 0.3F);
      buffer.addVertex(
            pose, segmentEndX + (rightXPositive ? topRadius : -topRadius), (currentSegment + 1) * 16, segmentEndZ + (rightZPositive ? topRadius : -topRadius)
         )
         .setColor(0.45F, 0.45F, 0.5F, 0.3F);
      buffer.addVertex(
            pose, segmentEndX + (leftXPositive ? topRadius : -topRadius), (currentSegment + 1) * 16, segmentEndZ + (leftZPositive ? topRadius : -topRadius)
         )
         .setColor(0.45F, 0.45F, 0.5F, 0.3F);
      buffer.addVertex(
            pose,
            segmentStartX + (leftXPositive ? bottomRadius : -bottomRadius),
            currentSegment * 16,
            segmentStartZ + (leftZPositive ? bottomRadius : -bottomRadius)
         )
         .setColor(0.45F, 0.45F, 0.5F, 0.3F);
   }

   public LightningBoltRenderState createRenderState() {
      return new LightningBoltRenderState();
   }

   public void extractRenderState(final LightningBolt entity, final LightningBoltRenderState state, final float partialTicks) {
      super.extractRenderState(entity, state, partialTicks);
      state.seed = entity.seed;
   }

   protected boolean affectedByCulling(final LightningBolt entity) {
      return false;
   }
}
