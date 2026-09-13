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

   public static void addPieces(
      final StructureTemplateManager structureTemplateManager,
      final StructurePieceAccessor structurePieceAccessor,
      final RandomSource random,
      final BlockPos position
   ) {
      Rotation nextRotation = Rotation.getRandom(random);
      structurePieceAccessor.addPiece(
         new NetherFossilPieces.NetherFossilPiece(structureTemplateManager, Util.getRandom(FOSSILS, random), position, nextRotation)
      );
   }

   public static class NetherFossilPiece extends TemplateStructurePiece {
      public NetherFossilPiece(
         final StructureTemplateManager structureTemplateManager, final Identifier templateLocation, final BlockPos position, final Rotation rotation
      ) {
         super(StructurePieceType.NETHER_FOSSIL, 0, structureTemplateManager, templateLocation, templateLocation.toString(), makeSettings(rotation), position);
      }

      public NetherFossilPiece(final StructureTemplateManager structureTemplateManager, final CompoundTag tag) {
         super(
            StructurePieceType.NETHER_FOSSIL,
            tag,
            structureTemplateManager,
            location -> makeSettings(tag.<Rotation>read("Rot", Rotation.LEGACY_CODEC).orElseThrow())
         );
      }

      private static StructurePlaceSettings makeSettings(final Rotation rotation) {
         return new StructurePlaceSettings().setRotation(rotation).setMirror(Mirror.NONE).addProcessor(BlockIgnoreProcessor.STRUCTURE_AND_AIR);
      }

      @Override
      protected void addAdditionalSaveData(final StructurePieceSerializationContext context, final CompoundTag tag) {
         super.addAdditionalSaveData(context, tag);
         tag.store("Rot", Rotation.LEGACY_CODEC, this.placeSettings.getRotation());
      }

      @Override
      protected void handleDataMarker(
         final String markerId, final BlockPos position, final ServerLevelAccessor level, final RandomSource random, final BoundingBox chunkBB
      ) {
      }

      @Override
      public void postProcess(
         final WorldGenLevel level,
         final StructureManager structureManager,
         final ChunkGenerator generator,
         final RandomSource random,
         final BoundingBox chunkBB,
         final ChunkPos chunkPos,
         final BlockPos referencePos
      ) {
         BoundingBox fossilBB = this.template.getBoundingBox(this.placeSettings, this.templatePosition);
         chunkBB.encapsulate(fossilBB);
         super.postProcess(level, structureManager, generator, random, chunkBB, chunkPos, referencePos);
         this.placeDriedGhast(level, random, fossilBB, chunkBB);
      }

      private void placeDriedGhast(final WorldGenLevel level, final RandomSource random, final BoundingBox fossilBB, final BoundingBox chunkBB) {
         RandomSource positionalRandom = RandomSource.createThreadLocalInstance(level.getSeed()).forkPositional().at(fossilBB.getCenter());
         if (positionalRandom.nextFloat() < 0.5F) {
            int x = fossilBB.minX() + positionalRandom.nextInt(fossilBB.getXSpan());
            int y = fossilBB.minY();
            int z = fossilBB.minZ() + positionalRandom.nextInt(fossilBB.getZSpan());
            BlockPos randomPos = new BlockPos(x, y, z);
            if (level.getBlockState(randomPos).isAir() && chunkBB.isInside(randomPos)) {
               level.setBlock(randomPos, Blocks.DRIED_GHAST.defaultBlockState().rotate(Rotation.getRandom(positionalRandom)), 2);
            }
         }
      }
   }
}
