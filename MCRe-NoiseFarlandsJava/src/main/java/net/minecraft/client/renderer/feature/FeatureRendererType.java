package net.minecraft.client.renderer.feature;

import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.client.renderer.feature.submit.SubmitNode;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record FeatureRendererType<Submit extends SubmitNode>(int id, String name) {
    private static final AtomicInteger NEXT_ID = new AtomicInteger();

    @Deprecated
    public FeatureRendererType {
    }

    public static <Submit extends SubmitNode> FeatureRendererType<Submit> create(final String name) {
        return new FeatureRendererType<>(NEXT_ID.getAndIncrement(), name);
    }

    @Override
    public String toString() {
        return this.name;
    }
}