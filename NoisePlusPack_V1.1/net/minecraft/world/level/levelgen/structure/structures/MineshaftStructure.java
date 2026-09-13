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
      p_227971_ -> p_227971_.group(settingsCodec(p_227971_), MineshaftStructure.Type.CODEC.fieldOf("mineshaft_type").forGetter(p_227969_ -> p_227969_.type))
         .apply(p_227971_, MineshaftStructure::new)
   );
   private final MineshaftStructure.Type type;

   public MineshaftStructure(Structure.StructureSettings p_227961_, MineshaftStructure.Type p_227962_) {
      super(p_227961_);
      this.type = p_227962_;
   }

   @Override
   public Optional<Structure.GenerationStub> findGenerationPoint(Structure.GenerationContext p_227964_) {
      p_227964_.random().nextDouble();
      ChunkPos chunkpos = p_227964_.chunkPos();
      BlockPos blockpos = new BlockPos(chunkpos.getMiddleBlockX(), 50, chunkpos.getMinBlockZ());
      StructurePiecesBuilder structurepiecesbuilder = new StructurePiecesBuilder();
      int i = this.generatePiecesAndAdjust(structurepiecesbuilder, p_227964_);
      return Optional.of(new Structure.GenerationStub(blockpos.offset(0, i, 0), Either.right(structurepiecesbuilder)));
   }

   private int generatePiecesAndAdjust(StructurePiecesBuilder p_227966_, Structure.GenerationContext p_227967_) {
      ChunkPos chunkpos = p_227967_.chunkPos();
      WorldgenRandom worldgenrandom = p_227967_.random();
      ChunkGenerator chunkgenerator = p_227967_.chunkGenerator();
      MineshaftPieces.MineShaftRoom mineshaftpieces$mineshaftroom = new MineshaftPieces.MineShaftRoom(
         0, worldgenrandom, chunkpos.getBlockX(2), chunkpos.getBlockZ(2), this.type
      );
      p_227966_.addPiece(mineshaftpieces$mineshaftroom);
      mineshaftpieces$mineshaftroom.addChildren(mineshaftpieces$mineshaftroom, p_227966_, worldgenrandom);
      int i = chunkgenerator.getSeaLevel();
      if (this.type == MineshaftStructure.Type.MESA) {
         BlockPos blockpos = p_227966_.getBoundingBox().getCenter();
         int j = chunkgenerator.getBaseHeight(
            blockpos.getX(), blockpos.getZ(), Heightmap.Types.WORLD_SURFACE_WG, p_227967_.heightAccessor(), p_227967_.randomState()
         );
         int k = j <= i ? i : Mth.randomBetweenInclusive(worldgenrandom, i, j);
         int l = k - blockpos.getY();
         p_227966_.offsetPiecesVertically(l);
         return l;
      } else {
         return p_227966_.moveBelowSeaLevel(i, chunkgenerator.getMinY(), worldgenrandom, 10);
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

      Type(final String p_227985_, final Block p_227986_, final Block p_227987_, final Block p_227988_) {
         this.name = p_227985_;
         this.woodState = p_227986_.defaultBlockState();
         this.planksState = p_227987_.defaultBlockState();
         this.fenceState = p_227988_.defaultBlockState();
      }

      public String getName() {
         return this.name;
      }

      public static MineshaftStructure.Type byId(int p_227991_) {
         return BY_ID.apply(p_227991_);
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
