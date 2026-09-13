package com.mojang.datafixers.optics.profunctors;

import com.mojang.datafixers.FunctionType;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.App2;
import com.mojang.datafixers.kinds.Functor;
import com.mojang.datafixers.kinds.K1;
import com.mojang.datafixers.kinds.K2;
import java.util.function.Function;

public class ProfunctorFunctorWrapper<P extends K2, F extends K1, G extends K1, A, B> implements App2<ProfunctorFunctorWrapper.Mu<P, F, G>, A, B> {
   private final App2<P, App<F, A>, App<G, B>> value;

   public static <P extends K2, F extends K1, G extends K1, A, B> ProfunctorFunctorWrapper<P, F, G, A, B> unbox(
      App2<ProfunctorFunctorWrapper.Mu<P, F, G>, A, B> box
   ) {
      return (ProfunctorFunctorWrapper<P, F, G, A, B>)box;
   }

   public ProfunctorFunctorWrapper(App2<P, App<F, A>, App<G, B>> value) {
      this.value = value;
   }

   public App2<P, App<F, A>, App<G, B>> value() {
      return this.value;
   }

   public static final class Instance<P extends K2, F extends K1, G extends K1>
      implements Profunctor<ProfunctorFunctorWrapper.Mu<P, F, G>, ProfunctorFunctorWrapper.Instance.Mu>,
      App<ProfunctorFunctorWrapper.Instance.Mu, ProfunctorFunctorWrapper.Mu<P, F, G>> {
      private final Profunctor<P, ? extends Profunctor.Mu> profunctor;
      private final Functor<F, ?> fFunctor;
      private final Functor<G, ?> gFunctor;

      public Instance(App<? extends Profunctor.Mu, P> proof, Functor<F, ?> fFunctor, Functor<G, ?> gFunctor) {
         this.profunctor = Profunctor.unbox(proof);
         this.fFunctor = fFunctor;
         this.gFunctor = gFunctor;
      }

      @Override
      public <A, B, C, D> FunctionType<App2<ProfunctorFunctorWrapper.Mu<P, F, G>, A, B>, App2<ProfunctorFunctorWrapper.Mu<P, F, G>, C, D>> dimap(
         Function<C, A> g, Function<B, D> h
      ) {
         return input -> {
            App2<P, App<F, A>, App<G, B>> value = ProfunctorFunctorWrapper.unbox(input).value();
            App2<P, App<F, C>, App<G, D>> newValue = this.profunctor.dimap(value, c -> this.fFunctor.map(g, c), b -> this.gFunctor.map(h, b));
            return new ProfunctorFunctorWrapper<>(newValue);
         };
      }

      public static final class Mu implements Profunctor.Mu {
      }
   }

   public static final class Mu<P extends K2, F extends K1, G extends K1> implements K2 {
   }
}
