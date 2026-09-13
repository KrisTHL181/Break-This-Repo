package net.minecraft.client.gui.screens.options.controls;

import com.google.common.collect.ImmutableList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.FocusableTextWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.lang3.ArrayUtils;

@OnlyIn(Dist.CLIENT)
public class KeyBindsList extends ContainerObjectSelectionList<KeyBindsList.Entry> {
    private static final int ITEM_HEIGHT = 20;
    private final KeyBindsScreen keyBindsScreen;
    private int maxNameWidth;

    public KeyBindsList(final KeyBindsScreen keyBindsScreen, final Minecraft minecraft) {
        super(minecraft, keyBindsScreen.width, keyBindsScreen.layout.getContentHeight(), keyBindsScreen.layout.getHeaderHeight(), 20);
        this.keyBindsScreen = keyBindsScreen;
        KeyMapping[] keyMappings = ArrayUtils.clone(minecraft.options.keyMappings);
        Arrays.sort(keyMappings);
        KeyMapping.Category previousCategory = null;

        for (KeyMapping key : keyMappings) {
            KeyMapping.Category category = key.getCategory();
            if (category != previousCategory) {
                previousCategory = category;
                this.addEntry(new KeyBindsList.CategoryEntry(category));
            }

            Component name = Component.translatable(key.getName());
            int width = minecraft.font.width(name);
            if (width > this.maxNameWidth) {
                this.maxNameWidth = width;
            }

            this.addEntry(new KeyBindsList.KeyEntry(key, name));
        }
    }

    public void resetMappingAndUpdateButtons() {
        KeyMapping.resetMapping();
        this.refreshEntries();
    }

    public void refreshEntries() {
        this.children().forEach(KeyBindsList.Entry::refreshEntry);
    }

    @Override
    public int getRowWidth() {
        return 340;
    }

    @OnlyIn(Dist.CLIENT)
    public class CategoryEntry extends KeyBindsList.Entry {
        private final FocusableTextWidget categoryName;

        public CategoryEntry(final KeyMapping.Category category) {
            this.categoryName = FocusableTextWidget.builder(category.label(), KeyBindsList.this.minecraft.font)
                .alwaysShowBorder(false)
                .backgroundFill(FocusableTextWidget.BackgroundFill.ON_FOCUS)
                .build();
        }

        @Override
        public void extractContent(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final boolean hovered, final float a) {
            this.categoryName
                .setPosition(KeyBindsList.this.width / 2 - this.categoryName.getWidth() / 2, this.getContentBottom() - this.categoryName.getHeight());
            this.categoryName.extractRenderState(graphics, mouseX, mouseY, a);
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return List.of(this.categoryName);
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return List.of(this.categoryName);
        }

        @Override
        public void refreshEntry() {
        }
    }

    @OnlyIn(Dist.CLIENT)
    public abstract static class Entry extends ContainerObjectSelectionList.Entry<KeyBindsList.Entry> {
        public abstract void refreshEntry();
    }

    @OnlyIn(Dist.CLIENT)
    public class KeyEntry extends KeyBindsList.Entry {
        private static final Component RESET_BUTTON_TITLE = Component.translatable("controls.reset");
        private static final int PADDING = 10;
        private final KeyMapping key;
        private final Component name;
        private final Button changeButton;
        private final Button resetButton;
        private boolean hasCollision = false;

        private KeyEntry(final KeyMapping key, final Component name) {
            this.key = key;
            this.name = name;
            this.changeButton = Button.builder(name, button -> {
                    KeyBindsList.this.keyBindsScreen.selectedKey = key;
                    KeyBindsList.this.resetMappingAndUpdateButtons();
                })
                .bounds(0, 0, 75, 20)
                .createNarration(
                    defaultNarrationSupplier -> key.isUnbound()
                        ? Component.translatable("narrator.controls.unbound", name)
                        : Component.translatable("narrator.controls.bound", name, defaultNarrationSupplier.get())
                )
                .build();
            this.resetButton = Button.builder(RESET_BUTTON_TITLE, button -> {
                key.setKey(key.getDefaultKey());
                KeyBindsList.this.resetMappingAndUpdateButtons();
            }).bounds(0, 0, 50, 20).createNarration(defaultNarrationSupplier -> Component.translatable("narrator.controls.reset", name)).build();
            this.refreshEntry();
        }

        @Override
        public void extractContent(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final boolean hovered, final float a) {
            int resetButtonX = KeyBindsList.this.scrollBarX() - this.resetButton.getWidth() - 10;
            int buttonY = this.getContentY() - 2;
            this.resetButton.setPosition(resetButtonX, buttonY);
            this.resetButton.extractRenderState(graphics, mouseX, mouseY, a);
            int changeButtonX = resetButtonX - 5 - this.changeButton.getWidth();
            this.changeButton.setPosition(changeButtonX, buttonY);
            this.changeButton.extractRenderState(graphics, mouseX, mouseY, a);
            graphics.text(KeyBindsList.this.minecraft.font, this.name, this.getContentX(), this.getContentYMiddle() - 9 / 2, -1);
            if (this.hasCollision) {
                int stripeWidth = 3;
                int stripeLeft = this.changeButton.getX() - 6;
                graphics.fill(stripeLeft, this.getContentY() - 1, stripeLeft + 3, this.getContentBottom(), -256);
            }
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return ImmutableList.of(this.changeButton, this.resetButton);
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return ImmutableList.of(this.changeButton, this.resetButton);
        }

        @Override
        public void refreshEntry() {
            this.changeButton.setMessage(this.key.getTranslatedKeyMessage());
            this.resetButton.active = !this.key.isDefault();
            this.hasCollision = false;
            MutableComponent tooltip = Component.empty();
            if (!this.key.isUnbound()) {
                for (KeyMapping otherKey : KeyBindsList.this.minecraft.options.keyMappings) {
                    if (otherKey != this.key && this.key.same(otherKey) && (!otherKey.isDefault() || !this.key.isDefault())) {
                        if (this.hasCollision) {
                            tooltip.append(", ");
                        }

                        this.hasCollision = true;
                        tooltip.append(Component.translatable(otherKey.getName()));
                    }
                }
            }

            if (this.hasCollision) {
                this.changeButton
                    .setMessage(
                        Component.literal("[ ")
                            .append(this.changeButton.getMessage().copy().withStyle(ChatFormatting.WHITE))
                            .append(" ]")
                            .withStyle(ChatFormatting.YELLOW)
                    );
                this.changeButton.setTooltip(Tooltip.create(Component.translatable("controls.keybinds.duplicateKeybinds", tooltip)));
            } else {
                this.changeButton.setTooltip(null);
            }

            if (KeyBindsList.this.keyBindsScreen.selectedKey == this.key) {
                this.changeButton
                    .setMessage(
                        Component.literal("> ")
                            .append(this.changeButton.getMessage().copy().withStyle(ChatFormatting.WHITE, ChatFormatting.UNDERLINE))
                            .append(" <")
                            .withStyle(ChatFormatting.YELLOW)
                    );
            }
        }
    }
}