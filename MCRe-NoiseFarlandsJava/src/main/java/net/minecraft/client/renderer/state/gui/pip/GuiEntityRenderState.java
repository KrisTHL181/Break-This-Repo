package net.minecraft.client.renderer.state.gui.pip;

import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Quaternionfc;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public record GuiEntityRenderState(
    EntityRenderState renderState,
    Vector3fc translation,
    Quaternionfc rotation,
    @Nullable Quaternionfc overrideCameraAngle,
    int x0,
    int y0,
    int x1,
    int y1,
    float scale,
    @Nullable ScreenRectangle scissorArea,
    @Nullable ScreenRectangle bounds
) implements PictureInPictureRenderState {
    public GuiEntityRenderState(
        final EntityRenderState renderState,
        final Vector3fc translation,
        final Quaternionfc rotation,
        final @Nullable Quaternionfc overrideCameraAngle,
        final int x0,
        final int y0,
        final int x1,
        final int y1,
        final float scale,
        final @Nullable ScreenRectangle scissorArea
    ) {
        this(
            renderState,
            translation,
            rotation,
            overrideCameraAngle,
            x0,
            y0,
            x1,
            y1,
            scale,
            scissorArea,
            PictureInPictureRenderState.getBounds(x0, y0, x1, y1, scissorArea)
        );
    }
}