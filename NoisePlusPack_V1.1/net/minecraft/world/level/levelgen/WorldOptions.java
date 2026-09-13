package net.minecraft.world.level.levelgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.OptionalLong;
import net.minecraft.util.RandomSource;
import org.apache.commons.lang3.StringUtils;

public class WorldOptions {
   public static final MapCodec<WorldOptions> CODEC = RecordCodecBuilder.mapCodec(
      p_327460_ -> p_327460_.group(
            Codec.LONG.fieldOf("seed").stable().forGetter(WorldOptions::seed),
            Codec.BOOL.fieldOf("generate_features").orElse(true).stable().forGetter(WorldOptions::generateStructures),
            Codec.BOOL.fieldOf("bonus_chest").orElse(false).stable().forGetter(WorldOptions::generateBonusChest),
            Codec.STRING.lenientOptionalFieldOf("legacy_custom_options").stable().forGetter(p_249400_ -> p_249400_.legacyCustomOptions)
         )
         .apply(p_327460_, p_327460_.stable(WorldOptions::new))
   );
   public static final WorldOptions DEMO_OPTIONS = new WorldOptions("North Carolina".hashCode(), true, true);
   private final long seed;
   private final boolean generateStructures;
   private final boolean generateBonusChest;
   private final Optional<String> legacyCustomOptions;

   public WorldOptions(long p_251567_, boolean p_250743_, boolean p_250454_) {
      this(p_251567_, p_250743_, p_250454_, Optional.empty());
   }

   public static WorldOptions defaultWithRandomSeed() {
      return new WorldOptions(randomSeed(), true, false);
   }

   public static WorldOptions testWorldWithRandomSeed() {
      return new WorldOptions(randomSeed(), false, false);
   }

   private WorldOptions(long p_249191_, boolean p_250927_, boolean p_249013_, Optional<String> p_250735_) {
      this.seed = p_249191_;
      this.generateStructures = p_250927_;
      this.generateBonusChest = p_249013_;
      this.legacyCustomOptions = p_250735_;
   }

   public long seed() {
      return this.seed;
   }

   public boolean generateStructures() {
      return this.generateStructures;
   }

   public boolean generateBonusChest() {
      return this.generateBonusChest;
   }

   public boolean isOldCustomizedWorld() {
      return this.legacyCustomOptions.isPresent();
   }

   public WorldOptions withBonusChest(boolean p_251744_) {
      return new WorldOptions(this.seed, this.generateStructures, p_251744_, this.legacyCustomOptions);
   }

   public WorldOptions withStructures(boolean p_251426_) {
      return new WorldOptions(this.seed, p_251426_, this.generateBonusChest, this.legacyCustomOptions);
   }

   public WorldOptions withSeed(OptionalLong p_261572_) {
      return new WorldOptions(p_261572_.orElse(randomSeed()), this.generateStructures, this.generateBonusChest, this.legacyCustomOptions);
   }

   public static OptionalLong parseSeed(String p_262144_) {
      p_262144_ = p_262144_.trim();
      if (StringUtils.isEmpty(p_262144_)) {
         return OptionalLong.empty();
      }

      try {
         return OptionalLong.of(Long.parseLong(p_262144_));
      } catch (NumberFormatException numberformatexception) {
         return OptionalLong.of(p_262144_.hashCode());
      }
   }

   public static long randomSeed() {
      return RandomSource.create().nextLong();
   }
}
