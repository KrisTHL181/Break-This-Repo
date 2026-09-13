package net.minecraft.client.gui.screens.advancements;

import com.google.common.collect.Maps;
import com.mojang.blaze3d.platform.cursor.CursorTypes;
import java.util.Map;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.ClientAdvancements;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundSeenAdvancementsPacket;
import net.minecraft.resources.Identifier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class AdvancementsScreen extends Screen implements ClientAdvancements.Listener {
   private static final Identifier WINDOW_LOCATION = Identifier.withDefaultNamespace("textures/gui/advancements/window.png");
   public static final int WINDOW_WIDTH = 252;
   public static final int WINDOW_HEIGHT = 140;
   private static final int WINDOW_INSIDE_X = 9;
   private static final int WINDOW_INSIDE_Y = 18;
   public static final int WINDOW_INSIDE_WIDTH = 234;
   public static final int WINDOW_INSIDE_HEIGHT = 113;
   private static final int WINDOW_TITLE_X = 8;
   private static final int WINDOW_TITLE_Y = 6;
   private static final int BACKGROUND_TEXTURE_WIDTH = 256;
   private static final int BACKGROUND_TEXTURE_HEIGHT = 256;
   public static final int BACKGROUND_TILE_WIDTH = 16;
   public static final int BACKGROUND_TILE_HEIGHT = 16;
   public static final int BACKGROUND_TILE_COUNT_X = 14;
   public static final int BACKGROUND_TILE_COUNT_Y = 7;
   private static final double SCROLL_SPEED = 16.0;
   private static final Component VERY_SAD_LABEL = Component.translatable("advancements.sad_label");
   private static final Component NO_ADVANCEMENTS_LABEL = Component.translatable("advancements.empty");
   private static final Component TITLE = Component.translatable("gui.advancements");
   private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
   private final @Nullable Screen lastScreen;
   private final ClientAdvancements advancements;
   private final Map<AdvancementHolder, AdvancementTab> tabs = Maps.newLinkedHashMap();
   private @Nullable AdvancementTab selectedTab;
   private boolean isScrolling;

   public AdvancementsScreen(ClientAdvancements p_97340_) {
      this(p_97340_, null);
   }

   public AdvancementsScreen(ClientAdvancements p_333280_, @Nullable Screen p_335811_) {
      super(TITLE);
      this.advancements = p_333280_;
      this.lastScreen = p_335811_;
   }

   @Override
   protected void init() {
      this.layout.addTitleHeader(TITLE, this.font);
      this.tabs.clear();
      this.selectedTab = null;
      this.advancements.setListener(this);
      if (this.selectedTab == null && !this.tabs.isEmpty()) {
         AdvancementTab advancementtab = this.tabs.values().iterator().next();
         this.advancements.setSelectedTab(advancementtab.getRootNode().holder(), true);
      } else {
         this.advancements.setSelectedTab(this.selectedTab == null ? null : this.selectedTab.getRootNode().holder(), true);
      }

      this.layout.addToFooter(Button.builder(CommonComponents.GUI_DONE, p_329618_ -> this.onClose()).width(200).build());
      this.layout.visitWidgets(p_335563_ -> {
         AbstractWidget abstractwidget = this.addRenderableWidget(p_335563_);
      });
      this.repositionElements();
   }

   @Override
   protected void repositionElements() {
      this.layout.arrangeElements();
   }

   @Override
   public void onClose() {
      this.minecraft.setScreen(this.lastScreen);
   }

   @Override
   public void removed() {
      this.advancements.setListener(null);
      ClientPacketListener clientpacketlistener = this.minecraft.getConnection();
      if (clientpacketlistener != null) {
         clientpacketlistener.send(ServerboundSeenAdvancementsPacket.closedScreen());
      }
   }

   @Override
   public boolean mouseClicked(MouseButtonEvent p_431395_, boolean p_430650_) {
      if (p_431395_.button() == 0) {
         int i = (this.width - 252) / 2;
         int j = (this.height - 140) / 2;

         for (AdvancementTab advancementtab : this.tabs.values()) {
            if (advancementtab.isMouseOver(i, j, p_431395_.x(), p_431395_.y())) {
               this.advancements.setSelectedTab(advancementtab.getRootNode().holder(), true);
               break;
            }
         }
      }

      return super.mouseClicked(p_431395_, p_430650_);
   }

   @Override
   public boolean keyPressed(KeyEvent p_429408_) {
      if (this.minecraft.options.keyAdvancements.matches(p_429408_)) {
         this.minecraft.setScreen(null);
         this.minecraft.mouseHandler.grabMouse();
         return true;
      } else {
         return super.keyPressed(p_429408_);
      }
   }

   @Override
   public void render(GuiGraphics p_282589_, int p_282255_, int p_283354_, float p_283123_) {
      super.render(p_282589_, p_282255_, p_283354_, p_283123_);
      int i = (this.width - 252) / 2;
      int j = (this.height - 140) / 2;
      p_282589_.nextStratum();
      this.renderInside(p_282589_, i, j);
      p_282589_.nextStratum();
      this.renderWindow(p_282589_, i, j, p_282255_, p_283354_);
      if (this.isScrolling && this.selectedTab != null) {
         if (this.selectedTab.canScrollHorizontally() && this.selectedTab.canScrollVertically()) {
            p_282589_.requestCursor(CursorTypes.RESIZE_ALL);
         } else if (this.selectedTab.canScrollHorizontally()) {
            p_282589_.requestCursor(CursorTypes.RESIZE_EW);
         } else if (this.selectedTab.canScrollVertically()) {
            p_282589_.requestCursor(CursorTypes.RESIZE_NS);
         }
      }

      this.renderTooltips(p_282589_, p_282255_, p_283354_, i, j);
   }

   @Override
   public boolean mouseDragged(MouseButtonEvent p_429951_, double p_97347_, double p_97348_) {
      if (p_429951_.button() != 0) {
         this.isScrolling = false;
         return false;
      }

      if (!this.isScrolling) {
         this.isScrolling = true;
      } else if (this.selectedTab != null) {
         this.selectedTab.scroll(p_97347_, p_97348_);
      }

      return true;
   }

   @Override
   public boolean mouseReleased(MouseButtonEvent p_457352_) {
      this.isScrolling = false;
      return super.mouseReleased(p_457352_);
   }

   @Override
   public boolean mouseScrolled(double p_300678_, double p_297858_, double p_301134_, double p_300488_) {
      if (this.selectedTab != null) {
         this.selectedTab.scroll(p_301134_ * 16.0, p_300488_ * 16.0);
         return true;
      } else {
         return false;
      }
   }

   private void renderInside(GuiGraphics p_282012_, int p_97375_, int p_97376_) {
      AdvancementTab advancementtab = this.selectedTab;
      if (advancementtab == null) {
         p_282012_.fill(p_97375_ + 9, p_97376_ + 18, p_97375_ + 9 + 234, p_97376_ + 18 + 113, -16777216);
         int i = p_97375_ + 9 + 117;
         p_282012_.drawCenteredString(this.font, NO_ADVANCEMENTS_LABEL, i, p_97376_ + 18 + 56 - 4, -1);
         p_282012_.drawCenteredString(this.font, VERY_SAD_LABEL, i, p_97376_ + 18 + 113 - 9, -1);
      } else {
         advancementtab.drawContents(p_282012_, p_97375_ + 9, p_97376_ + 18);
      }
   }

   public void renderWindow(GuiGraphics p_283395_, int p_281890_, int p_282532_, int p_451461_, int p_451545_) {
      p_283395_.blit(RenderPipelines.GUI_TEXTURED, WINDOW_LOCATION, p_281890_, p_282532_, 0.0F, 0.0F, 252, 140, 256, 256);
      if (this.tabs.size() > 1) {
         for (AdvancementTab advancementtab : this.tabs.values()) {
            advancementtab.drawTab(p_283395_, p_281890_, p_282532_, p_451461_, p_451545_, advancementtab == this.selectedTab);
         }

         for (AdvancementTab advancementtab1 : this.tabs.values()) {
            advancementtab1.drawIcon(p_283395_, p_281890_, p_282532_);
         }
      }

      p_283395_.drawString(this.font, this.selectedTab != null ? this.selectedTab.getTitle() : TITLE, p_281890_ + 8, p_282532_ + 6, -12566464, false);
   }

   private void renderTooltips(GuiGraphics p_282784_, int p_283556_, int p_282458_, int p_281519_, int p_283371_) {
      if (this.selectedTab != null) {
         p_282784_.pose().pushMatrix();
         p_282784_.pose().translate(p_281519_ + 9, p_283371_ + 18);
         p_282784_.nextStratum();
         this.selectedTab.drawTooltips(p_282784_, p_283556_ - p_281519_ - 9, p_282458_ - p_283371_ - 18, p_281519_, p_283371_);
         p_282784_.pose().popMatrix();
      }

      if (this.tabs.size() > 1) {
         for (AdvancementTab advancementtab : this.tabs.values()) {
            if (advancementtab.isMouseOver(p_281519_, p_283371_, p_283556_, p_282458_)) {
               p_282784_.setTooltipForNextFrame(this.font, advancementtab.getTitle(), p_283556_, p_282458_);
            }
         }
      }
   }

   @Override
   public void onAddAdvancementRoot(AdvancementNode p_300702_) {
      AdvancementTab advancementtab = AdvancementTab.create(this.minecraft, this, this.tabs.size(), p_300702_);
      if (advancementtab != null) {
         this.tabs.put(p_300702_.holder(), advancementtab);
      }
   }

   @Override
   public void onRemoveAdvancementRoot(AdvancementNode p_298890_) {
   }

   @Override
   public void onAddAdvancementTask(AdvancementNode p_297934_) {
      AdvancementTab advancementtab = this.getTab(p_297934_);
      if (advancementtab != null) {
         advancementtab.addAdvancement(p_297934_);
      }
   }

   @Override
   public void onRemoveAdvancementTask(AdvancementNode p_301169_) {
   }

   @Override
   public void onUpdateAdvancementProgress(AdvancementNode p_300708_, AdvancementProgress p_97369_) {
      AdvancementWidget advancementwidget = this.getAdvancementWidget(p_300708_);
      if (advancementwidget != null) {
         advancementwidget.setProgress(p_97369_);
      }
   }

   @Override
   public void onSelectedTabChanged(@Nullable AdvancementHolder p_297665_) {
      this.selectedTab = this.tabs.get(p_297665_);
   }

   @Override
   public void onAdvancementsCleared() {
      this.tabs.clear();
      this.selectedTab = null;
   }

   public @Nullable AdvancementWidget getAdvancementWidget(AdvancementNode p_298026_) {
      AdvancementTab advancementtab = this.getTab(p_298026_);
      return advancementtab == null ? null : advancementtab.getWidget(p_298026_.holder());
   }

   private @Nullable AdvancementTab getTab(AdvancementNode p_300894_) {
      AdvancementNode advancementnode = p_300894_.root();
      return this.tabs.get(advancementnode.holder());
   }
}
