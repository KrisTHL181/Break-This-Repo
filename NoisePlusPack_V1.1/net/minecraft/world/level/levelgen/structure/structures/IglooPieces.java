package net.minecraft.world.level.levelgen.structure.structures;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePieceAccessor;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;

public class IglooPieces {
   public static final int GENERATION_HEIGHT = 90;
   static final Identifier STRUCTURE_LOCATION_IGLOO = Identifier.withDefaultNamespace("igloo/top");
   private static final Identifier STRUCTURE_LOCATION_LADDER = Identifier.withDefaultNamespace("igloo/middle");
   private static final Identifier STRUCTURE_LOCATION_LABORATORY = Identifier.withDefaultNamespace("igloo/bottom");
   static final Map<Identifier, BlockPos> PIVOTS = ImmutableMap.of(
      STRUCTURE_LOCATION_IGLOO, new BlockPos(3, 5, 5), STRUCTURE_LOCATION_LADDER, new BlockPos(1, 3, 1), STRUCTURE_LOCATION_LABORATORY, new BlockPos(3, 6, 7)
   );
   static final Map<Identifier, BlockPos> OFFSETS = ImmutableMap.of(
      STRUCTURE_LOCATION_IGLOO, BlockPos.ZERO, STRUCTURE_LOCATION_LADDER, new BlockPos(2, -3, 4), STRUCTURE_LOCATION_LABORATORY, new BlockPos(0, -3, -2)
   );

   public static void addPieces(
      StructureTemplateManager p_227549_, BlockPos p_227550_, Rotation p_227551_, StructurePieceAccessor p_227552_, RandomSource p_227553_
   ) {
      if (p_227553_.nextDouble() < 0.5) {
         int i = p_227553_.nextInt(8) + 4;
         p_227552_.addPiece(new IglooPieces.IglooPiece(p_227549_, STRUCTURE_LOCATION_LABORATORY, p_227550_, p_227551_, i * 3));

         for (int j = 0; j < i - 1; j++) {
            p_227552_.addPiece(new IglooPieces.IglooPiece(p_227549_, STRUCTURE_LOCATION_LADDER, p_227550_, p_227551_, j * 3));
         }
      }

      p_227552_.addPiece(new IglooPieces.IglooPiece(p_227549_, STRUCTURE_LOCATION_IGLOO, p_227550_, p_227551_, 0));
   }

   public static class IglooPiece extends TemplateStructurePiece {
      public IglooPiece(StructureTemplateManager p_227555_, Identifier p_458311_, BlockPos p_227557_, Rotation p_227558_, int p_227559_) {
         super(
            StructurePieceType.IGLOO,
            0,
            p_227555_,
            p_458311_,
            p_458311_.toString(),
            makeSettings(p_227558_, p_458311_),
            makePosition(p_458311_, p_227557_, p_227559_)
         );
      }

      public IglooPiece(StructureTemplateManager p_227561_, CompoundTag p_227562_) {
         super(
            StructurePieceType.IGLOO,
            p_227562_,
            p_227561_,
            p_450043_ -> makeSettings(p_227562_.<Rotation>read("Rot", Rotation.LEGACY_CODEC).orElseThrow(), p_450043_)
         );
      }

      private static StructurePlaceSettings makeSettings(Rotation p_227576_, Identifier p_452090_) {
         return new StructurePlaceSettings()
            .setRotation(p_227576_)
            .setMirror(Mirror.NONE)
            .setRotationPivot(IglooPieces.PIVOTS.get(p_452090_))
            .addProcessor(BlockIgnoreProcessor.STRUCTURE_BLOCK)
            .setLiquidSettings(LiquidSettings.IGNORE_WATERLOGGING);
      }

      private static BlockPos makePosition(Identifier p_458160_, BlockPos p_227565_, int p_227566_) {
         return p_227565_.offset(IglooPieces.OFFSETS.get(p_458160_)).below(p_227566_);
      }

      @Override
      protected void addAdditionalSaveData(StructurePieceSerializationContext p_227579_, CompoundTag p_227580_) {
         super.addAdditionalSaveData(p_227579_, p_227580_);
         p_227580_.store("Rot", Rotation.LEGACY_CODEC, this.placeSettings.getRotation());
      }

      @Override
      protected void handleDataMarker(String p_227582_, BlockPos p_227583_, ServerLevelAccessor p_227584_, RandomSource p_227585_, BoundingBox p_227586_) {
         if ("chest".equals(p_227582_)) {
            p_227584_.setBlock(p_227583_, Blocks.AIR.defaultBlockState(), 3);
            BlockEntity blockentity = p_227584_.getBlockEntity(p_227583_.below());
            if (blockentity instanceof ChestBlockEntity) {
               ((ChestBlockEntity)blockentity).setLootTable(BuiltInLootTables.IGLOO_CHEST, p_227585_.nextLong());
            }
         }
      }

      @Override
      public void postProcess(
         WorldGenLevel p_227568_,
         StructureManager p_227569_,
         ChunkGenerator p_227570_,
         RandomSource p_227571_,
         BoundingBox p_227572_,
         ChunkPos p_227573_,
         BlockPos p_227574_
      ) {
         Identifier identifier = Identifier.parse(this.templateName);
         StructurePlaceSettings structureplacesettings = makeSettings(this.placeSettings.getRotation(), identifier);
         BlockPos blockpos = IglooPieces.OFFSETS.get(identifier);
         BlockPos blockpos1 = this.templatePosition
            .offset(StructureTemplate.calculateRelativePosition(structureplacesettings, new BlockPos(3 - blockpos.getX(), 0, -blockpos.getZ())));
         int i = p_227568_.getHeight(Heightmap.Types.WORLD_SURFACE_WG, blockpos1.getX(), blockpos1.getZ());
         BlockPos blockpos2 = this.templatePosition;
         this.templatePosition = this.templatePosition.offset(0, i - 90 - 1, 0);
         super.postProcess(p_227568_, p_227569_, p_227570_, p_227571_, p_227572_, p_227573_, p_227574_);
         if (identifier.equals(IglooPieces.STRUCTURE_LOCATION_IGLOO)) {
            BlockPos blockpos3 = this.templatePosition.offset(StructureTemplate.calculateRelativePosition(structureplacesettings, new BlockPos(3, 0, 5)));
            BlockState blockstate = p_227568_.getBlockState(blockpos3.below());
            if (!blockstate.isAir() && !blockstate.is(Blocks.LADDER)) {
               p_227568_.setBlock(blockpos3, Blocks.SNOW_BLOCK.defaultBlockState(), 3);
            }
         }

         this.templatePosition = blockpos2;
      }
   }
}
