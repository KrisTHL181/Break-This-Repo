package net.minecraft.client.gui.components;

import com.google.common.collect.Lists;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class OptionsList extends ContainerObjectSelectionList<OptionsList.AbstractEntry> {
    private static final int BIG_BUTTON_WIDTH = 310;
    private static final int DEFAULT_ITEM_HEIGHT = 25;
    private final OptionsSubScreen screen;

    public OptionsList(final Minecraft minecraft, final int width, final OptionsSubScreen screen) {
        super(minecraft, width, screen.layout.getContentHeight(), screen.layout.getHeaderHeight(), 25);
        this.centerListVertically = false;
        this.screen = screen;
    }

    public void addBig(final OptionInstance<?> option) {
        this.addEntry(OptionsList.Entry.big(this.minecraft.options, option, this.screen));
    }

    public void addBig(final AbstractWidget widget) {
        this.addEntry(OptionsList.Entry.big(widget, this.screen));
    }

    public void addSmall(final OptionInstance<?>... options) {
        for (int i = 0; i < options.length; i += 2) {
            OptionInstance<?> secondOption = i < options.length - 1 ? options[i + 1] : null;
            this.addEntry(OptionsList.Entry.small(this.minecraft.options, options[i], secondOption, this.screen));
        }
    }

    public void addSmall(final List<AbstractWidget> widgets) {
        for (int i = 0; i < widgets.size(); i += 2) {
            this.addSmall(widgets.get(i), i < widgets.size() - 1 ? widgets.get(i + 1) : null);
        }
    }

    public void addSmall(final AbstractWidget firstOption, final @Nullable AbstractWidget secondOption) {
        this.addEntry(OptionsList.Entry.small(firstOption, secondOption, this.screen));
    }

    public void addSmall(final AbstractWidget firstOption, final OptionInstance<?> firstOptionInstance, final @Nullable AbstractWidget secondOption) {
        this.addEntry(OptionsList.Entry.small(firstOption, firstOptionInstance, secondOption, this.screen));
    }

    public void addHeader(final Component text) {
        int lineHeight = 9;
        int paddingTop = this.children().isEmpty() ? 0 : lineHeight * 2;
        this.addEntry(new OptionsList.HeaderEntry(this.screen, text, paddingTop), paddingTop + lineHeight + 4);
    }

    @Override
    public int getRowWidth() {
        return 310;
    }

    public @Nullable AbstractWidget findOption(final OptionInstance<?> option) {
        for (OptionsList.AbstractEntry child : this.children()) {
            if (child instanceof OptionsList.Entry entry) {
                AbstractWidget widgetForOption = entry.findOption(option);
                if (widgetForOption != null) {
                    return widgetForOption;
                }
            }
        }

        return null;
    }

    public void applyUnsavedChanges() {
        for (OptionsList.AbstractEntry child : this.children()) {
            if (child instanceof OptionsList.Entry entry) {
                for (OptionsList.OptionInstanceWidget optionInstanceWidget : entry.children) {
                    if (optionInstanceWidget.optionInstance() != null
                        && optionInstanceWidget.widget() instanceof OptionInstance.OptionInstanceSliderButton<?> optionSlider) {
                        optionSlider.applyUnsavedValue();
                    }
                }
            }
        }
    }

    public void resetOption(final OptionInstance<?> option) {
        for (OptionsList.AbstractEntry child : this.children()) {
            if (child instanceof OptionsList.Entry entry) {
                for (OptionsList.OptionInstanceWidget optionInstanceWidget : entry.children) {
                    if (optionInstanceWidget.optionInstance() == option
                        && optionInstanceWidget.widget() instanceof ResettableOptionWidget resettableOptionWidget) {
                        resettableOptionWidget.resetValue();
                        return;
                    }
                }
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    protected abstract static class AbstractEntry extends ContainerObjectSelectionList.Entry<OptionsList.AbstractEntry> {
    }

    @OnlyIn(Dist.CLIENT)
    protected static class Entry extends OptionsList.AbstractEntry {
        private final List<OptionsList.OptionInstanceWidget> children;
        private final Screen screen;
        private static final int X_OFFSET = 160;

        private Entry(final List<OptionsList.OptionInstanceWidget> widgets, final Screen screen) {
            this.children = widgets;
            this.screen = screen;
        }

        public static OptionsList.Entry big(final Options options, final OptionInstance<?> optionInstance, final Screen screen) {
            return new OptionsList.Entry(List.of(new OptionsList.OptionInstanceWidget(optionInstance.createButton(options, 0, 0, 310), optionInstance)), screen);
        }

        public static OptionsList.Entry big(final AbstractWidget widget, final Screen screen) {
            widget.setWidth(310);
            return new OptionsList.Entry(List.of(new OptionsList.OptionInstanceWidget(widget, null)), screen);
        }

        public static OptionsList.Entry small(final AbstractWidget leftWidget, final @Nullable AbstractWidget rightWidget, final Screen screen) {
            return rightWidget == null
                ? new OptionsList.Entry(List.of(new OptionsList.OptionInstanceWidget(leftWidget)), screen)
                : new OptionsList.Entry(List.of(new OptionsList.OptionInstanceWidget(leftWidget), new OptionsList.OptionInstanceWidget(rightWidget)), screen);
        }

        public static OptionsList.Entry small(
            final AbstractWidget leftWidget, final OptionInstance<?> leftWidgetOptionInstance, final @Nullable AbstractWidget rightWidget, final Screen screen
        ) {
            return rightWidget == null
                ? new OptionsList.Entry(List.of(new OptionsList.OptionInstanceWidget(leftWidget, leftWidgetOptionInstance)), screen)
                : new OptionsList.Entry(
                    List.of(new OptionsList.OptionInstanceWidget(leftWidget, leftWidgetOptionInstance), new OptionsList.OptionInstanceWidget(rightWidget)),
                    screen
                );
        }

        public static OptionsList.Entry small(
            final Options options, final OptionInstance<?> optionA, final @Nullable OptionInstance<?> optionB, final OptionsSubScreen screen
        ) {
            AbstractWidget buttonA = optionA.createButton(options);
            return optionB == null
                ? new OptionsList.Entry(List.of(new OptionsList.OptionInstanceWidget(buttonA, optionA)), screen)
                : new OptionsList.Entry(
                    List.of(
                        new OptionsList.OptionInstanceWidget(buttonA, optionA), new OptionsList.OptionInstanceWidget(optionB.createButton(options), optionB)
                    ),
                    screen
                );
        }

        @Override
        public void extractContent(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final boolean hovered, final float a) {
            int xOffset = 0;
            int x = this.screen.width / 2 - 155;

            for (OptionsList.OptionInstanceWidget optionInstanceWidget : this.children) {
                optionInstanceWidget.widget().setPosition(x + xOffset, this.getContentY());
                optionInstanceWidget.widget().extractRenderState(graphics, mouseX, mouseY, a);
                xOffset += 160;
            }
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return Lists.transform(this.children, OptionsList.OptionInstanceWidget::widget);
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return Lists.transform(this.children, OptionsList.OptionInstanceWidget::widget);
        }

        public @Nullable AbstractWidget findOption(final OptionInstance<?> option) {
            for (OptionsList.OptionInstanceWidget child : this.children) {
                if (child.optionInstance == option) {
                    return child.widget();
                }
            }

            return null;
        }
    }

    @OnlyIn(Dist.CLIENT)
    protected static class HeaderEntry extends OptionsList.AbstractEntry {
        private final Screen screen;
        private final int paddingTop;
        private final StringWidget widget;

        protected HeaderEntry(final Screen screen, final Component text, final int paddingTop) {
            this.screen = screen;
            this.paddingTop = paddingTop;
            this.widget = new StringWidget(text, screen.getFont());
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return List.of(this.widget);
        }

        @Override
        public void extractContent(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final boolean hovered, final float a) {
            this.widget.setPosition(this.screen.width / 2 - 155, this.getContentY() + this.paddingTop);
            this.widget.extractRenderState(graphics, mouseX, mouseY, a);
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return List.of(this.widget);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public record OptionInstanceWidget(AbstractWidget widget, @Nullable OptionInstance<?> optionInstance) {
        public OptionInstanceWidget(final AbstractWidget widget) {
            this(widget, null);
        }
    }
}