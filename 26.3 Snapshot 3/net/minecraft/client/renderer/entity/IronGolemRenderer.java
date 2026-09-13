package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.animal.golem.IronGolemModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.entity.layers.IronGolemCrackinessLayer;
import net.minecraft.client.renderer.entity.layers.IronGolemFlowerLayer;
import net.minecraft.client.renderer.entity.state.IronGolemRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.level.block.Blocks;

public class IronGolemRenderer extends MobRenderer<IronGolem, IronGolemRenderState, IronGolemModel> {
   public static final BlockDisplayContext BLOCK_DISPLAY_CONTEXT = BlockDisplayContext.create();
   private static final Identifier GOLEM_LOCATION = Identifier.withDefaultNamespace("textures/entity/iron_golem/iron_golem.png");
   private final BlockModelResolver blockModelResolver;

   public IronGolemRenderer(final EntityRendererProvider.Context context) {
      super(context, new IronGolemModel(context.bakeLayer(ModelLayers.IRON_GOLEM)), 0.7F);
      this.blockModelResolver = context.getBlockModelResolver();
      this.addLayer(new IronGolemCrackinessLayer(this));
      this.addLayer(new IronGolemFlowerLayer(this));
   }

   public Identifier getTextureLocation(final IronGolemRenderState state) {
      return GOLEM_LOCATION;
   }

   public IronGolemRenderState createRenderState() {
      return new IronGolemRenderState();
   }

   public void extractRenderState(final IronGolem entity, final IronGolemRenderState state, final float partialTicks) {
      super.extractRenderState(entity, state, partialTicks);
      state.attackTicksRemaining = entity.getAttackAnimationTick() > 0.0F ? entity.getAttackAnimationTick() - partialTicks : 0.0F;
      state.offerFlowerTick = entity.getOfferFlowerTick();
      if (state.offerFlowerTick > 0) {
         this.blockModelResolver.update(state.flowerBlock, Blocks.POPPY.defaultBlockState(), BLOCK_DISPLAY_CONTEXT);
      } else {
         state.flowerBlock.clear();
      }

      state.crackiness = entity.getCrackiness();
   }

   protected void setupRotations(final IronGolemRenderState state, final PoseStack poseStack, final float bodyRot, final float entityScale) {
      super.setupRotations(state, poseStack, bodyRot, entityScale);
      if (!(state.walkAnimationSpeed < 0.01)) {
         float p = 13.0F;
         float wp = state.walkAnimationPos + 6.0F;
         float triangleWave = (Math.abs(wp % 13.0F - 6.5F) - 3.25F) / 3.25F;
         poseStack.mulPose(Axis.ZP.rotationDegrees(6.5F * triangleWave));
      }
   }
}
