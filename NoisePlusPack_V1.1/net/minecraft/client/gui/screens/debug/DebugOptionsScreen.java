package net.minecraft.client.gui.screens.debug;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.floats.FloatComparators;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.debug.DebugEntryCategory;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.client.gui.components.debug.DebugScreenEntry;
import net.minecraft.client.gui.components.debug.DebugScreenEntryStatus;
import net.minecraft.client.gui.components.debug.DebugScreenProfile;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.layouts.SpacerElement;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class DebugOptionsScreen extends Screen {
   private static final Component TITLE = Component.translatable("debug.options.title");
   private static final Component SUBTITLE = Component.translatable("debug.options.warning").withColor(-2142128);
   static final Component ENABLED_TEXT = Component.translatable("debug.entry.always");
   static final Component IN_OVERLAY_TEXT = Component.translatable("debug.entry.overlay");
   static final Component DISABLED_TEXT = CommonComponents.OPTION_OFF;
   static final Component NOT_ALLOWED_TOOLTIP = Component.translatable("debug.options.notAllowed.tooltip");
   private static final Component SEARCH = Component.translatable("debug.options.search").withStyle(EditBox.SEARCH_HINT_STYLE);
   final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this, 61, 33);
   private DebugOptionsScreen.@Nullable OptionList optionList;
   private EditBox searchBox;
   final List<Button> profileButtons = new ArrayList<>();

   public DebugOptionsScreen() {
      super(TITLE);
   }

   @Override
   protected void init() {
      LinearLayout linearlayout = this.layout.addToHeader(LinearLayout.vertical().spacing(8));
      this.optionList = new DebugOptionsScreen.OptionList();
      int i = this.optionList.getRowWidth();
      LinearLayout linearlayout1 = LinearLayout.horizontal().spacing(8);
      linearlayout1.addChild(new SpacerElement(i / 3, 1));
      linearlayout1.addChild(new StringWidget(TITLE, this.font), linearlayout1.newCellSettings().alignVerticallyMiddle());
      this.searchBox = new EditBox(this.font, 0, 0, i / 3, 20, this.searchBox, SEARCH);
      this.searchBox.setResponder(p_429834_ -> this.optionList.updateSearch(p_429834_));
      this.searchBox.setHint(SEARCH);
      linearlayout1.addChild(this.searchBox);
      linearlayout.addChild(linearlayout1, LayoutSettings::alignHorizontallyCenter);
      linearlayout.addChild(new MultiLineTextWidget(SUBTITLE, this.font).setMaxWidth(i).setCentered(true), LayoutSettings::alignHorizontallyCenter);
      this.layout.addToContents(this.optionList);
      LinearLayout linearlayout2 = this.layout.addToFooter(LinearLayout.horizontal().spacing(8));
      this.addProfileButton(DebugScreenProfile.DEFAULT, linearlayout2);
      this.addProfileButton(DebugScreenProfile.PERFORMANCE, linearlayout2);
      linearlayout2.addChild(Button.builder(CommonComponents.GUI_DONE, p_426150_ -> this.onClose()).width(60).build());
      this.layout.visitWidgets(p_426584_ -> {
         AbstractWidget abstractwidget = this.addRenderableWidget(p_426584_);
      });
      this.repositionElements();
   }

   @Override
   public void renderBlurredBackground(GuiGraphics p_431670_) {
      this.minecraft.gui.renderDebugOverlay(p_431670_);
      super.renderBlurredBackground(p_431670_);
   }

   @Override
   protected void setInitialFocus() {
      this.setInitialFocus(this.searchBox);
   }

   private void addProfileButton(DebugScreenProfile p_423993_, LinearLayout p_430879_) {
      Button button = Button.builder(Component.translatable(p_423993_.translationKey()), p_426076_ -> {
         this.minecraft.debugEntries.loadProfile(p_423993_);
         this.minecraft.debugEntries.save();
         this.optionList.refreshEntries();

         for (Button button1 : this.profileButtons) {
            button1.active = true;
         }

         p_426076_.active = false;
      }).width(120).build();
      button.active = !this.minecraft.debugEntries.isUsingProfile(p_423993_);
      this.profileButtons.add(button);
      p_430879_.addChild(button);
   }

   @Override
   protected void repositionElements() {
      this.layout.arrangeElements();
      if (this.optionList != null) {
         this.optionList.updateSize(this.width, this.layout);
      }
   }

   public DebugOptionsScreen.@Nullable OptionList getOptionList() {
      return this.optionList;
   }

   @OnlyIn(Dist.CLIENT)
   public abstract static class AbstractOptionEntry extends ContainerObjectSelectionList.Entry<DebugOptionsScreen.AbstractOptionEntry> {
      public abstract void refreshEntry();
   }

   @OnlyIn(Dist.CLIENT)
   class CategoryEntry extends DebugOptionsScreen.AbstractOptionEntry {
      final Component category;

      public CategoryEntry(final Component p_423713_) {
         this.category = p_423713_;
      }

      @Override
      public void renderContent(GuiGraphics p_428557_, int p_423607_, int p_430999_, boolean p_431305_, float p_431269_) {
         p_428557_.drawCenteredString(
            DebugOptionsScreen.this.minecraft.font, this.category, this.getContentX() + this.getContentWidth() / 2, this.getContentY() + 5, -1
         );
      }

      @Override
      public List<? extends GuiEventListener> children() {
         return ImmutableList.of();
      }

      @Override
      public List<? extends NarratableEntry> narratables() {
         return ImmutableList.of(new NarratableEntry() {
            @Override
            public NarratableEntry.NarrationPriority narrationPriority() {
               return NarratableEntry.NarrationPriority.HOVERED;
            }

            @Override
            public void updateNarration(NarrationElementOutput p_424152_) {
               p_424152_.add(NarratedElementType.TITLE, CategoryEntry.this.category);
            }
         });
      }

      @Override
      public void refreshEntry() {
      }
   }

   @OnlyIn(Dist.CLIENT)
   class OptionEntry extends DebugOptionsScreen.AbstractOptionEntry {
      private static final int BUTTON_WIDTH = 60;
      private final Identifier location;
      protected final List<AbstractWidget> children = Lists.newArrayList();
      private final CycleButton<Boolean> always;
      private final CycleButton<Boolean> overlay;
      private final CycleButton<Boolean> never;
      private final String name;
      private final boolean isAllowed;

      public OptionEntry(final Identifier p_458702_) {
         this.location = p_458702_;
         DebugScreenEntry debugscreenentry = DebugScreenEntries.getEntry(p_458702_);
         this.isAllowed = debugscreenentry != null && debugscreenentry.isAllowed(DebugOptionsScreen.this.minecraft.showOnlyReducedInfo());
         String s = p_458702_.getPath();
         if (this.isAllowed) {
            this.name = s;
         } else {
            this.name = ChatFormatting.ITALIC + s;
         }

         this.always = CycleButton.booleanBuilder(
               DebugOptionsScreen.ENABLED_TEXT.copy().withColor(-2142128), DebugOptionsScreen.ENABLED_TEXT.copy().withColor(-4539718), false
            )
            .displayOnlyValue()
            .withCustomNarration(this::narrateButton)
            .create(10, 5, 60, 16, Component.literal(s), (p_448033_, p_448034_) -> this.setValue(p_458702_, DebugScreenEntryStatus.ALWAYS_ON));
         this.overlay = CycleButton.booleanBuilder(
               DebugOptionsScreen.IN_OVERLAY_TEXT.copy().withColor(-171), DebugOptionsScreen.IN_OVERLAY_TEXT.copy().withColor(-4539718), false
            )
            .displayOnlyValue()
            .withCustomNarration(this::narrateButton)
            .create(10, 5, 60, 16, Component.literal(s), (p_448039_, p_448040_) -> this.setValue(p_458702_, DebugScreenEntryStatus.IN_OVERLAY));
         this.never = CycleButton.booleanBuilder(
               DebugOptionsScreen.DISABLED_TEXT.copy().withColor(-1), DebugOptionsScreen.DISABLED_TEXT.copy().withColor(-4539718), false
            )
            .displayOnlyValue()
            .withCustomNarration(this::narrateButton)
            .create(10, 5, 60, 16, Component.literal(s), (p_448036_, p_448037_) -> this.setValue(p_458702_, DebugScreenEntryStatus.NEVER));
         this.children.add(this.never);
         this.children.add(this.overlay);
         this.children.add(this.always);
         this.refreshEntry();
      }

      private MutableComponent narrateButton(CycleButton<Boolean> p_425638_) {
         DebugScreenEntryStatus debugscreenentrystatus = DebugOptionsScreen.this.minecraft.debugEntries.getStatus(this.location);
         MutableComponent mutablecomponent = Component.translatable("debug.entry.currently." + debugscreenentrystatus.getSerializedName(), this.name);
         return CommonComponents.optionNameValue(mutablecomponent, p_425638_.getMessage());
      }

      private void setValue(Identifier p_451704_, DebugScreenEntryStatus p_426716_) {
         DebugOptionsScreen.this.minecraft.debugEntries.setStatus(p_451704_, p_426716_);

         for (Button button : DebugOptionsScreen.this.profileButtons) {
            button.active = true;
         }

         this.refreshEntry();
      }

      @Override
      public List<? extends GuiEventListener> children() {
         return this.children;
      }

      @Override
      public List<? extends NarratableEntry> narratables() {
         return this.children;
      }

      @Override
      public void renderContent(GuiGraphics p_427669_, int p_422698_, int p_426525_, boolean p_427163_, float p_423082_) {
         int i = this.getContentX();
         int j = this.getContentY();
         p_427669_.drawString(DebugOptionsScreen.this.minecraft.font, this.name, i, j + 5, this.isAllowed ? -1 : -8355712);
         int k = i + this.getContentWidth() - this.never.getWidth() - this.overlay.getWidth() - this.always.getWidth();
         if (!this.isAllowed && p_427163_ && p_422698_ < k) {
            p_427669_.setTooltipForNextFrame(DebugOptionsScreen.NOT_ALLOWED_TOOLTIP, p_422698_, p_426525_);
         }

         this.never.setX(k);
         this.overlay.setX(this.never.getX() + this.never.getWidth());
         this.always.setX(this.overlay.getX() + this.overlay.getWidth());
         this.always.setY(j);
         this.overlay.setY(j);
         this.never.setY(j);
         this.always.render(p_427669_, p_422698_, p_426525_, p_423082_);
         this.overlay.render(p_427669_, p_422698_, p_426525_, p_423082_);
         this.never.render(p_427669_, p_422698_, p_426525_, p_423082_);
      }

      @Override
      public void refreshEntry() {
         DebugScreenEntryStatus debugscreenentrystatus = DebugOptionsScreen.this.minecraft.debugEntries.getStatus(this.location);
         this.always.setValue(debugscreenentrystatus == DebugScreenEntryStatus.ALWAYS_ON);
         this.overlay.setValue(debugscreenentrystatus == DebugScreenEntryStatus.IN_OVERLAY);
         this.never.setValue(debugscreenentrystatus == DebugScreenEntryStatus.NEVER);
         this.always.active = !this.always.getValue();
         this.overlay.active = !this.overlay.getValue();
         this.never.active = !this.never.getValue();
      }
   }

   @OnlyIn(Dist.CLIENT)
   public class OptionList extends ContainerObjectSelectionList<DebugOptionsScreen.AbstractOptionEntry> {
      private static final Comparator<Map.Entry<Identifier, DebugScreenEntry>> COMPARATOR = (p_448041_, p_448042_) -> {
         int i = FloatComparators.NATURAL_COMPARATOR.compare(p_448041_.getValue().category().sortKey(), p_448042_.getValue().category().sortKey());
         return i != 0 ? i : p_448041_.getKey().compareTo(p_448042_.getKey());
      };
      private static final int ITEM_HEIGHT = 20;

      public OptionList() {
         super(
            Minecraft.getInstance(),
            DebugOptionsScreen.this.width,
            DebugOptionsScreen.this.layout.getContentHeight(),
            DebugOptionsScreen.this.layout.getHeaderHeight(),
            20
         );
         this.updateSearch("");
      }

      @Override
      public void renderWidget(GuiGraphics p_428148_, int p_428689_, int p_426233_, float p_431560_) {
         super.renderWidget(p_428148_, p_428689_, p_426233_, p_431560_);
      }

      @Override
      public int getRowWidth() {
         return 350;
      }

      public void refreshEntries() {
         this.children().forEach(DebugOptionsScreen.AbstractOptionEntry::refreshEntry);
      }

      public void updateSearch(String p_424249_) {
         this.clearEntries();
         List<Map.Entry<Identifier, DebugScreenEntry>> list = new ArrayList<>(DebugScreenEntries.allEntries().entrySet());
         list.sort(COMPARATOR);
         DebugEntryCategory debugentrycategory = null;

         for (Map.Entry<Identifier, DebugScreenEntry> entry : list) {
            if (entry.getKey().getPath().contains(p_424249_)) {
               DebugEntryCategory debugentrycategory1 = entry.getValue().category();
               if (!debugentrycategory1.equals(debugentrycategory)) {
                  this.addEntry(DebugOptionsScreen.this.new CategoryEntry(debugentrycategory1.label()));
                  debugentrycategory = debugentrycategory1;
               }

               this.addEntry(DebugOptionsScreen.this.new OptionEntry(entry.getKey()));
            }
         }

         this.notifyListUpdated();
      }

      private void notifyListUpdated() {
         this.refreshScrollAmount();
         DebugOptionsScreen.this.triggerImmediateNarration(true);
      }
   }
}
