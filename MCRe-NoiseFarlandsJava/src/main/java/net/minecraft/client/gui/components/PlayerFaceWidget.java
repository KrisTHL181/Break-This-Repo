package net.minecraft.client.gui.components;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class PlayerFaceWidget extends AbstractWidget {
    private final ResolvableProfile skinProfile;

    public PlayerFaceWidget(final int size, final ResolvableProfile skinProfile) {
        super(0, 0, size, size, CommonComponents.EMPTY);
        this.skinProfile = skinProfile;
        this.active = false;
    }

    @Override
    protected void extractWidgetRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
        PlayerFaceExtractor.extractRenderState(graphics, this.skinProfile, this.getX(), this.getY(), this.getWidth());
    }

    @Override
    public void playDownSound(final SoundManager soundManager) {
    }

    @Override
    protected void updateWidgetNarration(final NarrationElementOutput output) {
    }
}