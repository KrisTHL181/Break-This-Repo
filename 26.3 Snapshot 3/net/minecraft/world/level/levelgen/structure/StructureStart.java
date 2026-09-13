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

   public StructureStart(final Structure structure, final ChunkPos chunkPos, final int references, final PiecesContainer pieceContainer) {
      this.structure = structure;
      this.chunkPos = chunkPos;
      this.references = references;
      this.pieceContainer = pieceContainer;
   }

   public static @Nullable StructureStart loadStaticStart(final StructurePieceSerializationContext context, final CompoundTag tag, final long seed) {
      String id = tag.getStringOr("id", "");
      if ("INVALID".equals(id)) {
         return INVALID_START;
      }

      Registry<Structure> structuresRegistry = context.registryAccess().lookupOrThrow(Registries.STRUCTURE);
      Structure stucture = structuresRegistry.getValue(Identifier.parse(id));
      if (stucture == null) {
         LOGGER.error("Unknown stucture id: {}", id);
         return null;
      }

      ChunkPos chunkPos = new ChunkPos(tag.getIntOr("ChunkX", 0), tag.getIntOr("ChunkZ", 0));
      int references = tag.getIntOr("references", 0);
      ListTag children = tag.getListOrEmpty("Children");

      try {
         PiecesContainer pieces = PiecesContainer.load(children, context);
         if (stucture instanceof OceanMonumentStructure) {
            pieces = OceanMonumentStructure.regeneratePiecesAfterLoad(chunkPos, seed, pieces);
         }

         return new StructureStart(stucture, chunkPos, references, pieces);
      } catch (Exception e) {
         LOGGER.error("Failed Start with id {}", id, e);
         return null;
      }
   }

   public BoundingBox getBoundingBox() {
      BoundingBox boundingBox = this.cachedBoundingBox;
      if (boundingBox == null) {
         boundingBox = this.structure.adjustBoundingBox(this.pieceContainer.calculateBoundingBox());
         this.cachedBoundingBox = boundingBox;
      }

      return boundingBox;
   }

   public void placeInChunk(
      final WorldGenLevel level,
      final StructureManager structureManager,
      final ChunkGenerator generator,
      final RandomSource random,
      final BoundingBox chunkBB,
      final ChunkPos chunkPos
   ) {
      List<StructurePiece> pieces = this.pieceContainer.pieces();
      if (!pieces.isEmpty()) {
         BoundingBox centerBB = pieces.get(0).boundingBox;
         BlockPos centerPos = centerBB.getCenter();
         BlockPos referencePos = new BlockPos(centerPos.getX(), centerBB.minY(), centerPos.getZ());

         for (StructurePiece next : pieces) {
            if (next.getBoundingBox().intersects(chunkBB)) {
               next.postProcess(level, structureManager, generator, random, chunkBB, chunkPos, referencePos);
            }
         }

         this.structure.afterPlace(level, structureManager, generator, random, chunkBB, chunkPos, this.pieceContainer);
      }
   }

   public CompoundTag createTag(final StructurePieceSerializationContext context, final ChunkPos chunkPos) {
      CompoundTag tag = new CompoundTag();
      if (this.isValid()) {
         tag.putString("id", context.registryAccess().lookupOrThrow(Registries.STRUCTURE).getKey(this.structure).toString());
         tag.putInt("ChunkX", chunkPos.x());
         tag.putInt("ChunkZ", chunkPos.z());
         tag.putInt("references", this.references);
         tag.put("Children", this.pieceContainer.save(context));
         return tag;
      } else {
         tag.putString("id", "INVALID");
         return tag;
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
