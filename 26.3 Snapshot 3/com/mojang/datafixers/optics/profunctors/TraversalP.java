package com.mojang.datafixers.optics.profunctors;

import com.google.common.reflect.TypeToken;
import com.mojang.datafixers.FunctionType;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.App2;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.datafixers.kinds.K1;
import com.mojang.datafixers.kinds.K2;
import com.mojang.datafixers.kinds.Traversable;
import com.mojang.datafixers.optics.Wander;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;

public interface TraversalP<P extends K2, Mu extends TraversalP.Mu> extends AffineP<P, Mu> {
   static <P extends K2, Proof extends TraversalP.Mu> TraversalP<P, Proof> unbox(App<Proof, P> proofBox) {
      return (TraversalP<P, Proof>)proofBox;
   }

   <S, T, A, B> App2<P, S, T> wander(Wander<S, T, A, B> var1, App2<P, A, B> var2);

   default <T extends K1, A, B> App2<P, App<T, A>, App<T, B>> traverse(final Traversable<T, ?> traversable, App2<P, A, B> input) {
      return this.wander(new Wander<App<T, A>, App<T, B>, A, B>() {
         @Override
         public <F extends K1> FunctionType<App<T, A>, App<F, App<T, B>>> wander(Applicative<F, ?> applicative, FunctionType<A, App<F, B>> function) {
            return ta -> traversable.traverse(applicative, function, ta);
         }
      }, input);
   }

   @Override
   default <A, B, C> App2<P, Pair<A, C>, Pair<B, C>> first(App2<P, A, B> input) {
      return this.dimap(this.traverse(new Pair.Instance(), input), box -> box, Pair::unbox);
   }

   @Override
   default <A, B, C> App2<P, Either<A, C>, Either<B, C>> left(App2<P, A, B> input) {
      return this.dimap(this.traverse(new Either.Instance(), input), box -> box, Either::unbox);
   }

   default FunctorProfunctor<Traversable.Mu, P, FunctorProfunctor.Mu<Traversable.Mu>> toFP3() {
      return new FunctorProfunctor<Traversable.Mu, P, FunctorProfunctor.Mu<Traversable.Mu>>() {
         @Override
         public <A, B, F extends K1> App2<P, App<F, A>, App<F, B>> distribute(App<? extends Traversable.Mu, F> proof, App2<P, A, B> input) {
            return TraversalP.this.traverse(Traversable.unbox(proof), input);
         }
      };
   }

   interface Mu extends AffineP.Mu {
      TypeToken<TraversalP.Mu> TYPE_TOKEN = new TypeToken<TraversalP.Mu>() {};
   }
}
