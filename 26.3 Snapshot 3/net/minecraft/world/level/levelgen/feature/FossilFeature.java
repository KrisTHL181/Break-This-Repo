package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import org.apache.commons.lang3.mutable.MutableInt;

public record FossilFeature(
   List<Identifier> fossilStructures,
   List<Identifier> overlayStructures,
   Holder<StructureProcessorList> fossilProcessors,
   Holder<StructureProcessorList> overlayProcessors,
   int maxEmptyCornersAllowed
) implements Feature {
   // ===== 修改：RecordCodecBuilder.<FossilFeature>group → i.group =====
   public static final MapCodec<FossilFeature> CODEC = RecordCodecBuilder.mapCodec(
         i -> i.group(
               ExtraCodecs.nonEmptyList(Identifier.CODEC.listOf()).fieldOf("fossil_structures").forGetter(FossilFeature::fossilStructures),
               ExtraCodecs.nonEmptyList(Identifier.CODEC.listOf()).fieldOf("overlay_structures").forGetter(FossilFeature::overlayStructures),
               StructureProcessorType.LIST_CODEC.fieldOf("fossil_processors").forGetter(FossilFeature::fossilProcessors),
               StructureProcessorType.LIST_CODEC.fieldOf("overlay_processors").forGetter(FossilFeature::overlayProcessors),
               Codec.intRange(0, 7).fieldOf("max_empty_corners_allowed").forGetter(FossilFeature::maxEmptyCornersAllowed)
            )
            .apply(i, FossilFeature::new)
      )
      .validate(
         (FossilFeature fossil) -> fossil.fossilStructures.size() == fossil.overlayStructures.size()
            ? DataResult.success(fossil)
            : DataResult.error(() -> "Fossil structure lists must be equal lengths")
      );

   @Override
   public MapCodec<FossilFeature> codec() {
      return CODEC;
   }

   @Override
   public boolean place(final WorldGenLevel level, final ChunkGenerator chunkGenerator, final RandomSource random, final BlockPos origin) {
      Rotation rotation = Rotation.getRandom(random);
      int fossilIndex = random.nextInt(this.fossilStructures().size());
      StructureTemplateManager structureTemplateManager = level.getLevel().getServer().getStructureManager();
      StructureTemplate fossilBase = structureTemplateManager.getOrCreate(this.fossilStructures().get(fossilIndex));
      StructureTemplate fossilOverlay = structureTemplateManager.getOrCreate(this.overlayStructures().get(fossilIndex));
      ChunkPos chunkPos = ChunkPos.containing(origin);
      BoundingBox boundingBox = new BoundingBox(
         chunkPos.getMinBlockX() - 16,
         level.getMinY(),
         chunkPos.getMinBlockZ() - 16,
         chunkPos.getMaxBlockX() + 16,
         level.getMaxY(),
         chunkPos.getMaxBlockZ() + 16
      );
      StructurePlaceSettings settings = new StructurePlaceSettings().setRotation(rotation).setBoundingBox(boundingBox).setRandom(random);
      Vec3i size = fossilBase.getSize(rotation);
      BlockPos lowCorner = origin.offset(-size.getX() / 2, 0, -size.getZ() / 2);
      int lowestSurfaceY = origin.getY();

      for (int xscan = 0; xscan < size.getX(); xscan++) {
         for (int zscan = 0; zscan < size.getZ(); zscan++) {
            lowestSurfaceY = Math.min(lowestSurfaceY, level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, lowCorner.getX() + xscan, lowCorner.getZ() + zscan));
         }
      }

      int targetY = Math.max(lowestSurfaceY - 15 - random.nextInt(10), level.getMinY() + 10);
      BlockPos targetPos = fossilBase.getZeroPositionWithTransform(lowCorner.atY(targetY), Mirror.NONE, rotation);
      if (countEmptyCorners(level, fossilBase.getBoundingBox(settings, targetPos)) > this.maxEmptyCornersAllowed()) {
         return false;
      }

      settings.clearProcessors();
      this.fossilProcessors().value().list().forEach(settings::addProcessor);
      fossilBase.placeInWorld(level, targetPos, targetPos, settings, random, 260);
      settings.clearProcessors();
      this.overlayProcessors().value().list().forEach(settings::addProcessor);
      fossilOverlay.placeInWorld(level, targetPos, targetPos, settings, random, 260);
      return true;
   }

   private static int countEmptyCorners(final WorldGenLevel level, final BoundingBox structureBounds) {
      MutableInt count = new MutableInt(0);
      structureBounds.forAllCorners(pos -> {
         BlockState state = level.getBlockState(pos);
         if (state.isAir() || state.is(Blocks.LAVA) || state.is(Blocks.WATER)) {
            count.add(1);
         }
      });
      return count.intValue();
   }
}