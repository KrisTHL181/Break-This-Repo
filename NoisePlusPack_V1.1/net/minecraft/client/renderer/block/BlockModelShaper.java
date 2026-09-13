package net.minecraft.client.renderer.block;

import java.util.Map;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BlockModelShaper {
   private Map<BlockState, BlockStateModel> modelByStateCache = Map.of();
   private final ModelManager modelManager;

   public BlockModelShaper(ModelManager p_110880_) {
      this.modelManager = p_110880_;
   }

   public TextureAtlasSprite getParticleIcon(BlockState p_110883_) {
      return this.getBlockModel(p_110883_).particleIcon();
   }

   public BlockStateModel getBlockModel(BlockState p_110894_) {
      BlockStateModel blockstatemodel = this.modelByStateCache.get(p_110894_);
      if (blockstatemodel == null) {
         blockstatemodel = this.modelManager.getMissingBlockStateModel();
      }

      return blockstatemodel;
   }

   public ModelManager getModelManager() {
      return this.modelManager;
   }

   public void replaceCache(Map<BlockState, BlockStateModel> p_248582_) {
      this.modelByStateCache = p_248582_;
   }
}
