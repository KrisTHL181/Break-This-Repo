package net.minecraft.util.parsing.packrat;

import java.util.Optional;
import org.jspecify.annotations.Nullable;

public interface ParseState<S> {
   Scope scope();

   ErrorCollector<S> errorCollector();

   default <T> Optional<T> parseTopRule(NamedRule<S, T> p_397823_) {
      T t = this.parse(p_397823_);
      if (t != null) {
         this.errorCollector().finish(this.mark());
      }

      if (!this.scope().hasOnlySingleFrame()) {
         throw new IllegalStateException("Malformed scope: " + this.scope());
      } else {
         return Optional.ofNullable(t);
      }
   }

   <T> @Nullable T parse(NamedRule<S, T> var1);

   S input();

   int mark();

   void restore(int var1);

   Control acquireControl();

   void releaseControl();

   ParseState<S> silent();
}
