package net.minecraft.client.gui.screens;

import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
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
import org.jspecify.annotations.Nullable;

public class CreateFlatWorldScreen extends Screen {
   private static final Component TITLE = Component.translatable("createWorld.customize.flat.title");
   private static final Identifier SLOT_SPRITE = Identifier.withDefaultNamespace("container/slot");
   private static final int SLOT_BG_SIZE = 18;
   private static final int SLOT_STAT_HEIGHT = 20;
   private static final int SLOT_BG_X = 1;
   private static final int SLOT_BG_Y = 1;
   private static final int SLOT_FG_X = 2;
   private static final int SLOT_FG_Y = 2;
   private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this, 33, 64);
   protected final CreateWorldScreen parent;
   private final Consumer<FlatLevelGeneratorSettings> applySettings;
   private FlatLevelGeneratorSettings generator;
   private CreateFlatWorldScreen.@Nullable DetailsList list;
   private @Nullable Button deleteLayerButton;

   public CreateFlatWorldScreen(
      final CreateWorldScreen parent, final Consumer<FlatLevelGeneratorSettings> applySettings, final FlatLevelGeneratorSettings generator
   ) {
      super(TITLE);
      this.parent = parent;
      this.applySettings = applySettings;
      this.generator = generator;
   }

   public FlatLevelGeneratorSettings settings() {
      return this.generator;
   }

   public void setConfig(final FlatLevelGeneratorSettings generator) {
      this.generator = generator;
      if (this.list != null) {
         this.list.resetRows();
         this.updateButtonValidity();
      }
   }

   @Override
   protected void init() {
      this.layout.addTitleHeader(this.title, this.font);
      this.list = this.layout.addToContents(new CreateFlatWorldScreen.DetailsList());
      LinearLayout footer = this.layout.addToFooter(LinearLayout.vertical().spacing(4));
      footer.defaultCellSetting().alignVerticallyMiddle();
      LinearLayout topFooterButtons = footer.addChild(LinearLayout.horizontal().spacing(8));
      LinearLayout bottomFooterButtons = footer.addChild(LinearLayout.horizontal().spacing(8));
      this.deleteLayerButton = topFooterButtons.addChild(Button.builder(Component.translatable("createWorld.customize.flat.removeLayer"), button -> {
         if (this.list != null && this.list.getSelected() instanceof CreateFlatWorldScreen.DetailsList.LayerEntry selectedLayerEntry) {
            this.list.deleteLayer(selectedLayerEntry);
         }
      }).build());
      topFooterButtons.addChild(Button.builder(Component.translatable("createWorld.customize.presets"), button -> {
         this.minecraft.gui.setScreen(new PresetFlatWorldScreen(this));
         this.generator.updateLayers();
         this.updateButtonValidity();
      }).build());
      bottomFooterButtons.addChild(Button.builder(CommonComponents.GUI_DONE, button -> {
         this.applySettings.accept(this.generator);
         this.onClose();
         this.generator.updateLayers();
      }).build());
      bottomFooterButtons.addChild(Button.builder(CommonComponents.GUI_CANCEL, button -> {
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

   private void updateButtonValidity() {
      if (this.deleteLayerButton != null) {
         this.deleteLayerButton.active = this.hasValidSelection();
      }
   }

   private boolean hasValidSelection() {
      return this.list != null && this.list.getSelected() instanceof CreateFlatWorldScreen.DetailsList.LayerEntry;
   }

   @Override
   public void onClose() {
      this.minecraft.gui.setScreen(this.parent);
   }

   private class DetailsList extends ObjectSelectionList<CreateFlatWorldScreen.DetailsList.Entry> {
      private static final Component LAYER_MATERIAL_TITLE = Component.translatable("createWorld.customize.flat.tile").withStyle(ChatFormatting.UNDERLINE);
      private static final Component HEIGHT_TITLE = Component.translatable("createWorld.customize.flat.height").withStyle(ChatFormatting.UNDERLINE);

      public DetailsList() {
         super(CreateFlatWorldScreen.this.minecraft, CreateFlatWorldScreen.this.width, CreateFlatWorldScreen.this.height - 103, 43, 24);
         this.populateList();
      }

      private void populateList() {
         this.addEntry(new CreateFlatWorldScreen.DetailsList.HeaderEntry(CreateFlatWorldScreen.this.font), (int)(9.0 * 1.5));
         List<FlatLayerInfo> layersInfo = CreateFlatWorldScreen.this.generator.getLayersInfo().reversed();

         for (int i = 0; i < layersInfo.size(); i++) {
            this.addEntry(new CreateFlatWorldScreen.DetailsList.LayerEntry(layersInfo.get(i), i));
         }
      }

      public void setSelected(final CreateFlatWorldScreen.DetailsList.@Nullable Entry selected) {
         super.setSelected(selected);
         CreateFlatWorldScreen.this.updateButtonValidity();
      }

      public void resetRows() {
         int index = this.children().indexOf(this.getSelected());
         this.clearEntries();
         this.populateList();
         List<CreateFlatWorldScreen.DetailsList.Entry> children = this.children();
         if (index >= 0 && index < children.size()) {
            this.setSelected(children.get(index));
         }
      }

      private void deleteLayer(final CreateFlatWorldScreen.DetailsList.LayerEntry selectedLayerEntry) {
         List<FlatLayerInfo> layersInfo = CreateFlatWorldScreen.this.generator.getLayersInfo();
         int deletedLayerIndex = this.children().indexOf(selectedLayerEntry);
         this.removeEntry(selectedLayerEntry);
         layersInfo.remove(selectedLayerEntry.layerInfo);
         this.setSelected(layersInfo.isEmpty() ? null : this.children().get(Math.min(deletedLayerIndex, layersInfo.size())));
         CreateFlatWorldScreen.this.generator.updateLayers();
         this.resetRows();
         CreateFlatWorldScreen.this.updateButtonValidity();
      }

      private abstract static class Entry extends ObjectSelectionList.Entry<CreateFlatWorldScreen.DetailsList.Entry> {
      }

      private static class HeaderEntry extends CreateFlatWorldScreen.DetailsList.Entry {
         private final Font font;

         public HeaderEntry(final Font font) {
            this.font = font;
         }

         @Override
         public void extractContent(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final boolean hovered, final float a) {
            graphics.text(this.font, CreateFlatWorldScreen.DetailsList.LAYER_MATERIAL_TITLE, this.getContentX(), this.getContentY(), -1);
            graphics.text(
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

      private class LayerEntry extends CreateFlatWorldScreen.DetailsList.Entry {
         private final FlatLayerInfo layerInfo;
         private final int index;

         public LayerEntry(final FlatLayerInfo layerInfo, final int index) {
            this.layerInfo = layerInfo;
            this.index = index;
         }

         @Override
         public void extractContent(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final boolean hovered, final float a) {
            BlockState blockState = this.layerInfo.getBlockState();
            ItemStack itemStack = this.getDisplayItem(blockState);
            this.blitSlot(graphics, this.getContentX(), this.getContentY(), itemStack);
            int y = this.getContentYMiddle() - 9 / 2;
            graphics.text(CreateFlatWorldScreen.this.font, itemStack.getHoverName(), this.getContentX() + 18 + 5, y, -1);
            Component height;
            if (this.index == 0) {
               height = Component.translatable("createWorld.customize.flat.layer.top", this.layerInfo.getHeight());
            } else if (this.index == CreateFlatWorldScreen.this.generator.getLayersInfo().size() - 1) {
               height = Component.translatable("createWorld.customize.flat.layer.bottom", this.layerInfo.getHeight());
            } else {
               height = Component.translatable("createWorld.customize.flat.layer", this.layerInfo.getHeight());
            }

            graphics.text(CreateFlatWorldScreen.this.font, height, this.getContentRight() - CreateFlatWorldScreen.this.font.width(height), y, -1);
         }

         private ItemStack getDisplayItem(final BlockState blockState) {
            Item item = blockState.getBlock().asItem();
            if (item == Items.AIR) {
               if (blockState.is(Blocks.WATER)) {
                  item = Items.WATER_BUCKET;
               } else if (blockState.is(Blocks.LAVA)) {
                  item = Items.LAVA_BUCKET;
               }
            }

            return new ItemStack(item);
         }

         @Override
         public Component getNarration() {
            ItemStack itemStack = this.getDisplayItem(this.layerInfo.getBlockState());
            return !itemStack.isEmpty()
               ? CommonComponents.joinForNarration(
                  Component.translatable("narrator.select", itemStack.getHoverName()),
                  CreateFlatWorldScreen.DetailsList.HEIGHT_TITLE,
                  Component.literal(String.valueOf(this.layerInfo.getHeight()))
               )
               : CommonComponents.EMPTY;
         }

         @Override
         public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
            DetailsList.this.setSelected(this);
            return super.mouseClicked(event, doubleClick);
         }

         private void blitSlot(final GuiGraphicsExtractor graphics, final int x, final int y, final ItemStack itemStack) {
            this.blitSlotBg(graphics, x + 1, y + 1);
            if (!itemStack.isEmpty()) {
               graphics.fakeItem(itemStack, x + 2, y + 2);
            }
         }

         private void blitSlotBg(final GuiGraphicsExtractor graphics, final int x, final int y) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, CreateFlatWorldScreen.SLOT_SPRITE, x, y, 18, 18);
         }
      }
   }
}
