package com.mojang.datafixers.optics;

import com.mojang.datafixers.util.Either;

public final class Inj2<F, G, G2> implements Prism<Either<F, G>, Either<F, G2>, G, G2> {
   public static final Inj2<?, ?, ?> INSTANCE = new Inj2();

   private Inj2() {
   }

   public Either<Either<F, G2>, G> match(Either<F, G> either) {
      return either.map(f -> Either.left(Either.left((F)f)), Either::right);
   }

   public Either<F, G2> build(G2 g2) {
      return Either.right(g2);
   }

   @Override
   public String toString() {
      return "inj2";
   }
}
