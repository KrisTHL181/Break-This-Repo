package net.minecraft.client.gui.screens.achievement;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.ItemDisplayWidget;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.components.tabs.GridLayoutTab;
import net.minecraft.client.gui.components.tabs.LoadingTab;
import net.minecraft.client.gui.components.tabs.TabManager;
import net.minecraft.client.gui.components.tabs.TabNavigationBar;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.stats.Stat;
import net.minecraft.stats.StatType;
import net.minecraft.stats.Stats;
import net.minecraft.stats.StatsCounter;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class StatsScreen extends Screen {
   private static final Component TITLE = Component.translatable("gui.stats");
   static final Identifier SLOT_SPRITE = Identifier.withDefaultNamespace("container/slot");
   static final Identifier HEADER_SPRITE = Identifier.withDefaultNamespace("statistics/header");
   static final Identifier SORT_UP_SPRITE = Identifier.withDefaultNamespace("statistics/sort_up");
   static final Identifier SORT_DOWN_SPRITE = Identifier.withDefaultNamespace("statistics/sort_down");
   private static final Component PENDING_TEXT = Component.translatable("multiplayer.downloadingStats");
   static final Component NO_VALUE_DISPLAY = Component.translatable("stats.none");
   private static final Component GENERAL_BUTTON = Component.translatable("stat.generalButton");
   private static final Component ITEMS_BUTTON = Component.translatable("stat.itemsButton");
   private static final Component MOBS_BUTTON = Component.translatable("stat.mobsButton");
   protected final Screen lastScreen;
   private static final int LIST_WIDTH = 280;
   final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
   private final TabManager tabManager = new TabManager(p_325374_ -> {
      AbstractWidget abstractwidget = this.addRenderableWidget(p_325374_);
   }, p_420749_ -> this.removeWidget(p_420749_));
   private @Nullable TabNavigationBar tabNavigationBar;
   final StatsCounter stats;
   private boolean isLoading = true;

   public StatsScreen(Screen p_96906_, StatsCounter p_96907_) {
      super(TITLE);
      this.lastScreen = p_96906_;
      this.stats = p_96907_;
   }

   @Override
   protected void init() {
      Component component = PENDING_TEXT;
      this.tabNavigationBar = TabNavigationBar.builder(this.tabManager, this.width)
         .addTabs(
            new LoadingTab(this.getFont(), GENERAL_BUTTON, component),
            new LoadingTab(this.getFont(), ITEMS_BUTTON, component),
            new LoadingTab(this.getFont(), MOBS_BUTTON, component)
         )
         .build();
      this.addRenderableWidget(this.tabNavigationBar);
      this.layout.addToFooter(Button.builder(CommonComponents.GUI_DONE, p_325372_ -> this.onClose()).width(200).build());
      this.tabNavigationBar.setTabActiveState(0, true);
      this.tabNavigationBar.setTabActiveState(1, false);
      this.tabNavigationBar.setTabActiveState(2, false);
      this.layout.visitWidgets(p_420747_ -> {
         p_420747_.setTabOrderGroup(1);
         this.addRenderableWidget(p_420747_);
      });
      this.tabNavigationBar.selectTab(0, false);
      this.repositionElements();
      this.minecraft.getConnection().send(new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.REQUEST_STATS));
   }

   public void onStatsUpdated() {
      if (this.isLoading) {
         if (this.tabNavigationBar != null) {
            this.removeWidget(this.tabNavigationBar);
         }

         this.tabNavigationBar = TabNavigationBar.builder(this.tabManager, this.width)
            .addTabs(
               new StatsScreen.StatisticsTab(GENERAL_BUTTON, new StatsScreen.GeneralStatisticsList(this.minecraft)),
               new StatsScreen.StatisticsTab(ITEMS_BUTTON, new StatsScreen.ItemStatisticsList(this.minecraft)),
               new StatsScreen.StatisticsTab(MOBS_BUTTON, new StatsScreen.MobsStatisticsList(this.minecraft))
            )
            .build();
         this.setFocused(this.tabNavigationBar);
         this.addRenderableWidget(this.tabNavigationBar);
         this.setTabActiveStateAndTooltip(1);
         this.setTabActiveStateAndTooltip(2);
         this.tabNavigationBar.selectTab(0, false);
         this.repositionElements();
         this.isLoading = false;
      }
   }

   private void setTabActiveStateAndTooltip(int p_427790_) {
      if (this.tabNavigationBar != null) {
         boolean flag = this.tabNavigationBar.getTabs().get(p_427790_) instanceof StatsScreen.StatisticsTab statsscreen$statisticstab
            && !statsscreen$statisticstab.list.children().isEmpty();
         this.tabNavigationBar.setTabActiveState(p_427790_, flag);
         if (flag) {
            this.tabNavigationBar.setTabTooltip(p_427790_, null);
         } else {
            this.tabNavigationBar.setTabTooltip(p_427790_, Tooltip.create(Component.translatable("gui.stats.none_found")));
         }
      }
   }

   @Override
   protected void repositionElements() {
      if (this.tabNavigationBar != null) {
         this.tabNavigationBar.setWidth(this.width);
         this.tabNavigationBar.arrangeElements();
         int i = this.tabNavigationBar.getRectangle().bottom();
         ScreenRectangle screenrectangle = new ScreenRectangle(0, i, this.width, this.height - this.layout.getFooterHeight() - i);
         this.tabNavigationBar.getTabs().forEach(p_420746_ -> p_420746_.visitChildren(p_420744_ -> p_420744_.setHeight(screenrectangle.height())));
         this.tabManager.setTabArea(screenrectangle);
         this.layout.setHeaderHeight(i);
         this.layout.arrangeElements();
      }
   }

   @Override
   public boolean keyPressed(KeyEvent p_426028_) {
      return this.tabNavigationBar != null && this.tabNavigationBar.keyPressed(p_426028_) ? true : super.keyPressed(p_426028_);
   }

   @Override
   public void render(GuiGraphics p_430813_, int p_428532_, int p_431744_, float p_425341_) {
      super.render(p_430813_, p_428532_, p_431744_, p_425341_);
      p_430813_.blit(RenderPipelines.GUI_TEXTURED, Screen.FOOTER_SEPARATOR, 0, this.height - this.layout.getFooterHeight(), 0.0F, 0.0F, this.width, 2, 32, 2);
   }

   @Override
   protected void renderMenuBackground(GuiGraphics p_427978_) {
      p_427978_.blit(RenderPipelines.GUI_TEXTURED, CreateWorldScreen.TAB_HEADER_BACKGROUND, 0, 0, 0.0F, 0.0F, this.width, this.layout.getHeaderHeight(), 16, 16);
      this.renderMenuBackground(p_427978_, 0, this.layout.getHeaderHeight(), this.width, this.height);
   }

   @Override
   public void onClose() {
      this.minecraft.setScreen(this.lastScreen);
   }

   static String getTranslationKey(Stat<Identifier> p_96947_) {
      return "stat." + p_96947_.getValue().toString().replace(':', '.');
   }

   @OnlyIn(Dist.CLIENT)
   class GeneralStatisticsList extends ObjectSelectionList<StatsScreen.GeneralStatisticsList.Entry> {
      public GeneralStatisticsList(final Minecraft p_96995_) {
         super(p_96995_, StatsScreen.this.width, StatsScreen.this.layout.getContentHeight(), 33, 14);
         ObjectArrayList<Stat<Identifier>> objectarraylist = new ObjectArrayList(Stats.CUSTOM.iterator());
         objectarraylist.sort(Comparator.comparing(p_96997_ -> I18n.get(StatsScreen.getTranslationKey((Stat<Identifier>)p_96997_))));
         ObjectListIterator var4 = objectarraylist.iterator();

         while (var4.hasNext()) {
            Stat<Identifier> stat = (Stat<Identifier>)var4.next();
            this.addEntry(new StatsScreen.GeneralStatisticsList.Entry(stat));
         }
      }

      @Override
      public int getRowWidth() {
         return 280;
      }

      @Override
      protected void renderListBackground(GuiGraphics p_427114_) {
      }

      @Override
      protected void renderListSeparators(GuiGraphics p_427813_) {
      }

      @OnlyIn(Dist.CLIENT)
      class Entry extends ObjectSelectionList.Entry<StatsScreen.GeneralStatisticsList.Entry> {
         private final Stat<Identifier> stat;
         private final Component statDisplay;

         Entry(final Stat<Identifier> p_97005_) {
            this.stat = p_97005_;
            this.statDisplay = Component.translatable(StatsScreen.getTranslationKey(p_97005_));
         }

         private String getValueText() {
            return this.stat.format(StatsScreen.this.stats.getValue(this.stat));
         }

         @Override
         public void renderContent(GuiGraphics p_429392_, int p_427216_, int p_427606_, boolean p_426771_, float p_427846_) {
            int i = this.getContentYMiddle() - 4;
            int j = GeneralStatisticsList.this.children().indexOf(this);
            int k = j % 2 == 0 ? -1 : -4539718;
            p_429392_.drawString(StatsScreen.this.font, this.statDisplay, this.getContentX() + 2, i, k);
            String s = this.getValueText();
            p_429392_.drawString(StatsScreen.this.font, s, this.getContentRight() - StatsScreen.this.font.width(s) - 4, i, k);
         }

         @Override
         public Component getNarration() {
            return Component.translatable(
               "narrator.select", Component.empty().append(this.statDisplay).append(CommonComponents.SPACE).append(this.getValueText())
            );
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   class ItemStatisticsList extends ContainerObjectSelectionList<StatsScreen.ItemStatisticsList.Entry> {
      private static final int SLOT_BG_SIZE = 18;
      private static final int SLOT_STAT_HEIGHT = 22;
      private static final int SLOT_BG_Y = 1;
      private static final int SORT_NONE = 0;
      private static final int SORT_DOWN = -1;
      private static final int SORT_UP = 1;
      protected final List<StatType<Block>> blockColumns;
      protected final List<StatType<Item>> itemColumns;
      protected final Comparator<StatsScreen.ItemStatisticsList.ItemRow> itemStatSorter = new StatsScreen.ItemStatisticsList.ItemRowComparator();
      protected @Nullable StatType<?> sortColumn;
      protected int sortOrder;

      public ItemStatisticsList(final Minecraft p_97032_) {
         super(p_97032_, StatsScreen.this.width, StatsScreen.this.layout.getContentHeight(), 33, 22);
         this.blockColumns = Lists.newArrayList();
         this.blockColumns.add(Stats.BLOCK_MINED);
         this.itemColumns = Lists.newArrayList(new StatType[]{Stats.ITEM_BROKEN, Stats.ITEM_CRAFTED, Stats.ITEM_USED, Stats.ITEM_PICKED_UP, Stats.ITEM_DROPPED});
         Set<Item> set = Sets.newIdentityHashSet();

         for (Item item : BuiltInRegistries.ITEM) {
            boolean flag = false;

            for (StatType<Item> stattype : this.itemColumns) {
               if (stattype.contains(item) && StatsScreen.this.stats.getValue(stattype.get(item)) > 0) {
                  flag = true;
               }
            }

            if (flag) {
               set.add(item);
            }
         }

         for (Block block : BuiltInRegistries.BLOCK) {
            boolean flag1 = false;

            for (StatType<Block> stattype1 : this.blockColumns) {
               if (stattype1.contains(block) && StatsScreen.this.stats.getValue(stattype1.get(block)) > 0) {
                  flag1 = true;
               }
            }

            if (flag1) {
               set.add(block.asItem());
            }
         }

         set.remove(Items.AIR);
         if (!set.isEmpty()) {
            this.addEntry(new StatsScreen.ItemStatisticsList.HeaderEntry());

            for (Item item1 : set) {
               this.addEntry(new StatsScreen.ItemStatisticsList.ItemRow(item1));
            }
         }
      }

      @Override
      protected void renderListBackground(GuiGraphics p_426711_) {
      }

      int getColumnX(int p_329609_) {
         return 75 + 40 * p_329609_;
      }

      @Override
      public int getRowWidth() {
         return 280;
      }

      StatType<?> getColumn(int p_97034_) {
         return p_97034_ < this.blockColumns.size() ? this.blockColumns.get(p_97034_) : this.itemColumns.get(p_97034_ - this.blockColumns.size());
      }

      int getColumnIndex(StatType<?> p_97059_) {
         int i = this.blockColumns.indexOf(p_97059_);
         if (i >= 0) {
            return i;
         }

         int j = this.itemColumns.indexOf(p_97059_);
         return j >= 0 ? j + this.blockColumns.size() : -1;
      }

      protected void sortByColumn(StatType<?> p_97039_) {
         if (p_97039_ != this.sortColumn) {
            this.sortColumn = p_97039_;
            this.sortOrder = -1;
         } else if (this.sortOrder == -1) {
            this.sortOrder = 1;
         } else {
            this.sortColumn = null;
            this.sortOrder = 0;
         }

         this.sortItems(this.itemStatSorter);
      }

      protected void sortItems(Comparator<StatsScreen.ItemStatisticsList.ItemRow> p_430805_) {
         List<StatsScreen.ItemStatisticsList.ItemRow> list = this.getItemRows();
         list.sort(p_430805_);
         this.clearEntriesExcept(this.children().getFirst());

         for (StatsScreen.ItemStatisticsList.ItemRow statsscreen$itemstatisticslist$itemrow : list) {
            this.addEntry(statsscreen$itemstatisticslist$itemrow);
         }
      }

      private List<StatsScreen.ItemStatisticsList.ItemRow> getItemRows() {
         List<StatsScreen.ItemStatisticsList.ItemRow> list = new ArrayList<>();
         this.children().forEach(p_428629_ -> {
            if (p_428629_ instanceof StatsScreen.ItemStatisticsList.ItemRow statsscreen$itemstatisticslist$itemrow) {
               list.add(statsscreen$itemstatisticslist$itemrow);
            }
         });
         return list;
      }

      @Override
      protected void renderListSeparators(GuiGraphics p_428163_) {
      }

      @OnlyIn(Dist.CLIENT)
      abstract static class Entry extends ContainerObjectSelectionList.Entry<StatsScreen.ItemStatisticsList.Entry> {
      }

      @OnlyIn(Dist.CLIENT)
      class HeaderEntry extends StatsScreen.ItemStatisticsList.Entry {
         private static final Identifier BLOCK_MINED_SPRITE = Identifier.withDefaultNamespace("statistics/block_mined");
         private static final Identifier ITEM_BROKEN_SPRITE = Identifier.withDefaultNamespace("statistics/item_broken");
         private static final Identifier ITEM_CRAFTED_SPRITE = Identifier.withDefaultNamespace("statistics/item_crafted");
         private static final Identifier ITEM_USED_SPRITE = Identifier.withDefaultNamespace("statistics/item_used");
         private static final Identifier ITEM_PICKED_UP_SPRITE = Identifier.withDefaultNamespace("statistics/item_picked_up");
         private static final Identifier ITEM_DROPPED_SPRITE = Identifier.withDefaultNamespace("statistics/item_dropped");
         private final StatsScreen.ItemStatisticsList.HeaderEntry.StatSortButton blockMined;
         private final StatsScreen.ItemStatisticsList.HeaderEntry.StatSortButton itemBroken;
         private final StatsScreen.ItemStatisticsList.HeaderEntry.StatSortButton itemCrafted;
         private final StatsScreen.ItemStatisticsList.HeaderEntry.StatSortButton itemUsed;
         private final StatsScreen.ItemStatisticsList.HeaderEntry.StatSortButton itemPickedUp;
         private final StatsScreen.ItemStatisticsList.HeaderEntry.StatSortButton itemDropped;
         private final List<AbstractWidget> children = new ArrayList<>();

         HeaderEntry() {
            this.blockMined = new StatsScreen.ItemStatisticsList.HeaderEntry.StatSortButton(0, BLOCK_MINED_SPRITE);
            this.itemBroken = new StatsScreen.ItemStatisticsList.HeaderEntry.StatSortButton(1, ITEM_BROKEN_SPRITE);
            this.itemCrafted = new StatsScreen.ItemStatisticsList.HeaderEntry.StatSortButton(2, ITEM_CRAFTED_SPRITE);
            this.itemUsed = new StatsScreen.ItemStatisticsList.HeaderEntry.StatSortButton(3, ITEM_USED_SPRITE);
            this.itemPickedUp = new StatsScreen.ItemStatisticsList.HeaderEntry.StatSortButton(4, ITEM_PICKED_UP_SPRITE);
            this.itemDropped = new StatsScreen.ItemStatisticsList.HeaderEntry.StatSortButton(5, ITEM_DROPPED_SPRITE);
            this.children.addAll(List.of(this.blockMined, this.itemBroken, this.itemCrafted, this.itemUsed, this.itemPickedUp, this.itemDropped));
         }

         @Override
         public void renderContent(GuiGraphics p_427361_, int p_427610_, int p_424788_, boolean p_422515_, float p_431410_) {
            this.blockMined.setPosition(this.getContentX() + ItemStatisticsList.this.getColumnX(0) - 18, this.getContentY() + 1);
            this.blockMined.render(p_427361_, p_427610_, p_424788_, p_431410_);
            this.itemBroken.setPosition(this.getContentX() + ItemStatisticsList.this.getColumnX(1) - 18, this.getContentY() + 1);
            this.itemBroken.render(p_427361_, p_427610_, p_424788_, p_431410_);
            this.itemCrafted.setPosition(this.getContentX() + ItemStatisticsList.this.getColumnX(2) - 18, this.getContentY() + 1);
            this.itemCrafted.render(p_427361_, p_427610_, p_424788_, p_431410_);
            this.itemUsed.setPosition(this.getContentX() + ItemStatisticsList.this.getColumnX(3) - 18, this.getContentY() + 1);
            this.itemUsed.render(p_427361_, p_427610_, p_424788_, p_431410_);
            this.itemPickedUp.setPosition(this.getContentX() + ItemStatisticsList.this.getColumnX(4) - 18, this.getContentY() + 1);
            this.itemPickedUp.render(p_427361_, p_427610_, p_424788_, p_431410_);
            this.itemDropped.setPosition(this.getContentX() + ItemStatisticsList.this.getColumnX(5) - 18, this.getContentY() + 1);
            this.itemDropped.render(p_427361_, p_427610_, p_424788_, p_431410_);
            if (ItemStatisticsList.this.sortColumn != null) {
               int i = ItemStatisticsList.this.getColumnX(ItemStatisticsList.this.getColumnIndex(ItemStatisticsList.this.sortColumn)) - 36;
               Identifier identifier = ItemStatisticsList.this.sortOrder == 1 ? StatsScreen.SORT_UP_SPRITE : StatsScreen.SORT_DOWN_SPRITE;
               p_427361_.blitSprite(RenderPipelines.GUI_TEXTURED, identifier, this.getContentX() + i, this.getContentY() + 1, 18, 18);
            }
         }

         @Override
         public List<? extends GuiEventListener> children() {
            return this.children;
         }

         @Override
         public List<? extends NarratableEntry> narratables() {
            return this.children;
         }

         @OnlyIn(Dist.CLIENT)
         class StatSortButton extends ImageButton {
            private final Identifier sprite;

            StatSortButton(final int p_429613_, final Identifier p_454378_) {
               super(
                  18,
                  18,
                  new WidgetSprites(StatsScreen.HEADER_SPRITE, StatsScreen.SLOT_SPRITE),
                  p_427562_ -> ItemStatisticsList.this.sortByColumn(ItemStatisticsList.this.getColumn(p_429613_)),
                  ItemStatisticsList.this.getColumn(p_429613_).getDisplayName()
               );
               this.sprite = p_454378_;
               this.setTooltip(Tooltip.create(this.getMessage()));
            }

            @Override
            public void renderContents(GuiGraphics p_457750_, int p_452558_, int p_459984_, float p_457335_) {
               Identifier identifier = this.sprites.get(this.isActive(), this.isHoveredOrFocused());
               p_457750_.blitSprite(RenderPipelines.GUI_TEXTURED, identifier, this.getX(), this.getY(), this.width, this.height);
               p_457750_.blitSprite(RenderPipelines.GUI_TEXTURED, this.sprite, this.getX(), this.getY(), this.width, this.height);
            }
         }
      }

      @OnlyIn(Dist.CLIENT)
      class ItemRow extends StatsScreen.ItemStatisticsList.Entry {
         private final Item item;
         private final StatsScreen.ItemStatisticsList.ItemRow.ItemRowWidget itemRowWidget;

         ItemRow(final Item p_169517_) {
            this.item = p_169517_;
            this.itemRowWidget = new StatsScreen.ItemStatisticsList.ItemRow.ItemRowWidget(p_169517_.getDefaultInstance());
         }

         protected Item getItem() {
            return this.item;
         }

         @Override
         public void renderContent(GuiGraphics p_427201_, int p_428041_, int p_427141_, boolean p_430845_, float p_430595_) {
            this.itemRowWidget.setPosition(this.getContentX(), this.getContentY());
            this.itemRowWidget.render(p_427201_, p_428041_, p_427141_, p_430595_);
            StatsScreen.ItemStatisticsList statsscreen$itemstatisticslist = ItemStatisticsList.this;
            int i = statsscreen$itemstatisticslist.children().indexOf(this);

            for (int j = 0; j < statsscreen$itemstatisticslist.blockColumns.size(); j++) {
               Stat<Block> stat;
               if (this.item instanceof BlockItem blockitem) {
                  stat = statsscreen$itemstatisticslist.blockColumns.get(j).get(blockitem.getBlock());
               } else {
                  stat = null;
               }

               this.renderStat(p_427201_, stat, this.getContentX() + ItemStatisticsList.this.getColumnX(j), this.getContentYMiddle() - 4, i % 2 == 0);
            }

            for (int k = 0; k < statsscreen$itemstatisticslist.itemColumns.size(); k++) {
               this.renderStat(
                  p_427201_,
                  statsscreen$itemstatisticslist.itemColumns.get(k).get(this.item),
                  this.getContentX() + ItemStatisticsList.this.getColumnX(k + statsscreen$itemstatisticslist.blockColumns.size()),
                  this.getContentYMiddle() - 4,
                  i % 2 == 0
               );
            }
         }

         protected void renderStat(GuiGraphics p_282544_, @Nullable Stat<?> p_97093_, int p_97094_, int p_97095_, boolean p_97096_) {
            Component component = p_97093_ == null
               ? StatsScreen.NO_VALUE_DISPLAY
               : Component.literal(p_97093_.format(StatsScreen.this.stats.getValue(p_97093_)));
            p_282544_.drawString(StatsScreen.this.font, component, p_97094_ - StatsScreen.this.font.width(component), p_97095_, p_97096_ ? -1 : -4539718);
         }

         @Override
         public List<? extends NarratableEntry> narratables() {
            return List.of(this.itemRowWidget);
         }

         @Override
         public List<? extends GuiEventListener> children() {
            return List.of(this.itemRowWidget);
         }

         @OnlyIn(Dist.CLIENT)
         class ItemRowWidget extends ItemDisplayWidget {
            ItemRowWidget(final ItemStack p_430329_) {
               super(ItemStatisticsList.this.minecraft, 1, 1, 18, 18, p_430329_.getHoverName(), p_430329_, false, true);
            }

            @Override
            protected void renderWidget(GuiGraphics p_425656_, int p_425685_, int p_429154_, float p_431497_) {
               p_425656_.blitSprite(RenderPipelines.GUI_TEXTURED, StatsScreen.SLOT_SPRITE, ItemRow.this.getContentX(), ItemRow.this.getContentY(), 18, 18);
               super.renderWidget(p_425656_, p_425685_, p_429154_, p_431497_);
            }

            @Override
            protected void renderTooltip(GuiGraphics p_428931_, int p_429864_, int p_431067_) {
               super.renderTooltip(p_428931_, ItemRow.this.getContentX() + 18, ItemRow.this.getContentY() + 18);
            }
         }
      }

      @OnlyIn(Dist.CLIENT)
      class ItemRowComparator implements Comparator<StatsScreen.ItemStatisticsList.ItemRow> {
         public int compare(StatsScreen.ItemStatisticsList.ItemRow p_169524_, StatsScreen.ItemStatisticsList.ItemRow p_169525_) {
            Item item = p_169524_.getItem();
            Item item1 = p_169525_.getItem();
            int i;
            int j;
            if (ItemStatisticsList.this.sortColumn == null) {
               i = 0;
               j = 0;
            } else if (ItemStatisticsList.this.blockColumns.contains(ItemStatisticsList.this.sortColumn)) {
               StatType<Block> stattype = (StatType<Block>)ItemStatisticsList.this.sortColumn;
               i = item instanceof BlockItem ? StatsScreen.this.stats.getValue(stattype, ((BlockItem)item).getBlock()) : -1;
               j = item1 instanceof BlockItem ? StatsScreen.this.stats.getValue(stattype, ((BlockItem)item1).getBlock()) : -1;
            } else {
               StatType<Item> stattype1 = (StatType<Item>)ItemStatisticsList.this.sortColumn;
               i = StatsScreen.this.stats.getValue(stattype1, item);
               j = StatsScreen.this.stats.getValue(stattype1, item1);
            }

            return i == j
               ? ItemStatisticsList.this.sortOrder * Integer.compare(Item.getId(item), Item.getId(item1))
               : ItemStatisticsList.this.sortOrder * Integer.compare(i, j);
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   class MobsStatisticsList extends ObjectSelectionList<StatsScreen.MobsStatisticsList.MobRow> {
      public MobsStatisticsList(final Minecraft p_97100_) {
         super(p_97100_, StatsScreen.this.width, StatsScreen.this.layout.getContentHeight(), 33, 36);

         for (EntityType<?> entitytype : BuiltInRegistries.ENTITY_TYPE) {
            if (StatsScreen.this.stats.getValue(Stats.ENTITY_KILLED.get(entitytype)) > 0
               || StatsScreen.this.stats.getValue(Stats.ENTITY_KILLED_BY.get(entitytype)) > 0) {
               this.addEntry(new StatsScreen.MobsStatisticsList.MobRow(entitytype));
            }
         }
      }

      @Override
      public int getRowWidth() {
         return 280;
      }

      @Override
      protected void renderListBackground(GuiGraphics p_422959_) {
      }

      @Override
      protected void renderListSeparators(GuiGraphics p_427958_) {
      }

      @OnlyIn(Dist.CLIENT)
      class MobRow extends ObjectSelectionList.Entry<StatsScreen.MobsStatisticsList.MobRow> {
         private final Component mobName;
         private final Component kills;
         private final Component killedBy;
         private final boolean hasKills;
         private final boolean wasKilledBy;

         public MobRow(final EntityType<?> p_97112_) {
            this.mobName = p_97112_.getDescription();
            int i = StatsScreen.this.stats.getValue(Stats.ENTITY_KILLED.get(p_97112_));
            if (i == 0) {
               this.kills = Component.translatable("stat_type.minecraft.killed.none", this.mobName);
               this.hasKills = false;
            } else {
               this.kills = Component.translatable("stat_type.minecraft.killed", i, this.mobName);
               this.hasKills = true;
            }

            int j = StatsScreen.this.stats.getValue(Stats.ENTITY_KILLED_BY.get(p_97112_));
            if (j == 0) {
               this.killedBy = Component.translatable("stat_type.minecraft.killed_by.none", this.mobName);
               this.wasKilledBy = false;
            } else {
               this.killedBy = Component.translatable("stat_type.minecraft.killed_by", this.mobName, j);
               this.wasKilledBy = true;
            }
         }

         @Override
         public void renderContent(GuiGraphics p_283265_, int p_97115_, int p_97116_, boolean p_97122_, float p_97123_) {
            p_283265_.drawString(StatsScreen.this.font, this.mobName, this.getContentX() + 2, this.getContentY() + 1, -1);
            p_283265_.drawString(
               StatsScreen.this.font, this.kills, this.getContentX() + 2 + 10, this.getContentY() + 1 + 9, this.hasKills ? -4539718 : -8355712
            );
            p_283265_.drawString(
               StatsScreen.this.font, this.killedBy, this.getContentX() + 2 + 10, this.getContentY() + 1 + 18, this.wasKilledBy ? -4539718 : -8355712
            );
         }

         @Override
         public Component getNarration() {
            return Component.translatable("narrator.select", CommonComponents.joinForNarration(this.kills, this.killedBy));
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   class StatisticsTab extends GridLayoutTab {
      protected final AbstractSelectionList<?> list;

      public StatisticsTab(final Component p_428143_, final AbstractSelectionList<?> p_431543_) {
         super(p_428143_);
         this.layout.addChild(p_431543_, 1, 1);
         this.list = p_431543_;
      }

      @Override
      public void doLayout(ScreenRectangle p_425944_) {
         this.list.updateSizeAndPosition(StatsScreen.this.width, StatsScreen.this.layout.getContentHeight(), StatsScreen.this.layout.getHeaderHeight());
         super.doLayout(p_425944_);
      }
   }
}
