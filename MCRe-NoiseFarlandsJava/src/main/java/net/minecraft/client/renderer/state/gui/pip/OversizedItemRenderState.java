package net.minecraft.client.renderer.state.gui.pip;

import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.state.gui.GuiItemRenderState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix3x2fc;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public record OversizedItemRenderState(GuiItemRenderState guiItemRenderState, int x0, int y0, int x1, int y1) implements PictureInPictureRenderState {
    @Override
    public float scale() {
        return 16.0F;
    }

    @Override
    public Matrix3x2fc pose() {
        return this.guiItemRenderState.pose();
    }

    @Override
    public @Nullable ScreenRectangle scissorArea() {
        return this.guiItemRenderState.scissorArea();
    }

    @Override
    public @Nullable ScreenRectangle bounds() {
        return this.guiItemRenderState.bounds();
    }
}