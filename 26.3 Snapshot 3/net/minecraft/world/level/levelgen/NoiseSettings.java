package net.minecraft.world.level.levelgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.function.Function;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.dimension.DimensionType;

public record NoiseSettings(int minY, int height, int noiseSizeHorizontal, int noiseSizeVertical) {
   // ===== 修改：RecordCodecBuilder.create 显式类型参数，group 改为 i.group，comapFlatMap 第二个参数改为 lambda =====
   public static final Codec<NoiseSettings> CODEC = RecordCodecBuilder.<NoiseSettings>create(
         i -> i.group(
               Codec.intRange(DimensionType.MIN_Y, DimensionType.MAX_Y).fieldOf("min_y").forGetter(NoiseSettings::minY),
               Codec.intRange(0, DimensionType.Y_SIZE).fieldOf("height").forGetter(NoiseSettings::height),
               Codec.intRange(1, 4).fieldOf("size_horizontal").forGetter(NoiseSettings::noiseSizeHorizontal),
               Codec.intRange(1, 4).fieldOf("size_vertical").forGetter(NoiseSettings::noiseSizeVertical)
            )
            .apply(i, NoiseSettings::new)
      )
      .comapFlatMap(
         (NoiseSettings settings) -> guardY(settings),
         (NoiseSettings settings) -> settings
      );

   static final NoiseSettings OVERWORLD_NOISE_SETTINGS = create(-64, 384, 1, 2);
   static final NoiseSettings NETHER_NOISE_SETTINGS = create(0, 128, 1, 2);
   static final NoiseSettings END_NOISE_SETTINGS = create(0, 128, 2, 1);
   static final NoiseSettings CAVES_NOISE_SETTINGS = create(-64, 192, 1, 2);
   static final NoiseSettings FLOATING_ISLANDS_NOISE_SETTINGS = create(0, 256, 2, 1);

   private static DataResult<NoiseSettings> guardY(final NoiseSettings dimensionType) {
      if (dimensionType.minY() + dimensionType.height() > DimensionType.MAX_Y + 1) {
         return DataResult.error(() -> "min_y + height cannot be higher than: " + (DimensionType.MAX_Y + 1));
      } else if (dimensionType.height() % 16 != 0) {
         return DataResult.error(() -> "height has to be a multiple of 16");
      } else {
         return dimensionType.minY() % 16 != 0 ? DataResult.error(() -> "min_y has to be a multiple of 16") : DataResult.success(dimensionType);
      }
   }

   public static NoiseSettings create(final int minY, final int height, final int noiseSizeHorizontal, final int noiseSizeVertical) {
      NoiseSettings noiseSettings = new NoiseSettings(minY, height, noiseSizeHorizontal, noiseSizeVertical);
      guardY(noiseSettings).error().ifPresent(error -> {
         throw new IllegalStateException(error.message());
      });
      return noiseSettings;
   }

   public int getCellHeight() {
      return QuartPos.toBlock(this.noiseSizeVertical());
   }

   public int getCellWidth() {
      return QuartPos.toBlock(this.noiseSizeHorizontal());
   }

   public NoiseSettings clampToHeightAccessor(final LevelHeightAccessor heightAccessor) {
      int newMinY = Math.max(this.minY, heightAccessor.getMinY());
      int newHeight = Math.min(this.minY + this.height, heightAccessor.getMaxY() + 1) - newMinY;
      return new NoiseSettings(newMinY, newHeight, this.noiseSizeHorizontal, this.noiseSizeVertical);
   }
}