package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.phantom.PhantomModel;
import net.minecraft.client.renderer.entity.layers.PhantomEyesLayer;
import net.minecraft.client.renderer.entity.state.PhantomRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.monster.Phantom;

public class PhantomRenderer extends MobRenderer<Phantom, PhantomRenderState, PhantomModel> {
   private static final Identifier PHANTOM_LOCATION = Identifier.withDefaultNamespace("textures/entity/phantom/phantom.png");

   public PhantomRenderer(final EntityRendererProvider.Context context) {
      super(context, new PhantomModel(context.bakeLayer(ModelLayers.PHANTOM)), 0.75F);
      this.addLayer(new PhantomEyesLayer(this));
   }

   public Identifier getTextureLocation(final PhantomRenderState state) {
      return PHANTOM_LOCATION;
   }

   public PhantomRenderState createRenderState() {
      return new PhantomRenderState();
   }

   public void extractRenderState(final Phantom entity, final PhantomRenderState state, final float partialTicks) {
      super.extractRenderState(entity, state, partialTicks);
      state.flapTime = entity.getUniqueFlapTickOffset() + state.ageInTicks;
      state.size = entity.getPhantomSize();
   }

   protected void scale(final PhantomRenderState state, final PoseStack poseStack) {
      float scale = 1.0F + 0.15F * state.size;
      poseStack.scale(scale, scale, scale);
      poseStack.translate(0.0F, 1.3125F, 0.1875F);
   }

   protected void setupRotations(final PhantomRenderState state, final PoseStack poseStack, final float bodyRot, final float entityScale) {
      super.setupRotations(state, poseStack, bodyRot, entityScale);
      poseStack.mulPose(Axis.XP.rotationDegrees(state.xRot));
   }
}
