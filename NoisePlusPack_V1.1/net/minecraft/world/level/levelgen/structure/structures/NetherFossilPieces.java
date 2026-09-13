package net.minecraft.world.level.levelgen.structure.structures;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePieceAccessor;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

public class NetherFossilPieces {
   private static final Identifier[] FOSSILS = new Identifier[]{
      Identifier.withDefaultNamespace("nether_fossils/fossil_1"),
      Identifier.withDefaultNamespace("nether_fossils/fossil_2"),
      Identifier.withDefaultNamespace("nether_fossils/fossil_3"),
      Identifier.withDefaultNamespace("nether_fossils/fossil_4"),
      Identifier.withDefaultNamespace("nether_fossils/fossil_5"),
      Identifier.withDefaultNamespace("nether_fossils/fossil_6"),
      Identifier.withDefaultNamespace("nether_fossils/fossil_7"),
      Identifier.withDefaultNamespace("nether_fossils/fossil_8"),
      Identifier.withDefaultNamespace("nether_fossils/fossil_9"),
      Identifier.withDefaultNamespace("nether_fossils/fossil_10"),
      Identifier.withDefaultNamespace("nether_fossils/fossil_11"),
      Identifier.withDefaultNamespace("nether_fossils/fossil_12"),
      Identifier.withDefaultNamespace("nether_fossils/fossil_13"),
      Identifier.withDefaultNamespace("nether_fossils/fossil_14")
   };

   public static void addPieces(StructureTemplateManager p_228535_, StructurePieceAccessor p_228536_, RandomSource p_228537_, BlockPos p_228538_) {
      Rotation rotation = Rotation.getRandom(p_228537_);
      p_228536_.addPiece(new NetherFossilPieces.NetherFossilPiece(p_228535_, Util.getRandom(FOSSILS, p_228537_), p_228538_, rotation));
   }

   public static class NetherFossilPiece extends TemplateStructurePiece {
      public NetherFossilPiece(StructureTemplateManager p_228540_, Identifier p_461014_, BlockPos p_228542_, Rotation p_228543_) {
         super(StructurePieceType.NETHER_FOSSIL, 0, p_228540_, p_461014_, p_461014_.toString(), makeSettings(p_228543_), p_228542_);
      }

      public NetherFossilPiece(StructureTemplateManager p_228545_, CompoundTag p_228546_) {
         super(
            StructurePieceType.NETHER_FOSSIL,
            p_228546_,
            p_228545_,
            p_456035_ -> makeSettings(p_228546_.<Rotation>read("Rot", Rotation.LEGACY_CODEC).orElseThrow())
         );
      }

      private static StructurePlaceSettings makeSettings(Rotation p_228556_) {
         return new StructurePlaceSettings().setRotation(p_228556_).setMirror(Mirror.NONE).addProcessor(BlockIgnoreProcessor.STRUCTURE_AND_AIR);
      }

      @Override
      protected void addAdditionalSaveData(StructurePieceSerializationContext p_228558_, CompoundTag p_228559_) {
         super.addAdditionalSaveData(p_228558_, p_228559_);
         p_228559_.store("Rot", Rotation.LEGACY_CODEC, this.placeSettings.getRotation());
      }

      @Override
      protected void handleDataMarker(String p_228561_, BlockPos p_228562_, ServerLevelAccessor p_228563_, RandomSource p_228564_, BoundingBox p_228565_) {
      }

      @Override
      public void postProcess(
         WorldGenLevel p_228548_,
         StructureManager p_228549_,
         ChunkGenerator p_228550_,
         RandomSource p_228551_,
         BoundingBox p_228552_,
         ChunkPos p_228553_,
         BlockPos p_228554_
      ) {
         BoundingBox boundingbox = this.template.getBoundingBox(this.placeSettings, this.templatePosition);
         p_228552_.encapsulate(boundingbox);
         super.postProcess(p_228548_, p_228549_, p_228550_, p_228551_, p_228552_, p_228553_, p_228554_);
         this.placeDriedGhast(p_228548_, p_228551_, boundingbox, p_228552_);
      }

      private void placeDriedGhast(WorldGenLevel p_410517_, RandomSource p_410140_, BoundingBox p_407104_, BoundingBox p_406541_) {
         RandomSource randomsource = RandomSource.create(p_410517_.getSeed()).forkPositional().at(p_407104_.getCenter());
         if (randomsource.nextFloat() < 0.5F) {
            int i = p_407104_.minX() + randomsource.nextInt(p_407104_.getXSpan());
            int j = p_407104_.minY();
            int k = p_407104_.minZ() + randomsource.nextInt(p_407104_.getZSpan());
            BlockPos blockpos = new BlockPos(i, j, k);
            if (p_410517_.getBlockState(blockpos).isAir() && p_406541_.isInside(blockpos)) {
               p_410517_.setBlock(blockpos, Blocks.DRIED_GHAST.defaultBlockState().rotate(Rotation.getRandom(randomsource)), 2);
            }
         }
      }
   }
}
