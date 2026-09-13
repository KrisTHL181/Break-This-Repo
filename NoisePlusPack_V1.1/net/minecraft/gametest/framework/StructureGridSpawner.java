package net.minecraft.gametest.framework;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;

public class StructureGridSpawner implements GameTestRunner.StructureSpawner {
   private static final int SPACE_BETWEEN_COLUMNS = 5;
   private static final int SPACE_BETWEEN_ROWS = 6;
   private final int testsPerRow;
   private int currentRowCount;
   private AABB rowBounds;
   private final BlockPos.MutableBlockPos nextTestNorthWestCorner;
   private final BlockPos firstTestNorthWestCorner;
   private final boolean clearOnBatch;
   private float maxX = -1.0F;
   private final Collection<GameTestInfo> testInLastBatch = new ArrayList<>();

   public StructureGridSpawner(BlockPos p_329915_, int p_328380_, boolean p_342481_) {
      this.testsPerRow = p_328380_;
      this.nextTestNorthWestCorner = p_329915_.mutable();
      this.rowBounds = new AABB(this.nextTestNorthWestCorner);
      this.firstTestNorthWestCorner = p_329915_;
      this.clearOnBatch = p_342481_;
   }

   @Override
   public void onBatchStart(ServerLevel p_345035_) {
      if (this.clearOnBatch) {
         this.testInLastBatch.forEach(p_389785_ -> {
            BoundingBox boundingbox = p_389785_.getTestInstanceBlockEntity().getStructureBoundingBox();
            StructureUtils.clearSpaceForStructure(boundingbox, p_345035_);
         });
         this.testInLastBatch.clear();
         this.rowBounds = new AABB(this.firstTestNorthWestCorner);
         this.nextTestNorthWestCorner.set(this.firstTestNorthWestCorner);
      }
   }

   @Override
   public Optional<GameTestInfo> spawnStructure(GameTestInfo p_335013_) {
      BlockPos blockpos = new BlockPos(this.nextTestNorthWestCorner);
      p_335013_.setTestBlockPos(blockpos);
      GameTestInfo gametestinfo = p_335013_.prepareTestStructure();
      if (gametestinfo == null) {
         return Optional.empty();
      }

      gametestinfo.startExecution(1);
      AABB aabb = p_335013_.getTestInstanceBlockEntity().getStructureBounds();
      this.rowBounds = this.rowBounds.minmax(aabb);
      this.nextTestNorthWestCorner.move((int)aabb.getXsize() + 5, 0, 0);
      if (this.nextTestNorthWestCorner.getX() > this.maxX) {
         this.maxX = this.nextTestNorthWestCorner.getX();
      }

      if (++this.currentRowCount >= this.testsPerRow) {
         this.currentRowCount = 0;
         this.nextTestNorthWestCorner.move(0, 0, (int)this.rowBounds.getZsize() + 6);
         this.nextTestNorthWestCorner.setX(this.firstTestNorthWestCorner.getX());
         this.rowBounds = new AABB(this.nextTestNorthWestCorner);
      }

      this.testInLastBatch.add(p_335013_);
      return Optional.of(p_335013_);
   }
}
