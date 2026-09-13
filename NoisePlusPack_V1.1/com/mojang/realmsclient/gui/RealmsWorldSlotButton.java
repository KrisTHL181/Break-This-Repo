package com.mojang.realmsclient.gui;

import com.mojang.realmsclient.RealmsMainScreen;
import com.mojang.realmsclient.dto.RealmsServer;
import com.mojang.realmsclient.dto.RealmsSlot;
import com.mojang.realmsclient.util.RealmsTextureManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class RealmsWorldSlotButton extends Button {
   private static final Identifier SLOT_FRAME_SPRITE = Identifier.withDefaultNamespace("widget/slot_frame");
   public static final Identifier EMPTY_SLOT_LOCATION = Identifier.withDefaultNamespace("textures/gui/realms/empty_frame.png");
   public static final Identifier DEFAULT_WORLD_SLOT_1 = Identifier.withDefaultNamespace("textures/gui/title/background/panorama_0.png");
   public static final Identifier DEFAULT_WORLD_SLOT_2 = Identifier.withDefaultNamespace("textures/gui/title/background/panorama_2.png");
   public static final Identifier DEFAULT_WORLD_SLOT_3 = Identifier.withDefaultNamespace("textures/gui/title/background/panorama_3.png");
   private static final Component SWITCH_TO_MINIGAME_SLOT_TOOLTIP = Component.translatable("mco.configure.world.slot.tooltip.minigame");
   private static final Component SWITCH_TO_WORLD_SLOT_TOOLTIP = Component.translatable("mco.configure.world.slot.tooltip");
   static final Component MINIGAME = Component.translatable("mco.worldSlot.minigame");
   private static final int WORLD_NAME_MAX_WIDTH = 64;
   private static final String DOTS = "...";
   private final int slotIndex;
   private RealmsWorldSlotButton.State state;

   public RealmsWorldSlotButton(int p_87929_, int p_87930_, int p_87931_, int p_87932_, int p_87935_, RealmsServer p_409959_, Button.OnPress p_87936_) {
      super(p_87929_, p_87930_, p_87931_, p_87932_, CommonComponents.EMPTY, p_87936_, DEFAULT_NARRATION);
      this.slotIndex = p_87935_;
      this.state = this.setServerData(p_409959_);
   }

   public RealmsWorldSlotButton.State getState() {
      return this.state;
   }

   public RealmsWorldSlotButton.State setServerData(RealmsServer p_310623_) {
      this.state = new RealmsWorldSlotButton.State(p_310623_, this.slotIndex);
      this.setTooltipAndNarration(this.state, p_310623_.minigameName);
      return this.state;
   }

   private void setTooltipAndNarration(RealmsWorldSlotButton.State p_312604_, @Nullable String p_310582_) {
      Component component = switch (p_312604_.action) {
         case SWITCH_SLOT -> p_312604_.minigame ? SWITCH_TO_MINIGAME_SLOT_TOOLTIP : SWITCH_TO_WORLD_SLOT_TOOLTIP;
         default -> null;
      };
      if (component != null) {
         this.setTooltip(Tooltip.create(component));
      }

      MutableComponent mutablecomponent = Component.literal(p_312604_.slotName);
      if (p_312604_.minigame && p_310582_ != null) {
         mutablecomponent = mutablecomponent.append(CommonComponents.SPACE).append(p_310582_);
      }

      this.setMessage(mutablecomponent);
   }

   static RealmsWorldSlotButton.Action getAction(boolean p_87961_, boolean p_87962_, boolean p_425476_) {
      return !p_87961_ && (!p_87962_ || !p_425476_) ? RealmsWorldSlotButton.Action.SWITCH_SLOT : RealmsWorldSlotButton.Action.NOTHING;
   }

   @Override
   public boolean isActive() {
      return this.state.action != RealmsWorldSlotButton.Action.NOTHING && super.isActive();
   }

   @Override
   public void renderContents(GuiGraphics p_282947_, int p_87965_, int p_87966_, float p_87967_) {
      int i = this.getX();
      int j = this.getY();
      boolean flag = this.isHoveredOrFocused();
      Identifier identifier;
      if (this.state.minigame) {
         identifier = RealmsTextureManager.worldTemplate(String.valueOf(this.state.imageId), this.state.image);
      } else if (this.state.empty) {
         identifier = EMPTY_SLOT_LOCATION;
      } else if (this.state.image != null && this.state.imageId != -1L) {
         identifier = RealmsTextureManager.worldTemplate(String.valueOf(this.state.imageId), this.state.image);
      } else if (this.slotIndex == 1) {
         identifier = DEFAULT_WORLD_SLOT_1;
      } else if (this.slotIndex == 2) {
         identifier = DEFAULT_WORLD_SLOT_2;
      } else if (this.slotIndex == 3) {
         identifier = DEFAULT_WORLD_SLOT_3;
      } else {
         identifier = EMPTY_SLOT_LOCATION;
      }

      int k = -1;
      if (!this.state.activeSlot) {
         k = ARGB.colorFromFloat(1.0F, 0.56F, 0.56F, 0.56F);
      }

      p_282947_.blit(RenderPipelines.GUI_TEXTURED, identifier, i + 1, j + 1, 0.0F, 0.0F, this.width - 2, this.height - 2, 74, 74, 74, 74, k);
      if (flag && this.state.action != RealmsWorldSlotButton.Action.NOTHING) {
         p_282947_.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_FRAME_SPRITE, i, j, this.width, this.height);
      } else if (this.state.activeSlot) {
         p_282947_.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_FRAME_SPRITE, i, j, this.width, this.height, ARGB.colorFromFloat(1.0F, 0.8F, 0.8F, 0.8F));
      } else {
         p_282947_.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_FRAME_SPRITE, i, j, this.width, this.height, ARGB.colorFromFloat(1.0F, 0.56F, 0.56F, 0.56F));
      }

      if (this.state.hardcore) {
         p_282947_.blitSprite(RenderPipelines.GUI_TEXTURED, RealmsMainScreen.HARDCORE_MODE_SPRITE, i + 3, j + 4, 9, 8);
      }

      Font font = Minecraft.getInstance().font;
      String s = this.state.slotName;
      if (font.width(s) > 64) {
         s = font.plainSubstrByWidth(s, 64 - font.width("...")) + "...";
      }

      p_282947_.drawCenteredString(font, s, i + this.width / 2, j + this.height - 14, -1);
      if (this.state.activeSlot) {
         p_282947_.drawCenteredString(
            font,
            RealmsMainScreen.getVersionComponent(this.state.slotVersion, this.state.compatibility.isCompatible()),
            i + this.width / 2,
            j + this.height + 2,
            -1
         );
      }
   }

   @OnlyIn(Dist.CLIENT)
   public enum Action {
      NOTHING,
      SWITCH_SLOT;
   }

   @OnlyIn(Dist.CLIENT)
   public static class State {
      final String slotName;
      final String slotVersion;
      final RealmsServer.Compatibility compatibility;
      final long imageId;
      final @Nullable String image;
      public final boolean empty;
      public final boolean minigame;
      public final RealmsWorldSlotButton.Action action;
      public final boolean hardcore;
      public final boolean activeSlot;

      public State(RealmsServer p_309960_, int p_309979_) {
         this.minigame = p_309979_ == 4;
         if (this.minigame) {
            this.slotName = RealmsWorldSlotButton.MINIGAME.getString();
            this.imageId = p_309960_.minigameId;
            this.image = p_309960_.minigameImage;
            this.empty = p_309960_.minigameId == -1;
            this.slotVersion = "";
            this.compatibility = RealmsServer.Compatibility.UNVERIFIABLE;
            this.hardcore = false;
            this.activeSlot = p_309960_.isMinigameActive();
         } else {
            RealmsSlot realmsslot = p_309960_.slots.get(p_309979_);
            this.slotName = realmsslot.options.getSlotName(p_309979_);
            this.imageId = realmsslot.options.templateId;
            this.image = realmsslot.options.templateImage;
            this.empty = realmsslot.options.empty;
            this.slotVersion = realmsslot.options.version;
            this.compatibility = realmsslot.options.compatibility;
            this.hardcore = realmsslot.isHardcore();
            this.activeSlot = p_309960_.activeSlot == p_309979_ && !p_309960_.isMinigameActive();
         }

         this.action = RealmsWorldSlotButton.getAction(this.activeSlot, this.empty, p_309960_.expired);
      }
   }
}
