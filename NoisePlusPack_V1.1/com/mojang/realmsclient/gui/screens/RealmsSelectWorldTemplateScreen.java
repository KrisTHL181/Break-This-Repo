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
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
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
   static final Logger LOGGER = LogUtils.getLogger();
   static final Identifier SLOT_FRAME_SPRITE = Identifier.withDefaultNamespace("widget/slot_frame");
   private static final Component SELECT_BUTTON_NAME = Component.translatable("mco.template.button.select");
   private static final Component TRAILER_BUTTON_NAME = Component.translatable("mco.template.button.trailer");
   private static final Component PUBLISHER_BUTTON_NAME = Component.translatable("mco.template.button.publisher");
   private static final int BUTTON_WIDTH = 100;
   final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
   final Consumer<WorldTemplate> callback;
   RealmsSelectWorldTemplateScreen.WorldTemplateList worldTemplateList;
   private final RealmsServer.WorldType worldType;
   private final List<Component> subtitle;
   private Button selectButton;
   private Button trailerButton;
   private Button publisherButton;
   @Nullable WorldTemplate selectedTemplate = null;
   @Nullable String currentLink;
   @Nullable List<TextRenderingUtils.Line> noTemplatesMessage;

   public RealmsSelectWorldTemplateScreen(
      Component p_167485_, Consumer<WorldTemplate> p_167486_, RealmsServer.WorldType p_167487_, @Nullable WorldTemplatePaginatedList p_167488_
   ) {
      this(p_167485_, p_167486_, p_167487_, p_167488_, List.of());
   }

   public RealmsSelectWorldTemplateScreen(
      Component p_167481_,
      Consumer<WorldTemplate> p_167482_,
      RealmsServer.WorldType p_167483_,
      @Nullable WorldTemplatePaginatedList p_429281_,
      List<Component> p_431000_
   ) {
      super(p_167481_);
      this.callback = p_167482_;
      this.worldType = p_167483_;
      if (p_429281_ == null) {
         this.worldTemplateList = new RealmsSelectWorldTemplateScreen.WorldTemplateList();
         this.fetchTemplatesAsync(new WorldTemplatePaginatedList(10));
      } else {
         this.worldTemplateList = new RealmsSelectWorldTemplateScreen.WorldTemplateList(Lists.newArrayList(p_429281_.templates()));
         this.fetchTemplatesAsync(p_429281_);
      }

      this.subtitle = p_431000_;
   }

   @Override
   public void init() {
      this.layout.setHeaderHeight(33 + this.subtitle.size() * 13);
      LinearLayout linearlayout = this.layout.addToHeader(LinearLayout.vertical().spacing(4));
      linearlayout.defaultCellSetting().alignHorizontallyCenter();
      linearlayout.addChild(new StringWidget(this.title, this.font));
      this.subtitle.forEach(p_447750_ -> linearlayout.addChild(new StringWidget(p_447750_, this.font)));
      this.worldTemplateList = this.layout.addToContents(new RealmsSelectWorldTemplateScreen.WorldTemplateList(this.worldTemplateList.getTemplates()));
      LinearLayout linearlayout1 = this.layout.addToFooter(LinearLayout.horizontal().spacing(8));
      linearlayout1.defaultCellSetting().alignHorizontallyCenter();
      this.trailerButton = linearlayout1.addChild(Button.builder(TRAILER_BUTTON_NAME, p_89701_ -> this.onTrailer()).width(100).build());
      this.selectButton = linearlayout1.addChild(Button.builder(SELECT_BUTTON_NAME, p_89696_ -> this.selectTemplate()).width(100).build());
      linearlayout1.addChild(Button.builder(CommonComponents.GUI_CANCEL, p_89691_ -> this.onClose()).width(100).build());
      this.publisherButton = linearlayout1.addChild(Button.builder(PUBLISHER_BUTTON_NAME, p_89679_ -> this.onPublish()).width(100).build());
      this.updateButtonStates();
      this.layout.visitWidgets(p_325159_ -> {
         AbstractWidget abstractwidget = this.addRenderableWidget(p_325159_);
      });
      this.repositionElements();
   }

   @Override
   protected void repositionElements() {
      this.worldTemplateList.updateSize(this.width, this.layout);
      this.layout.arrangeElements();
   }

   @Override
   public Component getNarrationMessage() {
      List<Component> list = Lists.newArrayListWithCapacity(2);
      list.add(this.title);
      list.addAll(this.subtitle);
      return CommonComponents.joinLines(list);
   }

   void updateButtonStates() {
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

   private void fetchTemplatesAsync(final WorldTemplatePaginatedList p_89654_) {
      (new Thread("realms-template-fetcher") {
            @Override
            public void run() {
               WorldTemplatePaginatedList worldtemplatepaginatedlist = p_89654_;
               RealmsClient realmsclient = RealmsClient.getOrCreate();

               while (worldtemplatepaginatedlist != null) {
                  Either<WorldTemplatePaginatedList, Exception> either = RealmsSelectWorldTemplateScreen.this.fetchTemplates(
                     worldtemplatepaginatedlist, realmsclient
                  );
                  worldtemplatepaginatedlist = RealmsSelectWorldTemplateScreen.this.minecraft
                     .submit(
                        () -> {
                           if (either.right().isPresent()) {
                              RealmsSelectWorldTemplateScreen.LOGGER.error("Couldn't fetch templates", (Throwable)either.right().get());
                              if (RealmsSelectWorldTemplateScreen.this.worldTemplateList.isEmpty()) {
                                 RealmsSelectWorldTemplateScreen.this.noTemplatesMessage = TextRenderingUtils.decompose(I18n.get("mco.template.select.failure"));
                              }

                              return null;
                           } else {
                              WorldTemplatePaginatedList worldtemplatepaginatedlist1 = (WorldTemplatePaginatedList)either.left().get();

                              for (WorldTemplate worldtemplate : worldtemplatepaginatedlist1.templates()) {
                                 RealmsSelectWorldTemplateScreen.this.worldTemplateList.addEntry(worldtemplate);
                              }

                              if (worldtemplatepaginatedlist1.templates().isEmpty()) {
                                 if (RealmsSelectWorldTemplateScreen.this.worldTemplateList.isEmpty()) {
                                    String s = I18n.get("mco.template.select.none", "%link");
                                    TextRenderingUtils.LineSegment textrenderingutils$linesegment = TextRenderingUtils.LineSegment.link(
                                       I18n.get("mco.template.select.none.linkTitle"), CommonLinks.REALMS_CONTENT_CREATION.toString()
                                    );
                                    RealmsSelectWorldTemplateScreen.this.noTemplatesMessage = TextRenderingUtils.decompose(s, textrenderingutils$linesegment);
                                 }

                                 return null;
                              } else {
                                 return worldtemplatepaginatedlist1;
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

   Either<WorldTemplatePaginatedList, Exception> fetchTemplates(WorldTemplatePaginatedList p_89656_, RealmsClient p_89657_) {
      try {
         return Either.left(p_89657_.fetchWorldTemplates(p_89656_.page() + 1, p_89656_.size(), this.worldType));
      } catch (RealmsServiceException realmsserviceexception) {
         return Either.right(realmsserviceexception);
      }
   }

   @Override
   public void render(GuiGraphics p_282162_, int p_89640_, int p_89641_, float p_89642_) {
      super.render(p_282162_, p_89640_, p_89641_, p_89642_);
      this.currentLink = null;
      if (this.noTemplatesMessage != null) {
         this.renderMultilineMessage(p_282162_, p_89640_, p_89641_, this.noTemplatesMessage);
      }
   }

   private void renderMultilineMessage(GuiGraphics p_282398_, int p_282163_, int p_282021_, List<TextRenderingUtils.Line> p_282203_) {
      for (int i = 0; i < p_282203_.size(); i++) {
         TextRenderingUtils.Line textrenderingutils$line = p_282203_.get(i);
         int j = row(4 + i);
         int k = textrenderingutils$line.segments.stream().mapToInt(p_280748_ -> this.font.width(p_280748_.renderedText())).sum();
         int l = this.width / 2 - k / 2;

         for (TextRenderingUtils.LineSegment textrenderingutils$linesegment : textrenderingutils$line.segments) {
            int i1 = textrenderingutils$linesegment.isLink() ? -13408581 : -1;
            String s = textrenderingutils$linesegment.renderedText();
            p_282398_.drawString(this.font, s, l, j, i1);
            int j1 = l + this.font.width(s);
            if (textrenderingutils$linesegment.isLink() && p_282163_ > l && p_282163_ < j1 && p_282021_ > j - 3 && p_282021_ < j + 8) {
               p_282398_.setTooltipForNextFrame(Component.literal(textrenderingutils$linesegment.getLinkUrl()), p_282163_, p_282021_);
               this.currentLink = textrenderingutils$linesegment.getLinkUrl();
            }

            l = j1;
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   class Entry extends ObjectSelectionList.Entry<RealmsSelectWorldTemplateScreen.Entry> {
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

      public Entry(final WorldTemplate p_89753_) {
         this.template = p_89753_;
         if (!p_89753_.link().isBlank()) {
            this.websiteButton = new ImageButton(
               15, 15, WEBSITE_LINK_SPRITES, ConfirmLinkScreen.confirmLink(RealmsSelectWorldTemplateScreen.this, p_89753_.link()), PUBLISHER_LINK_TOOLTIP
            );
            this.websiteButton.setTooltip(Tooltip.create(PUBLISHER_LINK_TOOLTIP));
         }

         if (!p_89753_.trailer().isBlank()) {
            this.trailerButton = new ImageButton(
               15, 15, TRAILER_LINK_SPRITES, ConfirmLinkScreen.confirmLink(RealmsSelectWorldTemplateScreen.this, p_89753_.trailer()), TRAILER_LINK_TOOLTIP
            );
            this.trailerButton.setTooltip(Tooltip.create(TRAILER_LINK_TOOLTIP));
         }
      }

      @Override
      public boolean mouseClicked(MouseButtonEvent p_426092_, boolean p_428672_) {
         RealmsSelectWorldTemplateScreen.this.selectedTemplate = this.template;
         RealmsSelectWorldTemplateScreen.this.updateButtonStates();
         if (p_428672_ && this.isFocused()) {
            RealmsSelectWorldTemplateScreen.this.callback.accept(this.template);
         }

         if (this.websiteButton != null) {
            this.websiteButton.mouseClicked(p_426092_, p_428672_);
         }

         if (this.trailerButton != null) {
            this.trailerButton.mouseClicked(p_426092_, p_428672_);
         }

         return super.mouseClicked(p_426092_, p_428672_);
      }

      @Override
      public void renderContent(GuiGraphics p_425202_, int p_429841_, int p_430872_, boolean p_431284_, float p_423571_) {
         p_425202_.blit(
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
         p_425202_.blitSprite(
            RenderPipelines.GUI_TEXTURED, RealmsSelectWorldTemplateScreen.SLOT_FRAME_SPRITE, this.getContentX(), this.getContentY() + 1, 40, 40
         );
         int i = 5;
         int j = RealmsSelectWorldTemplateScreen.this.font.width(this.template.version());
         if (this.websiteButton != null) {
            this.websiteButton.setPosition(this.getContentRight() - j - this.websiteButton.getWidth() - 10, this.getContentY());
            this.websiteButton.render(p_425202_, p_429841_, p_430872_, p_423571_);
         }

         if (this.trailerButton != null) {
            this.trailerButton.setPosition(this.getContentRight() - j - this.trailerButton.getWidth() * 2 - 15, this.getContentY());
            this.trailerButton.render(p_425202_, p_429841_, p_430872_, p_423571_);
         }

         int k = this.getContentX() + 45 + 20;
         int l = this.getContentY() + 5;
         p_425202_.drawString(RealmsSelectWorldTemplateScreen.this.font, this.template.name(), k, l, -1);
         p_425202_.drawString(RealmsSelectWorldTemplateScreen.this.font, this.template.version(), this.getContentRight() - j - 5, l, -6250336);
         p_425202_.drawString(RealmsSelectWorldTemplateScreen.this.font, this.template.author(), k, l + 9 + 5, -6250336);
         if (!this.template.recommendedPlayers().isBlank()) {
            p_425202_.drawString(RealmsSelectWorldTemplateScreen.this.font, this.template.recommendedPlayers(), k, this.getContentBottom() - 4 - 5, -8355712);
         }
      }

      @Override
      public Component getNarration() {
         Component component = CommonComponents.joinLines(
            Component.literal(this.template.name()),
            Component.translatable("mco.template.select.narrate.authors", this.template.author()),
            Component.literal(this.template.recommendedPlayers()),
            Component.translatable("mco.template.select.narrate.version", this.template.version())
         );
         return Component.translatable("narrator.select", component);
      }
   }

   @OnlyIn(Dist.CLIENT)
   class WorldTemplateList extends ObjectSelectionList<RealmsSelectWorldTemplateScreen.Entry> {
      public WorldTemplateList() {
         this(Collections.emptyList());
      }

      public WorldTemplateList(final Iterable<WorldTemplate> p_89795_) {
         super(
            Minecraft.getInstance(),
            RealmsSelectWorldTemplateScreen.this.width,
            RealmsSelectWorldTemplateScreen.this.layout.getContentHeight(),
            RealmsSelectWorldTemplateScreen.this.layout.getHeaderHeight(),
            46
         );
         p_89795_.forEach(this::addEntry);
      }

      public void addEntry(WorldTemplate p_89805_) {
         this.addEntry(RealmsSelectWorldTemplateScreen.this.new Entry(p_89805_));
      }

      @Override
      public boolean mouseClicked(MouseButtonEvent p_427175_, boolean p_429010_) {
         if (RealmsSelectWorldTemplateScreen.this.currentLink != null) {
            ConfirmLinkScreen.confirmLinkNow(RealmsSelectWorldTemplateScreen.this, RealmsSelectWorldTemplateScreen.this.currentLink);
            return true;
         } else {
            return super.mouseClicked(p_427175_, p_429010_);
         }
      }

      public void setSelected(RealmsSelectWorldTemplateScreen.@Nullable Entry p_89807_) {
         super.setSelected(p_89807_);
         RealmsSelectWorldTemplateScreen.this.selectedTemplate = p_89807_ == null ? null : p_89807_.template;
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
         return this.children().stream().map(p_89814_ -> p_89814_.template).collect(Collectors.toList());
      }
   }
}
