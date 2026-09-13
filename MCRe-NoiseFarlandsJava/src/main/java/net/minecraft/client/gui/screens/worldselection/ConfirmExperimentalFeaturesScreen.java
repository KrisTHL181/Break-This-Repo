package net.minecraft.client.gui.screens.worldselection;

import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import java.util.Collection;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.TextAlignment;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.Style;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class ConfirmExperimentalFeaturesScreen extends Screen {
    private static final Component TITLE = Component.translatable("selectWorld.experimental.title");
    private static final Component MESSAGE = Component.translatable("selectWorld.experimental.message");
    private static final Component DETAILS_BUTTON = Component.translatable("selectWorld.experimental.details");
    private static final int COLUMN_SPACING = 10;
    private static final int DETAILS_BUTTON_WIDTH = 100;
    private final BooleanConsumer callback;
    private final Collection<Pack> enabledPacks;
    private final GridLayout layout = new GridLayout().columnSpacing(10).rowSpacing(20);

    public ConfirmExperimentalFeaturesScreen(final Collection<Pack> enabledPacks, final BooleanConsumer callback) {
        super(TITLE);
        this.enabledPacks = enabledPacks;
        this.callback = callback;
    }

    @Override
    public Component getNarrationMessage() {
        return CommonComponents.joinForNarration(super.getNarrationMessage(), MESSAGE);
    }

    @Override
    protected void init() {
        super.init();
        GridLayout.RowHelper helper = this.layout.createRowHelper(2);
        LayoutSettings centered = helper.newCellSettings().alignHorizontallyCenter();
        helper.addChild(new StringWidget(this.title, this.font), 2, centered);
        MultiLineTextWidget messageLabel = helper.addChild(new MultiLineTextWidget(MESSAGE, this.font).setCentered(true), 2, centered);
        messageLabel.setMaxWidth(310);
        helper.addChild(
            Button.builder(DETAILS_BUTTON, var1x -> this.minecraft.gui.setScreen(new ConfirmExperimentalFeaturesScreen.DetailsScreen())).width(100).build(),
            2,
            centered
        );
        helper.addChild(Button.builder(CommonComponents.GUI_PROCEED, button -> this.callback.accept(true)).build());
        helper.addChild(Button.builder(CommonComponents.GUI_BACK, button -> this.callback.accept(false)).build());
        this.layout.visitWidgets(x$0 -> this.addRenderableWidget(x$0));
        this.layout.arrangeElements();
        this.repositionElements();
    }

    @Override
    protected void repositionElements() {
        FrameLayout.alignInRectangle(this.layout, 0, 0, this.width, this.height, 0.5F, 0.5F);
    }

    @Override
    public void onClose() {
        this.callback.accept(false);
    }

    @OnlyIn(Dist.CLIENT)
    private class DetailsScreen extends Screen {
        private static final Component TITLE = Component.translatable("selectWorld.experimental.details.title");
        private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
        private ConfirmExperimentalFeaturesScreen.DetailsScreen.@Nullable PackList list;

        private DetailsScreen() {
            super(TITLE);
        }

        @Override
        protected void init() {
            this.layout.addTitleHeader(TITLE, this.font);
            this.list = this.layout
                .addToContents(
                    new ConfirmExperimentalFeaturesScreen.DetailsScreen.PackList(this.minecraft, ConfirmExperimentalFeaturesScreen.this.enabledPacks)
                );
            this.layout.addToFooter(Button.builder(CommonComponents.GUI_BACK, button -> this.onClose()).build());
            this.layout.visitWidgets(x$0 -> this.addRenderableWidget(x$0));
            this.repositionElements();
        }

        @Override
        protected void repositionElements() {
            if (this.list != null) {
                this.list.updateSize(this.width, this.layout);
            }

            this.layout.arrangeElements();
        }

        @Override
        public void onClose() {
            this.minecraft.gui.setScreen(ConfirmExperimentalFeaturesScreen.this);
        }

        @OnlyIn(Dist.CLIENT)
        private class PackList extends ObjectSelectionList<ConfirmExperimentalFeaturesScreen.DetailsScreen.PackListEntry> {
            public PackList(final Minecraft minecraft, final Collection<Pack> selectedPacks) {
                super(
                    minecraft, DetailsScreen.this.width, DetailsScreen.this.layout.getContentHeight(), DetailsScreen.this.layout.getHeaderHeight(), (9 + 2) * 3
                );

                for (Pack pack : selectedPacks) {
                    String nonVanillaFeatures = FeatureFlags.printMissingFlags(FeatureFlags.VANILLA_SET, pack.getRequestedFeatures());
                    if (!nonVanillaFeatures.isEmpty()) {
                        Component title = ComponentUtils.mergeStyles(pack.getTitle(), Style.EMPTY.withBold(true));
                        Component message = Component.translatable("selectWorld.experimental.details.entry", nonVanillaFeatures);
                        this.addEntry(
                            DetailsScreen.this.new PackListEntry(title, message, MultiLineLabel.create(DetailsScreen.this.font, message, this.getRowWidth()))
                        );
                    }
                }
            }

            @Override
            public int getRowWidth() {
                return this.width * 3 / 4;
            }
        }

        @OnlyIn(Dist.CLIENT)
        private class PackListEntry extends ObjectSelectionList.Entry<ConfirmExperimentalFeaturesScreen.DetailsScreen.PackListEntry> {
            private final Component packId;
            private final Component message;
            private final MultiLineLabel splitMessage;

            private PackListEntry(final Component packId, final Component message, final MultiLineLabel splitMessage) {
                this.packId = packId;
                this.message = message;
                this.splitMessage = splitMessage;
            }

            @Override
            public void extractContent(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final boolean hovered, final float a) {
                ActiveTextCollector textRenderer = graphics.textRenderer();
                graphics.text(DetailsScreen.this.minecraft.font, this.packId, this.getContentX(), this.getContentY(), -1);
                this.splitMessage.visitLines(TextAlignment.LEFT, this.getContentX(), this.getContentY() + 12, 9, textRenderer);
            }

            @Override
            public Component getNarration() {
                return Component.translatable("narrator.select", CommonComponents.joinForNarration(this.packId, this.message));
            }
        }
    }
}