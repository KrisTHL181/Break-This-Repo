package net.minecraft.client.gui.components;

import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public abstract class AbstractContainerWidget extends AbstractScrollArea implements ContainerEventHandler {
    private @Nullable GuiEventListener focused;
    private boolean isDragging;

    public AbstractContainerWidget(final int x, final int y, final int width, final int height, final Component message) {
        super(x, y, width, height, message, AbstractScrollArea.ScrollbarSettings.NO_SCROLL);
    }

    public AbstractContainerWidget(
        final int x, final int y, final int width, final int height, final Component message, final AbstractScrollArea.ScrollbarSettings scrollbarSettings
    ) {
        super(x, y, width, height, message, scrollbarSettings);
    }

    @Override
    public final boolean isDragging() {
        return this.isDragging;
    }

    @Override
    public final void setDragging(final boolean dragging) {
        this.isDragging = dragging;
    }

    @Override
    public @Nullable GuiEventListener getFocused() {
        return this.focused;
    }

    @Override
    public void setFocused(final @Nullable GuiEventListener focused) {
        if (this.focused != null) {
            this.focused.setFocused(false);
        }

        if (focused != null) {
            focused.setFocused(true);
        }

        this.focused = focused;
    }

    @Override
    public @Nullable ComponentPath nextFocusPath(final FocusNavigationEvent navigationEvent) {
        return ContainerEventHandler.super.nextFocusPath(navigationEvent);
    }

    @Override
    public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
        boolean scrolling = this.updateScrolling(event);
        return ContainerEventHandler.super.mouseClicked(event, doubleClick) || scrolling;
    }

    @Override
    public boolean mouseReleased(final MouseButtonEvent event) {
        super.mouseReleased(event);
        return ContainerEventHandler.super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(final MouseButtonEvent event, final double dx, final double dy) {
        super.mouseDragged(event, dx, dy);
        return ContainerEventHandler.super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean isFocused() {
        return ContainerEventHandler.super.isFocused();
    }

    @Override
    public void setFocused(final boolean focused) {
        ContainerEventHandler.super.setFocused(focused);
    }
}