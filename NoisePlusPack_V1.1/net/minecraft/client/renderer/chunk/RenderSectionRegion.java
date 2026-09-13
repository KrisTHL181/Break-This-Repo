package net.minecraft.client.renderer.chunk;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class RenderSectionRegion implements BlockAndTintGetter {
   public static final int RADIUS = 1;
   public static final int SIZE = 3;
   private final int minSectionX;
   private final int minSectionY;
   private final int minSectionZ;
   private final SectionCopy[] sections;
   private final Level level;

   RenderSectionRegion(Level p_409799_, int p_409303_, int p_407338_, int p_409456_, SectionCopy[] p_408990_) {
      this.level = p_409799_;
      this.minSectionX = p_409303_;
      this.minSectionY = p_407338_;
      this.minSectionZ = p_409456_;
      this.sections = p_408990_;
   }

   @Override
   public BlockState getBlockState(BlockPos p_406800_) {
      return this.getSection(
            SectionPos.blockToSectionCoord(p_406800_.getX()),
            SectionPos.blockToSectionCoord(p_406800_.getY()),
            SectionPos.blockToSectionCoord(p_406800_.getZ())
         )
         .getBlockState(p_406800_);
   }

   @Override
   public FluidState getFluidState(BlockPos p_410161_) {
      return this.getSection(
            SectionPos.blockToSectionCoord(p_410161_.getX()),
            SectionPos.blockToSectionCoord(p_410161_.getY()),
            SectionPos.blockToSectionCoord(p_410161_.getZ())
         )
         .getBlockState(p_410161_)
         .getFluidState();
   }

   @Override
   public float getShade(Direction p_407825_, boolean p_407266_) {
      return this.level.getShade(p_407825_, p_407266_);
   }

   @Override
   public LevelLightEngine getLightEngine() {
      return this.level.getLightEngine();
   }

   @Override
   public @Nullable BlockEntity getBlockEntity(BlockPos p_408091_) {
      return this.getSection(
            SectionPos.blockToSectionCoord(p_408091_.getX()),
            SectionPos.blockToSectionCoord(p_408091_.getY()),
            SectionPos.blockToSectionCoord(p_408091_.getZ())
         )
         .getBlockEntity(p_408091_);
   }

   private SectionCopy getSection(int p_406718_, int p_406216_, int p_406392_) {
      return this.sections[index(this.minSectionX, this.minSectionY, this.minSectionZ, p_406718_, p_406216_, p_406392_)];
   }

   @Override
   public int getBlockTint(BlockPos p_407872_, ColorResolver p_407807_) {
      return this.level.getBlockTint(p_407872_, p_407807_);
   }

   @Override
   public int getMinY() {
      return this.level.getMinY();
   }

   @Override
   public int getHeight() {
      return this.level.getHeight();
   }

   public static int index(int p_409495_, int p_409337_, int p_407713_, int p_409077_, int p_406510_, int p_408332_) {
      return p_409077_ - p_409495_ + (p_406510_ - p_409337_) * 3 + (p_408332_ - p_407713_) * 3 * 3;
   }
}
