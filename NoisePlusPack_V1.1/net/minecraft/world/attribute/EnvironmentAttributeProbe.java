package net.minecraft.world.attribute;

import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class EnvironmentAttributeProbe {
   private final Map<EnvironmentAttribute<?>, EnvironmentAttributeProbe.ValueProbe<?>> valueProbes = new Reference2ObjectOpenHashMap();
   private final Function<EnvironmentAttribute<?>, EnvironmentAttributeProbe.ValueProbe<?>> valueProbeFactory = p_455534_ -> new EnvironmentAttributeProbe.ValueProbe<>(
      p_455534_
   );
   @Nullable Level level;
   @Nullable Vec3 position;
   final SpatialAttributeInterpolator biomeInterpolator = new SpatialAttributeInterpolator();

   public void reset() {
      this.level = null;
      this.position = null;
      this.biomeInterpolator.clear();
      this.valueProbes.clear();
   }

   public void tick(Level p_458965_, Vec3 p_460324_) {
      this.level = p_458965_;
      this.position = p_460324_;
      this.valueProbes.values().removeIf(EnvironmentAttributeProbe.ValueProbe::tick);
      this.biomeInterpolator.clear();
      GaussianSampler.sample(
         p_460324_.scale(0.25),
         p_458965_.getBiomeManager()::getNoiseBiomeAtQuart,
         (p_454142_, p_452087_) -> this.biomeInterpolator.accumulate(p_454142_, p_452087_.value().getAttributes())
      );
   }

   public <Value> Value getValue(EnvironmentAttribute<Value> p_457127_, float p_458942_) {
      EnvironmentAttributeProbe.ValueProbe<Value> valueprobe = (EnvironmentAttributeProbe.ValueProbe<Value>)this.valueProbes
         .computeIfAbsent(p_457127_, this.valueProbeFactory);
      return valueprobe.get(p_457127_, p_458942_);
   }

   class ValueProbe<Value> {
      private Value lastValue;
      private @Nullable Value newValue;

      public ValueProbe(final EnvironmentAttribute<Value> p_460570_) {
         Value value = this.getValueFromLevel(p_460570_);
         this.lastValue = value;
         this.newValue = value;
      }

      private Value getValueFromLevel(EnvironmentAttribute<Value> p_460533_) {
         return EnvironmentAttributeProbe.this.level != null && EnvironmentAttributeProbe.this.position != null
            ? EnvironmentAttributeProbe.this.level
               .environmentAttributes()
               .getValue(p_460533_, EnvironmentAttributeProbe.this.position, EnvironmentAttributeProbe.this.biomeInterpolator)
            : p_460533_.defaultValue();
      }

      public boolean tick() {
         if (this.newValue == null) {
            return true;
         }

         this.lastValue = this.newValue;
         this.newValue = null;
         return false;
      }

      public Value get(EnvironmentAttribute<Value> p_455637_, float p_457151_) {
         if (this.newValue == null) {
            this.newValue = this.getValueFromLevel(p_455637_);
         }

         return p_455637_.type().partialTickLerp().apply(p_457151_, this.lastValue, this.newValue);
      }
   }
}
