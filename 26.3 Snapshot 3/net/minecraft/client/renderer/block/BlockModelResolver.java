package net.minecraft.client.renderer.block;

import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.entity.ItemFrameRenderer;
import net.minecraft.client.resources.model.BlockStateDefinitions;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.block.state.BlockState;

public class BlockModelResolver {
   private static final long MODEL_SEED = 42L;
   private final ModelManager modelManager;

   public BlockModelResolver(final ModelManager modelManager) {
      this.modelManager = modelManager;
   }

   public void update(final BlockModelRenderState renderState, final BlockState blockState, final BlockDisplayContext displayContext) {
      renderState.clear();
      this.modelManager.getBlockModelSet().get(blockState).update(renderState, blockState, displayContext, 42L);
      renderState.blockLightCoords = blockState.emissiveRendering() ? 15728880 : LightCoordsUtil.pack(blockState.getLightEmission(), 0);
   }

   public void updateForItemFrame(final BlockModelRenderState renderState, final boolean isGlowing, final boolean map) {
      BlockState fakeState = BlockStateDefinitions.getItemFrameFakeState(isGlowing, map);
      this.update(renderState, fakeState, ItemFrameRenderer.BLOCK_DISPLAY_CONTEXT);
   }
}
