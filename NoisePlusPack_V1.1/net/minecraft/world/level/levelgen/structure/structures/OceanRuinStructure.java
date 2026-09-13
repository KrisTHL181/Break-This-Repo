package net.minecraft.world.level.levelgen.structure.structures;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;

public class OceanRuinStructure extends Structure {
   public static final MapCodec<OceanRuinStructure> CODEC = RecordCodecBuilder.mapCodec(
      p_229075_ -> p_229075_.group(
            settingsCodec(p_229075_),
            OceanRuinStructure.Type.CODEC.fieldOf("biome_temp").forGetter(p_229079_ -> p_229079_.biomeTemp),
            Codec.floatRange(0.0F, 1.0F).fieldOf("large_probability").forGetter(p_229077_ -> p_229077_.largeProbability),
            Codec.floatRange(0.0F, 1.0F).fieldOf("cluster_probability").forGetter(p_229073_ -> p_229073_.clusterProbability)
         )
         .apply(p_229075_, OceanRuinStructure::new)
   );
   public final OceanRuinStructure.Type biomeTemp;
   public final float largeProbability;
   public final float clusterProbability;

   public OceanRuinStructure(Structure.StructureSettings p_229060_, OceanRuinStructure.Type p_229061_, float p_229062_, float p_229063_) {
      super(p_229060_);
      this.biomeTemp = p_229061_;
      this.largeProbability = p_229062_;
      this.clusterProbability = p_229063_;
   }

   @Override
   public Optional<Structure.GenerationStub> findGenerationPoint(Structure.GenerationContext p_229065_) {
      return onTopOfChunkCenter(p_229065_, Heightmap.Types.OCEAN_FLOOR_WG, p_229068_ -> this.generatePieces(p_229068_, p_229065_));
   }

   private void generatePieces(StructurePiecesBuilder p_229070_, Structure.GenerationContext p_229071_) {
      BlockPos blockpos = new BlockPos(p_229071_.chunkPos().getMinBlockX(), 90, p_229071_.chunkPos().getMinBlockZ());
      Rotation rotation = Rotation.getRandom(p_229071_.random());
      OceanRuinPieces.addPieces(p_229071_.structureTemplateManager(), blockpos, rotation, p_229070_, p_229071_.random(), this);
   }

   @Override
   public StructureType<?> type() {
      return StructureType.OCEAN_RUIN;
   }

   public enum Type implements StringRepresentable {
      WARM("warm"),
      COLD("cold");

      public static final Codec<OceanRuinStructure.Type> CODEC = StringRepresentable.fromEnum(OceanRuinStructure.Type::values);
      @Deprecated
      public static final Codec<OceanRuinStructure.Type> LEGACY_CODEC = ExtraCodecs.legacyEnum(OceanRuinStructure.Type::valueOf);
      private final String name;

      Type(final String p_229090_) {
         this.name = p_229090_;
      }

      public String getName() {
         return this.name;
      }

      @Override
      public String getSerializedName() {
         return this.name;
      }
   }
}
