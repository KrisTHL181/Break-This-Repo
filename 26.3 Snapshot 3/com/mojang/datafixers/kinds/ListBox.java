package com.mojang.datafixers.kinds;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class ListBox<T> implements App<ListBox.Mu, T> {
   private final List<T> value;

   public static <T> List<T> unbox(App<ListBox.Mu, T> box) {
      return ((ListBox)box).value;
   }

   public static <T> ListBox<T> create(List<T> value) {
      return new ListBox<>(value);
   }

   private ListBox(List<T> value) {
      this.value = value;
   }

   // ===== 修改：移除错误的强制转换 =====
   public static <F extends K1, A, B> App<F, List<B>> traverse(Applicative<F, ?> applicative, Function<A, App<F, B>> function, List<A> input) {
      return applicative.map(ListBox::unbox, ListBox.Instance.INSTANCE.traverse(applicative, function, create(input)));
   }

   // ===== 修改：移除错误的强制转换 =====
   public static <F extends K1, A> App<F, List<A>> flip(Applicative<F, ?> applicative, List<App<F, A>> input) {
      return applicative.map(ListBox::unbox, ListBox.Instance.INSTANCE.flip(applicative, create(input)));
   }

   public enum Instance implements Traversable<ListBox.Mu, ListBox.Instance.Mu> {
      INSTANCE;

      @Override
      public <T, R> App<ListBox.Mu, R> map(Function<? super T, ? extends R> func, App<ListBox.Mu, T> ts) {
         return ListBox.create(ListBox.unbox(ts).stream().map(func).collect(Collectors.toList()));
      }

      @Override
      public <F extends K1, A, B> App<F, App<ListBox.Mu, B>> traverse(Applicative<F, ?> applicative, Function<A, App<F, B>> function, App<ListBox.Mu, A> input) {
         List<? extends A> list = ListBox.unbox(input);
         App<F, Builder<B>> result = applicative.point(ImmutableList.builder());

         for (A a : list) {
            App<F, B> fb = function.apply(a);
            result = applicative.ap2(applicative.point(Builder::add), result, fb);
         }

         return applicative.map(b -> ListBox.create(b.build()), result);
      }

      public static final class Mu implements Traversable.Mu {
      }
   }

   public static final class Mu implements K1 {
   }
}