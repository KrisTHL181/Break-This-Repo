package net.minecraft.world.attribute;

import it.unimi.dsi.fastutil.objects.Reference2DoubleArrayMap;
import it.unimi.dsi.fastutil.objects.Reference2DoubleMaps;
import it.unimi.dsi.fastutil.objects.Reference2DoubleMap.Entry;
import java.util.Objects;

public class SpatialAttributeInterpolator {
    private final Reference2DoubleArrayMap<EnvironmentAttributeMap> weightsBySource = new Reference2DoubleArrayMap<>();

    public void clear() {
        this.weightsBySource.clear();
    }

    public SpatialAttributeInterpolator accumulate(final double weight, final EnvironmentAttributeMap attributes) {
        this.weightsBySource.mergeDouble(attributes, weight, Double::sum);
        return this;
    }

    public <Value> Value applyAttributeLayer(final EnvironmentAttribute<Value> attribute, final Value baseValue) {
        if (this.weightsBySource.isEmpty()) {
            return baseValue;
        }

        if (this.weightsBySource.size() == 1) {
            EnvironmentAttributeMap sourceAttributes = this.weightsBySource.keySet().iterator().next();
            return sourceAttributes.applyModifier(attribute, baseValue);
        }

        LerpFunction<Value> lerp = attribute.type().spatialLerp();
        Value resultValue = null;
        double accumulatedWeight = 0.0;

        for (Entry<EnvironmentAttributeMap> entry : Reference2DoubleMaps.fastIterable(this.weightsBySource)) {
            EnvironmentAttributeMap sourceAttributes = entry.getKey();
            double sourceWeight = entry.getDoubleValue();
            Value sourceValue = sourceAttributes.applyModifier(attribute, baseValue);
            accumulatedWeight += sourceWeight;
            if (resultValue == null) {
                resultValue = sourceValue;
            } else {
                float relativeFraction = (float)(sourceWeight / accumulatedWeight);
                resultValue = lerp.apply(relativeFraction, resultValue, sourceValue);
            }
        }

        return Objects.requireNonNull(resultValue);
    }
}