package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.animal.squid.SquidModel;
import net.minecraft.client.renderer.entity.state.SquidRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.animal.squid.Squid;

public class SquidRenderer<T extends Squid> extends AgeableMobRenderer<T, SquidRenderState, SquidModel> {
   private static final Identifier SQUID_LOCATION = Identifier.withDefaultNamespace("textures/entity/squid/squid.png");
   private static final Identifier SQUID_BABY_LOCATION = Identifier.withDefaultNamespace("textures/entity/squid/squid_baby.png");

   public SquidRenderer(final EntityRendererProvider.Context context, final SquidModel model, final SquidModel babyModel) {
      super(context, model, babyModel, 0.7F);
   }

   public Identifier getTextureLocation(final SquidRenderState state) {
      return state.isBaby ? SQUID_BABY_LOCATION : SQUID_LOCATION;
   }

   public SquidRenderState createRenderState() {
      return new SquidRenderState();
   }

   public void extractRenderState(final T entity, final SquidRenderState state, final float partialTicks) {
      super.extractRenderState(entity, state, partialTicks);
      state.tentacleAngle = Mth.lerp(partialTicks, entity.oldTentacleAngle, entity.tentacleAngle);
      state.xBodyRot = Mth.lerp(partialTicks, entity.xBodyRotO, entity.xBodyRot);
      state.zBodyRot = Mth.lerp(partialTicks, entity.zBodyRotO, entity.zBodyRot);
   }

   protected void setupRotations(final SquidRenderState state, final PoseStack poseStack, final float bodyRot, final float entityScale) {
      poseStack.translate(0.0F, state.isBaby ? 0.25F : 0.5F, 0.0F);
      poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - bodyRot));
      poseStack.mulPose(Axis.XP.rotationDegrees(state.xBodyRot));
      poseStack.mulPose(Axis.YP.rotationDegrees(state.zBodyRot));
      poseStack.translate(0.0F, state.isBaby ? -0.6F : -1.2F, 0.0F);
   }
}
