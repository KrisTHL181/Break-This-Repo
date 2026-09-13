package com.mojang.realmsclient.gui.screens;

import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Either;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.dto.RealmsServer;
import com.mojang.realmsclient.dto.WorldTemplate;
import com.mojang.realmsclient.dto.WorldTemplatePaginatedList;
import com.mojang.realmsclient.exception.RealmsServiceException;
import com.mojang.realmsclient.util.RealmsTextureManager;
import com.mojang.realmsclient.util.TextRenderingUtils;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.realms.RealmsScreen;
import net.minecraft.resources.Identifier;
import net.minecraft.util.CommonLinks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class RealmsSelectWorldTemplateScreen extends RealmsScreen {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Identifier SLOT_FRAME_SPRITE = Identifier.withDefaultNamespace("widget/slot_frame");
    private static final Component SELECT_BUTTON_NAME = Component.translatable("mco.template.button.select");
    private static final Component TRAILER_BUTTON_NAME = Component.translatable("mco.template.button.trailer");
    private static final Component PUBLISHER_BUTTON_NAME = Component.translatable("mco.template.button.publisher");
    private static final int BUTTON_WIDTH = 100;
    private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
    private final Consumer<WorldTemplate> callback;
    private RealmsSelectWorldTemplateScreen.WorldTemplateList worldTemplateList;
    private final RealmsServer.WorldType worldType;
    private final List<Component> subtitle;
    private Button selectButton;
    private Button trailerButton;
    private Button publisherButton;
    private @Nullable WorldTemplate selectedTemplate = null;
    private @Nullable String currentLink;
    private @Nullable List<TextRenderingUtils.Line> noTemplatesMessage;

    public RealmsSelectWorldTemplateScreen(
        final Component title,
        final Consumer<WorldTemplate> callback,
        final RealmsServer.WorldType worldType,
        final @Nullable WorldTemplatePaginatedList alreadyFetched
    ) {
        this(title, callback, worldType, alreadyFetched, List.of());
    }

    public RealmsSelectWorldTemplateScreen(
        final Component title,
        final Consumer<WorldTemplate> callback,
        final RealmsServer.WorldType worldType,
        final @Nullable WorldTemplatePaginatedList alreadyFetched,
        final List<Component> subtitle
    ) {
        super(title);
        this.callback = callback;
        this.worldType = worldType;
        if (alreadyFetched == null) {
            this.worldTemplateList = new RealmsSelectWorldTemplateScreen.WorldTemplateList();
            this.fetchTemplatesAsync(new WorldTemplatePaginatedList(10));
        } else {
            this.worldTemplateList = new RealmsSelectWorldTemplateScreen.WorldTemplateList(Lists.newArrayList(alreadyFetched.templates()));
            this.fetchTemplatesAsync(alreadyFetched);
        }

        this.subtitle = subtitle;
    }

    @Override
    public void init() {
        this.layout.setHeaderHeight(33 + this.subtitle.size() * (9 + 4));
        LinearLayout header = this.layout.addToHeader(LinearLayout.vertical().spacing(4));
        header.defaultCellSetting().alignHorizontallyCenter();
        header.addChild(new StringWidget(this.title, this.font));
        this.subtitle.forEach(warning -> header.addChild(new StringWidget(warning, this.font)));
        this.worldTemplateList = this.layout.addToContents(new RealmsSelectWorldTemplateScreen.WorldTemplateList(this.worldTemplateList.getTemplates()));
        LinearLayout bottomButtons = this.layout.addToFooter(LinearLayout.horizontal().spacing(8));
        bottomButtons.defaultCellSetting().alignHorizontallyCenter();
        this.trailerButton = bottomButtons.addChild(Button.builder(TRAILER_BUTTON_NAME, button -> this.onTrailer()).width(100).build());
        this.selectButton = bottomButtons.addChild(Button.builder(SELECT_BUTTON_NAME, button -> this.selectTemplate()).width(100).build());
        bottomButtons.addChild(Button.builder(CommonComponents.GUI_CANCEL, button -> this.onClose()).width(100).build());
        this.publisherButton = bottomButtons.addChild(Button.builder(PUBLISHER_BUTTON_NAME, button -> this.onPublish()).width(100).build());
        this.updateButtonStates();
        this.layout.visitWidgets(x$0 -> this.addRenderableWidget(x$0));
        this.repositionElements();
    }

    @Override
    protected void repositionElements() {
        this.worldTemplateList.updateSize(this.width, this.layout);
        this.layout.arrangeElements();
    }

    @Override
    public Component getNarrationMessage() {
        List<Component> parts = Lists.newArrayListWithCapacity(2);
        parts.add(this.title);
        parts.addAll(this.subtitle);
        return CommonComponents.joinLines(parts);
    }

    private void updateButtonStates() {
        this.publisherButton.visible = this.selectedTemplate != null && !this.selectedTemplate.link().isEmpty();
        this.trailerButton.visible = this.selectedTemplate != null && !this.selectedTemplate.trailer().isEmpty();
        this.selectButton.active = this.selectedTemplate != null;
    }

    @Override
    public void onClose() {
        this.callback.accept(null);
    }

    private void selectTemplate() {
        if (this.selectedTemplate != null) {
            this.callback.accept(this.selectedTemplate);
        }
    }

    private void onTrailer() {
        if (this.selectedTemplate != null && !this.selectedTemplate.trailer().isBlank()) {
            ConfirmLinkScreen.confirmLinkNow(this, this.selectedTemplate.trailer());
        }
    }

    private void onPublish() {
        if (this.selectedTemplate != null && !this.selectedTemplate.link().isBlank()) {
            ConfirmLinkScreen.confirmLinkNow(this, this.selectedTemplate.link());
        }
    }

    private void fetchTemplatesAsync(final WorldTemplatePaginatedList startPage) {
        (new Thread("realms-template-fetcher") {
                @Override
                public void run() {
                    WorldTemplatePaginatedList page = startPage;
                    RealmsClient client = RealmsClient.getOrCreate();

                    while (page != null) {
                        Either<WorldTemplatePaginatedList, Exception> result = RealmsSelectWorldTemplateScreen.this.fetchTemplates(page, client);
                        page = RealmsSelectWorldTemplateScreen.this.minecraft
                            .submit(
                                () -> {
                                    if (result.right().isPresent()) {
                                        RealmsSelectWorldTemplateScreen.LOGGER.error("Couldn't fetch templates", result.right().get());
                                        if (RealmsSelectWorldTemplateScreen.this.worldTemplateList.isEmpty()) {
                                            RealmsSelectWorldTemplateScreen.this.noTemplatesMessage = TextRenderingUtils.decompose(
                                                I18n.get("mco.template.select.failure")
                                            );
                                        }

                                        return null;
                                    } else {
                                        WorldTemplatePaginatedList currentPage = result.left().get();

                                        for (WorldTemplate template : currentPage.templates()) {
                                            RealmsSelectWorldTemplateScreen.this.worldTemplateList.addEntry(template);
                                        }

                                        if (currentPage.templates().isEmpty()) {
                                            if (RealmsSelectWorldTemplateScreen.this.worldTemplateList.isEmpty()) {
                                                String withoutLink = I18n.get("mco.template.select.none", "%link");
                                                TextRenderingUtils.LineSegment link = TextRenderingUtils.LineSegment.link(
                                                    I18n.get("mco.template.select.none.linkTitle"), CommonLinks.REALMS_CONTENT_CREATION.toString()
                                                );
                                                RealmsSelectWorldTemplateScreen.this.noTemplatesMessage = TextRenderingUtils.decompose(withoutLink, link);
                                            }

                                            return null;
                                        } else {
                                            return currentPage;
                                        }
                                    }
                                }
                            )
                            .join();
                    }
                }
            })
            .start();
    }

    private Either<WorldTemplatePaginatedList, Exception> fetchTemplates(final WorldTemplatePaginatedList paginatedList, final RealmsClient client) {
        try {
            return Either.left(client.fetchWorldTemplates(paginatedList.page() + 1, paginatedList.size(), this.worldType));
        } catch (RealmsServiceException e) {
            return Either.right(e);
        }
    }

    @Override
    public void extractRenderState(final GuiGraphicsExtractor graphics, final int xm, final int ym, final float a) {
        super.extractRenderState(graphics, xm, ym, a);
        this.currentLink = null;
        if (this.noTemplatesMessage != null) {
            this.extractMultilineMessage(graphics, xm, ym, this.noTemplatesMessage);
        }
    }

    private void extractMultilineMessage(
        final GuiGraphicsExtractor graphics, final int xm, final int ym, final List<TextRenderingUtils.Line> noTemplatesMessage
    ) {
        for (int i = 0; i < noTemplatesMessage.size(); i++) {
            TextRenderingUtils.Line line = noTemplatesMessage.get(i);
            int lineY = row(4 + i);
            int lineWidth = line.segments.stream().mapToInt(s -> this.font.width(s.renderedText())).sum();
            int startX = this.width / 2 - lineWidth / 2;

            for (TextRenderingUtils.LineSegment segment : line.segments) {
                int color = segment.isLink() ? -13408581 : -1;
                String text = segment.renderedText();
                graphics.text(this.font, text, startX, lineY, color);
                int endX = startX + this.font.width(text);
                if (segment.isLink() && xm > startX && xm < endX && ym > lineY - 3 && ym < lineY + 8) {
                    graphics.setTooltipForNextFrame(Component.literal(segment.getLinkUrl()), xm, ym);
                    this.currentLink = segment.getLinkUrl();
                }

                startX = endX;
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    private class Entry extends ObjectSelectionList.Entry<RealmsSelectWorldTemplateScreen.Entry> {
        private static final WidgetSprites WEBSITE_LINK_SPRITES = new WidgetSprites(
            Identifier.withDefaultNamespace("icon/link"), Identifier.withDefaultNamespace("icon/link_highlighted")
        );
        private static final WidgetSprites TRAILER_LINK_SPRITES = new WidgetSprites(
            Identifier.withDefaultNamespace("icon/video_link"), Identifier.withDefaultNamespace("icon/video_link_highlighted")
        );
        private static final Component PUBLISHER_LINK_TOOLTIP = Component.translatable("mco.template.info.tooltip");
        private static final Component TRAILER_LINK_TOOLTIP = Component.translatable("mco.template.trailer.tooltip");
        public final WorldTemplate template;
        private @Nullable ImageButton websiteButton;
        private @Nullable ImageButton trailerButton;

        public Entry(final WorldTemplate template) {
            this.template = template;
            if (!template.link().isBlank()) {
                this.websiteButton = new ImageButton(
                    15, 15, WEBSITE_LINK_SPRITES, ConfirmLinkScreen.confirmLink(RealmsSelectWorldTemplateScreen.this, template.link()), PUBLISHER_LINK_TOOLTIP
                );
                this.websiteButton.setTooltip(Tooltip.create(PUBLISHER_LINK_TOOLTIP));
            }

            if (!template.trailer().isBlank()) {
                this.trailerButton = new ImageButton(
                    15, 15, TRAILER_LINK_SPRITES, ConfirmLinkScreen.confirmLink(RealmsSelectWorldTemplateScreen.this, template.trailer()), TRAILER_LINK_TOOLTIP
                );
                this.trailerButton.setTooltip(Tooltip.create(TRAILER_LINK_TOOLTIP));
            }
        }

        @Override
        public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
            RealmsSelectWorldTemplateScreen.this.selectedTemplate = this.template;
            RealmsSelectWorldTemplateScreen.this.updateButtonStates();
            if (doubleClick && this.isFocused()) {
                RealmsSelectWorldTemplateScreen.this.callback.accept(this.template);
            }

            if (this.websiteButton != null) {
                this.websiteButton.mouseClicked(event, doubleClick);
            }

            if (this.trailerButton != null) {
                this.trailerButton.mouseClicked(event, doubleClick);
            }

            return super.mouseClicked(event, doubleClick);
        }

        @Override
        public void extractContent(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final boolean hovered, final float a) {
            graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                RealmsTextureManager.worldTemplate(this.template.id(), this.template.image()),
                this.getContentX() + 1,
                this.getContentY() + 1 + 1,
                0.0F,
                0.0F,
                38,
                38,
                38,
                38
            );
            graphics.blitSprite(
                RenderPipelines.GUI_TEXTURED, RealmsSelectWorldTemplateScreen.SLOT_FRAME_SPRITE, this.getContentX(), this.getContentY() + 1, 40, 40
            );
            int padding = 5;
            int versionTextWidth = RealmsSelectWorldTemplateScreen.this.font.width(this.template.version());
            if (this.websiteButton != null) {
                this.websiteButton.setPosition(this.getContentRight() - versionTextWidth - this.websiteButton.getWidth() - 10, this.getContentY());
                this.websiteButton.extractRenderState(graphics, mouseX, mouseY, a);
            }

            if (this.trailerButton != null) {
                this.trailerButton.setPosition(this.getContentRight() - versionTextWidth - this.trailerButton.getWidth() * 2 - 15, this.getContentY());
                this.trailerButton.extractRenderState(graphics, mouseX, mouseY, a);
            }

            int textX = this.getContentX() + 45 + 20;
            int textY = this.getContentY() + 5;
            graphics.text(RealmsSelectWorldTemplateScreen.this.font, this.template.name(), textX, textY, -1);
            graphics.text(RealmsSelectWorldTemplateScreen.this.font, this.template.version(), this.getContentRight() - versionTextWidth - 5, textY, -6250336);
            graphics.text(RealmsSelectWorldTemplateScreen.this.font, this.template.author(), textX, textY + 9 + 5, -6250336);
            if (!this.template.recommendedPlayers().isBlank()) {
                graphics.text(
                    RealmsSelectWorldTemplateScreen.this.font, this.template.recommendedPlayers(), textX, this.getContentBottom() - 9 / 2 - 5, -8355712
                );
            }
        }

        @Override
        public Component getNarration() {
            Component entryName = CommonComponents.joinLines(
                Component.literal(this.template.name()),
                Component.translatable("mco.template.select.narrate.authors", this.template.author()),
                Component.literal(this.template.recommendedPlayers()),
                Component.translatable("mco.template.select.narrate.version", this.template.version())
            );
            return Component.translatable("narrator.select", entryName);
        }
    }

    @OnlyIn(Dist.CLIENT)
    private class WorldTemplateList extends ObjectSelectionList<RealmsSelectWorldTemplateScreen.Entry> {
        public WorldTemplateList() {
            this(Collections.emptyList());
        }

        public WorldTemplateList(final Iterable<WorldTemplate> templates) {
            super(
                Minecraft.getInstance(),
                RealmsSelectWorldTemplateScreen.this.width,
                RealmsSelectWorldTemplateScreen.this.layout.getContentHeight(),
                RealmsSelectWorldTemplateScreen.this.layout.getHeaderHeight(),
                46
            );
            templates.forEach(this::addEntry);
        }

        public void addEntry(final WorldTemplate template) {
            this.addEntry(RealmsSelectWorldTemplateScreen.this.new Entry(template));
        }

        @Override
        public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
            if (RealmsSelectWorldTemplateScreen.this.currentLink != null) {
                ConfirmLinkScreen.confirmLinkNow(RealmsSelectWorldTemplateScreen.this, RealmsSelectWorldTemplateScreen.this.currentLink);
                return true;
            } else {
                return super.mouseClicked(event, doubleClick);
            }
        }

        public void setSelected(final RealmsSelectWorldTemplateScreen.@Nullable Entry selected) {
            super.setSelected(selected);
            RealmsSelectWorldTemplateScreen.this.selectedTemplate = selected == null ? null : selected.template;
            RealmsSelectWorldTemplateScreen.this.updateButtonStates();
        }

        @Override
        public int getRowWidth() {
            return 300;
        }

        public boolean isEmpty() {
            return this.getItemCount() == 0;
        }

        public List<WorldTemplate> getTemplates() {
            return this.children().stream().map(c -> c.template).collect(Collectors.toList());
        }
    }
}