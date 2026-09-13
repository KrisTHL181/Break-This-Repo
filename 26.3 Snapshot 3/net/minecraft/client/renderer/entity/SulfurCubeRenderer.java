package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.slime.SmallSulfurCubeModel;
import net.minecraft.client.model.monster.slime.SulfurCubeModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.layers.SulfurCubeInnerLayer;
import net.minecraft.client.renderer.entity.state.SulfurCubeRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.monster.cubemob.SulfurCube;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.block.AbstractSkullBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class SulfurCubeRenderer extends AbstractCubeMobRenderer<SulfurCube, SulfurCubeRenderState, SulfurCubeModel> {
   private static final Identifier SULFUR_CUBE_LOCATION = Identifier.withDefaultNamespace("textures/entity/sulfur_cube/sulfur_cube_outer.png");
   private static final Identifier SULFUR_CUBE_SMALL_LOCATION = Identifier.withDefaultNamespace("textures/entity/sulfur_cube/sulfur_cube_outer_small.png");
   public static final BlockDisplayContext BLOCK_DISPLAY_CONTEXT = BlockDisplayContext.create();
   private static final float COUNTER_SKULL_SCALE = 0.84210527F;
   public static final CustomHeadLayer.Transforms CUSTOM_HEAD_TRANSFORMS = new CustomHeadLayer.Transforms(
      0.0F, 0.625F, 0.84210527F, 0.84210527F, CustomHeadLayer.Transforms.CUTOUT_PLAYER_SKIN_RESOLVER
   );
   private final SulfurCubeModel normalModel;
   private final SmallSulfurCubeModel smallModel;
   private final BlockModelResolver blockModelResolver;

   public SulfurCubeRenderer(final EntityRendererProvider.Context context) {
      SulfurCubeModel normalModel = new SulfurCubeModel(context.bakeLayer(ModelLayers.SULFUR_CUBE));
      super(context, normalModel);
      this.normalModel = normalModel;
      this.smallModel = new SmallSulfurCubeModel(context.bakeLayer(ModelLayers.SULFUR_CUBE_SMALL));
      this.blockModelResolver = context.getBlockModelResolver();
      this.addLayer(new SulfurCubeInnerLayer(this, context.getModelSet()));
      this.addLayer(new CustomHeadLayer<>(this, context.getModelSet(), context.getPlayerSkinRenderCache(), CUSTOM_HEAD_TRANSFORMS));
   }

   public void submit(
      final SulfurCubeRenderState state, final PoseStack poseStack, final SubmitNodeCollector submitNodeCollector, final CameraRenderState camera
   ) {
      this.model = state.isBaby ? this.smallModel : this.normalModel;
      super.submit(state, poseStack, submitNodeCollector, camera);
   }

   protected void scale(final SulfurCubeRenderState state, final PoseStack poseStack) {
      this.downscaleSlightly(poseStack);
      super.scale(state, poseStack);
      float fuse = state.fuseRemainingTicks;
      if (fuse < 10.0F && fuse > 0.0F) {
         float s = 1.0F + TntRenderer.getSwellAmount(fuse);
         poseStack.scale(s, s, s);
      }

      float vOffset = state.isBaby ? 1.24F : 0.98F;
      float extraDownscale = state.isBaby ? 1.0F : 0.5F;
      float onePixelUpIfVisible = (state.isInvisible ? 0.0F : 1.0F) / 16.0F;
      poseStack.scale(extraDownscale, extraDownscale, extraDownscale);
      poseStack.translate(-0.0F, vOffset - onePixelUpIfVisible, -0.0F);
   }

   public Identifier getTextureLocation(final SulfurCubeRenderState state) {
      return state.isBaby ? SULFUR_CUBE_SMALL_LOCATION : SULFUR_CUBE_LOCATION;
   }

   public SulfurCubeRenderState createRenderState() {
      return new SulfurCubeRenderState();
   }

   public void extractRenderState(final SulfurCube entity, final SulfurCubeRenderState state, final float partialTicks) {
      super.extractRenderState(entity, state, partialTicks);
      state.fuseRemainingTicks = entity.isPrimed() ? entity.getFuse() - partialTicks + 1.0F : 0.0F;
      ItemStack containedBlock = entity.getBodyArmorItem();
      if (!containedBlock.isEmpty()) {
         if (containedBlock.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof AbstractSkullBlock skullBlock) {
            state.wornHeadType = skullBlock.getType();
            state.wornHeadProfile = containedBlock.get(DataComponents.PROFILE);
         } else {
            BlockItemStateProperties blockItemState = containedBlock.getOrDefault(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY);
            BlockState blockState = blockItemState.apply(Block.byItem(containedBlock.getItem()).defaultBlockState());
            this.blockModelResolver.update(state.containedBlock, blockState, BLOCK_DISPLAY_CONTEXT);
         }
      }
   }

   protected void applySizeAndSquish(final SulfurCubeRenderState state, final PoseStack poseStack) {
      float size = state.size;
      float ss = state.containedBlock.isEmpty() ? state.squish / (size * 0.5F + 1.0F) : 0.0F;
      float w = 1.0F / (ss + 1.0F);
      poseStack.scale(w * size, 1.0F / w * size, w * size);
   }
}
