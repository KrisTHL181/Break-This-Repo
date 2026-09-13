package net.minecraft.client.gui.screens.inventory;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.BrewingStandMenu;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BrewingStandScreen extends AbstractContainerScreen<BrewingStandMenu> {
   private static final Identifier FUEL_LENGTH_SPRITE = Identifier.withDefaultNamespace("container/brewing_stand/fuel_length");
   private static final Identifier BREW_PROGRESS_SPRITE = Identifier.withDefaultNamespace("container/brewing_stand/brew_progress");
   private static final Identifier BUBBLES_SPRITE = Identifier.withDefaultNamespace("container/brewing_stand/bubbles");
   private static final Identifier BREWING_STAND_LOCATION = Identifier.withDefaultNamespace("textures/gui/container/brewing_stand.png");
   private static final int[] BUBBLELENGTHS = new int[]{29, 24, 20, 16, 11, 6, 0};

   public BrewingStandScreen(BrewingStandMenu p_98332_, Inventory p_98333_, Component p_98334_) {
      super(p_98332_, p_98333_, p_98334_);
   }

   @Override
   protected void init() {
      super.init();
      this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
   }

   @Override
   public void render(GuiGraphics p_283297_, int p_283600_, int p_282033_, float p_283410_) {
      super.render(p_283297_, p_283600_, p_282033_, p_283410_);
      this.renderTooltip(p_283297_, p_283600_, p_282033_);
   }

   @Override
   protected void renderBg(GuiGraphics p_282963_, float p_282080_, int p_283365_, int p_283150_) {
      int i = (this.width - this.imageWidth) / 2;
      int j = (this.height - this.imageHeight) / 2;
      p_282963_.blit(RenderPipelines.GUI_TEXTURED, BREWING_STAND_LOCATION, i, j, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);
      int k = this.menu.getFuel();
      int l = Mth.clamp((18 * k + 20 - 1) / 20, 0, 18);
      if (l > 0) {
         p_282963_.blitSprite(RenderPipelines.GUI_TEXTURED, FUEL_LENGTH_SPRITE, 18, 4, 0, 0, i + 60, j + 44, l, 4);
      }

      int i1 = this.menu.getBrewingTicks();
      if (i1 > 0) {
         int j1 = (int)(28.0F * (1.0F - i1 / 400.0F));
         if (j1 > 0) {
            p_282963_.blitSprite(RenderPipelines.GUI_TEXTURED, BREW_PROGRESS_SPRITE, 9, 28, 0, 0, i + 97, j + 16, 9, j1);
         }

         j1 = BUBBLELENGTHS[i1 / 2 % 7];
         if (j1 > 0) {
            p_282963_.blitSprite(RenderPipelines.GUI_TEXTURED, BUBBLES_SPRITE, 12, 29, 0, 29 - j1, i + 63, j + 14 + 29 - j1, 12, j1);
         }
      }
   }
}
