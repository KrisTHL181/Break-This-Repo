package net.minecraft.client.renderer.chunk;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.CardinalLighting;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import org.jspecify.annotations.Nullable;

public class RenderSectionRegion implements BlockAndTintGetter {
   public static final int RADIUS = 1;
   public static final int SIZE = 3;
   private final int minSectionX;
   private final int minSectionY;
   private final int minSectionZ;
   private final SectionCopy[] sections;
   private final ClientLevel level;
   private final CardinalLighting cardinalLighting;
   private final LevelLightEngine lightEngine;

   public RenderSectionRegion(final ClientLevel level, final int minSectionX, final int minSectionY, final int minSectionZ, final SectionCopy[] sections) {
      this.level = level;
      this.minSectionX = minSectionX;
      this.minSectionY = minSectionY;
      this.minSectionZ = minSectionZ;
      this.sections = sections;
      this.cardinalLighting = level.cardinalLighting();
      this.lightEngine = level.getLightEngine();
   }

   @Override
   public BlockState getBlockState(final BlockPos pos) {
      return this.getSection(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getY()), SectionPos.blockToSectionCoord(pos.getZ()))
         .getBlockState(pos);
   }

   @Override
   public FluidState getFluidState(final BlockPos pos) {
      return this.getSection(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getY()), SectionPos.blockToSectionCoord(pos.getZ()))
         .getBlockState(pos)
         .getFluidState();
   }

   @Override
   public CardinalLighting cardinalLighting() {
      return this.cardinalLighting;
   }

   @Override
   public LevelLightEngine getLightEngine() {
      return this.lightEngine;
   }

   @Override
   public @Nullable BlockEntity getBlockEntity(final BlockPos pos) {
      return this.getSection(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getY()), SectionPos.blockToSectionCoord(pos.getZ()))
         .getBlockEntity(pos);
   }

   private SectionCopy getSection(final int sectionX, final int sectionY, final int sectionZ) {
      return this.sections[index(this.minSectionX, this.minSectionY, this.minSectionZ, sectionX, sectionY, sectionZ)];
   }

   @Override
   public int getBlockTint(final BlockPos pos, final ColorResolver resolver) {
      return this.level.getBlockTint(pos, resolver);
   }

   @Override
   public int getMinY() {
      return this.level.getMinY();
   }

   @Override
   public int getHeight() {
      return this.level.getHeight();
   }

   public static int index(final int minSectionX, final int minSectionY, final int minSectionZ, final int sectionX, final int sectionY, final int sectionZ) {
      return sectionX - minSectionX + (sectionY - minSectionY) * 3 + (sectionZ - minSectionZ) * 3 * 3;
   }
}
