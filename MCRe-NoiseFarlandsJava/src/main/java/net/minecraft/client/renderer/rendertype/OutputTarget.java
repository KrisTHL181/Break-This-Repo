package net.minecraft.client.renderer.rendertype;

import com.mojang.blaze3d.pipeline.RenderTarget;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class OutputTarget {
    private final String name;
    private final Supplier<@Nullable RenderTarget> renderTargetSupplier;
    public static final OutputTarget MAIN_TARGET = new OutputTarget("main_target", () -> Minecraft.getInstance().gameRenderer.mainRenderTarget());
    public static final OutputTarget OUTLINE_TARGET = new OutputTarget("outline_target", () -> Minecraft.getInstance().levelRenderer.entityOutlineTarget());
    public static final OutputTarget WEATHER_TARGET = new OutputTarget("weather_target", () -> Minecraft.getInstance().levelRenderer.weatherTarget());
    public static final OutputTarget ITEM_ENTITY_TARGET = new OutputTarget("item_entity_target", () -> Minecraft.getInstance().levelRenderer.itemEntityTarget());

    public OutputTarget(final String name, final Supplier<@Nullable RenderTarget> renderTargetSupplier) {
        this.name = name;
        this.renderTargetSupplier = renderTargetSupplier;
    }

    public RenderTarget getRenderTarget() {
        RenderTarget preferredTarget = this.renderTargetSupplier.get();
        return preferredTarget != null ? preferredTarget : Minecraft.getInstance().gameRenderer.mainRenderTarget();
    }

    @Override
    public String toString() {
        return "OutputTarget[" + this.name + "]";
    }
}