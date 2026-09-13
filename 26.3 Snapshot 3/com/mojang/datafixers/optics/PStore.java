package com.mojang.datafixers.optics;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Functor;
import com.mojang.datafixers.kinds.K1;
import java.util.function.Function;

interface PStore<I, J, X> extends App<PStore.Mu<I, J>, X> {
   static <I, J, X> PStore<I, J, X> unbox(App<PStore.Mu<I, J>, X> box) {
      return (PStore<I, J, X>)box;
   }

   X peek(J var1);

   I pos();

   final class Instance<I, J> implements Functor<PStore.Mu<I, J>, PStore.Instance.Mu<I, J>> {
      @Override
      public <T, R> App<PStore.Mu<I, J>, R> map(Function<? super T, ? extends R> func, App<PStore.Mu<I, J>, T> ts) {
         PStore<I, J, T> input = PStore.unbox(ts);
         return Optics.pStore(func.compose(input::peek)::apply, input::pos);
      }

      public static final class Mu<I, J> implements Functor.Mu {
      }
   }

   final class Mu<I, J> implements K1 {
   }
}
