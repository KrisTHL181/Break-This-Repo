package net.minecraft.util.parsing.packrat;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

public interface ErrorCollector<S> {
   void store(int var1, SuggestionSupplier<S> var2, Object var3);

   default void store(int p_330627_, Object p_332187_) {
      this.store(p_330627_, SuggestionSupplier.empty(), p_332187_);
   }

   void finish(int var1);

   class LongestOnly<S> implements ErrorCollector<S> {
      private ErrorCollector.LongestOnly.@Nullable MutableErrorEntry<S>[] entries = new ErrorCollector.LongestOnly.MutableErrorEntry[16];
      private int nextErrorEntry;
      private int lastCursor = -1;

      private void discardErrorsFromShorterParse(int p_331637_) {
         if (p_331637_ > this.lastCursor) {
            this.lastCursor = p_331637_;
            this.nextErrorEntry = 0;
         }
      }

      @Override
      public void finish(int p_334009_) {
         this.discardErrorsFromShorterParse(p_334009_);
      }

      @Override
      public void store(int p_331115_, SuggestionSupplier<S> p_329965_, Object p_332125_) {
         this.discardErrorsFromShorterParse(p_331115_);
         if (p_331115_ == this.lastCursor) {
            this.addErrorEntry(p_329965_, p_332125_);
         }
      }

      private void addErrorEntry(SuggestionSupplier<S> p_397931_, Object p_397847_) {
         int i = this.entries.length;
         if (this.nextErrorEntry >= i) {
            int j = Util.growByHalf(i, this.nextErrorEntry + 1);
            ErrorCollector.LongestOnly.MutableErrorEntry<S>[] mutableerrorentry = new ErrorCollector.LongestOnly.MutableErrorEntry[j];
            System.arraycopy(this.entries, 0, mutableerrorentry, 0, i);
            this.entries = mutableerrorentry;
         }

         int k = this.nextErrorEntry++;
         ErrorCollector.LongestOnly.MutableErrorEntry<S> mutableerrorentry1 = this.entries[k];
         if (mutableerrorentry1 == null) {
            mutableerrorentry1 = new ErrorCollector.LongestOnly.MutableErrorEntry<>();
            this.entries[k] = mutableerrorentry1;
         }

         mutableerrorentry1.suggestions = p_397931_;
         mutableerrorentry1.reason = p_397847_;
      }

      public List<ErrorEntry<S>> entries() {
         int i = this.nextErrorEntry;
         if (i == 0) {
            return List.of();
         }

         List<ErrorEntry<S>> list = new ArrayList<>(i);

         for (int j = 0; j < i; j++) {
            ErrorCollector.LongestOnly.MutableErrorEntry<S> mutableerrorentry = this.entries[j];
            list.add(new ErrorEntry<>(this.lastCursor, mutableerrorentry.suggestions, mutableerrorentry.reason));
         }

         return list;
      }

      public int cursor() {
         return this.lastCursor;
      }

      static class MutableErrorEntry<S> {
         SuggestionSupplier<S> suggestions = SuggestionSupplier.empty();
         Object reason = "empty";
      }
   }

   class Nop<S> implements ErrorCollector<S> {
      @Override
      public void store(int p_393771_, SuggestionSupplier<S> p_392237_, Object p_393518_) {
      }

      @Override
      public void finish(int p_396049_) {
      }
   }
}
