package net.minecraft.world.level;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;

public interface LevelHeightAccessor {
   int getHeight();

   int getMinY();

   default int getMaxY() {
      return this.getMinY() + this.getHeight() - 1;
   }

   default int getSectionsCount() {
      return this.getMaxSectionY() - this.getMinSectionY() + 1;
   }

   default int getMinSectionY() {
      return SectionPos.blockToSectionCoord(this.getMinY());
   }

   default int getMaxSectionY() {
      return SectionPos.blockToSectionCoord(this.getMaxY());
   }

   default boolean isInsideBuildHeight(int p_362913_) {
      return p_362913_ >= this.getMinY() && p_362913_ <= this.getMaxY();
   }

   default boolean isOutsideBuildHeight(BlockPos p_151571_) {
      return this.isOutsideBuildHeight(p_151571_.getY());
   }

   default boolean isOutsideBuildHeight(int p_151563_) {
      return p_151563_ < this.getMinY() || p_151563_ > this.getMaxY();
   }

   default int getSectionIndex(int p_151565_) {
      return this.getSectionIndexFromSectionY(SectionPos.blockToSectionCoord(p_151565_));
   }

   default int getSectionIndexFromSectionY(int p_151567_) {
      return p_151567_ - this.getMinSectionY();
   }

   default int getSectionYFromSectionIndex(int p_151569_) {
      return p_151569_ + this.getMinSectionY();
   }

   static LevelHeightAccessor create(final int p_186488_, final int p_186489_) {
      return new LevelHeightAccessor() {
         @Override
         public int getHeight() {
            return p_186489_;
         }

         @Override
         public int getMinY() {
            return p_186488_;
         }
      };
   }
}
