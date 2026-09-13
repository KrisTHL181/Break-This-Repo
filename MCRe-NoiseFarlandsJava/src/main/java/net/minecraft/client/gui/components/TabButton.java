package net.minecraft.client.gui.components;

import net.minecraft.client.gui.components.tabs.Tab;
import net.minecraft.client.gui.components.tabs.TabManager;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class TabButton extends AbstractWidget.WithInactiveMessage {
    private final TabManager tabManager;
    private final Tab tab;

    public TabButton(final TabManager tabManager, final Tab tab, final int width, final int height) {
        super(0, 0, width, height, tab.getTabTitle());
        this.tabManager = tabManager;
        this.tab = tab;
    }

    @Override
    protected void updateWidgetNarration(final NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, Component.translatable("gui.narrate.tab", this.tab.getTabTitle()));
        output.add(NarratedElementType.HINT, this.tab().getTabExtraNarration());
    }

    @Override
    public void playDownSound(final SoundManager soundManager) {
    }

    public Tab tab() {
        return this.tab;
    }

    public boolean isSelected() {
        return this.tabManager.getCurrentTab() == this.tab;
    }
}