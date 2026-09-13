package net.minecraft.advancements.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;

public record FluidPredicate(Optional<HolderSet<Fluid>> fluids, Optional<StatePropertiesPredicate> properties) {
   public static final Codec<FluidPredicate> CODEC = RecordCodecBuilder.create(
      p_456810_ -> p_456810_.group(
            RegistryCodecs.homogeneousList(Registries.FLUID).optionalFieldOf("fluids").forGetter(FluidPredicate::fluids),
            StatePropertiesPredicate.CODEC.optionalFieldOf("state").forGetter(FluidPredicate::properties)
         )
         .apply(p_456810_, FluidPredicate::new)
   );

   public boolean matches(ServerLevel p_453068_, BlockPos p_455016_) {
      if (!p_453068_.isLoaded(p_455016_)) {
         return false;
      }

      FluidState fluidstate = p_453068_.getFluidState(p_455016_);
      return this.fluids.isPresent() && !fluidstate.is(this.fluids.get()) ? false : !this.properties.isPresent() || this.properties.get().matches(fluidstate);
   }

   public static class Builder {
      private Optional<HolderSet<Fluid>> fluids = Optional.empty();
      private Optional<StatePropertiesPredicate> properties = Optional.empty();

      private Builder() {
      }

      public static FluidPredicate.Builder fluid() {
         return new FluidPredicate.Builder();
      }

      public FluidPredicate.Builder of(Fluid p_457396_) {
         this.fluids = Optional.of(HolderSet.direct(p_457396_.builtInRegistryHolder()));
         return this;
      }

      public FluidPredicate.Builder of(HolderSet<Fluid> p_460704_) {
         this.fluids = Optional.of(p_460704_);
         return this;
      }

      public FluidPredicate.Builder setProperties(StatePropertiesPredicate p_454561_) {
         this.properties = Optional.of(p_454561_);
         return this;
      }

      public FluidPredicate build() {
         return new FluidPredicate(this.fluids, this.properties);
      }
   }
}
