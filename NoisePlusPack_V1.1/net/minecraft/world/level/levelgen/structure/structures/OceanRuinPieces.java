package net.minecraft.world.level.levelgen.structure.structures;

import com.google.common.collect.Lists;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.zombie.Drowned;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
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
import net.minecraft.world.level.levelgen.structure.templatesystem.AlwaysTrueTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockMatchTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockRotProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.CappedProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.PosAlwaysTrueTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.ProcessorRule;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.levelgen.structure.templatesystem.rule.blockentity.AppendLoot;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootTable;

public class OceanRuinPieces {
   static final StructureProcessor WARM_SUSPICIOUS_BLOCK_PROCESSOR = archyRuleProcessor(
      Blocks.SAND, Blocks.SUSPICIOUS_SAND, BuiltInLootTables.OCEAN_RUIN_WARM_ARCHAEOLOGY
   );
   static final StructureProcessor COLD_SUSPICIOUS_BLOCK_PROCESSOR = archyRuleProcessor(
      Blocks.GRAVEL, Blocks.SUSPICIOUS_GRAVEL, BuiltInLootTables.OCEAN_RUIN_COLD_ARCHAEOLOGY
   );
   private static final Identifier[] WARM_RUINS = new Identifier[]{
      Identifier.withDefaultNamespace("underwater_ruin/warm_1"),
      Identifier.withDefaultNamespace("underwater_ruin/warm_2"),
      Identifier.withDefaultNamespace("underwater_ruin/warm_3"),
      Identifier.withDefaultNamespace("underwater_ruin/warm_4"),
      Identifier.withDefaultNamespace("underwater_ruin/warm_5"),
      Identifier.withDefaultNamespace("underwater_ruin/warm_6"),
      Identifier.withDefaultNamespace("underwater_ruin/warm_7"),
      Identifier.withDefaultNamespace("underwater_ruin/warm_8")
   };
   private static final Identifier[] RUINS_BRICK = new Identifier[]{
      Identifier.withDefaultNamespace("underwater_ruin/brick_1"),
      Identifier.withDefaultNamespace("underwater_ruin/brick_2"),
      Identifier.withDefaultNamespace("underwater_ruin/brick_3"),
      Identifier.withDefaultNamespace("underwater_ruin/brick_4"),
      Identifier.withDefaultNamespace("underwater_ruin/brick_5"),
      Identifier.withDefaultNamespace("underwater_ruin/brick_6"),
      Identifier.withDefaultNamespace("underwater_ruin/brick_7"),
      Identifier.withDefaultNamespace("underwater_ruin/brick_8")
   };
   private static final Identifier[] RUINS_CRACKED = new Identifier[]{
      Identifier.withDefaultNamespace("underwater_ruin/cracked_1"),
      Identifier.withDefaultNamespace("underwater_ruin/cracked_2"),
      Identifier.withDefaultNamespace("underwater_ruin/cracked_3"),
      Identifier.withDefaultNamespace("underwater_ruin/cracked_4"),
      Identifier.withDefaultNamespace("underwater_ruin/cracked_5"),
      Identifier.withDefaultNamespace("underwater_ruin/cracked_6"),
      Identifier.withDefaultNamespace("underwater_ruin/cracked_7"),
      Identifier.withDefaultNamespace("underwater_ruin/cracked_8")
   };
   private static final Identifier[] RUINS_MOSSY = new Identifier[]{
      Identifier.withDefaultNamespace("underwater_ruin/mossy_1"),
      Identifier.withDefaultNamespace("underwater_ruin/mossy_2"),
      Identifier.withDefaultNamespace("underwater_ruin/mossy_3"),
      Identifier.withDefaultNamespace("underwater_ruin/mossy_4"),
      Identifier.withDefaultNamespace("underwater_ruin/mossy_5"),
      Identifier.withDefaultNamespace("underwater_ruin/mossy_6"),
      Identifier.withDefaultNamespace("underwater_ruin/mossy_7"),
      Identifier.withDefaultNamespace("underwater_ruin/mossy_8")
   };
   private static final Identifier[] BIG_RUINS_BRICK = new Identifier[]{
      Identifier.withDefaultNamespace("underwater_ruin/big_brick_1"),
      Identifier.withDefaultNamespace("underwater_ruin/big_brick_2"),
      Identifier.withDefaultNamespace("underwater_ruin/big_brick_3"),
      Identifier.withDefaultNamespace("underwater_ruin/big_brick_8")
   };
   private static final Identifier[] BIG_RUINS_MOSSY = new Identifier[]{
      Identifier.withDefaultNamespace("underwater_ruin/big_mossy_1"),
      Identifier.withDefaultNamespace("underwater_ruin/big_mossy_2"),
      Identifier.withDefaultNamespace("underwater_ruin/big_mossy_3"),
      Identifier.withDefaultNamespace("underwater_ruin/big_mossy_8")
   };
   private static final Identifier[] BIG_RUINS_CRACKED = new Identifier[]{
      Identifier.withDefaultNamespace("underwater_ruin/big_cracked_1"),
      Identifier.withDefaultNamespace("underwater_ruin/big_cracked_2"),
      Identifier.withDefaultNamespace("underwater_ruin/big_cracked_3"),
      Identifier.withDefaultNamespace("underwater_ruin/big_cracked_8")
   };
   private static final Identifier[] BIG_WARM_RUINS = new Identifier[]{
      Identifier.withDefaultNamespace("underwater_ruin/big_warm_4"),
      Identifier.withDefaultNamespace("underwater_ruin/big_warm_5"),
      Identifier.withDefaultNamespace("underwater_ruin/big_warm_6"),
      Identifier.withDefaultNamespace("underwater_ruin/big_warm_7")
   };

   private static StructureProcessor archyRuleProcessor(Block p_277376_, Block p_277934_, ResourceKey<LootTable> p_330231_) {
      return new CappedProcessor(
         new RuleProcessor(
            List.of(
               new ProcessorRule(
                  new BlockMatchTest(p_277376_), AlwaysTrueTest.INSTANCE, PosAlwaysTrueTest.INSTANCE, p_277934_.defaultBlockState(), new AppendLoot(p_330231_)
               )
            )
         ),
         ConstantInt.of(5)
      );
   }

   private static Identifier getSmallWarmRuin(RandomSource p_228983_) {
      return Util.getRandom(WARM_RUINS, p_228983_);
   }

   private static Identifier getBigWarmRuin(RandomSource p_229011_) {
      return Util.getRandom(BIG_WARM_RUINS, p_229011_);
   }

   public static void addPieces(
      StructureTemplateManager p_228995_,
      BlockPos p_228996_,
      Rotation p_228997_,
      StructurePieceAccessor p_228998_,
      RandomSource p_228999_,
      OceanRuinStructure p_229000_
   ) {
      boolean flag = p_228999_.nextFloat() <= p_229000_.largeProbability;
      float f = flag ? 0.9F : 0.8F;
      addPiece(p_228995_, p_228996_, p_228997_, p_228998_, p_228999_, p_229000_, flag, f);
      if (flag && p_228999_.nextFloat() <= p_229000_.clusterProbability) {
         addClusterRuins(p_228995_, p_228999_, p_228997_, p_228996_, p_229000_, p_228998_);
      }
   }

   private static void addClusterRuins(
      StructureTemplateManager p_228988_,
      RandomSource p_228989_,
      Rotation p_228990_,
      BlockPos p_228991_,
      OceanRuinStructure p_228992_,
      StructurePieceAccessor p_228993_
   ) {
      BlockPos blockpos = new BlockPos(p_228991_.getX(), 90, p_228991_.getZ());
      BlockPos blockpos1 = StructureTemplate.transform(new BlockPos(15, 0, 15), Mirror.NONE, p_228990_, BlockPos.ZERO).offset(blockpos);
      BoundingBox boundingbox = BoundingBox.fromCorners(blockpos, blockpos1);
      BlockPos blockpos2 = new BlockPos(Math.min(blockpos.getX(), blockpos1.getX()), blockpos.getY(), Math.min(blockpos.getZ(), blockpos1.getZ()));
      List<BlockPos> list = allPositions(p_228989_, blockpos2);
      int i = Mth.nextInt(p_228989_, 4, 8);

      for (int j = 0; j < i; j++) {
         if (!list.isEmpty()) {
            int k = p_228989_.nextInt(list.size());
            BlockPos blockpos3 = list.remove(k);
            Rotation rotation = Rotation.getRandom(p_228989_);
            BlockPos blockpos4 = StructureTemplate.transform(new BlockPos(5, 0, 6), Mirror.NONE, rotation, BlockPos.ZERO).offset(blockpos3);
            BoundingBox boundingbox1 = BoundingBox.fromCorners(blockpos3, blockpos4);
            if (!boundingbox1.intersects(boundingbox)) {
               addPiece(p_228988_, blockpos3, rotation, p_228993_, p_228989_, p_228992_, false, 0.8F);
            }
         }
      }
   }

   private static List<BlockPos> allPositions(RandomSource p_228985_, BlockPos p_228986_) {
      List<BlockPos> list = Lists.newArrayList();
      list.add(p_228986_.offset(-16 + Mth.nextInt(p_228985_, 1, 8), 0, 16 + Mth.nextInt(p_228985_, 1, 7)));
      list.add(p_228986_.offset(-16 + Mth.nextInt(p_228985_, 1, 8), 0, Mth.nextInt(p_228985_, 1, 7)));
      list.add(p_228986_.offset(-16 + Mth.nextInt(p_228985_, 1, 8), 0, -16 + Mth.nextInt(p_228985_, 4, 8)));
      list.add(p_228986_.offset(Mth.nextInt(p_228985_, 1, 7), 0, 16 + Mth.nextInt(p_228985_, 1, 7)));
      list.add(p_228986_.offset(Mth.nextInt(p_228985_, 1, 7), 0, -16 + Mth.nextInt(p_228985_, 4, 6)));
      list.add(p_228986_.offset(16 + Mth.nextInt(p_228985_, 1, 7), 0, 16 + Mth.nextInt(p_228985_, 3, 8)));
      list.add(p_228986_.offset(16 + Mth.nextInt(p_228985_, 1, 7), 0, Mth.nextInt(p_228985_, 1, 7)));
      list.add(p_228986_.offset(16 + Mth.nextInt(p_228985_, 1, 7), 0, -16 + Mth.nextInt(p_228985_, 4, 8)));
      return list;
   }

   private static void addPiece(
      StructureTemplateManager p_229002_,
      BlockPos p_229003_,
      Rotation p_229004_,
      StructurePieceAccessor p_229005_,
      RandomSource p_229006_,
      OceanRuinStructure p_229007_,
      boolean p_229008_,
      float p_229009_
   ) {
      switch (p_229007_.biomeTemp) {
         case WARM:
         default:
            Identifier identifier = p_229008_ ? getBigWarmRuin(p_229006_) : getSmallWarmRuin(p_229006_);
            p_229005_.addPiece(new OceanRuinPieces.OceanRuinPiece(p_229002_, identifier, p_229003_, p_229004_, p_229009_, p_229007_.biomeTemp, p_229008_));
            break;
         case COLD:
            Identifier[] aidentifier = p_229008_ ? BIG_RUINS_BRICK : RUINS_BRICK;
            Identifier[] aidentifier1 = p_229008_ ? BIG_RUINS_CRACKED : RUINS_CRACKED;
            Identifier[] aidentifier2 = p_229008_ ? BIG_RUINS_MOSSY : RUINS_MOSSY;
            int i = p_229006_.nextInt(aidentifier.length);
            p_229005_.addPiece(new OceanRuinPieces.OceanRuinPiece(p_229002_, aidentifier[i], p_229003_, p_229004_, p_229009_, p_229007_.biomeTemp, p_229008_));
            p_229005_.addPiece(new OceanRuinPieces.OceanRuinPiece(p_229002_, aidentifier1[i], p_229003_, p_229004_, 0.7F, p_229007_.biomeTemp, p_229008_));
            p_229005_.addPiece(new OceanRuinPieces.OceanRuinPiece(p_229002_, aidentifier2[i], p_229003_, p_229004_, 0.5F, p_229007_.biomeTemp, p_229008_));
      }
   }

   public static class OceanRuinPiece extends TemplateStructurePiece {
      private final OceanRuinStructure.Type biomeType;
      private final float integrity;
      private final boolean isLarge;

      public OceanRuinPiece(
         StructureTemplateManager p_229018_,
         Identifier p_456956_,
         BlockPos p_229020_,
         Rotation p_229021_,
         float p_229022_,
         OceanRuinStructure.Type p_229023_,
         boolean p_229024_
      ) {
         super(StructurePieceType.OCEAN_RUIN, 0, p_229018_, p_456956_, p_456956_.toString(), makeSettings(p_229021_, p_229022_, p_229023_), p_229020_);
         this.integrity = p_229022_;
         this.biomeType = p_229023_;
         this.isLarge = p_229024_;
      }

      private OceanRuinPiece(
         StructureTemplateManager p_277563_, CompoundTag p_277610_, Rotation p_277637_, float p_277437_, OceanRuinStructure.Type p_277873_, boolean p_277924_
      ) {
         super(StructurePieceType.OCEAN_RUIN, p_277610_, p_277563_, p_459939_ -> makeSettings(p_277637_, p_277437_, p_277873_));
         this.integrity = p_277437_;
         this.biomeType = p_277873_;
         this.isLarge = p_277924_;
      }

      private static StructurePlaceSettings makeSettings(Rotation p_277572_, float p_277489_, OceanRuinStructure.Type p_277631_) {
         StructureProcessor structureprocessor = p_277631_ == OceanRuinStructure.Type.COLD
            ? OceanRuinPieces.COLD_SUSPICIOUS_BLOCK_PROCESSOR
            : OceanRuinPieces.WARM_SUSPICIOUS_BLOCK_PROCESSOR;
         return new StructurePlaceSettings()
            .setRotation(p_277572_)
            .setMirror(Mirror.NONE)
            .addProcessor(new BlockRotProcessor(p_277489_))
            .addProcessor(BlockIgnoreProcessor.STRUCTURE_AND_AIR)
            .addProcessor(structureprocessor);
      }

      public static OceanRuinPieces.OceanRuinPiece create(StructureTemplateManager p_277874_, CompoundTag p_277773_) {
         Rotation rotation = p_277773_.<Rotation>read("Rot", Rotation.LEGACY_CODEC).orElseThrow();
         float f = p_277773_.getFloatOr("Integrity", 0.0F);
         OceanRuinStructure.Type oceanruinstructure$type = p_277773_.<OceanRuinStructure.Type>read("BiomeType", OceanRuinStructure.Type.LEGACY_CODEC)
            .orElseThrow();
         boolean flag = p_277773_.getBooleanOr("IsLarge", false);
         return new OceanRuinPieces.OceanRuinPiece(p_277874_, p_277773_, rotation, f, oceanruinstructure$type, flag);
      }

      @Override
      protected void addAdditionalSaveData(StructurePieceSerializationContext p_229039_, CompoundTag p_229040_) {
         super.addAdditionalSaveData(p_229039_, p_229040_);
         p_229040_.store("Rot", Rotation.LEGACY_CODEC, this.placeSettings.getRotation());
         p_229040_.putFloat("Integrity", this.integrity);
         p_229040_.store("BiomeType", OceanRuinStructure.Type.LEGACY_CODEC, this.biomeType);
         p_229040_.putBoolean("IsLarge", this.isLarge);
      }

      @Override
      protected void handleDataMarker(String p_229046_, BlockPos p_229047_, ServerLevelAccessor p_229048_, RandomSource p_229049_, BoundingBox p_229050_) {
         if ("chest".equals(p_229046_)) {
            p_229048_.setBlock(
               p_229047_, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.WATERLOGGED, p_229048_.getFluidState(p_229047_).is(FluidTags.WATER)), 2
            );
            BlockEntity blockentity = p_229048_.getBlockEntity(p_229047_);
            if (blockentity instanceof ChestBlockEntity) {
               ((ChestBlockEntity)blockentity)
                  .setLootTable(this.isLarge ? BuiltInLootTables.UNDERWATER_RUIN_BIG : BuiltInLootTables.UNDERWATER_RUIN_SMALL, p_229049_.nextLong());
            }
         } else if ("drowned".equals(p_229046_)) {
            Drowned drowned = EntityType.DROWNED.create(p_229048_.getLevel(), EntitySpawnReason.STRUCTURE);
            if (drowned != null) {
               drowned.setPersistenceRequired();
               drowned.snapTo(p_229047_, 0.0F, 0.0F);
               drowned.finalizeSpawn(p_229048_, p_229048_.getCurrentDifficultyAt(p_229047_), EntitySpawnReason.STRUCTURE, null);
               p_229048_.addFreshEntityWithPassengers(drowned);
               if (p_229047_.getY() > p_229048_.getSeaLevel()) {
                  p_229048_.setBlock(p_229047_, Blocks.AIR.defaultBlockState(), 2);
               } else {
                  p_229048_.setBlock(p_229047_, Blocks.WATER.defaultBlockState(), 2);
               }
            }
         }
      }

      @Override
      public void postProcess(
         WorldGenLevel p_229029_,
         StructureManager p_229030_,
         ChunkGenerator p_229031_,
         RandomSource p_229032_,
         BoundingBox p_229033_,
         ChunkPos p_229034_,
         BlockPos p_229035_
      ) {
         int i = p_229029_.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, this.templatePosition.getX(), this.templatePosition.getZ());
         this.templatePosition = new BlockPos(this.templatePosition.getX(), i, this.templatePosition.getZ());
         BlockPos blockpos = StructureTemplate.transform(
               new BlockPos(this.template.getSize().getX() - 1, 0, this.template.getSize().getZ() - 1),
               Mirror.NONE,
               this.placeSettings.getRotation(),
               BlockPos.ZERO
            )
            .offset(this.templatePosition);
         this.templatePosition = new BlockPos(
            this.templatePosition.getX(), this.getHeight(this.templatePosition, p_229029_, blockpos), this.templatePosition.getZ()
         );
         super.postProcess(p_229029_, p_229030_, p_229031_, p_229032_, p_229033_, p_229034_, p_229035_);
      }

      private int getHeight(BlockPos p_229042_, BlockGetter p_229043_, BlockPos p_229044_) {
         int i = p_229042_.getY();
         int j = 512;
         int k = i - 1;
         int l = 0;

         for (BlockPos blockpos : BlockPos.betweenClosed(p_229042_, p_229044_)) {
            int i1 = blockpos.getX();
            int j1 = blockpos.getZ();
            int k1 = p_229042_.getY() - 1;
            BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos(i1, k1, j1);
            BlockState blockstate = p_229043_.getBlockState(blockpos$mutableblockpos);

            for (FluidState fluidstate = p_229043_.getFluidState(blockpos$mutableblockpos);
               (blockstate.isAir() || fluidstate.is(FluidTags.WATER) || blockstate.is(BlockTags.ICE)) && k1 > p_229043_.getMinY() + 1;
               fluidstate = p_229043_.getFluidState(blockpos$mutableblockpos)
            ) {
               blockpos$mutableblockpos.set(i1, --k1, j1);
               blockstate = p_229043_.getBlockState(blockpos$mutableblockpos);
            }

            j = Math.min(j, k1);
            if (k1 < k - 2) {
               l++;
            }
         }

         int l1 = Math.abs(p_229042_.getX() - p_229044_.getX());
         if (k - j > 2 && l > l1 - 2) {
            i = j + 1;
         }

         return i;
      }
   }
}
