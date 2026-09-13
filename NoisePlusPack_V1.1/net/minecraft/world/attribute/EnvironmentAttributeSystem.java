package net.minecraft.world.attribute;

import com.google.common.annotations.VisibleForTesting;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.LongSupplier;
import java.util.stream.Stream;
import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.timeline.Timeline;
import org.jspecify.annotations.Nullable;

public class EnvironmentAttributeSystem implements EnvironmentAttributeReader {
   private final Map<EnvironmentAttribute<?>, EnvironmentAttributeSystem.ValueSampler<?>> attributeSamplers = new Reference2ObjectOpenHashMap();

   EnvironmentAttributeSystem(Map<EnvironmentAttribute<?>, List<EnvironmentAttributeLayer<?>>> p_451818_) {
      p_451818_.forEach(
         (p_452769_, p_461073_) -> this.attributeSamplers
            .put(
               (EnvironmentAttribute<?>)p_452769_,
               this.bakeLayerSampler((EnvironmentAttribute<?>)p_452769_, (List<? extends EnvironmentAttributeLayer<?>>)p_461073_)
            )
      );
   }

   private <Value> EnvironmentAttributeSystem.ValueSampler<Value> bakeLayerSampler(
      EnvironmentAttribute<Value> p_451977_, List<? extends EnvironmentAttributeLayer<?>> p_459413_
   ) {
      List<EnvironmentAttributeLayer<Value>> list = new ArrayList<>((Collection<? extends EnvironmentAttributeLayer<Value>>)p_459413_);
      Value value = p_451977_.defaultValue();

      while (!list.isEmpty()) {
         if (!(list.getFirst() instanceof EnvironmentAttributeLayer.Constant<Value> constant)) {
            break;
         }

         value = constant.applyConstant(value);
         list.removeFirst();
      }

      boolean flag = list.stream().anyMatch(p_451340_ -> p_451340_ instanceof EnvironmentAttributeLayer.Positional);
      return new EnvironmentAttributeSystem.ValueSampler<>(p_451977_, value, List.copyOf(list), flag);
   }

   public static EnvironmentAttributeSystem.Builder builder() {
      return new EnvironmentAttributeSystem.Builder();
   }

   static void addDefaultLayers(EnvironmentAttributeSystem.Builder p_456444_, Level p_456488_) {
      RegistryAccess registryaccess = p_456488_.registryAccess();
      BiomeManager biomemanager = p_456488_.getBiomeManager();
      LongSupplier longsupplier = p_456488_::getDayTime;
      addDimensionLayer(p_456444_, p_456488_.dimensionType());
      addBiomeLayer(p_456444_, registryaccess.lookupOrThrow(Registries.BIOME), biomemanager);
      p_456488_.dimensionType().timelines().forEach(p_455567_ -> p_456444_.addTimelineLayer((Holder<Timeline>)p_455567_, longsupplier));
      if (p_456488_.canHaveWeather()) {
         WeatherAttributes.addBuiltinLayers(p_456444_, WeatherAttributes.WeatherAccess.from(p_456488_));
      }
   }

   private static void addDimensionLayer(EnvironmentAttributeSystem.Builder p_452692_, DimensionType p_454150_) {
      p_452692_.addConstantLayer(p_454150_.attributes());
   }

   private static void addBiomeLayer(EnvironmentAttributeSystem.Builder p_455842_, HolderLookup<Biome> p_457508_, BiomeManager p_454942_) {
      Stream<EnvironmentAttribute<?>> stream = p_457508_.listElements().flatMap(p_459075_ -> p_459075_.value().getAttributes().keySet().stream()).distinct();
      stream.forEach(p_452625_ -> addBiomeLayerForAttribute(p_455842_, (EnvironmentAttribute<?>)p_452625_, p_454942_));
   }

   private static <Value> void addBiomeLayerForAttribute(
      EnvironmentAttributeSystem.Builder p_457459_, EnvironmentAttribute<Value> p_457763_, BiomeManager p_450401_
   ) {
      p_457459_.addPositionalLayer(p_457763_, (p_450899_, p_452302_, p_460237_) -> {
         if (p_460237_ != null && p_457763_.isSpatiallyInterpolated()) {
            return p_460237_.applyAttributeLayer(p_457763_, p_450899_);
         }

         Holder<Biome> holder = p_450401_.getNoiseBiomeAtPosition(p_452302_.x, p_452302_.y, p_452302_.z);
         return holder.value().getAttributes().applyModifier(p_457763_, p_450899_);
      });
   }

   public void invalidateTickCache() {
      this.attributeSamplers.values().forEach(EnvironmentAttributeSystem.ValueSampler::invalidateTickCache);
   }

   private <Value> EnvironmentAttributeSystem.@Nullable ValueSampler<Value> getValueSampler(EnvironmentAttribute<Value> p_457192_) {
      return (EnvironmentAttributeSystem.ValueSampler<Value>)this.attributeSamplers.get(p_457192_);
   }

   @Override
   public <Value> Value getDimensionValue(EnvironmentAttribute<Value> p_455078_) {
      if (SharedConstants.IS_RUNNING_IN_IDE && p_455078_.isPositional()) {
         throw new IllegalStateException("Position must always be provided for positional attribute " + p_455078_);
      }

      EnvironmentAttributeSystem.ValueSampler<Value> valuesampler = this.getValueSampler(p_455078_);
      return valuesampler == null ? p_455078_.defaultValue() : valuesampler.getDimensionValue();
   }

   @Override
   public <Value> Value getValue(EnvironmentAttribute<Value> p_451155_, Vec3 p_450338_, @Nullable SpatialAttributeInterpolator p_455239_) {
      EnvironmentAttributeSystem.ValueSampler<Value> valuesampler = this.getValueSampler(p_451155_);
      return valuesampler == null ? p_451155_.defaultValue() : valuesampler.getValue(p_450338_, p_455239_);
   }

   @VisibleForTesting
   <Value> Value getConstantBaseValue(EnvironmentAttribute<Value> p_450600_) {
      EnvironmentAttributeSystem.ValueSampler<Value> valuesampler = this.getValueSampler(p_450600_);
      return valuesampler != null ? valuesampler.baseValue : p_450600_.defaultValue();
   }

   @VisibleForTesting
   boolean isAffectedByPosition(EnvironmentAttribute<?> p_450445_) {
      EnvironmentAttributeSystem.ValueSampler<?> valuesampler = this.getValueSampler(p_450445_);
      return valuesampler != null && valuesampler.isAffectedByPosition;
   }

   public static class Builder {
      private final Map<EnvironmentAttribute<?>, List<EnvironmentAttributeLayer<?>>> layersByAttribute = new HashMap<>();

      Builder() {
      }

      public EnvironmentAttributeSystem.Builder addDefaultLayers(Level p_461065_) {
         EnvironmentAttributeSystem.addDefaultLayers(this, p_461065_);
         return this;
      }

      public EnvironmentAttributeSystem.Builder addConstantLayer(EnvironmentAttributeMap p_460339_) {
         for (EnvironmentAttribute<?> environmentattribute : p_460339_.keySet()) {
            this.addConstantEntry(environmentattribute, p_460339_);
         }

         return this;
      }

      private <Value> EnvironmentAttributeSystem.Builder addConstantEntry(EnvironmentAttribute<Value> p_450586_, EnvironmentAttributeMap p_451685_) {
         EnvironmentAttributeMap.Entry<Value, ?> entry = p_451685_.get(p_450586_);
         if (entry == null) {
            throw new IllegalArgumentException("Missing attribute " + p_450586_);
         } else {
            return this.addConstantLayer(p_450586_, entry::applyModifier);
         }
      }

      public <Value> EnvironmentAttributeSystem.Builder addConstantLayer(
         EnvironmentAttribute<Value> p_460232_, EnvironmentAttributeLayer.Constant<Value> p_459474_
      ) {
         return this.addLayer(p_460232_, p_459474_);
      }

      public <Value> EnvironmentAttributeSystem.Builder addTimeBasedLayer(
         EnvironmentAttribute<Value> p_451227_, EnvironmentAttributeLayer.TimeBased<Value> p_459337_
      ) {
         return this.addLayer(p_451227_, p_459337_);
      }

      public <Value> EnvironmentAttributeSystem.Builder addPositionalLayer(
         EnvironmentAttribute<Value> p_461044_, EnvironmentAttributeLayer.Positional<Value> p_453019_
      ) {
         return this.addLayer(p_461044_, p_453019_);
      }

      private <Value> EnvironmentAttributeSystem.Builder addLayer(EnvironmentAttribute<Value> p_450723_, EnvironmentAttributeLayer<Value> p_455018_) {
         this.layersByAttribute.computeIfAbsent(p_450723_, p_457972_ -> new ArrayList<>()).add(p_455018_);
         return this;
      }

      public EnvironmentAttributeSystem.Builder addTimelineLayer(Holder<Timeline> p_456574_, LongSupplier p_450733_) {
         for (EnvironmentAttribute<?> environmentattribute : p_456574_.value().attributes()) {
            this.addTimelineLayerForAttribute(p_456574_, environmentattribute, p_450733_);
         }

         return this;
      }

      private <Value> void addTimelineLayerForAttribute(Holder<Timeline> p_453110_, EnvironmentAttribute<Value> p_458332_, LongSupplier p_454508_) {
         this.addTimeBasedLayer(p_458332_, p_453110_.value().createTrackSampler(p_458332_, p_454508_));
      }

      public EnvironmentAttributeSystem build() {
         return new EnvironmentAttributeSystem(this.layersByAttribute);
      }
   }

   static class ValueSampler<Value> {
      private final EnvironmentAttribute<Value> attribute;
      final Value baseValue;
      private final List<EnvironmentAttributeLayer<Value>> layers;
      final boolean isAffectedByPosition;
      private @Nullable Value cachedTickValue;
      private int cacheTickId;

      ValueSampler(EnvironmentAttribute<Value> p_459989_, Value p_450371_, List<EnvironmentAttributeLayer<Value>> p_453087_, boolean p_459058_) {
         this.attribute = p_459989_;
         this.baseValue = p_450371_;
         this.layers = p_453087_;
         this.isAffectedByPosition = p_459058_;
      }

      public void invalidateTickCache() {
         this.cachedTickValue = null;
         this.cacheTickId++;
      }

      public Value getDimensionValue() {
         if (this.cachedTickValue != null) {
            return this.cachedTickValue;
         }

         Value value = this.computeValueNotPositional();
         this.cachedTickValue = value;
         return value;
      }

      public Value getValue(Vec3 p_459610_, @Nullable SpatialAttributeInterpolator p_453795_) {
         return !this.isAffectedByPosition ? this.getDimensionValue() : this.computeValuePositional(p_459610_, p_453795_);
      }

      private Value computeValuePositional(Vec3 p_456420_, @Nullable SpatialAttributeInterpolator p_455257_) {
         Value value = this.baseValue;

         for (EnvironmentAttributeLayer<Value> environmentattributelayer : this.layers) {
            value = (Value)(switch (environmentattributelayer) {
               case EnvironmentAttributeLayer.Constant<Value> constant -> constant.applyConstant(value);
               case EnvironmentAttributeLayer.TimeBased<Value> timebased -> timebased.applyTimeBased(value, this.cacheTickId);
               case EnvironmentAttributeLayer.Positional<Value> positional -> positional.applyPositional(value, Objects.requireNonNull(p_456420_), p_455257_);
               default -> throw new MatchException(null, null);
            });
         }

         return this.attribute.sanitizeValue(value);
      }

      private Value computeValueNotPositional() {
         Value value = this.baseValue;

         for (EnvironmentAttributeLayer<Value> environmentattributelayer : this.layers) {
            value = (Value)(switch (environmentattributelayer) {
               case EnvironmentAttributeLayer.Constant<Value> constant -> constant.applyConstant(value);
               case EnvironmentAttributeLayer.TimeBased<Value> timebased -> timebased.applyTimeBased(value, this.cacheTickId);
               case EnvironmentAttributeLayer.Positional<Value> positional -> value;
               default -> throw new MatchException(null, null);
            });
         }

         return this.attribute.sanitizeValue(value);
      }
   }
}
