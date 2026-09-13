package net.minecraft.client.gui.screens.inventory;

import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.BundleMouseActions;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.ItemSlotMouseAction;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector2i;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public abstract class AbstractContainerScreen<T extends AbstractContainerMenu> extends Screen implements MenuAccess<T> {
   public static final Identifier INVENTORY_LOCATION = Identifier.withDefaultNamespace("textures/gui/container/inventory.png");
   private static final Identifier SLOT_HIGHLIGHT_BACK_SPRITE = Identifier.withDefaultNamespace("container/slot_highlight_back");
   private static final Identifier SLOT_HIGHLIGHT_FRONT_SPRITE = Identifier.withDefaultNamespace("container/slot_highlight_front");
   protected static final int BACKGROUND_TEXTURE_WIDTH = 256;
   protected static final int BACKGROUND_TEXTURE_HEIGHT = 256;
   private static final float SNAPBACK_SPEED = 100.0F;
   private static final int QUICKDROP_DELAY = 500;
   protected int imageWidth = 176;
   protected int imageHeight = 166;
   protected int titleLabelX;
   protected int titleLabelY;
   protected int inventoryLabelX;
   protected int inventoryLabelY;
   private final List<ItemSlotMouseAction> itemSlotMouseActions;
   protected final T menu;
   protected final Component playerInventoryTitle;
   protected @Nullable Slot hoveredSlot;
   private @Nullable Slot clickedSlot;
   private @Nullable Slot quickdropSlot;
   private @Nullable Slot lastClickSlot;
   private AbstractContainerScreen.@Nullable SnapbackData snapbackData;
   protected int leftPos;
   protected int topPos;
   private boolean isSplittingStack;
   private ItemStack draggingItem = ItemStack.EMPTY;
   private long quickdropTime;
   protected final Set<Slot> quickCraftSlots = Sets.newHashSet();
   protected boolean isQuickCrafting;
   private int quickCraftingType;
   @MouseButtonInfo.MouseButton
   private int quickCraftingButton;
   private boolean skipNextRelease;
   private int quickCraftingRemainder;
   private boolean doubleclick;
   private ItemStack lastQuickMoved = ItemStack.EMPTY;

   public AbstractContainerScreen(T p_97741_, Inventory p_97742_, Component p_97743_) {
      super(p_97743_);
      this.menu = p_97741_;
      this.playerInventoryTitle = p_97742_.getDisplayName();
      this.skipNextRelease = true;
      this.titleLabelX = 8;
      this.titleLabelY = 6;
      this.inventoryLabelX = 8;
      this.inventoryLabelY = this.imageHeight - 94;
      this.itemSlotMouseActions = new ArrayList<>();
   }

   @Override
   protected void init() {
      this.leftPos = (this.width - this.imageWidth) / 2;
      this.topPos = (this.height - this.imageHeight) / 2;
      this.itemSlotMouseActions.clear();
      this.addItemSlotMouseAction(new BundleMouseActions(this.minecraft));
   }

   protected void addItemSlotMouseAction(ItemSlotMouseAction p_362248_) {
      this.itemSlotMouseActions.add(p_362248_);
   }

   @Override
   public void render(GuiGraphics p_283479_, int p_283661_, int p_281248_, float p_281886_) {
      this.renderContents(p_283479_, p_283661_, p_281248_, p_281886_);
      this.renderCarriedItem(p_283479_, p_283661_, p_281248_);
      this.renderSnapbackItem(p_283479_);
   }

   public void renderContents(GuiGraphics p_409971_, int p_409213_, int p_408205_, float p_408282_) {
      int i = this.leftPos;
      int j = this.topPos;
      super.render(p_409971_, p_409213_, p_408205_, p_408282_);
      p_409971_.pose().pushMatrix();
      p_409971_.pose().translate(i, j);
      this.renderLabels(p_409971_, p_409213_, p_408205_);
      Slot slot = this.hoveredSlot;
      this.hoveredSlot = this.getHoveredSlot(p_409213_, p_408205_);
      this.renderSlotHighlightBack(p_409971_);
      this.renderSlots(p_409971_, p_409213_, p_408205_);
      this.renderSlotHighlightFront(p_409971_);
      if (slot != null && slot != this.hoveredSlot) {
         this.onStopHovering(slot);
      }

      p_409971_.pose().popMatrix();
   }

   public void renderCarriedItem(GuiGraphics p_407332_, int p_408787_, int p_409296_) {
      ItemStack itemstack = this.draggingItem.isEmpty() ? this.menu.getCarried() : this.draggingItem;
      if (!itemstack.isEmpty()) {
         int i = 8;
         int j = this.draggingItem.isEmpty() ? 8 : 16;
         String s = null;
         if (!this.draggingItem.isEmpty() && this.isSplittingStack) {
            itemstack = itemstack.copyWithCount(Mth.ceil(itemstack.getCount() / 2.0F));
         } else if (this.isQuickCrafting && this.quickCraftSlots.size() > 1) {
            itemstack = itemstack.copyWithCount(this.quickCraftingRemainder);
            if (itemstack.isEmpty()) {
               s = ChatFormatting.YELLOW + "0";
            }
         }

         p_407332_.nextStratum();
         this.renderFloatingItem(p_407332_, itemstack, p_408787_ - 8, p_409296_ - j, s);
      }
   }

   public void renderSnapbackItem(GuiGraphics p_406927_) {
      if (this.snapbackData != null) {
         float f = Mth.clamp((float)(Util.getMillis() - this.snapbackData.time) / 100.0F, 0.0F, 1.0F);
         int i = this.snapbackData.end.x - this.snapbackData.start.x;
         int j = this.snapbackData.end.y - this.snapbackData.start.y;
         int k = this.snapbackData.start.x + (int)(i * f);
         int l = this.snapbackData.start.y + (int)(j * f);
         p_406927_.nextStratum();
         this.renderFloatingItem(p_406927_, this.snapbackData.item, k, l, null);
         if (f >= 1.0F) {
            this.snapbackData = null;
         }
      }
   }

   protected void renderSlots(GuiGraphics p_366639_, int p_458409_, int p_458008_) {
      for (Slot slot : this.menu.slots) {
         if (slot.isActive()) {
            this.renderSlot(p_366639_, slot, p_458409_, p_458008_);
         }
      }
   }

   @Override
   public void renderBackground(GuiGraphics p_300197_, int p_297538_, int p_300104_, float p_298759_) {
      super.renderBackground(p_300197_, p_297538_, p_300104_, p_298759_);
      this.renderBg(p_300197_, p_298759_, p_297538_, p_300104_);
   }

   @Override
   public boolean mouseScrolled(double p_367670_, double p_363682_, double p_364454_, double p_367273_) {
      if (this.hoveredSlot != null && this.hoveredSlot.hasItem()) {
         for (ItemSlotMouseAction itemslotmouseaction : this.itemSlotMouseActions) {
            if (itemslotmouseaction.matches(this.hoveredSlot)
               && itemslotmouseaction.onMouseScrolled(p_364454_, p_367273_, this.hoveredSlot.index, this.hoveredSlot.getItem())) {
               return true;
            }
         }
      }

      return false;
   }

   private void renderSlotHighlightBack(GuiGraphics p_365614_) {
      if (this.hoveredSlot != null && this.hoveredSlot.isHighlightable()) {
         p_365614_.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_HIGHLIGHT_BACK_SPRITE, this.hoveredSlot.x - 4, this.hoveredSlot.y - 4, 24, 24);
      }
   }

   private void renderSlotHighlightFront(GuiGraphics p_362870_) {
      if (this.hoveredSlot != null && this.hoveredSlot.isHighlightable()) {
         p_362870_.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_HIGHLIGHT_FRONT_SPRITE, this.hoveredSlot.x - 4, this.hoveredSlot.y - 4, 24, 24);
      }
   }

   protected void renderTooltip(GuiGraphics p_283594_, int p_282171_, int p_281909_) {
      if (this.hoveredSlot != null && this.hoveredSlot.hasItem()) {
         ItemStack itemstack = this.hoveredSlot.getItem();
         if (this.menu.getCarried().isEmpty() || this.showTooltipWithItemInHand(itemstack)) {
            p_283594_.setTooltipForNextFrame(
               this.font,
               this.getTooltipFromContainerItem(itemstack),
               itemstack.getTooltipImage(),
               p_282171_,
               p_281909_,
               itemstack.get(DataComponents.TOOLTIP_STYLE)
            );
         }
      }
   }

   private boolean showTooltipWithItemInHand(ItemStack p_367274_) {
      return p_367274_.getTooltipImage().map(ClientTooltipComponent::create).map(ClientTooltipComponent::showTooltipWithItemInHand).orElse(false);
   }

   protected List<Component> getTooltipFromContainerItem(ItemStack p_283689_) {
      return getTooltipFromItem(this.minecraft, p_283689_);
   }

   private void renderFloatingItem(GuiGraphics p_282567_, ItemStack p_281330_, int p_281772_, int p_281689_, @Nullable String p_282568_) {
      p_282567_.renderItem(p_281330_, p_281772_, p_281689_);
      p_282567_.renderItemDecorations(this.font, p_281330_, p_281772_, p_281689_ - (this.draggingItem.isEmpty() ? 0 : 8), p_282568_);
   }

   protected void renderLabels(GuiGraphics p_281635_, int p_282681_, int p_283686_) {
      p_281635_.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, -12566464, false);
      p_281635_.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, -12566464, false);
   }

   protected abstract void renderBg(GuiGraphics var1, float var2, int var3, int var4);

   protected void renderSlot(GuiGraphics p_281607_, Slot p_282613_, int p_453429_, int p_451633_) {
      int i = p_282613_.x;
      int j = p_282613_.y;
      ItemStack itemstack = p_282613_.getItem();
      boolean flag = false;
      boolean flag1 = p_282613_ == this.clickedSlot && !this.draggingItem.isEmpty() && !this.isSplittingStack;
      ItemStack itemstack1 = this.menu.getCarried();
      String s = null;
      if (p_282613_ == this.clickedSlot && !this.draggingItem.isEmpty() && this.isSplittingStack && !itemstack.isEmpty()) {
         itemstack = itemstack.copyWithCount(itemstack.getCount() / 2);
      } else if (this.isQuickCrafting && this.quickCraftSlots.contains(p_282613_) && !itemstack1.isEmpty()) {
         if (this.quickCraftSlots.size() == 1) {
            return;
         }

         if (AbstractContainerMenu.canItemQuickReplace(p_282613_, itemstack1, true) && this.menu.canDragTo(p_282613_)) {
            flag = true;
            int k = Math.min(itemstack1.getMaxStackSize(), p_282613_.getMaxStackSize(itemstack1));
            int l = p_282613_.getItem().isEmpty() ? 0 : p_282613_.getItem().getCount();
            int i1 = AbstractContainerMenu.getQuickCraftPlaceCount(this.quickCraftSlots, this.quickCraftingType, itemstack1) + l;
            if (i1 > k) {
               i1 = k;
               s = ChatFormatting.YELLOW.toString() + k;
            }

            itemstack = itemstack1.copyWithCount(i1);
         } else {
            this.quickCraftSlots.remove(p_282613_);
            this.recalculateQuickCraftRemaining();
         }
      }

      if (itemstack.isEmpty() && p_282613_.isActive()) {
         Identifier identifier = p_282613_.getNoItemIcon();
         if (identifier != null) {
            p_281607_.blitSprite(RenderPipelines.GUI_TEXTURED, identifier, i, j, 16, 16);
            flag1 = true;
         }
      }

      if (!flag1) {
         if (flag) {
            p_281607_.fill(i, j, i + 16, j + 16, -2130706433);
         }

         int j1 = p_282613_.x + p_282613_.y * this.imageWidth;
         if (p_282613_.isFake()) {
            p_281607_.renderFakeItem(itemstack, i, j, j1);
         } else {
            p_281607_.renderItem(itemstack, i, j, j1);
         }

         p_281607_.renderItemDecorations(this.font, itemstack, i, j, s);
      }
   }

   private void recalculateQuickCraftRemaining() {
      ItemStack itemstack = this.menu.getCarried();
      if (!itemstack.isEmpty() && this.isQuickCrafting) {
         if (this.quickCraftingType == 2) {
            this.quickCraftingRemainder = itemstack.getMaxStackSize();
         } else {
            this.quickCraftingRemainder = itemstack.getCount();

            for (Slot slot : this.quickCraftSlots) {
               ItemStack itemstack1 = slot.getItem();
               int i = itemstack1.isEmpty() ? 0 : itemstack1.getCount();
               int j = Math.min(itemstack.getMaxStackSize(), slot.getMaxStackSize(itemstack));
               int k = Math.min(AbstractContainerMenu.getQuickCraftPlaceCount(this.quickCraftSlots, this.quickCraftingType, itemstack) + i, j);
               this.quickCraftingRemainder -= k - i;
            }
         }
      }
   }

   private @Nullable Slot getHoveredSlot(double p_367266_, double p_363404_) {
      for (Slot slot : this.menu.slots) {
         if (slot.isActive() && this.isHovering(slot, p_367266_, p_363404_)) {
            return slot;
         }
      }

      return null;
   }

   @Override
   public boolean mouseClicked(MouseButtonEvent p_427253_, boolean p_427622_) {
      if (super.mouseClicked(p_427253_, p_427622_)) {
         return true;
      }

      boolean flag = this.minecraft.options.keyPickItem.matchesMouse(p_427253_) && this.minecraft.player.hasInfiniteMaterials();
      Slot slot = this.getHoveredSlot(p_427253_.x(), p_427253_.y());
      this.doubleclick = this.lastClickSlot == slot && p_427622_;
      this.skipNextRelease = false;
      if (p_427253_.button() != 0 && p_427253_.button() != 1 && !flag) {
         this.checkHotbarMouseClicked(p_427253_);
      } else {
         int i = this.leftPos;
         int j = this.topPos;
         boolean flag1 = this.hasClickedOutside(p_427253_.x(), p_427253_.y(), i, j);
         int k = -1;
         if (slot != null) {
            k = slot.index;
         }

         if (flag1) {
            k = -999;
         }

         if (this.minecraft.options.touchscreen().get() && flag1 && this.menu.getCarried().isEmpty()) {
            this.onClose();
            return true;
         }

         if (k != -1) {
            if (this.minecraft.options.touchscreen().get()) {
               if (slot != null && slot.hasItem()) {
                  this.clickedSlot = slot;
                  this.draggingItem = ItemStack.EMPTY;
                  this.isSplittingStack = p_427253_.button() == 1;
               } else {
                  this.clickedSlot = null;
               }
            } else if (!this.isQuickCrafting) {
               if (this.menu.getCarried().isEmpty()) {
                  if (flag) {
                     this.slotClicked(slot, k, p_427253_.button(), ClickType.CLONE);
                  } else {
                     boolean flag2 = k != -999 && p_427253_.hasShiftDown();
                     ClickType clicktype = ClickType.PICKUP;
                     if (flag2) {
                        this.lastQuickMoved = slot != null && slot.hasItem() ? slot.getItem().copy() : ItemStack.EMPTY;
                        clicktype = ClickType.QUICK_MOVE;
                     } else if (k == -999) {
                        clicktype = ClickType.THROW;
                     }

                     this.slotClicked(slot, k, p_427253_.button(), clicktype);
                  }

                  this.skipNextRelease = true;
               } else {
                  this.isQuickCrafting = true;
                  this.quickCraftingButton = p_427253_.button();
                  this.quickCraftSlots.clear();
                  if (p_427253_.button() == 0) {
                     this.quickCraftingType = 0;
                  } else if (p_427253_.button() == 1) {
                     this.quickCraftingType = 1;
                  } else if (flag) {
                     this.quickCraftingType = 2;
                  }
               }
            }
         }
      }

      this.lastClickSlot = slot;
      return true;
   }

   private void checkHotbarMouseClicked(MouseButtonEvent p_427404_) {
      if (this.hoveredSlot != null && this.menu.getCarried().isEmpty()) {
         if (this.minecraft.options.keySwapOffhand.matchesMouse(p_427404_)) {
            this.slotClicked(this.hoveredSlot, this.hoveredSlot.index, 40, ClickType.SWAP);
            return;
         }

         for (int i = 0; i < 9; i++) {
            if (this.minecraft.options.keyHotbarSlots[i].matchesMouse(p_427404_)) {
               this.slotClicked(this.hoveredSlot, this.hoveredSlot.index, i, ClickType.SWAP);
            }
         }
      }
   }

   protected boolean hasClickedOutside(double p_97757_, double p_97758_, int p_97759_, int p_97760_) {
      return p_97757_ < p_97759_ || p_97758_ < p_97760_ || p_97757_ >= p_97759_ + this.imageWidth || p_97758_ >= p_97760_ + this.imageHeight;
   }

   @Override
   public boolean mouseDragged(MouseButtonEvent p_423035_, double p_97752_, double p_97753_) {
      Slot slot = this.getHoveredSlot(p_423035_.x(), p_423035_.y());
      ItemStack itemstack = this.menu.getCarried();
      if (this.clickedSlot != null && this.minecraft.options.touchscreen().get()) {
         if (p_423035_.button() == 0 || p_423035_.button() == 1) {
            if (this.draggingItem.isEmpty()) {
               if (slot != this.clickedSlot && !this.clickedSlot.getItem().isEmpty()) {
                  this.draggingItem = this.clickedSlot.getItem().copy();
               }
            } else if (this.draggingItem.getCount() > 1 && slot != null && AbstractContainerMenu.canItemQuickReplace(slot, this.draggingItem, false)) {
               long i = Util.getMillis();
               if (this.quickdropSlot == slot) {
                  if (i - this.quickdropTime > 500L) {
                     this.slotClicked(this.clickedSlot, this.clickedSlot.index, 0, ClickType.PICKUP);
                     this.slotClicked(slot, slot.index, 1, ClickType.PICKUP);
                     this.slotClicked(this.clickedSlot, this.clickedSlot.index, 0, ClickType.PICKUP);
                     this.quickdropTime = i + 750L;
                     this.draggingItem.shrink(1);
                  }
               } else {
                  this.quickdropSlot = slot;
                  this.quickdropTime = i;
               }
            }
         }

         return true;
      } else if (this.isQuickCrafting
         && slot != null
         && !itemstack.isEmpty()
         && (itemstack.getCount() > this.quickCraftSlots.size() || this.quickCraftingType == 2)
         && AbstractContainerMenu.canItemQuickReplace(slot, itemstack, true)
         && slot.mayPlace(itemstack)
         && this.menu.canDragTo(slot)) {
         this.quickCraftSlots.add(slot);
         this.recalculateQuickCraftRemaining();
         return true;
      } else {
         return slot == null && this.menu.getCarried().isEmpty() ? super.mouseDragged(p_423035_, p_97752_, p_97753_) : true;
      }
   }

   @Override
   public boolean mouseReleased(MouseButtonEvent p_430939_) {
      Slot slot = this.getHoveredSlot(p_430939_.x(), p_430939_.y());
      int i = this.leftPos;
      int j = this.topPos;
      boolean flag = this.hasClickedOutside(p_430939_.x(), p_430939_.y(), i, j);
      int k = -1;
      if (slot != null) {
         k = slot.index;
      }

      if (flag) {
         k = -999;
      }

      if (this.doubleclick && slot != null && p_430939_.button() == 0 && this.menu.canTakeItemForPickAll(ItemStack.EMPTY, slot)) {
         if (p_430939_.hasShiftDown()) {
            if (!this.lastQuickMoved.isEmpty()) {
               for (Slot slot2 : this.menu.slots) {
                  if (slot2 != null
                     && slot2.mayPickup(this.minecraft.player)
                     && slot2.hasItem()
                     && slot2.container == slot.container
                     && AbstractContainerMenu.canItemQuickReplace(slot2, this.lastQuickMoved, true)) {
                     this.slotClicked(slot2, slot2.index, p_430939_.button(), ClickType.QUICK_MOVE);
                  }
               }
            }
         } else {
            this.slotClicked(slot, k, p_430939_.button(), ClickType.PICKUP_ALL);
         }

         this.doubleclick = false;
      } else {
         if (this.isQuickCrafting && this.quickCraftingButton != p_430939_.button()) {
            this.isQuickCrafting = false;
            this.quickCraftSlots.clear();
            this.skipNextRelease = true;
            return true;
         }

         if (this.skipNextRelease) {
            this.skipNextRelease = false;
            return true;
         }

         if (this.clickedSlot != null && this.minecraft.options.touchscreen().get()) {
            if (p_430939_.button() == 0 || p_430939_.button() == 1) {
               if (this.draggingItem.isEmpty() && slot != this.clickedSlot) {
                  this.draggingItem = this.clickedSlot.getItem();
               }

               boolean flag2 = AbstractContainerMenu.canItemQuickReplace(slot, this.draggingItem, false);
               if (k != -1 && !this.draggingItem.isEmpty() && flag2) {
                  this.slotClicked(this.clickedSlot, this.clickedSlot.index, p_430939_.button(), ClickType.PICKUP);
                  this.slotClicked(slot, k, 0, ClickType.PICKUP);
                  if (this.menu.getCarried().isEmpty()) {
                     this.snapbackData = null;
                  } else {
                     this.slotClicked(this.clickedSlot, this.clickedSlot.index, p_430939_.button(), ClickType.PICKUP);
                     this.snapbackData = new AbstractContainerScreen.SnapbackData(
                        this.draggingItem,
                        new Vector2i((int)p_430939_.x(), (int)p_430939_.y()),
                        new Vector2i(this.clickedSlot.x + i, this.clickedSlot.y + j),
                        Util.getMillis()
                     );
                  }
               } else if (!this.draggingItem.isEmpty()) {
                  this.snapbackData = new AbstractContainerScreen.SnapbackData(
                     this.draggingItem,
                     new Vector2i((int)p_430939_.x(), (int)p_430939_.y()),
                     new Vector2i(this.clickedSlot.x + i, this.clickedSlot.y + j),
                     Util.getMillis()
                  );
               }

               this.clearDraggingState();
            }
         } else if (this.isQuickCrafting && !this.quickCraftSlots.isEmpty()) {
            this.slotClicked(null, -999, AbstractContainerMenu.getQuickcraftMask(0, this.quickCraftingType), ClickType.QUICK_CRAFT);

            for (Slot slot1 : this.quickCraftSlots) {
               this.slotClicked(slot1, slot1.index, AbstractContainerMenu.getQuickcraftMask(1, this.quickCraftingType), ClickType.QUICK_CRAFT);
            }

            this.slotClicked(null, -999, AbstractContainerMenu.getQuickcraftMask(2, this.quickCraftingType), ClickType.QUICK_CRAFT);
         } else if (!this.menu.getCarried().isEmpty()) {
            if (this.minecraft.options.keyPickItem.matchesMouse(p_430939_)) {
               this.slotClicked(slot, k, p_430939_.button(), ClickType.CLONE);
            } else {
               boolean flag1 = k != -999 && p_430939_.hasShiftDown();
               if (flag1) {
                  this.lastQuickMoved = slot != null && slot.hasItem() ? slot.getItem().copy() : ItemStack.EMPTY;
               }

               this.slotClicked(slot, k, p_430939_.button(), flag1 ? ClickType.QUICK_MOVE : ClickType.PICKUP);
            }
         }
      }

      this.isQuickCrafting = false;
      return true;
   }

   public void clearDraggingState() {
      this.draggingItem = ItemStack.EMPTY;
      this.clickedSlot = null;
   }

   private boolean isHovering(Slot p_97775_, double p_97776_, double p_97777_) {
      return this.isHovering(p_97775_.x, p_97775_.y, 16, 16, p_97776_, p_97777_);
   }

   protected boolean isHovering(int p_97768_, int p_97769_, int p_97770_, int p_97771_, double p_97772_, double p_97773_) {
      int i = this.leftPos;
      int j = this.topPos;
      p_97772_ -= i;
      p_97773_ -= j;
      return p_97772_ >= p_97768_ - 1 && p_97772_ < p_97768_ + p_97770_ + 1 && p_97773_ >= p_97769_ - 1 && p_97773_ < p_97769_ + p_97771_ + 1;
   }

   private void onStopHovering(Slot p_366155_) {
      if (p_366155_.hasItem()) {
         for (ItemSlotMouseAction itemslotmouseaction : this.itemSlotMouseActions) {
            if (itemslotmouseaction.matches(p_366155_)) {
               itemslotmouseaction.onStopHovering(p_366155_);
            }
         }
      }
   }

   protected void slotClicked(Slot p_97778_, int p_97779_, int p_97780_, ClickType p_97781_) {
      if (p_97778_ != null) {
         p_97779_ = p_97778_.index;
      }

      this.onMouseClickAction(p_97778_, p_97781_);
      this.minecraft.gameMode.handleInventoryMouseClick(this.menu.containerId, p_97779_, p_97780_, p_97781_, this.minecraft.player);
   }

   void onMouseClickAction(@Nullable Slot p_363727_, ClickType p_363931_) {
      if (p_363727_ != null && p_363727_.hasItem()) {
         for (ItemSlotMouseAction itemslotmouseaction : this.itemSlotMouseActions) {
            if (itemslotmouseaction.matches(p_363727_)) {
               itemslotmouseaction.onSlotClicked(p_363727_, p_363931_);
            }
         }
      }
   }

   protected void handleSlotStateChanged(int p_310652_, int p_312119_, boolean p_310240_) {
      this.minecraft.gameMode.handleSlotStateChanged(p_310652_, p_312119_, p_310240_);
   }

   @Override
   public boolean keyPressed(KeyEvent p_424324_) {
      if (super.keyPressed(p_424324_)) {
         return true;
      }

      if (this.minecraft.options.keyInventory.matches(p_424324_)) {
         this.onClose();
         return true;
      }

      this.checkHotbarKeyPressed(p_424324_);
      if (this.hoveredSlot != null && this.hoveredSlot.hasItem()) {
         if (this.minecraft.options.keyPickItem.matches(p_424324_)) {
            this.slotClicked(this.hoveredSlot, this.hoveredSlot.index, 0, ClickType.CLONE);
         } else if (this.minecraft.options.keyDrop.matches(p_424324_)) {
            this.slotClicked(this.hoveredSlot, this.hoveredSlot.index, p_424324_.hasControlDown() ? 1 : 0, ClickType.THROW);
         }
      }

      return false;
   }

   protected boolean checkHotbarKeyPressed(KeyEvent p_428976_) {
      if (this.menu.getCarried().isEmpty() && this.hoveredSlot != null) {
         if (this.minecraft.options.keySwapOffhand.matches(p_428976_)) {
            this.slotClicked(this.hoveredSlot, this.hoveredSlot.index, 40, ClickType.SWAP);
            return true;
         }

         for (int i = 0; i < 9; i++) {
            if (this.minecraft.options.keyHotbarSlots[i].matches(p_428976_)) {
               this.slotClicked(this.hoveredSlot, this.hoveredSlot.index, i, ClickType.SWAP);
               return true;
            }
         }
      }

      return false;
   }

   @Override
   public void removed() {
      if (this.minecraft.player != null) {
         this.menu.removed(this.minecraft.player);
      }
   }

   @Override
   public boolean isPauseScreen() {
      return false;
   }

   @Override
   public boolean isInGameUi() {
      return true;
   }

   @Override
   public final void tick() {
      super.tick();
      if (this.minecraft.player.isAlive() && !this.minecraft.player.isRemoved()) {
         this.containerTick();
      } else {
         this.minecraft.player.closeContainer();
      }
   }

   protected void containerTick() {
   }

   @Override
   public T getMenu() {
      return this.menu;
   }

   @Override
   public void onClose() {
      this.minecraft.player.closeContainer();
      if (this.hoveredSlot != null) {
         this.onStopHovering(this.hoveredSlot);
      }

      super.onClose();
   }

   @OnlyIn(Dist.CLIENT)
   record SnapbackData(ItemStack item, Vector2i start, Vector2i end, long time) {
   }
}
