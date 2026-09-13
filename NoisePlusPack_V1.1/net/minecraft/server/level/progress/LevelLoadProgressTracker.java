package net.minecraft.server.level.progress;

import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

public class LevelLoadProgressTracker implements LevelLoadListener {
   private static final int PREPARE_SERVER_WEIGHT = 10;
   private static final int EXPECTED_PLAYER_CHUNKS = Mth.square(7);
   private final boolean includePlayerChunks;
   private int totalWeight;
   private int finalizedWeight;
   private int segmentWeight;
   private float segmentFraction;
   private volatile float progress;

   public LevelLoadProgressTracker(boolean p_423979_) {
      this.includePlayerChunks = p_423979_;
   }

   @Override
   public void start(LevelLoadListener.Stage p_429902_, int p_427483_) {
      if (this.tracksStage(p_429902_)) {
         switch (p_429902_) {
            case LOAD_INITIAL_CHUNKS:
               int i = this.includePlayerChunks ? EXPECTED_PLAYER_CHUNKS : 0;
               this.totalWeight = 10 + p_427483_ + i;
               this.beginSegment(10);
               this.finishSegment();
               this.beginSegment(p_427483_);
               break;
            case LOAD_PLAYER_CHUNKS:
               this.beginSegment(EXPECTED_PLAYER_CHUNKS);
         }
      }
   }

   private void beginSegment(int p_423550_) {
      this.segmentWeight = p_423550_;
      this.segmentFraction = 0.0F;
      this.updateProgress();
   }

   @Override
   public void update(LevelLoadListener.Stage p_423330_, int p_431181_, int p_423764_) {
      if (this.tracksStage(p_423330_)) {
         this.segmentFraction = p_423764_ == 0 ? 0.0F : (float)p_431181_ / p_423764_;
         this.updateProgress();
      }
   }

   @Override
   public void finish(LevelLoadListener.Stage p_431553_) {
      if (this.tracksStage(p_431553_)) {
         this.finishSegment();
      }
   }

   private void finishSegment() {
      this.finalizedWeight = this.finalizedWeight + this.segmentWeight;
      this.segmentWeight = 0;
      this.updateProgress();
   }

   private boolean tracksStage(LevelLoadListener.Stage p_428896_) {
      return switch (p_428896_) {
         case LOAD_INITIAL_CHUNKS -> true;
         case LOAD_PLAYER_CHUNKS -> this.includePlayerChunks;
         default -> false;
      };
   }

   private void updateProgress() {
      if (this.totalWeight == 0) {
         this.progress = 0.0F;
      } else {
         float f = this.finalizedWeight + this.segmentFraction * this.segmentWeight;
         this.progress = f / this.totalWeight;
      }
   }

   public float get() {
      return this.progress;
   }

   @Override
   public void updateFocus(ResourceKey<Level> p_431339_, ChunkPos p_431403_) {
   }
}
