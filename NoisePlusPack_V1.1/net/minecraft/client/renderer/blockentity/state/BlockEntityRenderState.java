package net.minecraft.client.renderer.blockentity.state;

import net.minecraft.CrashReportCategory;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class BlockEntityRenderState {
   public BlockPos blockPos = BlockPos.ZERO;
   public BlockState blockState = Blocks.AIR.defaultBlockState();
   public BlockEntityType<?> blockEntityType = BlockEntityType.TEST_BLOCK;
   public int lightCoords;
   public ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress;

   public static void extractBase(BlockEntity p_424180_, BlockEntityRenderState p_430964_, ModelFeatureRenderer.@Nullable CrumblingOverlay p_429379_) {
      p_430964_.blockPos = p_424180_.getBlockPos();
      p_430964_.blockState = p_424180_.getBlockState();
      p_430964_.blockEntityType = p_424180_.getType();
      p_430964_.lightCoords = p_424180_.getLevel() != null ? LevelRenderer.getLightColor(p_424180_.getLevel(), p_424180_.getBlockPos()) : 15728880;
      p_430964_.breakProgress = p_429379_;
   }

   public void fillCrashReportCategory(CrashReportCategory p_429635_) {
      p_429635_.setDetail("BlockEntityRenderState", this.getClass().getCanonicalName());
      p_429635_.setDetail("Position", this.blockPos);
      p_429635_.setDetail("Block state", this.blockState::toString);
   }
}
