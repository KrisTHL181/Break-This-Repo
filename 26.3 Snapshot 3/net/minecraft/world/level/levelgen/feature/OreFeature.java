package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.MapCodec;
import java.util.BitSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.BulkSectionAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;

public class OreFeature extends AbstractOreFeature {
   public static final MapCodec<OreFeature> CODEC = makeCodec(OreFeature::new);

   public OreFeature(final List<BlockReplacement> targetStates, final int size, final float discardChanceOnAirExposure) {
      super(targetStates, size, discardChanceOnAirExposure);
   }

   public OreFeature(final List<BlockReplacement> targetStates, final int size) {
      this(targetStates, size, 0.0F);
   }

   public OreFeature(final RuleTest target, final BlockState state, final int size) {
      this(List.of(new BlockReplacement(target, state)), size, 0.0F);
   }

   @Override
   public MapCodec<OreFeature> codec() {
      return CODEC;
   }

   @Override
   public boolean place(final WorldGenLevel level, final ChunkGenerator chunkGenerator, final RandomSource random, final BlockPos origin) {
      float dir = random.nextFloat() * (float) Math.PI;
      float spreadXY = this.size / 8.0F;
      int maxRadius = Mth.ceil((this.size / 16.0F * 2.0F + 1.0F) / 2.0F);
      double x0 = origin.getX() + Math.sin(dir) * spreadXY;
      double x1 = origin.getX() - Math.sin(dir) * spreadXY;
      double z0 = origin.getZ() + Math.cos(dir) * spreadXY;
      double z1 = origin.getZ() - Math.cos(dir) * spreadXY;
      int spreadY = 2;
      double y0 = origin.getY() + random.nextInt(3) - 2;
      double y1 = origin.getY() + random.nextInt(3) - 2;
      int xStart = origin.getX() - Mth.ceil(spreadXY) - maxRadius;
      int yStart = origin.getY() - 2 - maxRadius;
      int zStart = origin.getZ() - Mth.ceil(spreadXY) - maxRadius;
      int sizeXZ = 2 * (Mth.ceil(spreadXY) + maxRadius);
      int sizeY = 2 * (2 + maxRadius);

      for (int xprobe = xStart; xprobe <= xStart + sizeXZ; xprobe++) {
         for (int zprobe = zStart; zprobe <= zStart + sizeXZ; zprobe++) {
            if (yStart <= level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, xprobe, zprobe)) {
               return this.doPlace(level, random, x0, x1, z0, z1, y0, y1, xStart, yStart, zStart, sizeXZ, sizeY);
            }
         }
      }

      return false;
   }

   protected boolean doPlace(
      final WorldGenLevel level,
      final RandomSource random,
      final double x0,
      final double x1,
      final double z0,
      final double z1,
      final double y0,
      final double y1,
      final int xStart,
      final int yStart,
      final int zStart,
      final int sizeXZ,
      final int sizeY
   ) {
      int placed = 0;
      BitSet tested = new BitSet(sizeXZ * sizeY * sizeXZ);
      BlockPos.MutableBlockPos orePos = new BlockPos.MutableBlockPos();
      double[] data = new double[this.size * 4];

      for (int i = 0; i < this.size; i++) {
         float step = (float)i / this.size;
         double xx = Mth.lerp(step, x0, x1);
         double yy = Mth.lerp(step, y0, y1);
         double zz = Mth.lerp(step, z0, z1);
         double ss = random.nextDouble() * this.size / 16.0;
         double r = ((Mth.sin((float) Math.PI * step) + 1.0F) * ss + 1.0) / 2.0;
         data[i * 4 + 0] = xx;
         data[i * 4 + 1] = yy;
         data[i * 4 + 2] = zz;
         data[i * 4 + 3] = r;
      }

      for (int i1 = 0; i1 < this.size - 1; i1++) {
         if (!(data[i1 * 4 + 3] <= 0.0)) {
            for (int i2 = i1 + 1; i2 < this.size; i2++) {
               if (!(data[i2 * 4 + 3] <= 0.0)) {
                  double dx = data[i1 * 4 + 0] - data[i2 * 4 + 0];
                  double dy = data[i1 * 4 + 1] - data[i2 * 4 + 1];
                  double dz = data[i1 * 4 + 2] - data[i2 * 4 + 2];
                  double dr = data[i1 * 4 + 3] - data[i2 * 4 + 3];
                  if (dr * dr > dx * dx + dy * dy + dz * dz) {
                     if (dr > 0.0) {
                        data[i2 * 4 + 3] = -1.0;
                     } else {
                        data[i1 * 4 + 3] = -1.0;
                     }
                  }
               }
            }
         }
      }

      try (BulkSectionAccess sectionGetter = new BulkSectionAccess(level)) {
         for (int i = 0; i < this.size; i++) {
            double r = data[i * 4 + 3];
            if (!(r < 0.0)) {
               double xx = data[i * 4 + 0];
               double yy = data[i * 4 + 1];
               double zz = data[i * 4 + 2];
               int xMin = Math.max(Mth.floor(xx - r), xStart);
               int yMin = Math.max(Mth.floor(yy - r), yStart);
               int zMin = Math.max(Mth.floor(zz - r), zStart);
               int xMax = Math.max(Mth.floor(xx + r), xMin);
               int yMax = Math.max(Mth.floor(yy + r), yMin);
               int zMax = Math.max(Mth.floor(zz + r), zMin);

               for (int x = xMin; x <= xMax; x++) {
                  double xd = (x + 0.5 - xx) / r;
                  if (xd * xd < 1.0) {
                     for (int y = yMin; y <= yMax; y++) {
                        double yd = (y + 0.5 - yy) / r;
                        if (xd * xd + yd * yd < 1.0) {
                           for (int z = zMin; z <= zMax; z++) {
                              double zd = (z + 0.5 - zz) / r;
                              if (xd * xd + yd * yd + zd * zd < 1.0 && !level.isOutsideBuildHeight(y)) {
                                 int bitSetIndex = x - xStart + (y - yStart) * sizeXZ + (z - zStart) * sizeXZ * sizeY;
                                 if (!tested.get(bitSetIndex)) {
                                    tested.set(bitSetIndex);
                                    orePos.set(x, y, z);
                                    if (level.ensureCanWrite(orePos)) {
                                       LevelChunkSection section = sectionGetter.getSection(orePos);
                                       if (section != null) {
                                          int sectionRelativeX = SectionPos.sectionRelative(x);
                                          int sectionRelativeY = SectionPos.sectionRelative(y);
                                          int sectionRelativeZ = SectionPos.sectionRelative(z);
                                          BlockState blockState = section.getBlockState(sectionRelativeX, sectionRelativeY, sectionRelativeZ);

                                          for (BlockReplacement targetState : this.targetStates) {
                                             if (this.canPlaceOre(blockState, sectionGetter::getBlockState, random, targetState, orePos)) {
                                                section.setBlockState(sectionRelativeX, sectionRelativeY, sectionRelativeZ, targetState.state(), false);
                                                placed++;
                                                break;
                                             }
                                          }
                                       }
                                    }
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }

      return placed > 0;
   }
}
