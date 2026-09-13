package net.minecraft.client.gui.screens.options;

import java.util.List;
import java.util.Locale;
import java.util.SortedMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.language.LanguageInfo;
import net.minecraft.client.resources.language.LanguageManager;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class LanguageSelectScreen extends OptionsSubScreen {
    private static final Component WARNING_LABEL = Component.translatable("options.languageAccuracyWarning").withColor(-4539718);
    private static final int FOOTER_HEIGHT = 53;
    private static final Component SEARCH_HINT = Component.translatable("gui.language.search").withStyle(EditBox.SEARCH_HINT_STYLE);
    private static final int SEARCH_BOX_HEIGHT = 15;
    private final LanguageManager languageManager;
    private LanguageSelectScreen.@Nullable LanguageSelectionList languageSelectionList;
    private @Nullable EditBox search;

    public LanguageSelectScreen(final Screen lastScreen, final Options options, final LanguageManager languageManager) {
        super(lastScreen, options, Component.translatable("options.language.title"));
        this.languageManager = languageManager;
        this.layout.setFooterHeight(53);
    }

    @Override
    protected void addTitle() {
        LinearLayout header = this.layout.addToHeader(LinearLayout.vertical().spacing(4));
        header.defaultCellSetting().alignHorizontallyCenter();
        header.addChild(new StringWidget(this.title, this.font));
        this.search = header.addChild(new EditBox(this.font, 0, 0, 200, 15, Component.empty()));
        this.search.setHint(SEARCH_HINT);
        this.search.setResponder(string -> {
            if (this.languageSelectionList != null) {
                this.languageSelectionList.filterEntries(string);
            }
        });
        this.layout.setHeaderHeight((int)(12.0 + 9.0 + 15.0));
    }

    @Override
    protected void setInitialFocus() {
        if (this.search != null) {
            this.setInitialFocus(this.search);
        } else {
            super.setInitialFocus();
        }
    }

    @Override
    protected void addContents() {
        this.languageSelectionList = this.layout.addToContents(new LanguageSelectScreen.LanguageSelectionList(this.minecraft));
    }

    @Override
    protected void addOptions() {
    }

    @Override
    protected void addFooter() {
        LinearLayout footer = this.layout.addToFooter(LinearLayout.vertical()).spacing(8);
        footer.defaultCellSetting().alignHorizontallyCenter();
        footer.addChild(new StringWidget(WARNING_LABEL, this.font));
        LinearLayout bottomButtons = footer.addChild(LinearLayout.horizontal().spacing(8));
        bottomButtons.addChild(
            Button.builder(Component.translatable("options.font"), var1x -> this.minecraft.gui.setScreen(new FontOptionsScreen(this, this.options))).build()
        );
        bottomButtons.addChild(Button.builder(CommonComponents.GUI_DONE, button -> this.onDone()).build());
    }

    @Override
    protected void repositionElements() {
        super.repositionElements();
        if (this.languageSelectionList != null) {
            this.languageSelectionList.updateSize(this.width, this.layout);
        }
    }

    private void onDone() {
        if (this.languageSelectionList != null && this.languageSelectionList.getSelected() != null) {
            LanguageSelectScreen.LanguageSelectionList.Entry selectedEntry = this.languageSelectionList.getSelected();
            if (!selectedEntry.code.equals(this.languageManager.getSelected())) {
                this.languageManager.setSelected(selectedEntry.code);
                this.options.languageCode = selectedEntry.code;
                this.minecraft.reloadResourcePacks();
            }
        }

        this.minecraft.gui.setScreen(this.lastScreen);
    }

    @OnlyIn(Dist.CLIENT)
    private class LanguageSelectionList extends ObjectSelectionList<LanguageSelectScreen.LanguageSelectionList.Entry> {
        public LanguageSelectionList(final Minecraft minecraft) {
            super(minecraft, LanguageSelectScreen.this.width, LanguageSelectScreen.this.height - 33 - 53, 33, 18);
            String selectedLanguage = LanguageSelectScreen.this.languageManager.getSelected();
            LanguageSelectScreen.this.languageManager.getLanguages().forEach((code, info) -> {
                LanguageSelectScreen.LanguageSelectionList.Entry entry = new LanguageSelectScreen.LanguageSelectionList.Entry(code, info);
                this.addEntry(entry);
                if (selectedLanguage.equals(code)) {
                    this.setSelected(entry);
                }
            });
            if (this.getSelected() != null) {
                this.centerScrollOn(this.getSelected());
            }
        }

        private void filterEntries(final String filter) {
            SortedMap<String, LanguageInfo> languages = LanguageSelectScreen.this.languageManager.getLanguages();
            List<LanguageSelectScreen.LanguageSelectionList.Entry> filteredEntries = languages.entrySet()
                .stream()
                .filter(
                    entry -> filter.isEmpty()
                        || entry.getValue().name().toLowerCase(Locale.ROOT).contains(filter.toLowerCase(Locale.ROOT))
                        || entry.getValue().region().toLowerCase(Locale.ROOT).contains(filter.toLowerCase(Locale.ROOT))
                )
                .map(entry -> new LanguageSelectScreen.LanguageSelectionList.Entry(entry.getKey(), entry.getValue()))
                .toList();
            this.replaceEntries(filteredEntries);
            this.refreshScrollAmount();
        }

        @Override
        public int getRowWidth() {
            return super.getRowWidth() + 50;
        }

        @OnlyIn(Dist.CLIENT)
        public class Entry extends ObjectSelectionList.Entry<LanguageSelectScreen.LanguageSelectionList.Entry> {
            private final String code;
            private final Component language;

            public Entry(final String code, final LanguageInfo language) {
                this.code = code;
                this.language = language.toComponent();
            }

            @Override
            public void extractContent(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final boolean hovered, final float a) {
                graphics.centeredText(LanguageSelectScreen.this.font, this.language, LanguageSelectionList.this.width / 2, this.getContentYMiddle() - 9 / 2, -1);
            }

            @Override
            public boolean keyPressed(final KeyEvent event) {
                if (event.isSelection()) {
                    this.select();
                    LanguageSelectScreen.this.onDone();
                    return true;
                } else {
                    return super.keyPressed(event);
                }
            }

            @Override
            public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
                this.select();
                if (doubleClick) {
                    LanguageSelectScreen.this.onDone();
                }

                return super.mouseClicked(event, doubleClick);
            }

            private void select() {
                LanguageSelectionList.this.setSelected(this);
            }

            @Override
            public Component getNarration() {
                return Component.translatable("narrator.select", this.language);
            }
        }
    }
}