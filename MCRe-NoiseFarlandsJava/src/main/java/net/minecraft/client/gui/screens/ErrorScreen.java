package net.minecraft.client.gui.screens;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ErrorScreen extends Screen {
    private final Component message;

    public ErrorScreen(final Component title, final Component message) {
        super(title);
        this.message = message;
    }

    @Override
    protected void init() {
        super.init();
        this.addRenderableWidget(
            Button.builder(CommonComponents.GUI_CANCEL, button -> this.minecraft.gui.setScreen(null)).bounds(this.width / 2 - 100, 140, 200, 20).build()
        );
    }

    @Override
    public void extractRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);
        graphics.centeredText(this.font, this.title, this.width / 2, 90, -1);
        graphics.centeredText(this.font, this.message, this.width / 2, 110, -1);
    }

    @Override
    public void extractBackground(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
        graphics.fillGradient(0, 0, this.width, this.height, -12574688, -11530224);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }
}