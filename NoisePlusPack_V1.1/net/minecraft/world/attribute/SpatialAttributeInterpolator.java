package net.minecraft.world.attribute;

import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.Reference2DoubleArrayMap;
import it.unimi.dsi.fastutil.objects.Reference2DoubleMaps;
import it.unimi.dsi.fastutil.objects.Reference2DoubleMap.Entry;
import java.util.Objects;

public class SpatialAttributeInterpolator {
   private final Reference2DoubleArrayMap<EnvironmentAttributeMap> weightsBySource = new Reference2DoubleArrayMap();

   public void clear() {
      this.weightsBySource.clear();
   }

   public SpatialAttributeInterpolator accumulate(double p_457391_, EnvironmentAttributeMap p_457338_) {
      this.weightsBySource.mergeDouble(p_457338_, p_457391_, Double::sum);
      return this;
   }

   public <Value> Value applyAttributeLayer(EnvironmentAttribute<Value> p_453750_, Value p_453240_) {
      if (this.weightsBySource.isEmpty()) {
         return p_453240_;
      }

      if (this.weightsBySource.size() == 1) {
         EnvironmentAttributeMap environmentattributemap1 = (EnvironmentAttributeMap)this.weightsBySource.keySet().iterator().next();
         return environmentattributemap1.applyModifier(p_453750_, p_453240_);
      }

      LerpFunction<Value> lerpfunction = p_453750_.type().spatialLerp();
      Value value = null;
      double d0 = 0.0;
      ObjectIterator var7 = Reference2DoubleMaps.fastIterable(this.weightsBySource).iterator();

      while (var7.hasNext()) {
         Entry<EnvironmentAttributeMap> entry = (Entry<EnvironmentAttributeMap>)var7.next();
         EnvironmentAttributeMap environmentattributemap = (EnvironmentAttributeMap)entry.getKey();
         double d1 = entry.getDoubleValue();
         Value value1 = environmentattributemap.applyModifier(p_453750_, p_453240_);
         d0 += d1;
         if (value == null) {
            value = value1;
         } else {
            float f = (float)(d1 / d0);
            value = lerpfunction.apply(f, value, value1);
         }
      }

      return Objects.requireNonNull(value);
   }
}
