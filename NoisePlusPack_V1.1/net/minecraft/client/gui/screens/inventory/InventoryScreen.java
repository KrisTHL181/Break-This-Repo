package net.minecraft.client.gui.screens.inventory;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenPosition;
import net.minecraft.client.gui.screens.recipebook.CraftingRecipeBookComponent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Quaternionf;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public class InventoryScreen extends AbstractRecipeBookScreen<InventoryMenu> {
   private float xMouse;
   private float yMouse;
   private boolean buttonClicked;
   private final EffectsInInventory effects;

   public InventoryScreen(Player p_98839_) {
      super(
         p_98839_.inventoryMenu, new CraftingRecipeBookComponent(p_98839_.inventoryMenu), p_98839_.getInventory(), Component.translatable("container.crafting")
      );
      this.titleLabelX = 97;
      this.effects = new EffectsInInventory(this);
   }

   @Override
   public void containerTick() {
      super.containerTick();
      if (this.minecraft.player.hasInfiniteMaterials()) {
         this.minecraft
            .setScreen(
               new CreativeModeInventoryScreen(
                  this.minecraft.player, this.minecraft.player.connection.enabledFeatures(), this.minecraft.options.operatorItemsTab().get()
               )
            );
      }
   }

   @Override
   protected void init() {
      if (this.minecraft.player.hasInfiniteMaterials()) {
         this.minecraft
            .setScreen(
               new CreativeModeInventoryScreen(
                  this.minecraft.player, this.minecraft.player.connection.enabledFeatures(), this.minecraft.options.operatorItemsTab().get()
               )
            );
      } else {
         super.init();
      }
   }

   @Override
   protected ScreenPosition getRecipeBookButtonPosition() {
      return new ScreenPosition(this.leftPos + 104, this.height / 2 - 22);
   }

   @Override
   protected void onRecipeBookButtonClick() {
      this.buttonClicked = true;
   }

   @Override
   protected void renderLabels(GuiGraphics p_281654_, int p_283517_, int p_283464_) {
      p_281654_.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, -12566464, false);
   }

   @Override
   public void render(GuiGraphics p_283246_, int p_98876_, int p_98877_, float p_98878_) {
      this.effects.render(p_283246_, p_98876_, p_98877_);
      super.render(p_283246_, p_98876_, p_98877_, p_98878_);
      this.xMouse = p_98876_;
      this.yMouse = p_98877_;
   }

   @Override
   public boolean showsActiveEffects() {
      return this.effects.canSeeEffects();
   }

   @Override
   protected boolean isBiggerResultSlot() {
      return false;
   }

   @Override
   protected void renderBg(GuiGraphics p_281500_, float p_281299_, int p_283481_, int p_281831_) {
      int i = this.leftPos;
      int j = this.topPos;
      p_281500_.blit(RenderPipelines.GUI_TEXTURED, INVENTORY_LOCATION, i, j, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);
      renderEntityInInventoryFollowsMouse(p_281500_, i + 26, j + 8, i + 75, j + 78, 30, 0.0625F, this.xMouse, this.yMouse, this.minecraft.player);
   }

   public static void renderEntityInInventoryFollowsMouse(
      GuiGraphics p_282802_,
      int p_275688_,
      int p_275245_,
      int p_275535_,
      int p_301381_,
      int p_299741_,
      float p_275604_,
      float p_275546_,
      float p_300682_,
      LivingEntity p_275689_
   ) {
      float f = (p_275688_ + p_275535_) / 2.0F;
      float f1 = (p_275245_ + p_301381_) / 2.0F;
      float f2 = (float)Math.atan((f - p_275546_) / 40.0F);
      float f3 = (float)Math.atan((f1 - p_300682_) / 40.0F);
      Quaternionf quaternionf = new Quaternionf().rotateZ((float) Math.PI);
      Quaternionf quaternionf1 = new Quaternionf().rotateX(f3 * 20.0F * (float) (Math.PI / 180.0));
      quaternionf.mul(quaternionf1);
      EntityRenderState entityrenderstate = extractRenderState(p_275689_);
      if (entityrenderstate instanceof LivingEntityRenderState livingentityrenderstate) {
         livingentityrenderstate.bodyRot = 180.0F + f2 * 20.0F;
         livingentityrenderstate.yRot = f2 * 20.0F;
         if (livingentityrenderstate.pose != Pose.FALL_FLYING) {
            livingentityrenderstate.xRot = -f3 * 20.0F;
         } else {
            livingentityrenderstate.xRot = 0.0F;
         }

         livingentityrenderstate.boundingBoxWidth = livingentityrenderstate.boundingBoxWidth / livingentityrenderstate.scale;
         livingentityrenderstate.boundingBoxHeight = livingentityrenderstate.boundingBoxHeight / livingentityrenderstate.scale;
         livingentityrenderstate.scale = 1.0F;
      }

      Vector3f vector3f = new Vector3f(0.0F, entityrenderstate.boundingBoxHeight / 2.0F + p_275604_, 0.0F);
      p_282802_.submitEntityRenderState(entityrenderstate, p_299741_, vector3f, quaternionf, quaternionf1, p_275688_, p_275245_, p_275535_, p_301381_);
   }

   private static EntityRenderState extractRenderState(LivingEntity p_454677_) {
      EntityRenderDispatcher entityrenderdispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
      EntityRenderer<? super LivingEntity, ?> entityrenderer = entityrenderdispatcher.getRenderer(p_454677_);
      EntityRenderState entityrenderstate = entityrenderer.createRenderState(p_454677_, 1.0F);
      entityrenderstate.lightCoords = 15728880;
      entityrenderstate.shadowPieces.clear();
      entityrenderstate.outlineColor = 0;
      return entityrenderstate;
   }

   @Override
   public boolean mouseReleased(MouseButtonEvent p_427654_) {
      if (this.buttonClicked) {
         this.buttonClicked = false;
         return true;
      } else {
         return super.mouseReleased(p_427654_);
      }
   }
}
