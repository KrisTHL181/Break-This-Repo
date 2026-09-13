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

   ContextMap(Map<ContextKey<?>, Object> p_362494_) {
      this.params = p_362494_;
   }

   public boolean has(ContextKey<?> p_365248_) {
      return this.params.containsKey(p_365248_);
   }

   public <T> T getOrThrow(ContextKey<T> p_366932_) {
      T t = (T)this.params.get(p_366932_);
      if (t == null) {
         throw new NoSuchElementException(p_366932_.name().toString());
      } else {
         return t;
      }
   }

   public <T> @Nullable T getOptional(ContextKey<T> p_362151_) {
      return (T)this.params.get(p_362151_);
   }

   @Contract("_,!null->!null; _,_->_")
   public <T> @Nullable T getOrDefault(ContextKey<T> p_368195_, @Nullable T p_368521_) {
      return (T)this.params.getOrDefault(p_368195_, p_368521_);
   }

   public static class Builder {
      private final Map<ContextKey<?>, Object> params = new IdentityHashMap<>();

      public <T> ContextMap.Builder withParameter(ContextKey<T> p_369289_, T p_362485_) {
         this.params.put(p_369289_, p_362485_);
         return this;
      }

      public <T> ContextMap.Builder withOptionalParameter(ContextKey<T> p_366888_, @Nullable T p_366368_) {
         if (p_366368_ == null) {
            this.params.remove(p_366888_);
         } else {
            this.params.put(p_366888_, p_366368_);
         }

         return this;
      }

      public <T> T getParameter(ContextKey<T> p_363188_) {
         T t = (T)this.params.get(p_363188_);
         if (t == null) {
            throw new NoSuchElementException(p_363188_.name().toString());
         } else {
            return t;
         }
      }

      public <T> @Nullable T getOptionalParameter(ContextKey<T> p_362877_) {
         return (T)this.params.get(p_362877_);
      }

      public ContextMap create(ContextKeySet p_369803_) {
         Set<ContextKey<?>> set = Sets.difference(this.params.keySet(), p_369803_.allowed());
         if (!set.isEmpty()) {
            throw new IllegalArgumentException("Parameters not allowed in this parameter set: " + set);
         } else {
            Set<ContextKey<?>> set1 = Sets.difference(p_369803_.required(), this.params.keySet());
            if (!set1.isEmpty()) {
               throw new IllegalArgumentException("Missing required parameters: " + set1);
            } else {
               return new ContextMap(this.params);
            }
         }
      }
   }
}
