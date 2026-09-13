package net.minecraft.util.parsing.packrat;

import java.util.ArrayList;
import java.util.List;

public interface Term<S> {
   boolean parse(ParseState<S> var1, Scope var2, Control var3);

   static <S, T> Term<S> marker(Atom<T> p_333477_, T p_335010_) {
      return new Term.Marker<>(p_333477_, p_335010_);
   }

   @SafeVarargs
   static <S> Term<S> sequence(Term<S>... p_331306_) {
      return new Term.Sequence<>(p_331306_);
   }

   @SafeVarargs
   static <S> Term<S> alternative(Term<S>... p_334441_) {
      return new Term.Alternative<>(p_334441_);
   }

   static <S> Term<S> optional(Term<S> p_335256_) {
      return new Term.Maybe<>(p_335256_);
   }

   static <S, T> Term<S> repeated(NamedRule<S, T> p_391997_, Atom<List<T>> p_395278_) {
      return repeated(p_391997_, p_395278_, 0);
   }

   static <S, T> Term<S> repeated(NamedRule<S, T> p_394090_, Atom<List<T>> p_396235_, int p_397596_) {
      return new Term.Repeated<>(p_394090_, p_396235_, p_397596_);
   }

   static <S, T> Term<S> repeatedWithTrailingSeparator(NamedRule<S, T> p_395582_, Atom<List<T>> p_391503_, Term<S> p_394717_) {
      return repeatedWithTrailingSeparator(p_395582_, p_391503_, p_394717_, 0);
   }

   static <S, T> Term<S> repeatedWithTrailingSeparator(NamedRule<S, T> p_397489_, Atom<List<T>> p_392214_, Term<S> p_395301_, int p_391759_) {
      return new Term.RepeatedWithSeparator<>(p_397489_, p_392214_, p_395301_, p_391759_, true);
   }

   static <S, T> Term<S> repeatedWithoutTrailingSeparator(NamedRule<S, T> p_392740_, Atom<List<T>> p_397250_, Term<S> p_393470_) {
      return repeatedWithoutTrailingSeparator(p_392740_, p_397250_, p_393470_, 0);
   }

   static <S, T> Term<S> repeatedWithoutTrailingSeparator(NamedRule<S, T> p_393071_, Atom<List<T>> p_395445_, Term<S> p_395475_, int p_392015_) {
      return new Term.RepeatedWithSeparator<>(p_393071_, p_395445_, p_395475_, p_392015_, false);
   }

   static <S> Term<S> positiveLookahead(Term<S> p_395551_) {
      return new Term.LookAhead<>(p_395551_, true);
   }

   static <S> Term<S> negativeLookahead(Term<S> p_395059_) {
      return new Term.LookAhead<>(p_395059_, false);
   }

   static <S> Term<S> cut() {
      return new Term<S>() {
         @Override
         public boolean parse(ParseState<S> p_333527_, Scope p_336097_, Control p_335047_) {
            p_335047_.cut();
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
         public boolean parse(ParseState<S> p_328418_, Scope p_332040_, Control p_328784_) {
            return true;
         }

         @Override
         public String toString() {
            return "ε";
         }
      };
   }

   static <S> Term<S> fail(final Object p_396725_) {
      return new Term<S>() {
         @Override
         public boolean parse(ParseState<S> p_394241_, Scope p_396858_, Control p_393969_) {
            p_394241_.errorCollector().store(p_394241_.mark(), p_396725_);
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
      public boolean parse(ParseState<S> p_328094_, Scope p_331753_, Control p_334626_) {
         Control control = p_328094_.acquireControl();

         try {
            int i = p_328094_.mark();
            p_331753_.splitFrame();

            for (Term<S> term : this.elements) {
               if (term.parse(p_328094_, p_331753_, control)) {
                  p_331753_.mergeFrame();
                  return true;
               }

               p_331753_.clearFrameValues();
               p_328094_.restore(i);
               if (control.hasCut()) {
                  break;
               }
            }

            p_331753_.popFrame();
            return false;
         } finally {
            p_328094_.releaseControl();
         }
      }
   }

   record LookAhead<S>(Term<S> term, boolean positive) implements Term<S> {
      @Override
      public boolean parse(ParseState<S> p_391493_, Scope p_395827_, Control p_391882_) {
         int i = p_391493_.mark();
         boolean flag = this.term.parse(p_391493_.silent(), p_395827_, p_391882_);
         p_391493_.restore(i);
         return this.positive == flag;
      }
   }

   record Marker<S, T>(Atom<T> name, T value) implements Term<S> {
      @Override
      public boolean parse(ParseState<S> p_332878_, Scope p_331621_, Control p_334053_) {
         p_331621_.put(this.name, this.value);
         return true;
      }
   }

   record Maybe<S>(Term<S> term) implements Term<S> {
      @Override
      public boolean parse(ParseState<S> p_332001_, Scope p_329861_, Control p_331352_) {
         int i = p_332001_.mark();
         if (!this.term.parse(p_332001_, p_329861_, p_331352_)) {
            p_332001_.restore(i);
         }

         return true;
      }
   }

   record Repeated<S, T>(NamedRule<S, T> element, Atom<List<T>> listName, int minRepetitions) implements Term<S> {
      @Override
      public boolean parse(ParseState<S> p_393223_, Scope p_397132_, Control p_396901_) {
         int i = p_393223_.mark();
         List<T> list = new ArrayList<>(this.minRepetitions);

         while (true) {
            int j = p_393223_.mark();
            T t = p_393223_.parse(this.element);
            if (t == null) {
               p_393223_.restore(j);
               if (list.size() < this.minRepetitions) {
                  p_393223_.restore(i);
                  return false;
               } else {
                  p_397132_.put(this.listName, list);
                  return true;
               }
            }

            list.add(t);
         }
      }
   }

   record RepeatedWithSeparator<S, T>(NamedRule<S, T> element, Atom<List<T>> listName, Term<S> separator, int minRepetitions, boolean allowTrailingSeparator)
      implements Term<S> {
      @Override
      public boolean parse(ParseState<S> p_396935_, Scope p_392276_, Control p_393022_) {
         int i = p_396935_.mark();
         List<T> list = new ArrayList<>(this.minRepetitions);
         boolean flag = true;

         while (true) {
            int j = p_396935_.mark();
            if (!flag && !this.separator.parse(p_396935_, p_392276_, p_393022_)) {
               p_396935_.restore(j);
               break;
            }

            int k = p_396935_.mark();
            T t = p_396935_.parse(this.element);
            if (t == null) {
               if (flag) {
                  p_396935_.restore(k);
               } else {
                  if (!this.allowTrailingSeparator) {
                     p_396935_.restore(i);
                     return false;
                  }

                  p_396935_.restore(k);
               }
               break;
            }

            list.add(t);
            flag = false;
         }

         if (list.size() < this.minRepetitions) {
            p_396935_.restore(i);
            return false;
         } else {
            p_392276_.put(this.listName, list);
            return true;
         }
      }
   }

   record Sequence<S>(Term<S>[] elements) implements Term<S> {
      @Override
      public boolean parse(ParseState<S> p_330195_, Scope p_336361_, Control p_328798_) {
         int i = p_330195_.mark();

         for (Term<S> term : this.elements) {
            if (!term.parse(p_330195_, p_336361_, p_328798_)) {
               p_330195_.restore(i);
               return false;
            }
         }

         return true;
      }
   }
}
