package net.minecraft.util.parsing.packrat;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;

public class Dictionary<S> {
   private final Map<Atom<?>, Dictionary.Entry<S, ?>> terms = new IdentityHashMap<>();

   public <T> NamedRule<S, T> put(Atom<T> p_333993_, Rule<S, T> p_397298_) {
      Dictionary.Entry<S, T> entry = (Dictionary.Entry<S, T>)this.terms.computeIfAbsent(p_333993_, Dictionary.Entry::new);
      if (entry.value != null) {
         throw new IllegalArgumentException("Trying to override rule: " + p_333993_);
      }

      entry.value = p_397298_;
      return entry;
   }

   public <T> NamedRule<S, T> putComplex(Atom<T> p_393852_, Term<S> p_391921_, Rule.RuleAction<S, T> p_393539_) {
      return this.put(p_393852_, Rule.fromTerm(p_391921_, p_393539_));
   }

   public <T> NamedRule<S, T> put(Atom<T> p_329080_, Term<S> p_392956_, Rule.SimpleRuleAction<S, T> p_396305_) {
      return this.put(p_329080_, Rule.fromTerm(p_392956_, p_396305_));
   }

   public void checkAllBound() {
      List<? extends Atom<?>> list = this.terms.entrySet().stream().filter(p_449344_ -> p_449344_.getValue().value == null).map(Map.Entry::getKey).toList();
      if (!list.isEmpty()) {
         throw new IllegalStateException("Unbound names: " + list);
      }
   }

   public <T> NamedRule<S, T> getOrThrow(Atom<T> p_397598_) {
      return (NamedRule<S, T>)Objects.requireNonNull(this.terms.get(p_397598_), () -> "No rule called " + p_397598_);
   }

   public <T> NamedRule<S, T> forward(Atom<T> p_392500_) {
      return this.getOrCreateEntry(p_392500_);
   }

   private <T> Dictionary.Entry<S, T> getOrCreateEntry(Atom<T> p_395883_) {
      return (Dictionary.Entry<S, T>)this.terms.computeIfAbsent(p_395883_, Dictionary.Entry::new);
   }

   public <T> Term<S> named(Atom<T> p_392444_) {
      return new Dictionary.Reference<>(this.getOrCreateEntry(p_392444_), p_392444_);
   }

   public <T> Term<S> namedWithAlias(Atom<T> p_396057_, Atom<T> p_391365_) {
      return new Dictionary.Reference<>(this.getOrCreateEntry(p_396057_), p_391365_);
   }

   static class Entry<S, T> implements NamedRule<S, T>, Supplier<String> {
      private final Atom<T> name;
      @Nullable Rule<S, T> value;

      private Entry(Atom<T> p_396611_) {
         this.name = p_396611_;
      }

      @Override
      public Atom<T> name() {
         return this.name;
      }

      @Override
      public Rule<S, T> value() {
         return Objects.requireNonNull(this.value, this);
      }

      public String get() {
         return "Unbound rule " + this.name;
      }
   }

   record Reference<S, T>(Dictionary.Entry<S, T> ruleToParse, Atom<T> nameToStore) implements Term<S> {
      @Override
      public boolean parse(ParseState<S> p_397182_, Scope p_391380_, Control p_391695_) {
         T t = p_397182_.parse(this.ruleToParse);
         if (t == null) {
            return false;
         }

         p_391380_.put(this.nameToStore, t);
         return true;
      }
   }
}
