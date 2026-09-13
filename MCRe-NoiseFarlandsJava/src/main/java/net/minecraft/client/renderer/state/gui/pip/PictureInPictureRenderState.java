package net.minecraft.client.renderer.state.gui.pip;

import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.state.gui.ScreenArea;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public interface PictureInPictureRenderState extends ScreenArea {
    Matrix3x2fc IDENTITY_POSE = new Matrix3x2f();

    int x0();

    int x1();

    int y0();

    int y1();

    float scale();

    default Matrix3x2fc pose() {
        return IDENTITY_POSE;
    }

    @Nullable ScreenRectangle scissorArea();

    static @Nullable ScreenRectangle getBounds(final int x0, final int y0, final int x1, final int y1, final @Nullable ScreenRectangle scissorArea) {
        ScreenRectangle bounds = new ScreenRectangle(x0, y0, x1 - x0, y1 - y0);
        return scissorArea != null ? scissorArea.intersection(bounds) : bounds;
    }
}