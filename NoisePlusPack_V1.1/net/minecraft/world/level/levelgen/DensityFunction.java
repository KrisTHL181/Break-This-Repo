package net.minecraft.world.level.levelgen;

import com.mojang.serialization.Codec;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import org.jspecify.annotations.Nullable;

public interface DensityFunction {
   Codec<DensityFunction> DIRECT_CODEC = DensityFunctions.DIRECT_CODEC;
   Codec<Holder<DensityFunction>> CODEC = RegistryFileCodec.create(Registries.DENSITY_FUNCTION, DIRECT_CODEC);
   Codec<DensityFunction> HOLDER_HELPER_CODEC = CODEC.xmap(
      DensityFunctions.HolderHolder::new,
      p_208226_ -> p_208226_ instanceof DensityFunctions.HolderHolder densityfunctions$holderholder
         ? densityfunctions$holderholder.function()
         : new Holder.Direct<>(p_208226_)
   );

   double compute(DensityFunction.FunctionContext var1);

   void fillArray(double[] var1, DensityFunction.ContextProvider var2);

   DensityFunction mapAll(DensityFunction.Visitor var1);

   double minValue();

   double maxValue();

   KeyDispatchDataCodec<? extends DensityFunction> codec();

   default DensityFunction clamp(double p_208221_, double p_208222_) {
      return new DensityFunctions.Clamp(this, p_208221_, p_208222_);
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
      DensityFunction.FunctionContext forIndex(int var1);

      void fillAllDirectly(double[] var1, DensityFunction var2);
   }

   interface FunctionContext {
      int blockX();

      int blockY();

      int blockZ();

      default Blender getBlender() {
         return Blender.empty();
      }
   }

   record NoiseHolder(Holder<NormalNoise.NoiseParameters> noiseData, @Nullable NormalNoise noise) {
      public static final Codec<DensityFunction.NoiseHolder> CODEC = NormalNoise.NoiseParameters.CODEC
         .xmap(p_224011_ -> new DensityFunction.NoiseHolder(p_224011_, null), DensityFunction.NoiseHolder::noiseData);

      public NoiseHolder(Holder<NormalNoise.NoiseParameters> p_224001_) {
         this(p_224001_, null);
      }

      public double getValue(double p_224007_, double p_224008_, double p_224009_) {
         return this.noise == null ? 0.0 : this.noise.getValue(p_224007_, p_224008_, p_224009_);
      }

      public double maxValue() {
         return this.noise == null ? 2.0 : this.noise.maxValue();
      }
   }

   interface SimpleFunction extends DensityFunction {
      @Override
      default void fillArray(double[] p_208241_, DensityFunction.ContextProvider p_208242_) {
         p_208242_.fillAllDirectly(p_208241_, this);
      }

      @Override
      default DensityFunction mapAll(DensityFunction.Visitor p_208239_) {
         return p_208239_.apply(this);
      }
   }

   record SinglePointContext(int blockX, int blockY, int blockZ) implements DensityFunction.FunctionContext {
   }

   interface Visitor {
      DensityFunction apply(DensityFunction var1);

      default DensityFunction.NoiseHolder visitNoise(DensityFunction.NoiseHolder p_224018_) {
         return p_224018_;
      }
   }
}
