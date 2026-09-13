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
      i -> i.group(
            settingsCodec(i),
            OceanRuinStructure.Type.CODEC.fieldOf("biome_temp").forGetter(c -> c.biomeTemp),
            Codec.floatRange(0.0F, 1.0F).fieldOf("large_probability").forGetter(c -> c.largeProbability),
            Codec.floatRange(0.0F, 1.0F).fieldOf("cluster_probability").forGetter(c -> c.clusterProbability)
         )
         .apply(i, OceanRuinStructure::new)
   );
   public final OceanRuinStructure.Type biomeTemp;
   public final float largeProbability;
   public final float clusterProbability;

   public OceanRuinStructure(
      final Structure.StructureSettings settings, final OceanRuinStructure.Type biomeTemp, final float largeProbability, final float clusterProbability
   ) {
      super(settings);
      this.biomeTemp = biomeTemp;
      this.largeProbability = largeProbability;
      this.clusterProbability = clusterProbability;
   }

   @Override
   public Optional<Structure.GenerationStub> findGenerationPoint(final Structure.GenerationContext context) {
      return onTopOfChunkCenter(context, Heightmap.Types.OCEAN_FLOOR_WG, builder -> this.generatePieces(builder, context));
   }

   private void generatePieces(final StructurePiecesBuilder builder, final Structure.GenerationContext context) {
      BlockPos offset = new BlockPos(context.chunkPos().getMinBlockX(), 90, context.chunkPos().getMinBlockZ());
      Rotation rotation = Rotation.getRandom(context.random());
      OceanRuinPieces.addPieces(context.structureTemplateManager(), offset, rotation, builder, context.random(), this);
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

      Type(final String name) {
         this.name = name;
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
