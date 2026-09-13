package net.minecraft.util;

import java.util.Objects;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;

public class SingleKeyCache<K, V> {
   private final Function<K, V> computeValue;
   private @Nullable K cacheKey = (K)null;
   private @Nullable V cachedValue;

   public SingleKeyCache(Function<K, V> p_270132_) {
      this.computeValue = p_270132_;
   }

   public V getValue(K p_270953_) {
      if (this.cachedValue == null || !Objects.equals(this.cacheKey, p_270953_)) {
         this.cachedValue = this.computeValue.apply(p_270953_);
         this.cacheKey = p_270953_;
      }

      return this.cachedValue;
   }
}
