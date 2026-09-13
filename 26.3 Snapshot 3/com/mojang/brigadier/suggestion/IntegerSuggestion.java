package com.mojang.brigadier.suggestion;

import com.mojang.brigadier.Message;
import com.mojang.brigadier.context.StringRange;
import java.util.Objects;

public class IntegerSuggestion extends Suggestion {
   private int value;

   public IntegerSuggestion(StringRange range, int value) {
      this(range, value, null);
   }

   public IntegerSuggestion(StringRange range, int value, Message tooltip) {
      super(range, Integer.toString(value), tooltip);
      this.value = value;
   }

   public int getValue() {
      return this.value;
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      }

      if (!(o instanceof IntegerSuggestion)) {
         return false;
      }

      IntegerSuggestion that = (IntegerSuggestion)o;
      return this.value == that.value && super.equals(o);
   }

   @Override
   public int hashCode() {
      return Objects.hash(super.hashCode(), this.value);
   }

   @Override
   public String toString() {
      return "IntegerSuggestion{value="
         + this.value
         + ", range="
         + this.getRange()
         + ", text='"
         + this.getText()
         + '\''
         + ", tooltip='"
         + this.getTooltip()
         + '\''
         + '}';
   }

   @Override
   public int compareTo(Suggestion o) {
      return o instanceof IntegerSuggestion ? Integer.compare(this.value, ((IntegerSuggestion)o).value) : super.compareTo(o);
   }

   @Override
   public int compareToIgnoreCase(Suggestion b) {
      return this.compareTo(b);
   }
}
