package net.minecraft.client.renderer.rendertype;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.function.Consumer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class LayeringTransform {
    private final String name;
    private final @Nullable Consumer<Matrix4f> modifier;
    public static final LayeringTransform NO_LAYERING = new LayeringTransform("no_layering", null);
    public static final LayeringTransform VIEW_OFFSET_Z_LAYERING = new LayeringTransform(
        "view_offset_z_layering", modelViewMatrix -> RenderSystem.getProjectionType().applyLayeringTransform(modelViewMatrix, 1.0F)
    );
    public static final LayeringTransform VIEW_OFFSET_Z_LAYERING_FORWARD = new LayeringTransform(
        "view_offset_z_layering_forward", modelViewMatrix -> RenderSystem.getProjectionType().applyLayeringTransform(modelViewMatrix, -1.0F)
    );

    public LayeringTransform(final String name, final @Nullable Consumer<Matrix4f> modifier) {
        this.name = name;
        this.modifier = modifier;
    }

    @Override
    public String toString() {
        return "LayeringTransform[" + this.name + "]";
    }

    public @Nullable Consumer<Matrix4f> getModifier() {
        return this.modifier;
    }
}