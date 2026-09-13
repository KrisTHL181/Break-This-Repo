package net.minecraft.client.renderer.feature;

import com.google.common.collect.Iterables;
import java.util.Arrays;
import java.util.Objects;
import net.minecraft.client.renderer.feature.submit.SubmitNode;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class FeatureRendererMap implements AutoCloseable {
    private @Nullable FeatureRenderer<?>[] renderers = new FeatureRenderer[0];

    public <Submit extends SubmitNode> void put(final FeatureRendererType<Submit> type, final FeatureRenderer<Submit> renderer) {
        if (this.renderers.length <= type.id()) {
            this.renderers = Arrays.copyOf(this.renderers, Mth.roundToward(type.id() + 1, 16));
        }

        this.renderers[type.id()] = renderer;
    }

    public <Submit extends SubmitNode> @Nullable FeatureRenderer<Submit> get(final FeatureRendererType<Submit> type) {
        return (FeatureRenderer<Submit>)(type.id() >= this.renderers.length ? null : this.renderers[type.id()]);
    }

    public <Submit extends SubmitNode> FeatureRenderer<Submit> getOrThrow(final FeatureRendererType<Submit> type) {
        FeatureRenderer<Submit> renderer = this.get(type);
        if (renderer == null) {
            throw new IllegalArgumentException("No FeatureRenderer for type " + type);
        } else {
            return renderer;
        }
    }

    public Iterable<FeatureRenderer<?>> values() {
        return Iterables.filter(Arrays.asList(this.renderers), Objects::nonNull);
    }

    @Override
    public void close() {
        for (FeatureRenderer<?> renderer : this.renderers) {
            if (renderer != null) {
                renderer.close();
            }
        }
    }
}