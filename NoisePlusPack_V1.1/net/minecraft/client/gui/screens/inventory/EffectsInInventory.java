package net.minecraft.client.gui.screens.inventory;

import com.google.common.collect.Ordering;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class EffectsInInventory {
   private static final Identifier EFFECT_BACKGROUND_SPRITE = Identifier.withDefaultNamespace("container/inventory/effect_background");
   private static final Identifier EFFECT_BACKGROUND_AMBIENT_SPRITE = Identifier.withDefaultNamespace("container/inventory/effect_background_ambient");
   private static final int ICON_SIZE = 18;
   public static final int SPACING = 7;
   private static final int TEXT_X_OFFSET = 32;
   public static final int SPRITE_SQUARE_SIZE = 32;
   private final AbstractContainerScreen<?> screen;
   private final Minecraft minecraft;

   public EffectsInInventory(AbstractContainerScreen<?> p_367800_) {
      this.screen = p_367800_;
      this.minecraft = Minecraft.getInstance();
   }

   public boolean canSeeEffects() {
      int i = this.screen.leftPos + this.screen.imageWidth + 2;
      int j = this.screen.width - i;
      return j >= 32;
   }

   public void render(GuiGraphics p_456221_, int p_453611_, int p_457243_) {
      int i = this.screen.leftPos + this.screen.imageWidth + 2;
      int j = this.screen.width - i;
      Collection<MobEffectInstance> collection = this.minecraft.player.getActiveEffects();
      if (!collection.isEmpty() && j >= 32) {
         int k = j >= 120 ? j - 7 : 32;
         int l = 33;
         if (collection.size() > 5) {
            l = 132 / (collection.size() - 1);
         }

         this.renderEffects(p_456221_, collection, i, l, p_453611_, p_457243_, k);
      }
   }

   private void renderEffects(
      GuiGraphics p_362146_, Collection<MobEffectInstance> p_453354_, int p_370153_, int p_365612_, int p_457242_, int p_451307_, int p_457235_
   ) {
      Iterable<MobEffectInstance> iterable = Ordering.natural().sortedCopy(p_453354_);
      int i = this.screen.topPos;
      Font font = this.screen.getFont();

      for (MobEffectInstance mobeffectinstance : iterable) {
         boolean flag = mobeffectinstance.isAmbient();
         Component component = this.getEffectName(mobeffectinstance);
         Component component1 = MobEffectUtil.formatDuration(mobeffectinstance, 1.0F, this.minecraft.level.tickRateManager().tickrate());
         int j = this.renderBackground(p_362146_, font, component, component1, p_370153_, i, flag, p_457235_);
         this.renderText(p_362146_, component, component1, font, p_370153_, i, j, p_365612_, p_457242_, p_451307_);
         p_362146_.blitSprite(RenderPipelines.GUI_TEXTURED, Gui.getMobEffectSprite(mobeffectinstance.getEffect()), p_370153_ + 7, i + 7, 18, 18);
         i += p_365612_;
      }
   }

   private int renderBackground(
      GuiGraphics p_451266_, Font p_450618_, Component p_451190_, Component p_450358_, int p_459472_, int p_458932_, boolean p_460666_, int p_450197_
   ) {
      int i = 32 + p_450618_.width(p_451190_) + 7;
      int j = 32 + p_450618_.width(p_450358_) + 7;
      int k = Math.min(p_450197_, Math.max(i, j));
      p_451266_.blitSprite(RenderPipelines.GUI_TEXTURED, p_460666_ ? EFFECT_BACKGROUND_AMBIENT_SPRITE : EFFECT_BACKGROUND_SPRITE, p_459472_, p_458932_, k, 32);
      return k;
   }

   private void renderText(
      GuiGraphics p_455304_,
      Component p_450545_,
      Component p_459253_,
      Font p_457441_,
      int p_454404_,
      int p_453283_,
      int p_459389_,
      int p_459278_,
      int p_454976_,
      int p_455245_
   ) {
      int i = p_454404_ + 32;
      int j = p_453283_ + 7;
      int k = p_459389_ - 32 - 7;
      boolean flag;
      if (k > 0) {
         boolean flag1 = p_457441_.width(p_450545_) > k;
         FormattedCharSequence formattedcharsequence = flag1 ? StringWidget.clipText(p_450545_, p_457441_, k) : p_450545_.getVisualOrderText();
         p_455304_.drawString(p_457441_, formattedcharsequence, i, j, -1);
         p_455304_.drawString(p_457441_, p_459253_, i, j + 9, -8355712);
         flag = flag1;
      } else {
         flag = true;
      }

      if (flag && p_454976_ >= p_454404_ && p_454976_ <= p_454404_ + p_459389_ && p_455245_ >= p_453283_ && p_455245_ <= p_453283_ + p_459278_) {
         p_455304_.setTooltipForNextFrame(this.screen.getFont(), List.of(p_450545_, p_459253_), Optional.empty(), p_454976_, p_455245_);
      }
   }

   private Component getEffectName(MobEffectInstance p_368169_) {
      MutableComponent mutablecomponent = p_368169_.getEffect().value().getDisplayName().copy();
      if (p_368169_.getAmplifier() >= 1 && p_368169_.getAmplifier() <= 9) {
         mutablecomponent.append(CommonComponents.SPACE).append(Component.translatable("enchantment.level." + (p_368169_.getAmplifier() + 1)));
      }

      return mutablecomponent;
   }
}
