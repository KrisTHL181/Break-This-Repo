package com.mojang.datafixers.optics;

import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import java.util.Objects;

public final class InjTagged<K, A, B> implements Prism<Pair<K, ?>, Pair<K, ?>, A, B> {
   private final K key;

   public InjTagged(K key) {
      this.key = key;
   }

   public Either<Pair<K, ?>, A> match(Pair<K, ?> pair) {
      return Objects.equals(this.key, pair.getFirst()) ? Either.right((A)pair.getSecond()) : Either.left(pair);
   }

   public Pair<K, ?> build(B b) {
      return Pair.of(this.key, b);
   }

   @Override
   public String toString() {
      return "inj[" + this.key + "]";
   }

   @Override
   public boolean equals(Object obj) {
      return obj instanceof InjTagged && Objects.equals(((InjTagged)obj).key, this.key);
   }

   @Override
   public int hashCode() {
      return this.key.hashCode();
   }
}
