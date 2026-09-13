package net.minecraft.client.gui.screens.inventory;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundRenameItemPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class AnvilScreen extends ItemCombinerScreen<AnvilMenu> {
   private static final Identifier TEXT_FIELD_SPRITE = Identifier.withDefaultNamespace("container/anvil/text_field");
   private static final Identifier TEXT_FIELD_DISABLED_SPRITE = Identifier.withDefaultNamespace("container/anvil/text_field_disabled");
   private static final Identifier ERROR_SPRITE = Identifier.withDefaultNamespace("container/anvil/error");
   private static final Identifier ANVIL_LOCATION = Identifier.withDefaultNamespace("textures/gui/container/anvil.png");
   private static final Component TOO_EXPENSIVE_TEXT = Component.translatable("container.repair.expensive");
   private EditBox name;
   private final Player player;

   public AnvilScreen(AnvilMenu p_97874_, Inventory p_97875_, Component p_97876_) {
      super(p_97874_, p_97875_, p_97876_, ANVIL_LOCATION);
      this.player = p_97875_.player;
      this.titleLabelX = 60;
   }

   @Override
   protected void subInit() {
      int i = (this.width - this.imageWidth) / 2;
      int j = (this.height - this.imageHeight) / 2;
      this.name = new EditBox(this.font, i + 62, j + 24, 103, 12, Component.translatable("container.repair"));
      this.name.setCanLoseFocus(false);
      this.name.setTextColor(-1);
      this.name.setTextColorUneditable(-1);
      this.name.setInvertHighlightedTextColor(false);
      this.name.setBordered(false);
      this.name.setMaxLength(50);
      this.name.setResponder(this::onNameChanged);
      this.name.setValue("");
      this.addRenderableWidget(this.name);
      this.name.setEditable(this.menu.getSlot(0).hasItem());
   }

   @Override
   protected void containerTick() {
      super.containerTick();
      this.minecraft.player.experienceDisplayStartTick = this.minecraft.player.tickCount;
   }

   @Override
   protected void setInitialFocus() {
      this.setInitialFocus(this.name);
   }

   @Override
   public void resize(int p_97887_, int p_97888_) {
      String s = this.name.getValue();
      this.init(p_97887_, p_97888_);
      this.name.setValue(s);
   }

   @Override
   public boolean keyPressed(KeyEvent p_425627_) {
      if (p_425627_.isEscape()) {
         this.minecraft.player.closeContainer();
         return true;
      } else {
         return !this.name.keyPressed(p_425627_) && !this.name.canConsumeInput() ? super.keyPressed(p_425627_) : true;
      }
   }

   private void onNameChanged(String p_97899_) {
      Slot slot = this.menu.getSlot(0);
      if (slot.hasItem()) {
         String s = p_97899_;
         if (!slot.getItem().has(DataComponents.CUSTOM_NAME) && p_97899_.equals(slot.getItem().getHoverName().getString())) {
            s = "";
         }

         if (this.menu.setItemName(s)) {
            this.minecraft.player.connection.send(new ServerboundRenameItemPacket(s));
         }
      }
   }

   @Override
   protected void renderLabels(GuiGraphics p_281442_, int p_282417_, int p_283022_) {
      super.renderLabels(p_281442_, p_282417_, p_283022_);
      int i = this.menu.getCost();
      if (i > 0) {
         int j = -8323296;
         Component component;
         if (i >= 40 && !this.minecraft.player.hasInfiniteMaterials()) {
            component = TOO_EXPENSIVE_TEXT;
            j = -40864;
         } else if (!this.menu.getSlot(2).hasItem()) {
            component = null;
         } else {
            component = Component.translatable("container.repair.cost", i);
            if (!this.menu.getSlot(2).mayPickup(this.player)) {
               j = -40864;
            }
         }

         if (component != null) {
            int k = this.imageWidth - 8 - this.font.width(component) - 2;
            int l = 69;
            p_281442_.fill(k - 2, 67, this.imageWidth - 8, 79, 1325400064);
            p_281442_.drawString(this.font, component, k, 69, j);
         }
      }
   }

   @Override
   protected void renderBg(GuiGraphics p_283345_, float p_283412_, int p_282871_, int p_281306_) {
      super.renderBg(p_283345_, p_283412_, p_282871_, p_281306_);
      p_283345_.blitSprite(
         RenderPipelines.GUI_TEXTURED,
         this.menu.getSlot(0).hasItem() ? TEXT_FIELD_SPRITE : TEXT_FIELD_DISABLED_SPRITE,
         this.leftPos + 59,
         this.topPos + 20,
         110,
         16
      );
   }

   @Override
   protected void renderErrorIcon(GuiGraphics p_282905_, int p_283237_, int p_282237_) {
      if ((this.menu.getSlot(0).hasItem() || this.menu.getSlot(1).hasItem()) && !this.menu.getSlot(this.menu.getResultSlot()).hasItem()) {
         p_282905_.blitSprite(RenderPipelines.GUI_TEXTURED, ERROR_SPRITE, p_283237_ + 99, p_282237_ + 45, 28, 21);
      }
   }

   @Override
   public void slotChanged(AbstractContainerMenu p_97882_, int p_97883_, ItemStack p_97884_) {
      if (p_97883_ == 0) {
         this.name.setValue(p_97884_.isEmpty() ? "" : p_97884_.getHoverName().getString());
         this.name.setEditable(!p_97884_.isEmpty());
         this.setFocused(this.name);
      }
   }
}
