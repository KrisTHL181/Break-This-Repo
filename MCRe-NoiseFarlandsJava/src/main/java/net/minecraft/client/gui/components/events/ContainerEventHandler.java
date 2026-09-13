package net.minecraft.client.gui.components.events;

import com.mojang.datafixers.util.Pair;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.navigation.ScreenAxis;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.client.gui.navigation.ScreenPosition;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.PreeditEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector2i;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public interface ContainerEventHandler extends GuiEventListener {
    List<? extends GuiEventListener> children();

    default Optional<GuiEventListener> getChildAt(final double x, final double y) {
        for (GuiEventListener child : this.children()) {
            if (child.isMouseOver(x, y)) {
                return Optional.of(child);
            }
        }

        return Optional.empty();
    }

    @Override
    default boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
        Optional<GuiEventListener> child = this.getChildAt(event.x(), event.y());
        if (child.isEmpty()) {
            return false;
        }

        GuiEventListener widget = child.get();
        if (widget.mouseClicked(event, doubleClick) && widget.shouldTakeFocusAfterInteraction()) {
            this.setFocused(widget);
            if (event.button() == 0) {
                this.setDragging(true);
            }
        }

        return true;
    }

    @Override
    default boolean mouseReleased(final MouseButtonEvent event) {
        if (event.button() == 0 && this.isDragging()) {
            this.setDragging(false);
            if (this.getFocused() != null) {
                return this.getFocused().mouseReleased(event);
            }
        }

        return false;
    }

    @Override
    default boolean mouseDragged(final MouseButtonEvent event, final double dx, final double dy) {
        return this.getFocused() != null && this.isDragging() && event.button() == 0 ? this.getFocused().mouseDragged(event, dx, dy) : false;
    }

    boolean isDragging();

    void setDragging(boolean dragging);

    @Override
    default boolean mouseScrolled(final double x, final double y, final double scrollX, final double scrollY) {
        return this.getChildAt(x, y).filter(child -> child.mouseScrolled(x, y, scrollX, scrollY)).isPresent();
    }

    @Override
    default boolean keyPressed(final KeyEvent event) {
        return this.getFocused() != null && this.getFocused().keyPressed(event);
    }

    @Override
    default boolean keyReleased(final KeyEvent event) {
        return this.getFocused() != null && this.getFocused().keyReleased(event);
    }

    @Override
    default boolean charTyped(final CharacterEvent event) {
        return this.getFocused() != null && this.getFocused().charTyped(event);
    }

    @Override
    default boolean preeditUpdated(final @Nullable PreeditEvent event) {
        return this.getFocused() != null && this.getFocused().preeditUpdated(event);
    }

    @Override
    default ScreenRectangle getBorderForArrowNavigation(final ScreenDirection opposite) {
        GuiEventListener focused = this.getFocused();
        return focused != null ? focused.getBorderForArrowNavigation(opposite) : GuiEventListener.super.getBorderForArrowNavigation(opposite);
    }

    @Nullable GuiEventListener getFocused();

    void setFocused(final @Nullable GuiEventListener focused);

    @Override
    default void setFocused(final boolean focused) {
        if (!focused) {
            this.setFocused(null);
        }
    }

    @Override
    default boolean isFocused() {
        return this.getFocused() != null;
    }

    @Override
    default @Nullable ComponentPath getCurrentFocusPath() {
        GuiEventListener focused = this.getFocused();
        return focused != null ? ComponentPath.path(this, focused.getCurrentFocusPath()) : null;
    }

    @Override
    default @Nullable ComponentPath nextFocusPath(final FocusNavigationEvent navigationEvent) {
        GuiEventListener focus = this.getFocused();
        if (focus != null) {
            ComponentPath focusPath = focus.nextFocusPath(navigationEvent);
            if (focusPath != null) {
                return ComponentPath.path(this, focusPath);
            }
        }

        if (navigationEvent instanceof FocusNavigationEvent.TabNavigation tabNavigation) {
            return this.handleTabNavigation(tabNavigation);
        } else {
            return navigationEvent instanceof FocusNavigationEvent.ArrowNavigation arrowNavigation ? this.handleArrowNavigation(arrowNavigation) : null;
        }
    }

    private @Nullable ComponentPath handleTabNavigation(final FocusNavigationEvent.TabNavigation tabNavigation) {
        boolean forward = tabNavigation.forward();
        GuiEventListener focus = this.getFocused();
        List<? extends GuiEventListener> sortedChildren = new ArrayList<>(this.children());
        Collections.sort(sortedChildren, Comparator.comparingInt(childx -> childx.getTabOrderGroup()));
        int index = sortedChildren.indexOf(focus);
        int newIndex;
        if (focus != null && index >= 0) {
            newIndex = index + (forward ? 1 : 0);
        } else if (forward) {
            newIndex = 0;
        } else {
            newIndex = sortedChildren.size();
        }

        ListIterator<? extends GuiEventListener> iterator = sortedChildren.listIterator(newIndex);
        BooleanSupplier test = forward ? iterator::hasNext : iterator::hasPrevious;
        Supplier<? extends GuiEventListener> getter = forward ? iterator::next : iterator::previous;

        while (test.getAsBoolean()) {
            GuiEventListener child = getter.get();
            ComponentPath focusPath = child.nextFocusPath(tabNavigation);
            if (focusPath != null) {
                return ComponentPath.path(this, focusPath);
            }
        }

        return null;
    }

    private @Nullable ComponentPath handleArrowNavigation(final FocusNavigationEvent.ArrowNavigation arrowNavigation) {
        GuiEventListener focus = this.getFocused();
        ScreenDirection direction = arrowNavigation.direction();
        if (focus == null) {
            if (arrowNavigation.previousFocus() != null) {
                return ComponentPath.path(
                    this, this.nextFocusPathInDirection(arrowNavigation.previousFocus(), arrowNavigation.direction(), null, arrowNavigation)
                );
            }

            ScreenRectangle borderRectangle = this.getBorderForArrowNavigation(direction.getOpposite());
            return ComponentPath.path(this, this.nextFocusPathInDirection(borderRectangle, direction, null, arrowNavigation));
        } else {
            ScreenRectangle focusedRectangle = focus.getBorderForArrowNavigation(direction);
            return ComponentPath.path(
                this, this.nextFocusPathInDirection(focusedRectangle, arrowNavigation.direction(), focus, arrowNavigation.with(focusedRectangle))
            );
        }
    }

    private @Nullable ComponentPath nextFocusPathInDirection(
        final ScreenRectangle focusedRectangle,
        final ScreenDirection direction,
        final @Nullable GuiEventListener excluded,
        final FocusNavigationEvent.ArrowNavigation navigationEvent
    ) {
        ScreenAxis axis = direction.getAxis();
        ScreenAxis otherAxis = axis.orthogonal();
        ScreenDirection positiveDirectionOtherAxis = otherAxis.getPositive();
        int focusedFirstBound = focusedRectangle.getBoundInDirection(direction.getOpposite());
        List<GuiEventListener> potentialChildren = new ArrayList<>();

        for (GuiEventListener child : this.children()) {
            if (child != excluded) {
                ScreenRectangle childRectangle = child.getRectangle();
                if (childRectangle.overlapsInAxis(focusedRectangle, otherAxis)) {
                    int childFirstBound = childRectangle.getBoundInDirection(direction.getOpposite());
                    if (direction.isAfter(childFirstBound, focusedFirstBound)) {
                        potentialChildren.add(child);
                    } else if (childFirstBound == focusedFirstBound
                        && direction.isAfter(childRectangle.getBoundInDirection(direction), focusedRectangle.getBoundInDirection(direction))) {
                        potentialChildren.add(child);
                    }
                }
            }
        }

        Comparator<GuiEventListener> primaryComparator = Comparator.comparing(
            childx -> childx.getRectangle().getBoundInDirection(direction.getOpposite()), direction.coordinateValueComparator()
        );
        Comparator<GuiEventListener> secondaryComparator = Comparator.comparing(
            childx -> childx.getRectangle().getBoundInDirection(positiveDirectionOtherAxis.getOpposite()),
            positiveDirectionOtherAxis.coordinateValueComparator()
        );
        potentialChildren.sort(primaryComparator.thenComparing(secondaryComparator));

        for (GuiEventListener child : potentialChildren) {
            ComponentPath componentPath = child.nextFocusPath(navigationEvent);
            if (componentPath != null) {
                return componentPath;
            }
        }

        return this.nextFocusPathVaguelyInDirection(focusedRectangle, direction, excluded, navigationEvent);
    }

    private @Nullable ComponentPath nextFocusPathVaguelyInDirection(
        final ScreenRectangle focusedRectangle,
        final ScreenDirection direction,
        final @Nullable GuiEventListener excluded,
        final FocusNavigationEvent navigationEvent
    ) {
        ScreenAxis axis = direction.getAxis();
        ScreenAxis otherAxis = axis.orthogonal();
        List<Pair<GuiEventListener, Long>> potentialChildren = new ArrayList<>();
        ScreenPosition focusedSideCenter = ScreenPosition.of(axis, focusedRectangle.getBoundInDirection(direction), focusedRectangle.getCenterInAxis(otherAxis));

        for (GuiEventListener child : this.children()) {
            if (child != excluded) {
                ScreenRectangle childRectangle = child.getRectangle();
                ScreenPosition childOpposingSideCenter = ScreenPosition.of(
                    axis, childRectangle.getBoundInDirection(direction.getOpposite()), childRectangle.getCenterInAxis(otherAxis)
                );
                if (direction.isAfter(childOpposingSideCenter.getCoordinate(axis), focusedSideCenter.getCoordinate(axis))) {
                    long distanceSquared = Vector2i.distanceSquared(
                        focusedSideCenter.x(), focusedSideCenter.y(), childOpposingSideCenter.x(), childOpposingSideCenter.y()
                    );
                    potentialChildren.add(Pair.of(child, distanceSquared));
                }
            }
        }

        potentialChildren.sort(Comparator.comparingDouble(Pair::getSecond));

        for (Pair<GuiEventListener, Long> child : potentialChildren) {
            ComponentPath componentPath = child.getFirst().nextFocusPath(navigationEvent);
            if (componentPath != null) {
                return componentPath;
            }
        }

        return null;
    }
}