package net.minecraft.client.gui.screens.inventory;

import net.minecraft.client.gui.GuiGraphics;
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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class CartographyTableScreen extends AbstractContainerScreen<CartographyTableMenu> {
   private static final Identifier ERROR_SPRITE = Identifier.withDefaultNamespace("container/cartography_table/error");
   private static final Identifier SCALED_MAP_SPRITE = Identifier.withDefaultNamespace("container/cartography_table/scaled_map");
   private static final Identifier DUPLICATED_MAP_SPRITE = Identifier.withDefaultNamespace("container/cartography_table/duplicated_map");
   private static final Identifier MAP_SPRITE = Identifier.withDefaultNamespace("container/cartography_table/map");
   private static final Identifier LOCKED_SPRITE = Identifier.withDefaultNamespace("container/cartography_table/locked");
   private static final Identifier BG_LOCATION = Identifier.withDefaultNamespace("textures/gui/container/cartography_table.png");
   private final MapRenderState mapRenderState = new MapRenderState();

   public CartographyTableScreen(CartographyTableMenu p_98349_, Inventory p_98350_, Component p_98351_) {
      super(p_98349_, p_98350_, p_98351_);
      this.titleLabelY -= 2;
   }

   @Override
   public void render(GuiGraphics p_281331_, int p_281706_, int p_282996_, float p_283037_) {
      super.render(p_281331_, p_281706_, p_282996_, p_283037_);
      this.renderTooltip(p_281331_, p_281706_, p_282996_);
   }

   @Override
   protected void renderBg(GuiGraphics p_282101_, float p_282697_, int p_282380_, int p_282327_) {
      int i = this.leftPos;
      int j = this.topPos;
      p_282101_.blit(RenderPipelines.GUI_TEXTURED, BG_LOCATION, i, j, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);
      ItemStack itemstack = this.menu.getSlot(1).getItem();
      boolean flag = itemstack.is(Items.MAP);
      boolean flag1 = itemstack.is(Items.PAPER);
      boolean flag2 = itemstack.is(Items.GLASS_PANE);
      ItemStack itemstack1 = this.menu.getSlot(0).getItem();
      MapId mapid = itemstack1.get(DataComponents.MAP_ID);
      boolean flag3 = false;
      MapItemSavedData mapitemsaveddata;
      if (mapid != null) {
         mapitemsaveddata = MapItem.getSavedData(mapid, this.minecraft.level);
         if (mapitemsaveddata != null) {
            if (mapitemsaveddata.locked) {
               flag3 = true;
               if (flag1 || flag2) {
                  p_282101_.blitSprite(RenderPipelines.GUI_TEXTURED, ERROR_SPRITE, i + 35, j + 31, 28, 21);
               }
            }

            if (flag1 && mapitemsaveddata.scale >= 4) {
               flag3 = true;
               p_282101_.blitSprite(RenderPipelines.GUI_TEXTURED, ERROR_SPRITE, i + 35, j + 31, 28, 21);
            }
         }
      } else {
         mapitemsaveddata = null;
      }

      this.renderResultingMap(p_282101_, mapid, mapitemsaveddata, flag, flag1, flag2, flag3);
   }

   private void renderResultingMap(
      GuiGraphics p_282167_,
      @Nullable MapId p_335682_,
      @Nullable MapItemSavedData p_282045_,
      boolean p_282086_,
      boolean p_283531_,
      boolean p_282645_,
      boolean p_281646_
   ) {
      int i = this.leftPos;
      int j = this.topPos;
      if (p_283531_ && !p_281646_) {
         p_282167_.blitSprite(RenderPipelines.GUI_TEXTURED, SCALED_MAP_SPRITE, i + 67, j + 13, 66, 66);
         this.renderMap(p_282167_, p_335682_, p_282045_, i + 85, j + 31, 0.226F);
      } else if (p_282086_) {
         p_282167_.blitSprite(RenderPipelines.GUI_TEXTURED, DUPLICATED_MAP_SPRITE, i + 67 + 16, j + 13, 50, 66);
         this.renderMap(p_282167_, p_335682_, p_282045_, i + 86, j + 16, 0.34F);
         p_282167_.nextStratum();
         p_282167_.blitSprite(RenderPipelines.GUI_TEXTURED, DUPLICATED_MAP_SPRITE, i + 67, j + 13 + 16, 50, 66);
         this.renderMap(p_282167_, p_335682_, p_282045_, i + 70, j + 32, 0.34F);
      } else if (p_282645_) {
         p_282167_.blitSprite(RenderPipelines.GUI_TEXTURED, MAP_SPRITE, i + 67, j + 13, 66, 66);
         this.renderMap(p_282167_, p_335682_, p_282045_, i + 71, j + 17, 0.45F);
         p_282167_.blitSprite(RenderPipelines.GUI_TEXTURED, LOCKED_SPRITE, i + 118, j + 60, 10, 14);
      } else {
         p_282167_.blitSprite(RenderPipelines.GUI_TEXTURED, MAP_SPRITE, i + 67, j + 13, 66, 66);
         this.renderMap(p_282167_, p_335682_, p_282045_, i + 71, j + 17, 0.45F);
      }
   }

   private void renderMap(GuiGraphics p_282298_, @Nullable MapId p_334395_, @Nullable MapItemSavedData p_282897_, int p_281632_, int p_282115_, float p_283388_) {
      if (p_334395_ != null && p_282897_ != null) {
         p_282298_.pose().pushMatrix();
         p_282298_.pose().translate(p_281632_, p_282115_);
         p_282298_.pose().scale(p_283388_, p_283388_);
         this.minecraft.getMapRenderer().extractRenderState(p_334395_, p_282897_, this.mapRenderState);
         p_282298_.submitMapRenderState(this.mapRenderState);
         p_282298_.pose().popMatrix();
      }
   }
}
