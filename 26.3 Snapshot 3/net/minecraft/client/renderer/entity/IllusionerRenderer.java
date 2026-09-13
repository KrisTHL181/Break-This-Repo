package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Arrays;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.illager.IllagerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.state.IllusionerRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.monster.illager.Illusioner;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class IllusionerRenderer extends IllagerRenderer<Illusioner, IllusionerRenderState> {
   private static final Identifier ILLUSIONER = Identifier.withDefaultNamespace("textures/entity/illager/illusioner.png");

   public IllusionerRenderer(final EntityRendererProvider.Context context) {
      super(context, new IllagerModel<>(context.bakeLayer(ModelLayers.ILLUSIONER)), 0.5F);
      this.addLayer(
         new ItemInHandLayer<IllusionerRenderState, IllagerModel<IllusionerRenderState>>(this) {
            public void submit(
               final PoseStack poseStack,
               final SubmitNodeCollector submitNodeCollector,
               final int lightCoords,
               final IllusionerRenderState state,
               final float yRot,
               final float xRot
            ) {
               if (state.isCastingSpell || state.isAggressive) {
                  super.submit(poseStack, submitNodeCollector, lightCoords, state, yRot, xRot);
               }
            }
         }
      );
      this.model.getHat().visible = true;
   }

   public Identifier getTextureLocation(final IllusionerRenderState state) {
      return ILLUSIONER;
   }

   public IllusionerRenderState createRenderState() {
      return new IllusionerRenderState();
   }

   public void extractRenderState(final Illusioner entity, final IllusionerRenderState state, final float partialTicks) {
      super.extractRenderState(entity, state, partialTicks);
      Vec3[] illusionOffsets = entity.getIllusionOffsets(partialTicks);
      state.illusionOffsets = Arrays.copyOf(illusionOffsets, illusionOffsets.length);
      state.isCastingSpell = entity.isCastingSpell();
   }

   public void submit(
      final IllusionerRenderState state, final PoseStack poseStack, final SubmitNodeCollector submitNodeCollector, final CameraRenderState camera
   ) {
      if (state.isInvisible) {
         Vec3[] offsets = state.illusionOffsets;

         for (int i = 0; i < offsets.length; i++) {
            poseStack.pushPose();
            poseStack.translate(
               offsets[i].x + Mth.cos(i + state.ageInTicks * 0.5F) * 0.025,
               offsets[i].y + Mth.cos(i + state.ageInTicks * 0.75F) * 0.0125,
               offsets[i].z + Mth.cos(i + state.ageInTicks * 0.7F) * 0.025
            );
            super.submit(state, poseStack, submitNodeCollector, camera);
            poseStack.popPose();
         }
      } else {
         super.submit(state, poseStack, submitNodeCollector, camera);
      }
   }

   protected boolean isBodyVisible(final IllusionerRenderState state) {
      return true;
   }

   protected AABB getBoundingBoxForCulling(final Illusioner entity) {
      return super.getBoundingBoxForCulling(entity).inflate(3.0, 0.0, 3.0);
   }
}
