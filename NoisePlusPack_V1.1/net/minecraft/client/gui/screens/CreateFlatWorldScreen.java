package net.minecraft.client.gui.screens;

import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.flat.FlatLayerInfo;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class CreateFlatWorldScreen extends Screen {
   private static final Component TITLE = Component.translatable("createWorld.customize.flat.title");
   static final Identifier SLOT_SPRITE = Identifier.withDefaultNamespace("container/slot");
   private static final int SLOT_BG_SIZE = 18;
   private static final int SLOT_STAT_HEIGHT = 20;
   private static final int SLOT_BG_X = 1;
   private static final int SLOT_BG_Y = 1;
   private static final int SLOT_FG_X = 2;
   private static final int SLOT_FG_Y = 2;
   private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this, 33, 64);
   protected final CreateWorldScreen parent;
   private final Consumer<FlatLevelGeneratorSettings> applySettings;
   FlatLevelGeneratorSettings generator;
   private CreateFlatWorldScreen.@Nullable DetailsList list;
   private @Nullable Button deleteLayerButton;

   public CreateFlatWorldScreen(CreateWorldScreen p_95822_, Consumer<FlatLevelGeneratorSettings> p_95823_, FlatLevelGeneratorSettings p_95824_) {
      super(TITLE);
      this.parent = p_95822_;
      this.applySettings = p_95823_;
      this.generator = p_95824_;
   }

   public FlatLevelGeneratorSettings settings() {
      return this.generator;
   }

   public void setConfig(FlatLevelGeneratorSettings p_95826_) {
      this.generator = p_95826_;
      if (this.list != null) {
         this.list.resetRows();
         this.updateButtonValidity();
      }
   }

   @Override
   protected void init() {
      this.layout.addTitleHeader(this.title, this.font);
      this.list = this.layout.addToContents(new CreateFlatWorldScreen.DetailsList());
      LinearLayout linearlayout = this.layout.addToFooter(LinearLayout.vertical().spacing(4));
      linearlayout.defaultCellSetting().alignVerticallyMiddle();
      LinearLayout linearlayout1 = linearlayout.addChild(LinearLayout.horizontal().spacing(8));
      LinearLayout linearlayout2 = linearlayout.addChild(LinearLayout.horizontal().spacing(8));
      this.deleteLayerButton = linearlayout1.addChild(
         Button.builder(
               Component.translatable("createWorld.customize.flat.removeLayer"),
               p_420739_ -> {
                  if (this.list != null
                     && this.list.getSelected() instanceof CreateFlatWorldScreen.DetailsList.LayerEntry createflatworldscreen$detailslist$layerentry) {
                     this.list.deleteLayer(createflatworldscreen$detailslist$layerentry);
                  }
               }
            )
            .build()
      );
      linearlayout1.addChild(Button.builder(Component.translatable("createWorld.customize.presets"), p_280790_ -> {
         this.minecraft.setScreen(new PresetFlatWorldScreen(this));
         this.generator.updateLayers();
         this.updateButtonValidity();
      }).build());
      linearlayout2.addChild(Button.builder(CommonComponents.GUI_DONE, p_374574_ -> {
         this.applySettings.accept(this.generator);
         this.onClose();
         this.generator.updateLayers();
      }).build());
      linearlayout2.addChild(Button.builder(CommonComponents.GUI_CANCEL, p_374573_ -> {
         this.onClose();
         this.generator.updateLayers();
      }).build());
      this.generator.updateLayers();
      this.updateButtonValidity();
      this.layout.visitWidgets(this::addRenderableWidget);
      this.repositionElements();
   }

   @Override
   protected void repositionElements() {
      if (this.list != null) {
         this.list.updateSize(this.width, this.layout);
      }

      this.layout.arrangeElements();
   }

   void updateButtonValidity() {
      if (this.deleteLayerButton != null) {
         this.deleteLayerButton.active = this.hasValidSelection();
      }
   }

   private boolean hasValidSelection() {
      return this.list != null && this.list.getSelected() instanceof CreateFlatWorldScreen.DetailsList.LayerEntry;
   }

   @Override
   public void onClose() {
      this.minecraft.setScreen(this.parent);
   }

   @OnlyIn(Dist.CLIENT)
   class DetailsList extends ObjectSelectionList<CreateFlatWorldScreen.DetailsList.Entry> {
      static final Component LAYER_MATERIAL_TITLE = Component.translatable("createWorld.customize.flat.tile").withStyle(ChatFormatting.UNDERLINE);
      static final Component HEIGHT_TITLE = Component.translatable("createWorld.customize.flat.height").withStyle(ChatFormatting.UNDERLINE);

      public DetailsList() {
         super(CreateFlatWorldScreen.this.minecraft, CreateFlatWorldScreen.this.width, CreateFlatWorldScreen.this.height - 103, 43, 24);
         this.populateList();
      }

      private void populateList() {
         this.addEntry(new CreateFlatWorldScreen.DetailsList.HeaderEntry(CreateFlatWorldScreen.this.font), 13);
         List<FlatLayerInfo> list = CreateFlatWorldScreen.this.generator.getLayersInfo().reversed();

         for (int i = 0; i < list.size(); i++) {
            this.addEntry(new CreateFlatWorldScreen.DetailsList.LayerEntry(list.get(i), i));
         }
      }

      public void setSelected(CreateFlatWorldScreen.DetailsList.@Nullable Entry p_95855_) {
         super.setSelected(p_95855_);
         CreateFlatWorldScreen.this.updateButtonValidity();
      }

      public void resetRows() {
         int i = this.children().indexOf(this.getSelected());
         this.clearEntries();
         this.populateList();
         List<CreateFlatWorldScreen.DetailsList.Entry> list = this.children();
         if (i >= 0 && i < list.size()) {
            this.setSelected(list.get(i));
         }
      }

      void deleteLayer(CreateFlatWorldScreen.DetailsList.LayerEntry p_423537_) {
         List<FlatLayerInfo> list = CreateFlatWorldScreen.this.generator.getLayersInfo();
         int i = this.children().indexOf(p_423537_);
         this.removeEntry(p_423537_);
         list.remove(p_423537_.layerInfo);
         this.setSelected(list.isEmpty() ? null : this.children().get(Math.min(i, list.size())));
         CreateFlatWorldScreen.this.generator.updateLayers();
         this.resetRows();
         CreateFlatWorldScreen.this.updateButtonValidity();
      }

      @OnlyIn(Dist.CLIENT)
      abstract static class Entry extends ObjectSelectionList.Entry<CreateFlatWorldScreen.DetailsList.Entry> {
      }

      @OnlyIn(Dist.CLIENT)
      static class HeaderEntry extends CreateFlatWorldScreen.DetailsList.Entry {
         private final Font font;

         public HeaderEntry(Font p_423560_) {
            this.font = p_423560_;
         }

         @Override
         public void renderContent(GuiGraphics p_422710_, int p_427375_, int p_424594_, boolean p_431009_, float p_425838_) {
            p_422710_.drawString(this.font, CreateFlatWorldScreen.DetailsList.LAYER_MATERIAL_TITLE, this.getContentX(), this.getContentY(), -1);
            p_422710_.drawString(
               this.font,
               CreateFlatWorldScreen.DetailsList.HEIGHT_TITLE,
               this.getContentRight() - this.font.width(CreateFlatWorldScreen.DetailsList.HEIGHT_TITLE),
               this.getContentY(),
               -1
            );
         }

         @Override
         public Component getNarration() {
            return CommonComponents.joinForNarration(CreateFlatWorldScreen.DetailsList.LAYER_MATERIAL_TITLE, CreateFlatWorldScreen.DetailsList.HEIGHT_TITLE);
         }
      }

      @OnlyIn(Dist.CLIENT)
      class LayerEntry extends CreateFlatWorldScreen.DetailsList.Entry {
         final FlatLayerInfo layerInfo;
         private final int index;

         public LayerEntry(final FlatLayerInfo p_430645_, final int p_422736_) {
            this.layerInfo = p_430645_;
            this.index = p_422736_;
         }

         @Override
         public void renderContent(GuiGraphics p_423196_, int p_424830_, int p_430477_, boolean p_428252_, float p_430340_) {
            BlockState blockstate = this.layerInfo.getBlockState();
            ItemStack itemstack = this.getDisplayItem(blockstate);
            this.blitSlot(p_423196_, this.getContentX(), this.getContentY(), itemstack);
            int i = this.getContentYMiddle() - 4;
            p_423196_.drawString(CreateFlatWorldScreen.this.font, itemstack.getHoverName(), this.getContentX() + 18 + 5, i, -1);
            Component component;
            if (this.index == 0) {
               component = Component.translatable("createWorld.customize.flat.layer.top", this.layerInfo.getHeight());
            } else if (this.index == CreateFlatWorldScreen.this.generator.getLayersInfo().size() - 1) {
               component = Component.translatable("createWorld.customize.flat.layer.bottom", this.layerInfo.getHeight());
            } else {
               component = Component.translatable("createWorld.customize.flat.layer", this.layerInfo.getHeight());
            }

            p_423196_.drawString(CreateFlatWorldScreen.this.font, component, this.getContentRight() - CreateFlatWorldScreen.this.font.width(component), i, -1);
         }

         private ItemStack getDisplayItem(BlockState p_430359_) {
            Item item = p_430359_.getBlock().asItem();
            if (item == Items.AIR) {
               if (p_430359_.is(Blocks.WATER)) {
                  item = Items.WATER_BUCKET;
               } else if (p_430359_.is(Blocks.LAVA)) {
                  item = Items.LAVA_BUCKET;
               }
            }

            return new ItemStack(item);
         }

         @Override
         public Component getNarration() {
            ItemStack itemstack = this.getDisplayItem(this.layerInfo.getBlockState());
            return !itemstack.isEmpty()
               ? CommonComponents.joinForNarration(
                  Component.translatable("narrator.select", itemstack.getHoverName()),
                  CreateFlatWorldScreen.DetailsList.HEIGHT_TITLE,
                  Component.literal(String.valueOf(this.layerInfo.getHeight()))
               )
               : CommonComponents.EMPTY;
         }

         @Override
         public boolean mouseClicked(MouseButtonEvent p_423986_, boolean p_431640_) {
            DetailsList.this.setSelected(this);
            return super.mouseClicked(p_423986_, p_431640_);
         }

         private void blitSlot(GuiGraphics p_429433_, int p_428712_, int p_426633_, ItemStack p_426006_) {
            this.blitSlotBg(p_429433_, p_428712_ + 1, p_426633_ + 1);
            if (!p_426006_.isEmpty()) {
               p_429433_.renderFakeItem(p_426006_, p_428712_ + 2, p_426633_ + 2);
            }
         }

         private void blitSlotBg(GuiGraphics p_431206_, int p_430397_, int p_428448_) {
            p_431206_.blitSprite(RenderPipelines.GUI_TEXTURED, CreateFlatWorldScreen.SLOT_SPRITE, p_430397_, p_428448_, 18, 18);
         }
      }
   }
}
