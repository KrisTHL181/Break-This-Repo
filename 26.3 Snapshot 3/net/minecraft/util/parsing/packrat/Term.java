package net.minecraft.util.parsing.packrat;

import java.util.ArrayList;
import java.util.List;

public interface Term<S> {
   boolean parse(ParseState<S> state, Scope scope, Control control);

   static <S, T> Term<S> marker(final Atom<T> name, final T value) {
      return new Term.Marker<>(name, value);
   }

   @SafeVarargs
   static <S> Term<S> sequence(final Term<S>... terms) {
      return new Term.Sequence<>(terms);
   }

   @SafeVarargs
   static <S> Term<S> alternative(final Term<S>... terms) {
      return new Term.Alternative<>(terms);
   }

   static <S> Term<S> optional(final Term<S> term) {
      return new Term.Maybe<>(term);
   }

   static <S, T> Term<S> repeated(final NamedRule<S, T> element, final Atom<List<T>> listName) {
      return repeated(element, listName, 0);
   }

   static <S, T> Term<S> repeated(final NamedRule<S, T> element, final Atom<List<T>> listName, final int minRepetitions) {
      return new Term.Repeated<>(element, listName, minRepetitions);
   }

   static <S, T> Term<S> repeatedWithTrailingSeparator(final NamedRule<S, T> element, final Atom<List<T>> listName, final Term<S> separator) {
      return repeatedWithTrailingSeparator(element, listName, separator, 0);
   }

   static <S, T> Term<S> repeatedWithTrailingSeparator(
      final NamedRule<S, T> element, final Atom<List<T>> listName, final Term<S> separator, final int minRepetitions
   ) {
      return new Term.RepeatedWithSeparator<>(element, listName, separator, minRepetitions, true);
   }

   static <S, T> Term<S> repeatedWithoutTrailingSeparator(final NamedRule<S, T> element, final Atom<List<T>> listName, final Term<S> separator) {
      return repeatedWithoutTrailingSeparator(element, listName, separator, 0);
   }

   static <S, T> Term<S> repeatedWithoutTrailingSeparator(
      final NamedRule<S, T> element, final Atom<List<T>> listName, final Term<S> separator, final int minRepetitions
   ) {
      return new Term.RepeatedWithSeparator<>(element, listName, separator, minRepetitions, false);
   }

   static <S> Term<S> positiveLookahead(final Term<S> term) {
      return new Term.LookAhead<>(term, true);
   }

   static <S> Term<S> negativeLookahead(final Term<S> term) {
      return new Term.LookAhead<>(term, false);
   }

   static <S> Term<S> cut() {
      return new Term<S>() {
         @Override
         public boolean parse(final ParseState<S> state, final Scope scope, final Control control) {
            control.cut();
            return true;
         }

         @Override
         public String toString() {
            return "↑";
         }
      };
   }

   static <S> Term<S> empty() {
      return new Term<S>() {
         @Override
         public boolean parse(final ParseState<S> state, final Scope scope, final Control control) {
            return true;
         }

         @Override
         public String toString() {
            return "ε";
         }
      };
   }

   static <S> Term<S> fail(final Object message) {
      return new Term<S>() {
         @Override
         public boolean parse(final ParseState<S> state, final Scope scope, final Control control) {
            state.errorCollector().store(state.mark(), message);
            return false;
         }

         @Override
         public String toString() {
            return "fail";
         }
      };
   }

   record Alternative<S>(Term<S>[] elements) implements Term<S> {
      @Override
      public boolean parse(final ParseState<S> state, final Scope scope, final Control control) {
         Control controlForThis = state.acquireControl();

         try {
            int mark = state.mark();
            scope.splitFrame();

            for (Term<S> element : this.elements) {
               if (element.parse(state, scope, controlForThis)) {
                  scope.mergeFrame();
                  return true;
               }

               scope.clearFrameValues();
               state.restore(mark);
               if (controlForThis.hasCut()) {
                  break;
               }
            }

            scope.popFrame();
            return false;
         } finally {
            state.releaseControl();
         }
      }
   }

   record LookAhead<S>(Term<S> term, boolean positive) implements Term<S> {
      @Override
      public boolean parse(final ParseState<S> state, final Scope scope, final Control control) {
         int mark = state.mark();
         boolean result = this.term.parse(state.silent(), scope, control);
         state.restore(mark);
         return this.positive == result;
      }
   }

   record Marker<S, T>(Atom<T> name, T value) implements Term<S> {
      @Override
      public boolean parse(final ParseState<S> state, final Scope scope, final Control control) {
         scope.put(this.name, this.value);
         return true;
      }
   }

   record Maybe<S>(Term<S> term) implements Term<S> {
      @Override
      public boolean parse(final ParseState<S> state, final Scope scope, final Control control) {
         int mark = state.mark();
         if (!this.term.parse(state, scope, control)) {
            state.restore(mark);
         }

         return true;
      }
   }

   record Repeated<S, T>(NamedRule<S, T> element, Atom<List<T>> listName, int minRepetitions) implements Term<S> {
      @Override
      public boolean parse(final ParseState<S> state, final Scope scope, final Control control) {
         int mark = state.mark();
         List<T> elements = new ArrayList<>(this.minRepetitions);

         while (true) {
            int entryMark = state.mark();
            T parsedElement = state.parse(this.element);
            if (parsedElement == null) {
               state.restore(entryMark);
               if (elements.size() < this.minRepetitions) {
                  state.restore(mark);
                  return false;
               } else {
                  scope.put(this.listName, elements);
                  return true;
               }
            }

            elements.add(parsedElement);
         }
      }
   }

   record RepeatedWithSeparator<S, T>(NamedRule<S, T> element, Atom<List<T>> listName, Term<S> separator, int minRepetitions, boolean allowTrailingSeparator)
      implements Term<S> {
      @Override
      public boolean parse(final ParseState<S> state, final Scope scope, final Control control) {
         int listMark = state.mark();
         List<T> elements = new ArrayList<>(this.minRepetitions);
         boolean first = true;

         while (true) {
            int markBeforeSeparator = state.mark();
            if (!first && !this.separator.parse(state, scope, control)) {
               state.restore(markBeforeSeparator);
               break;
            }

            int markAfterSeparator = state.mark();
            T parsedElement = state.parse(this.element);
            if (parsedElement == null) {
               if (first) {
                  state.restore(markAfterSeparator);
               } else {
                  if (!this.allowTrailingSeparator) {
                     state.restore(listMark);
                     return false;
                  }

                  state.restore(markAfterSeparator);
               }
               break;
            }

            elements.add(parsedElement);
            first = false;
         }

         if (elements.size() < this.minRepetitions) {
            state.restore(listMark);
            return false;
         } else {
            scope.put(this.listName, elements);
            return true;
         }
      }
   }

   record Sequence<S>(Term<S>[] elements) implements Term<S> {
      @Override
      public boolean parse(final ParseState<S> state, final Scope scope, final Control control) {
         int mark = state.mark();

         for (Term<S> element : this.elements) {
            if (!element.parse(state, scope, control)) {
               state.restore(mark);
               return false;
            }
         }

         return true;
      }
   }
}
