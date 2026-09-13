package net.minecraft.advancements.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.levelgen.structure.Structure;

public record LocationPredicate(
   Optional<LocationPredicate.PositionPredicate> position,
   Optional<HolderSet<Biome>> biomes,
   Optional<HolderSet<Structure>> structures,
   Optional<ResourceKey<Level>> dimension,
   Optional<Boolean> smokey,
   Optional<LightPredicate> light,
   Optional<BlockPredicate> block,
   Optional<FluidPredicate> fluid,
   Optional<Boolean> canSeeSky
) {
   public static final Codec<LocationPredicate> CODEC = RecordCodecBuilder.create(
      p_451010_ -> p_451010_.group(
            LocationPredicate.PositionPredicate.CODEC.optionalFieldOf("position").forGetter(LocationPredicate::position),
            RegistryCodecs.homogeneousList(Registries.BIOME).optionalFieldOf("biomes").forGetter(LocationPredicate::biomes),
            RegistryCodecs.homogeneousList(Registries.STRUCTURE).optionalFieldOf("structures").forGetter(LocationPredicate::structures),
            ResourceKey.codec(Registries.DIMENSION).optionalFieldOf("dimension").forGetter(LocationPredicate::dimension),
            Codec.BOOL.optionalFieldOf("smokey").forGetter(LocationPredicate::smokey),
            LightPredicate.CODEC.optionalFieldOf("light").forGetter(LocationPredicate::light),
            BlockPredicate.CODEC.optionalFieldOf("block").forGetter(LocationPredicate::block),
            FluidPredicate.CODEC.optionalFieldOf("fluid").forGetter(LocationPredicate::fluid),
            Codec.BOOL.optionalFieldOf("can_see_sky").forGetter(LocationPredicate::canSeeSky)
         )
         .apply(p_451010_, LocationPredicate::new)
   );

   public boolean matches(ServerLevel p_451599_, double p_450482_, double p_458822_, double p_458355_) {
      if (this.position.isPresent() && !this.position.get().matches(p_450482_, p_458822_, p_458355_)) {
         return false;
      }

      if (this.dimension.isPresent() && this.dimension.get() != p_451599_.dimension()) {
         return false;
      }

      BlockPos blockpos = BlockPos.containing(p_450482_, p_458822_, p_458355_);
      boolean flag = p_451599_.isLoaded(blockpos);
      if (!this.biomes.isPresent() || flag && this.biomes.get().contains(p_451599_.getBiome(blockpos))) {
         if (!this.structures.isPresent() || flag && p_451599_.structureManager().getStructureWithPieceAt(blockpos, this.structures.get()).isValid()) {
            if (!this.smokey.isPresent() || flag && this.smokey.get() == CampfireBlock.isSmokeyPos(p_451599_, blockpos)) {
               if (this.light.isPresent() && !this.light.get().matches(p_451599_, blockpos)) {
                  return false;
               } else if (this.block.isPresent() && !this.block.get().matches(p_451599_, blockpos)) {
                  return false;
               } else {
                  return this.fluid.isPresent() && !this.fluid.get().matches(p_451599_, blockpos)
                     ? false
                     : !this.canSeeSky.isPresent() || this.canSeeSky.get() == p_451599_.canSeeSky(blockpos);
               }
            } else {
               return false;
            }
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   public static class Builder {
      private MinMaxBounds.Doubles x = MinMaxBounds.Doubles.ANY;
      private MinMaxBounds.Doubles y = MinMaxBounds.Doubles.ANY;
      private MinMaxBounds.Doubles z = MinMaxBounds.Doubles.ANY;
      private Optional<HolderSet<Biome>> biomes = Optional.empty();
      private Optional<HolderSet<Structure>> structures = Optional.empty();
      private Optional<ResourceKey<Level>> dimension = Optional.empty();
      private Optional<Boolean> smokey = Optional.empty();
      private Optional<LightPredicate> light = Optional.empty();
      private Optional<BlockPredicate> block = Optional.empty();
      private Optional<FluidPredicate> fluid = Optional.empty();
      private Optional<Boolean> canSeeSky = Optional.empty();

      public static LocationPredicate.Builder location() {
         return new LocationPredicate.Builder();
      }

      public static LocationPredicate.Builder inBiome(Holder<Biome> p_451949_) {
         return location().setBiomes(HolderSet.direct(p_451949_));
      }

      public static LocationPredicate.Builder inDimension(ResourceKey<Level> p_460458_) {
         return location().setDimension(p_460458_);
      }

      public static LocationPredicate.Builder inStructure(Holder<Structure> p_451625_) {
         return location().setStructures(HolderSet.direct(p_451625_));
      }

      public static LocationPredicate.Builder atYLocation(MinMaxBounds.Doubles p_453644_) {
         return location().setY(p_453644_);
      }

      public LocationPredicate.Builder setX(MinMaxBounds.Doubles p_452266_) {
         this.x = p_452266_;
         return this;
      }

      public LocationPredicate.Builder setY(MinMaxBounds.Doubles p_459549_) {
         this.y = p_459549_;
         return this;
      }

      public LocationPredicate.Builder setZ(MinMaxBounds.Doubles p_457840_) {
         this.z = p_457840_;
         return this;
      }

      public LocationPredicate.Builder setBiomes(HolderSet<Biome> p_457119_) {
         this.biomes = Optional.of(p_457119_);
         return this;
      }

      public LocationPredicate.Builder setStructures(HolderSet<Structure> p_459074_) {
         this.structures = Optional.of(p_459074_);
         return this;
      }

      public LocationPredicate.Builder setDimension(ResourceKey<Level> p_453660_) {
         this.dimension = Optional.of(p_453660_);
         return this;
      }

      public LocationPredicate.Builder setLight(LightPredicate.Builder p_455891_) {
         this.light = Optional.of(p_455891_.build());
         return this;
      }

      public LocationPredicate.Builder setBlock(BlockPredicate.Builder p_460329_) {
         this.block = Optional.of(p_460329_.build());
         return this;
      }

      public LocationPredicate.Builder setFluid(FluidPredicate.Builder p_460584_) {
         this.fluid = Optional.of(p_460584_.build());
         return this;
      }

      public LocationPredicate.Builder setSmokey(boolean p_455527_) {
         this.smokey = Optional.of(p_455527_);
         return this;
      }

      public LocationPredicate.Builder setCanSeeSky(boolean p_457609_) {
         this.canSeeSky = Optional.of(p_457609_);
         return this;
      }

      public LocationPredicate build() {
         Optional<LocationPredicate.PositionPredicate> optional = LocationPredicate.PositionPredicate.of(this.x, this.y, this.z);
         return new LocationPredicate(optional, this.biomes, this.structures, this.dimension, this.smokey, this.light, this.block, this.fluid, this.canSeeSky);
      }
   }

   record PositionPredicate(MinMaxBounds.Doubles x, MinMaxBounds.Doubles y, MinMaxBounds.Doubles z) {
      public static final Codec<LocationPredicate.PositionPredicate> CODEC = RecordCodecBuilder.create(
         p_457258_ -> p_457258_.group(
               MinMaxBounds.Doubles.CODEC.optionalFieldOf("x", MinMaxBounds.Doubles.ANY).forGetter(LocationPredicate.PositionPredicate::x),
               MinMaxBounds.Doubles.CODEC.optionalFieldOf("y", MinMaxBounds.Doubles.ANY).forGetter(LocationPredicate.PositionPredicate::y),
               MinMaxBounds.Doubles.CODEC.optionalFieldOf("z", MinMaxBounds.Doubles.ANY).forGetter(LocationPredicate.PositionPredicate::z)
            )
            .apply(p_457258_, LocationPredicate.PositionPredicate::new)
      );

      static Optional<LocationPredicate.PositionPredicate> of(MinMaxBounds.Doubles p_450497_, MinMaxBounds.Doubles p_453813_, MinMaxBounds.Doubles p_459506_) {
         return p_450497_.isAny() && p_453813_.isAny() && p_459506_.isAny()
            ? Optional.empty()
            : Optional.of(new LocationPredicate.PositionPredicate(p_450497_, p_453813_, p_459506_));
      }

      public boolean matches(double p_459791_, double p_453078_, double p_453079_) {
         return this.x.matches(p_459791_) && this.y.matches(p_453078_) && this.z.matches(p_453079_);
      }
   }
}
