package net.minecraft.util.context;

import com.google.common.collect.Sets;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.Nullable;

public class ContextMap {
   private final Map<ContextKey<?>, Object> params;

   private ContextMap(final Map<ContextKey<?>, Object> params) {
      this.params = params;
   }

   public boolean has(final ContextKey<?> key) {
      return this.params.containsKey(key);
   }

   public <T> T getOrThrow(final ContextKey<T> key) {
      T value = (T)this.params.get(key);
      if (value == null) {
         throw new NoSuchElementException(key.name().toString());
      } else {
         return value;
      }
   }

   public <T> @Nullable T getOptional(final ContextKey<T> key) {
      return (T)this.params.get(key);
   }

   @Contract("_,!null->!null; _,_->_")
   public <T> @Nullable T getOrDefault(final ContextKey<T> param, final @Nullable T _default) {
      return (T)this.params.getOrDefault(param, _default);
   }

   public static class Builder {
      private final Map<ContextKey<?>, Object> params = new IdentityHashMap<>();

      public <T> ContextMap.Builder withParameter(final ContextKey<T> param, final T value) {
         this.params.put(param, value);
         return this;
      }

      public <T> ContextMap.Builder withOptionalParameter(final ContextKey<T> param, final @Nullable T value) {
         if (value == null) {
            this.params.remove(param);
         } else {
            this.params.put(param, value);
         }

         return this;
      }

      public <T> T getParameter(final ContextKey<T> param) {
         T value = (T)this.params.get(param);
         if (value == null) {
            throw new NoSuchElementException(param.name().toString());
         } else {
            return value;
         }
      }

      public <T> @Nullable T getOptionalParameter(final ContextKey<T> param) {
         return (T)this.params.get(param);
      }

      public ContextMap create(final ContextKeySet paramSet) {
         Set<ContextKey<?>> notAllowed = Sets.difference(this.params.keySet(), paramSet.allowed());
         if (!notAllowed.isEmpty()) {
            throw new IllegalArgumentException("Parameters not allowed in this parameter set: " + notAllowed);
         } else {
            Set<ContextKey<?>> missingRequired = Sets.difference(paramSet.required(), this.params.keySet());
            if (!missingRequired.isEmpty()) {
               throw new IllegalArgumentException("Missing required parameters: " + missingRequired);
            } else {
               return new ContextMap(this.params);
            }
         }
      }
   }
}
