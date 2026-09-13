package net.minecraft.client.gui.components;

import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.Font;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class StringWidget extends AbstractStringWidget {
   private static final int TEXT_MARGIN = 2;
   private int maxWidth = 0;
   private int cachedWidth = 0;
   private boolean cachedWidthDirty = true;
   private StringWidget.TextOverflow textOverflow = StringWidget.TextOverflow.CLAMPED;

   public StringWidget(Component p_268211_, Font p_267963_) {
      this(0, 0, p_267963_.width(p_268211_.getVisualOrderText()), 9, p_268211_, p_267963_);
   }

   public StringWidget(int p_268183_, int p_268082_, Component p_268069_, Font p_268121_) {
      this(0, 0, p_268183_, p_268082_, p_268069_, p_268121_);
   }

   public StringWidget(int p_268199_, int p_268137_, int p_268178_, int p_268169_, Component p_268285_, Font p_268047_) {
      super(p_268199_, p_268137_, p_268178_, p_268169_, p_268285_, p_268047_);
      this.active = false;
   }

   @Override
   public void setMessage(Component p_431416_) {
      super.setMessage(p_431416_);
      this.cachedWidthDirty = true;
   }

   public StringWidget setMaxWidth(int p_427267_) {
      return this.setMaxWidth(p_427267_, StringWidget.TextOverflow.CLAMPED);
   }

   public StringWidget setMaxWidth(int p_426032_, StringWidget.TextOverflow p_430245_) {
      this.maxWidth = p_426032_;
      this.textOverflow = p_430245_;
      return this;
   }

   @Override
   public int getWidth() {
      if (this.maxWidth > 0) {
         if (this.cachedWidthDirty) {
            this.cachedWidth = Math.min(this.maxWidth, this.getFont().width(this.getMessage().getVisualOrderText()));
            this.cachedWidthDirty = false;
         }

         return this.cachedWidth;
      } else {
         return super.getWidth();
      }
   }

   @Override
   public void visitLines(ActiveTextCollector p_451720_) {
      Component component = this.getMessage();
      Font font = this.getFont();
      int i = this.maxWidth > 0 ? this.maxWidth : this.getWidth();
      int j = font.width(component);
      int k = this.getX();
      int l = this.getY() + (this.getHeight() - 9) / 2;
      boolean flag = j > i;
      if (flag) {
         switch (this.textOverflow) {
            case CLAMPED:
               p_451720_.accept(k, l, clipText(component, font, i));
               break;
            case SCROLLING:
               this.renderScrollingStringOverContents(p_451720_, component, 2);
         }
      } else {
         p_451720_.accept(k, l, component.getVisualOrderText());
      }
   }

   public static FormattedCharSequence clipText(Component p_301164_, Font p_460902_, int p_298237_) {
      FormattedText formattedtext = p_460902_.substrByWidth(p_301164_, p_298237_ - p_460902_.width(CommonComponents.ELLIPSIS));
      return Language.getInstance().getVisualOrder(FormattedText.composite(formattedtext, CommonComponents.ELLIPSIS));
   }

   @OnlyIn(Dist.CLIENT)
   public enum TextOverflow {
      CLAMPED,
      SCROLLING;
   }
}
