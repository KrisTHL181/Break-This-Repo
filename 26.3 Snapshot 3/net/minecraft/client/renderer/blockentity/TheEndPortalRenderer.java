package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Transformation;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.state.EndPortalRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.level.block.entity.TheEndPortalBlockEntity;
import org.joml.Vector3f;

public class TheEndPortalRenderer extends AbstractEndPortalRenderer<TheEndPortalBlockEntity, EndPortalRenderState> {
   private static final float BOTTOM = 0.375F;
   private static final float TOP = 0.75F;
   public static final Transformation TRANSFORMATION = new Transformation(new Vector3f(0.0F, 0.375F, 0.0F), null, new Vector3f(1.0F, 0.375F, 1.0F), null);

   public EndPortalRenderState createRenderState() {
      return new EndPortalRenderState();
   }

   public void submit(
      final EndPortalRenderState state, final PoseStack poseStack, final SubmitNodeCollector submitNodeCollector, final CameraRenderState camera
   ) {
      poseStack.pushPose();
      poseStack.mulPose(TRANSFORMATION);
      submitCube(state.facesToShow, RenderTypes.endPortal(), poseStack, submitNodeCollector);
      poseStack.popPose();
   }
}
