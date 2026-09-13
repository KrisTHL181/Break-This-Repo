package net.minecraft.client.renderer.block;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.BlockStateModelWrapper;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

public class BlockModelSet {
   private static final Matrix4fc IDENTITY = new Matrix4f();
   private final BlockStateModelSet fallback;
   private final BlockColors blockColors;
   private final Map<BlockState, BlockModel> blockModelByStateCache = new HashMap<>();

   public BlockModelSet(final BlockStateModelSet fallback, final Map<BlockState, BlockModel> blockModelByState, final BlockColors blockColors) {
      this.fallback = fallback;
      this.blockModelByStateCache.putAll(blockModelByState);
      this.blockColors = blockColors;
   }

   public BlockModel get(final BlockState blockState) {
      return this.blockModelByStateCache.computeIfAbsent(blockState, this::createFallbackModel);
   }

   private BlockModel createFallbackModel(final BlockState blockState) {
      List<BlockTintSource> tints = this.blockColors.getTintSources(blockState);
      BlockStateModel plainModel = this.fallback.get(blockState);
      return new BlockStateModelWrapper(plainModel, tints, IDENTITY);
   }
}
