package net.minecraft.world.level.levelgen;

import com.mojang.serialization.Codec;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.resources.RegistryFixedCodec;
import net.minecraft.util.Interval;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import org.jspecify.annotations.Nullable;

public interface DensityFunction {
   Codec<Holder<DensityFunction>> REFERENCE_CODEC = RegistryFixedCodec.create(Registries.DENSITY_FUNCTION);
   Codec<DensityFunction> CODEC = RegistryFileCodec.create(Registries.DENSITY_FUNCTION, DensityFunctions.DIRECT_CODEC).xmap(holder -> {
      return switch (holder) {
         case Holder.Direct<DensityFunction> direct -> (DensityFunction)direct.value();
         case Holder.Reference<DensityFunction> reference -> new DensityFunctions.HolderHolder(reference);
         default -> throw new MatchException(null, null);
      };
   }, value -> {
      return switch (value) {
         case DensityFunctions.HolderHolder(Holder<DensityFunction> function) -> function;
         default -> Holder.direct(value);
      };
   });

   double compute(final DensityFunction.FunctionContext context);

   void fillArray(final double[] output, final DensityFunction.ContextProvider contextProvider);

   DensityFunction mapChildren(final DensityFunction.Visitor visitor);

   default DensityFunction mapAll(final DensityFunction.Visitor visitor) {
      class RecursiveVisitor implements DensityFunction.Visitor {
         @Override
         public DensityFunction apply(final DensityFunction input) {
            return visitor.apply(input.mapChildren(this));
         }

         @Override
         public DensityFunction.NoiseHolder visitNoise(final DensityFunction.NoiseHolder noise) {
            return visitor.visitNoise(noise);
         }
      }

      return new RecursiveVisitor().apply(this);
   }

   Interval range();

   KeyDispatchDataCodec<? extends DensityFunction> codec();

   default DensityFunction clamp(final double min, final double max) {
      return new DensityFunctions.Clamp(this, min, max);
   }

   default DensityFunction abs() {
      return DensityFunctions.map(this, DensityFunctions.Mapped.Type.ABS);
   }

   default DensityFunction square() {
      return DensityFunctions.map(this, DensityFunctions.Mapped.Type.SQUARE);
   }

   default DensityFunction cube() {
      return DensityFunctions.map(this, DensityFunctions.Mapped.Type.CUBE);
   }

   default DensityFunction halfNegative() {
      return DensityFunctions.map(this, DensityFunctions.Mapped.Type.HALF_NEGATIVE);
   }

   default DensityFunction quarterNegative() {
      return DensityFunctions.map(this, DensityFunctions.Mapped.Type.QUARTER_NEGATIVE);
   }

   default DensityFunction invert() {
      return DensityFunctions.map(this, DensityFunctions.Mapped.Type.INVERT);
   }

   default DensityFunction squeeze() {
      return DensityFunctions.map(this, DensityFunctions.Mapped.Type.SQUEEZE);
   }

   interface ContextProvider {
      DensityFunction.FunctionContext forIndex(int index);

      void fillAllDirectly(double[] output, DensityFunction function);
   }

   interface FunctionContext {
      int blockX();

      int blockY();

      int blockZ();
   }

   record NoiseHolder(Holder<NormalNoise.NoiseParameters> noiseData, @Nullable NormalNoise noise) {
      public static final Codec<DensityFunction.NoiseHolder> CODEC = NormalNoise.NoiseParameters.CODEC
         .xmap(data -> new DensityFunction.NoiseHolder(data, null), DensityFunction.NoiseHolder::noiseData);

      public NoiseHolder(final Holder<NormalNoise.NoiseParameters> noiseData) {
         this(noiseData, null);
      }

      public double getValue(final double x, final double y, final double z) {
         return this.noise == null ? 0.0 : this.noise.getValue(x, y, z);
      }

      public double maxValue() {
         return this.noise == null ? 2.0 : this.noise.maxValue();
      }
   }

   interface SimpleFunction extends DensityFunction {
      @Override
      default void fillArray(final double[] output, final DensityFunction.ContextProvider contextProvider) {
         contextProvider.fillAllDirectly(output, this);
      }

      @Override
      default DensityFunction mapChildren(final DensityFunction.Visitor visitor) {
         return this;
      }
   }

   record SinglePointContext(int blockX, int blockY, int blockZ) implements DensityFunction.FunctionContext {
   }

   interface Visitor {
      DensityFunction apply(DensityFunction input);

      default DensityFunction.NoiseHolder visitNoise(final DensityFunction.NoiseHolder noise) {
         return noise;
      }
   }
}
