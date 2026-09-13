package net.minecraft.client.gui.screens.options;

import net.minecraft.client.NarratorStatus;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.OptionsList;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public abstract class OptionsSubScreen extends Screen {
    protected final Screen lastScreen;
    protected final Options options;
    protected @Nullable OptionsList list;
    public final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);

    public OptionsSubScreen(final Screen lastScreen, final Options options, final Component title) {
        super(title);
        this.lastScreen = lastScreen;
        this.options = options;
    }

    @Override
    protected void init() {
        this.addTitle();
        this.addContents();
        this.addFooter();
        this.layout.visitWidgets(x$0 -> this.addRenderableWidget(x$0));
        this.repositionElements();
    }

    protected void addTitle() {
        this.layout.addTitleHeader(this.title, this.font);
    }

    protected void addContents() {
        this.list = this.layout.addToContents(new OptionsList(this.minecraft, this.width, this));
        this.addOptions();
        if (this.list.findOption(this.options.narrator()) instanceof CycleButton<?> cycleButton) {
            this.narratorButton = (CycleButton<NarratorStatus>)cycleButton;
            this.narratorButton.active = this.minecraft.getNarrator().isActive();
        }
    }

    protected abstract void addOptions();

    protected void addFooter() {
        this.layout.addToFooter(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose()).width(200).build());
    }

    @Override
    protected void repositionElements() {
        this.layout.arrangeElements();
        if (this.list != null) {
            this.list.updateSize(this.width, this.layout);
        }
    }

    @Override
    public void removed() {
        this.minecraft.options.save();
    }

    @Override
    public void onClose() {
        if (this.list != null) {
            this.list.applyUnsavedChanges();
        }

        this.minecraft.gui.setScreen(this.lastScreen);
    }

    public void resetOption(final OptionInstance<?> option) {
        if (this.list != null) {
            this.list.resetOption(option);
        }
    }
}