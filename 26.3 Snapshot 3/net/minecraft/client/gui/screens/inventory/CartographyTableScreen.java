package net.minecraft.client.gui.screens.inventory;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.MapRenderState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.CartographyTableMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.jspecify.annotations.Nullable;

public class CartographyTableScreen extends AbstractContainerScreen<CartographyTableMenu> {
   private static final Identifier ERROR_SPRITE = Identifier.withDefaultNamespace("container/cartography_table/error");
   private static final Identifier SCALED_MAP_SPRITE = Identifier.withDefaultNamespace("container/cartography_table/scaled_map");
   private static final Identifier DUPLICATED_MAP_SPRITE = Identifier.withDefaultNamespace("container/cartography_table/duplicated_map");
   private static final Identifier MAP_SPRITE = Identifier.withDefaultNamespace("container/cartography_table/map");
   private static final Identifier LOCKED_SPRITE = Identifier.withDefaultNamespace("container/cartography_table/locked");
   private static final Identifier BG_LOCATION = Identifier.withDefaultNamespace("textures/gui/container/cartography_table.png");
   private final MapRenderState mapRenderState = new MapRenderState();

   public CartographyTableScreen(final CartographyTableMenu menu, final Inventory inventory, final Component title) {
      super(menu, inventory, title);
      this.titleLabelY -= 2;
   }

   @Override
   public void extractBackground(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
      super.extractBackground(graphics, mouseX, mouseY, a);
      int xo = this.leftPos;
      int yo = this.topPos;
      graphics.blit(RenderPipelines.GUI_TEXTURED, BG_LOCATION, xo, yo, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);
      ItemStack additionalItem = this.menu.getSlot(1).getItem();
      boolean isDuplication = additionalItem.is(Items.MAP);
      boolean isScaling = additionalItem.is(Items.PAPER);
      boolean isLocking = additionalItem.is(Items.GLASS_PANE);
      ItemStack map = this.menu.getSlot(0).getItem();
      MapId mapId = map.get(DataComponents.MAP_ID);
      boolean locked = false;
      MapItemSavedData mapData;
      if (mapId != null) {
         mapData = MapItem.getSavedData(mapId, this.minecraft.level);
         if (mapData != null) {
            if (mapData.locked) {
               locked = true;
               if (isScaling || isLocking) {
                  graphics.blitSprite(RenderPipelines.GUI_TEXTURED, ERROR_SPRITE, xo + 35, yo + 31, 28, 21);
               }
            }

            if (isScaling && mapData.scale >= 4) {
               locked = true;
               graphics.blitSprite(RenderPipelines.GUI_TEXTURED, ERROR_SPRITE, xo + 35, yo + 31, 28, 21);
            }
         }
      } else {
         mapData = null;
      }

      this.extractResultingMap(graphics, mapId, mapData, isDuplication, isScaling, isLocking, locked);
   }

   private void extractResultingMap(
      final GuiGraphicsExtractor graphics,
      final @Nullable MapId id,
      final @Nullable MapItemSavedData data,
      final boolean isDuplication,
      final boolean isScaling,
      final boolean isLocking,
      final boolean locked
   ) {
      int xo = this.leftPos;
      int yo = this.topPos;
      if (isScaling && !locked) {
         graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SCALED_MAP_SPRITE, xo + 67, yo + 13, 66, 66);
         this.extractMap(graphics, id, data, xo + 85, yo + 31, 0.226F);
      } else if (isDuplication) {
         graphics.blitSprite(RenderPipelines.GUI_TEXTURED, DUPLICATED_MAP_SPRITE, xo + 67 + 16, yo + 13, 50, 66);
         this.extractMap(graphics, id, data, xo + 86, yo + 16, 0.34F);
         graphics.nextStratum();
         graphics.blitSprite(RenderPipelines.GUI_TEXTURED, DUPLICATED_MAP_SPRITE, xo + 67, yo + 13 + 16, 50, 66);
         this.extractMap(graphics, id, data, xo + 70, yo + 32, 0.34F);
      } else if (isLocking) {
         graphics.blitSprite(RenderPipelines.GUI_TEXTURED, MAP_SPRITE, xo + 67, yo + 13, 66, 66);
         this.extractMap(graphics, id, data, xo + 71, yo + 17, 0.45F);
         graphics.blitSprite(RenderPipelines.GUI_TEXTURED, LOCKED_SPRITE, xo + 118, yo + 60, 10, 14);
      } else {
         graphics.blitSprite(RenderPipelines.GUI_TEXTURED, MAP_SPRITE, xo + 67, yo + 13, 66, 66);
         this.extractMap(graphics, id, data, xo + 71, yo + 17, 0.45F);
      }
   }

   private void extractMap(
      final GuiGraphicsExtractor graphics, final @Nullable MapId id, final @Nullable MapItemSavedData data, final int x, final int y, final float scale
   ) {
      if (id != null && data != null) {
         graphics.pose().pushMatrix();
         graphics.pose().translate(x, y);
         graphics.pose().scale(scale, scale);
         this.minecraft.getMapRenderer().extractRenderState(id, data, this.mapRenderState);
         graphics.map(this.mapRenderState);
         graphics.pose().popMatrix();
      }
   }
}
