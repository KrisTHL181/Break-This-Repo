package net.minecraft.client.gui.components;

import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.screens.LoadingDotsText;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class LoadingDotsWidget extends AbstractWidget {
    private static final int Y_PADDING = 2;
    private final Font font;

    public LoadingDotsWidget(final Font font, final Component message) {
        super(0, 0, font.width(message), 2 + 9 + 6 + 9 + 2, message);
        this.font = font;
    }

    @Override
    protected void extractWidgetRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
        int centerX = this.getX() + this.getWidth() / 2;
        Component message = this.getMessage();
        graphics.text(this.font, message, centerX - this.font.width(message) / 2, this.getY() + 2, -1);
        String dots = LoadingDotsText.get(Util.getMillis());
        graphics.text(this.font, dots, centerX - this.font.width(dots) / 2, this.getBottom() - 9 - 2, -8355712);
    }

    @Override
    protected void updateWidgetNarration(final NarrationElementOutput output) {
    }

    @Override
    public void playDownSound(final SoundManager soundManager) {
    }

    @Override
    public boolean isActive() {
        return false;
    }

    @Override
    public @Nullable ComponentPath nextFocusPath(final FocusNavigationEvent navigationEvent) {
        return null;
    }
}