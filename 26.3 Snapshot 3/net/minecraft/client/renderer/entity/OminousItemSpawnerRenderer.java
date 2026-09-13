package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.ItemClusterRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.OminousItemSpawner;
import net.minecraft.world.item.ItemStack;

public class OminousItemSpawnerRenderer extends EntityRenderer<OminousItemSpawner, ItemClusterRenderState> {
   private static final float ROTATION_SPEED = 40.0F;
   private static final int TICKS_SCALING = 50;
   private final ItemModelResolver itemModelResolver;
   private final RandomSource random = RandomSource.create();

   protected OminousItemSpawnerRenderer(final EntityRendererProvider.Context context) {
      super(context);
      this.itemModelResolver = context.getItemModelResolver();
   }

   public ItemClusterRenderState createRenderState() {
      return new ItemClusterRenderState();
   }

   public void extractRenderState(final OminousItemSpawner entity, final ItemClusterRenderState state, final float partialTicks) {
      super.extractRenderState(entity, state, partialTicks);
      ItemStack item = entity.getItem();
      state.extractItemGroupRenderState(entity, item, this.itemModelResolver);
   }

   public void submit(
      final ItemClusterRenderState state, final PoseStack poseStack, final SubmitNodeCollector submitNodeCollector, final CameraRenderState camera
   ) {
      if (!state.item.isEmpty()) {
         poseStack.pushPose();
         if (state.ageInTicks <= 50.0F) {
            float scale = Math.min(state.ageInTicks, 50.0F) / 50.0F;
            poseStack.scale(scale, scale, scale);
         }

         float currentSpin = Mth.wrapDegrees(state.ageInTicks * 40.0F);
         poseStack.mulPose(Axis.YP.rotationDegrees(currentSpin));
         ItemEntityRenderer.submitMultipleFromCount(poseStack, submitNodeCollector, 15728880, state, this.random);
         poseStack.popPose();
      }
   }
}
