package net.minecraft.network.chat;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.function.UnaryOperator;
import net.minecraft.ChatFormatting;
import net.minecraft.locale.Language;
import net.minecraft.util.FormattedCharSequence;
import org.jspecify.annotations.Nullable;

public final class MutableComponent implements Component {
   private final ComponentContents contents;
   private final List<Component> siblings;
   private Style style;
   private FormattedCharSequence visualOrderText = FormattedCharSequence.EMPTY;
   private @Nullable Language decomposedWith;

   MutableComponent(ComponentContents p_237200_, List<Component> p_237201_, Style p_237202_) {
      this.contents = p_237200_;
      this.siblings = p_237201_;
      this.style = p_237202_;
   }

   public static MutableComponent create(ComponentContents p_237205_) {
      return new MutableComponent(p_237205_, Lists.newArrayList(), Style.EMPTY);
   }

   @Override
   public ComponentContents getContents() {
      return this.contents;
   }

   @Override
   public List<Component> getSiblings() {
      return this.siblings;
   }

   public MutableComponent setStyle(Style p_130943_) {
      this.style = p_130943_;
      return this;
   }

   @Override
   public Style getStyle() {
      return this.style;
   }

   public MutableComponent append(String p_130947_) {
      return p_130947_.isEmpty() ? this : this.append(Component.literal(p_130947_));
   }

   public MutableComponent append(Component p_130942_) {
      this.siblings.add(p_130942_);
      return this;
   }

   public MutableComponent withStyle(UnaryOperator<Style> p_130939_) {
      this.setStyle(p_130939_.apply(this.getStyle()));
      return this;
   }

   public MutableComponent withStyle(Style p_130949_) {
      this.setStyle(p_130949_.applyTo(this.getStyle()));
      return this;
   }

   public MutableComponent withStyle(ChatFormatting... p_130945_) {
      this.setStyle(this.getStyle().applyFormats(p_130945_));
      return this;
   }

   public MutableComponent withStyle(ChatFormatting p_130941_) {
      this.setStyle(this.getStyle().applyFormat(p_130941_));
      return this;
   }

   public MutableComponent withColor(int p_312961_) {
      this.setStyle(this.getStyle().withColor(p_312961_));
      return this;
   }

   public MutableComponent withoutShadow() {
      this.setStyle(this.getStyle().withoutShadow());
      return this;
   }

   @Override
   public FormattedCharSequence getVisualOrderText() {
      Language language = Language.getInstance();
      if (this.decomposedWith != language) {
         this.visualOrderText = language.getVisualOrder(this);
         this.decomposedWith = language;
      }

      return this.visualOrderText;
   }

   @Override
   public boolean equals(Object p_237209_) {
      return this == p_237209_
         ? true
         : p_237209_ instanceof MutableComponent mutablecomponent
            && this.contents.equals(mutablecomponent.contents)
            && this.style.equals(mutablecomponent.style)
            && this.siblings.equals(mutablecomponent.siblings);
   }

   @Override
   public int hashCode() {
      int i = 1;
      i = 31 * i + this.contents.hashCode();
      i = 31 * i + this.style.hashCode();
      return 31 * i + this.siblings.hashCode();
   }

   @Override
   public String toString() {
      StringBuilder stringbuilder = new StringBuilder(this.contents.toString());
      boolean flag = !this.style.isEmpty();
      boolean flag1 = !this.siblings.isEmpty();
      if (flag || flag1) {
         stringbuilder.append('[');
         if (flag) {
            stringbuilder.append("style=");
            stringbuilder.append(this.style);
         }

         if (flag && flag1) {
            stringbuilder.append(", ");
         }

         if (flag1) {
            stringbuilder.append("siblings=");
            stringbuilder.append(this.siblings);
         }

         stringbuilder.append(']');
      }

      return stringbuilder.toString();
   }
}
