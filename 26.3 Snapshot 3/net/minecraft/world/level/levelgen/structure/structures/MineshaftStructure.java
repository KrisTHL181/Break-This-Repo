package net.minecraft.world.level.levelgen.structure.structures;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.function.IntFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.Mth;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;

public class MineshaftStructure extends Structure {
   public static final MapCodec<MineshaftStructure> CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(settingsCodec(i), MineshaftStructure.Type.CODEC.fieldOf("mineshaft_type").forGetter(c -> c.type)).apply(i, MineshaftStructure::new)
   );
   private final MineshaftStructure.Type type;

   public MineshaftStructure(final Structure.StructureSettings settings, final MineshaftStructure.Type type) {
      super(settings);
      this.type = type;
   }

   @Override
   public Optional<Structure.GenerationStub> findGenerationPoint(final Structure.GenerationContext context) {
      context.random().nextDouble();
      ChunkPos chunkPos = context.chunkPos();
      BlockPos startPos = new BlockPos(chunkPos.getMiddleBlockX(), 50, chunkPos.getMinBlockZ());
      StructurePiecesBuilder mineshaftPiecesBuilder = new StructurePiecesBuilder();
      int yOffset = this.generatePiecesAndAdjust(mineshaftPiecesBuilder, context);
      return Optional.of(new Structure.GenerationStub(startPos.offset(0, yOffset, 0), Either.right(mineshaftPiecesBuilder)));
   }

   private int generatePiecesAndAdjust(final StructurePiecesBuilder builder, final Structure.GenerationContext context) {
      ChunkPos chunkPos = context.chunkPos();
      WorldgenRandom random = context.random();
      ChunkGenerator chunkGenerator = context.chunkGenerator();
      MineshaftPieces.MineShaftRoom mineShaftRoom = new MineshaftPieces.MineShaftRoom(0, random, chunkPos.getBlockX(2), chunkPos.getBlockZ(2), this.type);
      builder.addPiece(mineShaftRoom);
      mineShaftRoom.addChildren(mineShaftRoom, builder, random);
      int seaLevel = chunkGenerator.getSeaLevel();
      if (this.type == MineshaftStructure.Type.MESA) {
         BlockPos center = builder.getBoundingBox().getCenter();
         int surfaceHeight = chunkGenerator.getBaseHeight(
            center.getX(), center.getZ(), Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState()
         );
         int targetYForCenter = surfaceHeight <= seaLevel ? seaLevel : Mth.randomBetweenInclusive(random, seaLevel, surfaceHeight);
         int dy = targetYForCenter - center.getY();
         builder.offsetPiecesVertically(dy);
         return dy;
      } else {
         return builder.moveBelowSeaLevel(seaLevel, chunkGenerator.getMinY(), random, 10);
      }
   }

   @Override
   public StructureType<?> type() {
      return StructureType.MINESHAFT;
   }

   public enum Type implements StringRepresentable {
      NORMAL("normal", Blocks.OAK_LOG, Blocks.OAK_PLANKS, Blocks.OAK_FENCE),
      MESA("mesa", Blocks.DARK_OAK_LOG, Blocks.DARK_OAK_PLANKS, Blocks.DARK_OAK_FENCE);

      public static final Codec<MineshaftStructure.Type> CODEC = StringRepresentable.fromEnum(MineshaftStructure.Type::values);
      private static final IntFunction<MineshaftStructure.Type> BY_ID = ByIdMap.continuous(Enum::ordinal, values(), ByIdMap.OutOfBoundsStrategy.ZERO);
      private final String name;
      private final BlockState woodState;
      private final BlockState planksState;
      private final BlockState fenceState;

      Type(final String name, final Block wood, final Block plank, final Block fence) {
         this.name = name;
         this.woodState = wood.defaultBlockState();
         this.planksState = plank.defaultBlockState();
         this.fenceState = fence.defaultBlockState();
      }

      public String getName() {
         return this.name;
      }

      public static MineshaftStructure.Type byId(final int id) {
         return BY_ID.apply(id);
      }

      public BlockState getWoodState() {
         return this.woodState;
      }

      public BlockState getPlanksState() {
         return this.planksState;
      }

      public BlockState getFenceState() {
         return this.fenceState;
      }

      @Override
      public String getSerializedName() {
         return this.name;
      }
   }
}
