package net.minecraft.util.context;

import com.google.common.base.Joiner;
import com.google.common.collect.Sets;
import java.util.Set;

public class ContextKeySet {
   private final Set<ContextKey<?>> required;
   private final Set<ContextKey<?>> allowed;

   ContextKeySet(Set<ContextKey<?>> p_366050_, Set<ContextKey<?>> p_362785_) {
      this.required = Set.copyOf(p_366050_);
      this.allowed = Set.copyOf(Sets.union(p_366050_, p_362785_));
   }

   public Set<ContextKey<?>> required() {
      return this.required;
   }

   public Set<ContextKey<?>> allowed() {
      return this.allowed;
   }

   @Override
   public String toString() {
      return "["
         + Joiner.on(", ").join(this.allowed.stream().map(p_449285_ -> (this.required.contains(p_449285_) ? "!" : "") + p_449285_.name()).iterator())
         + "]";
   }

   public static class Builder {
      private final Set<ContextKey<?>> required = Sets.newIdentityHashSet();
      private final Set<ContextKey<?>> optional = Sets.newIdentityHashSet();

      public ContextKeySet.Builder required(ContextKey<?> p_365799_) {
         if (this.optional.contains(p_365799_)) {
            throw new IllegalArgumentException("Parameter " + p_365799_.name() + " is already optional");
         }

         this.required.add(p_365799_);
         return this;
      }

      public ContextKeySet.Builder optional(ContextKey<?> p_361328_) {
         if (this.required.contains(p_361328_)) {
            throw new IllegalArgumentException("Parameter " + p_361328_.name() + " is already required");
         }

         this.optional.add(p_361328_);
         return this;
      }

      public ContextKeySet build() {
         return new ContextKeySet(this.required, this.optional);
      }
   }
}
