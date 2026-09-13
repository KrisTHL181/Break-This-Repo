package net.minecraft.client.gui.components;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.TextAlignment;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public interface MultiLineLabel {
   MultiLineLabel EMPTY = new MultiLineLabel() {
      @Override
      public int visitLines(TextAlignment p_456824_, int p_94389_, int p_94390_, int p_94391_, ActiveTextCollector p_450488_) {
         return p_94390_;
      }

      @Override
      public int getLineCount() {
         return 0;
      }

      @Override
      public int getWidth() {
         return 0;
      }
   };

   static MultiLineLabel create(Font p_94351_, Component... p_94352_) {
      return create(p_94351_, Integer.MAX_VALUE, Integer.MAX_VALUE, p_94352_);
   }

   static MultiLineLabel create(Font p_94342_, int p_94344_, Component... p_345312_) {
      return create(p_94342_, p_94344_, Integer.MAX_VALUE, p_345312_);
   }

   static MultiLineLabel create(Font p_94346_, Component p_344884_, int p_94348_) {
      return create(p_94346_, p_94348_, Integer.MAX_VALUE, p_344884_);
   }

   static MultiLineLabel create(final Font p_169037_, final int p_342954_, final int p_342610_, final Component... p_345091_) {
      return p_345091_.length == 0
         ? EMPTY
         : new MultiLineLabel() {
            private @Nullable List<MultiLineLabel.TextAndWidth> cachedTextAndWidth;
            private @Nullable Language splitWithLanguage;

            @Override
            public int visitLines(TextAlignment p_453127_, int p_456938_, int p_450235_, int p_460716_, ActiveTextCollector p_455343_) {
               int i = p_450235_;

               for (MultiLineLabel.TextAndWidth multilinelabel$textandwidth : this.getSplitMessage()) {
                  int j = p_453127_.calculateLeft(p_456938_, multilinelabel$textandwidth.width);
                  p_455343_.accept(j, i, multilinelabel$textandwidth.text);
                  i += p_460716_;
               }

               return i;
            }

            private List<MultiLineLabel.TextAndWidth> getSplitMessage() {
               Language language = Language.getInstance();
               if (this.cachedTextAndWidth != null && language == this.splitWithLanguage) {
                  return this.cachedTextAndWidth;
               }

               this.splitWithLanguage = language;
               List<FormattedText> list = new ArrayList<>();

               for (Component component : p_345091_) {
                  list.addAll(p_169037_.splitIgnoringLanguage(component, p_342954_));
               }

               this.cachedTextAndWidth = new ArrayList<>();
               int i = Math.min(list.size(), p_342610_);
               List<FormattedText> list1 = list.subList(0, i);

               for (int j = 0; j < list1.size(); j++) {
                  FormattedText formattedtext2 = list1.get(j);
                  FormattedCharSequence formattedcharsequence = Language.getInstance().getVisualOrder(formattedtext2);
                  if (j == list1.size() - 1 && i == p_342610_ && i != list.size()) {
                     FormattedText formattedtext = p_169037_.substrByWidth(
                        formattedtext2, p_169037_.width(formattedtext2) - p_169037_.width(CommonComponents.ELLIPSIS)
                     );
                     FormattedText formattedtext1 = FormattedText.composite(
                        formattedtext, CommonComponents.ELLIPSIS.copy().withStyle(p_345091_[p_345091_.length - 1].getStyle())
                     );
                     this.cachedTextAndWidth
                        .add(new MultiLineLabel.TextAndWidth(Language.getInstance().getVisualOrder(formattedtext1), p_169037_.width(formattedtext1)));
                  } else {
                     this.cachedTextAndWidth.add(new MultiLineLabel.TextAndWidth(formattedcharsequence, p_169037_.width(formattedcharsequence)));
                  }
               }

               return this.cachedTextAndWidth;
            }

            @Override
            public int getLineCount() {
               return this.getSplitMessage().size();
            }

            @Override
            public int getWidth() {
               return Math.min(p_342954_, this.getSplitMessage().stream().mapToInt(MultiLineLabel.TextAndWidth::width).max().orElse(0));
            }
         };
   }

   int visitLines(TextAlignment var1, int var2, int var3, int var4, ActiveTextCollector var5);

   int getLineCount();

   int getWidth();

   @OnlyIn(Dist.CLIENT)
   record TextAndWidth(FormattedCharSequence text, int width) {
   }
}
