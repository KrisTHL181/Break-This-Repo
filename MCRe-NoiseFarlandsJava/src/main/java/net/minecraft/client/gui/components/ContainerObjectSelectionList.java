package net.minecraft.client.gui.components;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.navigation.ScreenAxis;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public abstract class ContainerObjectSelectionList<E extends ContainerObjectSelectionList.Entry<E>> extends AbstractSelectionList<E> {
    public ContainerObjectSelectionList(final Minecraft minecraft, final int width, final int height, final int y, final int itemHeight) {
        super(minecraft, width, height, y, itemHeight);
    }

    @Override
    public @Nullable ComponentPath nextFocusPath(final FocusNavigationEvent navigationEvent) {
        if (this.getItemCount() == 0) {
            return null;
        } else if (!(navigationEvent instanceof FocusNavigationEvent.ArrowNavigation arrowNavigation)) {
            return super.nextFocusPath(navigationEvent);
        } else {
            E focused = this.getFocused();
            if (arrowNavigation.direction().getAxis() == ScreenAxis.HORIZONTAL && focused != null) {
                return ComponentPath.path(this, focused.nextFocusPath(navigationEvent));
            }

            int index = -1;
            ScreenDirection direction = arrowNavigation.direction();
            if (focused != null) {
                index = focused.children().indexOf(focused.getFocused());
            }

            if (index == -1) {
                switch (direction) {
                    case LEFT:
                        index = Integer.MAX_VALUE;
                        direction = ScreenDirection.DOWN;
                        break;
                    case RIGHT:
                        index = 0;
                        direction = ScreenDirection.DOWN;
                        break;
                    default:
                        index = 0;
                }
            }

            E entry = focused;

            ComponentPath componentPath;
            do {
                entry = this.nextEntry(direction, e -> !e.children().isEmpty(), entry);
                if (entry == null) {
                    return null;
                }

                componentPath = entry.focusPathAtIndex(arrowNavigation, index);
            } while (componentPath == null);

            return ComponentPath.path(this, componentPath);
        }
    }

    @Override
    public void setFocused(final @Nullable GuiEventListener focused) {
        if (this.getFocused() != focused) {
            super.setFocused(focused);
            if (focused == null) {
                this.setSelected(null);
            }
        }
    }

    @Override
    public NarratableEntry.NarrationPriority narrationPriority() {
        return this.isFocused() ? NarratableEntry.NarrationPriority.FOCUSED : super.narrationPriority();
    }

    @Override
    protected boolean entriesCanBeSelected() {
        return false;
    }

    @Override
    public void updateWidgetNarration(final NarrationElementOutput output) {
        E hovered = this.getHovered();
        if (hovered != null) {
            hovered.updateNarration(output.nest());
            this.narrateListElementPosition(output, hovered);
        } else {
            E focused = this.getFocused();
            if (focused != null) {
                focused.updateNarration(output.nest());
                this.narrateListElementPosition(output, focused);
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    public abstract static class Entry<E extends ContainerObjectSelectionList.Entry<E>> extends AbstractSelectionList.Entry<E> implements ContainerEventHandler {
        private @Nullable GuiEventListener focused;
        private @Nullable NarratableEntry lastNarratable;
        private boolean dragging;

        @Override
        public boolean isDragging() {
            return this.dragging;
        }

        @Override
        public void setDragging(final boolean dragging) {
            this.dragging = dragging;
        }

        @Override
        public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
            return ContainerEventHandler.super.mouseClicked(event, doubleClick);
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
        public @Nullable GuiEventListener getFocused() {
            return this.focused;
        }

        public @Nullable ComponentPath focusPathAtIndex(final FocusNavigationEvent navigationEvent, final int currentIndex) {
            if (this.children().isEmpty()) {
                return null;
            }

            ComponentPath componentPath = this.children().get(Math.min(currentIndex, this.children().size() - 1)).nextFocusPath(navigationEvent);
            return ComponentPath.path(this, componentPath);
        }

        @Override
        public @Nullable ComponentPath nextFocusPath(final FocusNavigationEvent navigationEvent) {
            if (navigationEvent instanceof FocusNavigationEvent.ArrowNavigation arrowNavigation) {
                int delta = switch (arrowNavigation.direction()) {
                    case LEFT -> -1;
                    case RIGHT -> 1;
                    case UP, DOWN -> 0;
                };
                if (delta == 0) {
                    return null;
                }

                int index = Mth.clamp(delta + this.children().indexOf(this.getFocused()), 0, this.children().size() - 1);

                for (int i = index; i >= 0 && i < this.children().size(); i += delta) {
                    GuiEventListener child = this.children().get(i);
                    ComponentPath componentPath = child.nextFocusPath(navigationEvent);
                    if (componentPath != null) {
                        return ComponentPath.path(this, componentPath);
                    }
                }
            }

            return ContainerEventHandler.super.nextFocusPath(navigationEvent);
        }

        public abstract List<? extends NarratableEntry> narratables();

        void updateNarration(final NarrationElementOutput output) {
            List<? extends NarratableEntry> narratables = this.narratables();
            Screen.NarratableSearchResult result = Screen.findNarratableWidget(narratables, this.lastNarratable);
            if (result != null) {
                if (result.priority().isTerminal()) {
                    this.lastNarratable = result.entry();
                }

                if (narratables.size() > 1) {
                    output.add(NarratedElementType.POSITION, Component.translatable("narrator.position.object_list", result.index() + 1, narratables.size()));
                }

                result.entry().updateNarration(output.nest());
            }
        }
    }
}