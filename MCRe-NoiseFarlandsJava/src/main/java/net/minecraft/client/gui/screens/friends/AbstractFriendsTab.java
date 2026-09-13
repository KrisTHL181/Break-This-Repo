package net.minecraft.client.gui.screens.friends;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.FocusableTextWidget;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.components.tabs.Tab;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.Layout;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class AbstractFriendsTab implements Tab {
    static final int SPACING = 8;
    static final int SCROLLBAR_SPACING = 2;
    static final int LIST_MARGIN = 8;
    protected final int width;
    protected int height;

    public AbstractFriendsTab(final int width, final int height) {
        this.width = width;
        this.height = height;
    }

    protected int getListContentWidth() {
        return this.width - 16;
    }

    abstract void rearrangeElements();

    protected abstract Layout entriesContainer();

    final void disable() {
        this.entriesContainer().visitWidgets(widget -> {
            if (widget instanceof AbstractFriendsEntryContainerWidget entry) {
                entry.disable();
            }
        });
    }

    void setHeight(final int height) {
        this.height = height;
        this.rearrangeElements();
    }

    FrameLayout createCenteredFrame(final LayoutElement child, final int frameWidth, final int frameHeight) {
        FrameLayout frameLayout = new FrameLayout(frameWidth, frameHeight);
        frameLayout.defaultChildLayoutSetting().alignHorizontallyCenter().alignVerticallyMiddle();
        frameLayout.addChild(child);
        return frameLayout;
    }

    FocusableTextWidget createText(final Component message, final Font font, final int maxWidth) {
        return FocusableTextWidget.builder(message, font)
            .maxWidth(maxWidth)
            .alwaysShowBorder(false)
            .backgroundFill(FocusableTextWidget.BackgroundFill.NEVER)
            .build();
    }

    MultiLineTextWidget createCenteredText(final Component message, final Font font, final int maxWidth) {
        return this.createText(message, font, maxWidth).setCentered(true);
    }
}