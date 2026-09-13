package net.minecraft.world.level.levelgen.structure;

import com.mojang.logging.LogUtils;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.structures.OceanMonumentStructure;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public final class StructureStart {
   public static final String INVALID_START_ID = "INVALID";
   public static final StructureStart INVALID_START = new StructureStart(null, new ChunkPos(0, 0), 0, new PiecesContainer(List.of()));
   private static final Logger LOGGER = LogUtils.getLogger();
   private final Structure structure;
   private final PiecesContainer pieceContainer;
   private final ChunkPos chunkPos;
   private int references;
   private volatile @Nullable BoundingBox cachedBoundingBox;

   public StructureStart(Structure p_226846_, ChunkPos p_226847_, int p_226848_, PiecesContainer p_226849_) {
      this.structure = p_226846_;
      this.chunkPos = p_226847_;
      this.references = p_226848_;
      this.pieceContainer = p_226849_;
   }

   public static @Nullable StructureStart loadStaticStart(StructurePieceSerializationContext p_226858_, CompoundTag p_226859_, long p_226860_) {
      String s = p_226859_.getStringOr("id", "");
      if ("INVALID".equals(s)) {
         return INVALID_START;
      }

      Registry<Structure> registry = p_226858_.registryAccess().lookupOrThrow(Registries.STRUCTURE);
      Structure structure = registry.getValue(Identifier.parse(s));
      if (structure == null) {
         LOGGER.error("Unknown stucture id: {}", s);
         return null;
      }

      ChunkPos chunkpos = new ChunkPos(p_226859_.getIntOr("ChunkX", 0), p_226859_.getIntOr("ChunkZ", 0));
      int i = p_226859_.getIntOr("references", 0);
      ListTag listtag = p_226859_.getListOrEmpty("Children");

      try {
         PiecesContainer piecescontainer = PiecesContainer.load(listtag, p_226858_);
         if (structure instanceof OceanMonumentStructure) {
            piecescontainer = OceanMonumentStructure.regeneratePiecesAfterLoad(chunkpos, p_226860_, piecescontainer);
         }

         return new StructureStart(structure, chunkpos, i, piecescontainer);
      } catch (Exception exception) {
         LOGGER.error("Failed Start with id {}", s, exception);
         return null;
      }
   }

   public BoundingBox getBoundingBox() {
      BoundingBox boundingbox = this.cachedBoundingBox;
      if (boundingbox == null) {
         boundingbox = this.structure.adjustBoundingBox(this.pieceContainer.calculateBoundingBox());
         this.cachedBoundingBox = boundingbox;
      }

      return boundingbox;
   }

   public void placeInChunk(
      WorldGenLevel p_226851_, StructureManager p_226852_, ChunkGenerator p_226853_, RandomSource p_226854_, BoundingBox p_226855_, ChunkPos p_226856_
   ) {
      List<StructurePiece> list = this.pieceContainer.pieces();
      if (!list.isEmpty()) {
         BoundingBox boundingbox = list.get(0).boundingBox;
         BlockPos blockpos = boundingbox.getCenter();
         BlockPos blockpos1 = new BlockPos(blockpos.getX(), boundingbox.minY(), blockpos.getZ());

         for (StructurePiece structurepiece : list) {
            if (structurepiece.getBoundingBox().intersects(p_226855_)) {
               structurepiece.postProcess(p_226851_, p_226852_, p_226853_, p_226854_, p_226855_, p_226856_, blockpos1);
            }
         }

         this.structure.afterPlace(p_226851_, p_226852_, p_226853_, p_226854_, p_226855_, p_226856_, this.pieceContainer);
      }
   }

   public CompoundTag createTag(StructurePieceSerializationContext p_192661_, ChunkPos p_192662_) {
      CompoundTag compoundtag = new CompoundTag();
      if (this.isValid()) {
         compoundtag.putString("id", p_192661_.registryAccess().lookupOrThrow(Registries.STRUCTURE).getKey(this.structure).toString());
         compoundtag.putInt("ChunkX", p_192662_.x);
         compoundtag.putInt("ChunkZ", p_192662_.z);
         compoundtag.putInt("references", this.references);
         compoundtag.put("Children", this.pieceContainer.save(p_192661_));
         return compoundtag;
      } else {
         compoundtag.putString("id", "INVALID");
         return compoundtag;
      }
   }

   public boolean isValid() {
      return !this.pieceContainer.isEmpty();
   }

   public ChunkPos getChunkPos() {
      return this.chunkPos;
   }

   public boolean canBeReferenced() {
      return this.references < this.getMaxReferences();
   }

   public void addReference() {
      this.references++;
   }

   public int getReferences() {
      return this.references;
   }

   protected int getMaxReferences() {
      return 1;
   }

   public Structure getStructure() {
      return this.structure;
   }

   public List<StructurePiece> getPieces() {
      return this.pieceContainer.pieces();
   }
}
