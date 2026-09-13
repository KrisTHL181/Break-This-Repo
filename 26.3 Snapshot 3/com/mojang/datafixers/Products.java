package com.mojang.datafixers;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.datafixers.kinds.IdF;
import com.mojang.datafixers.kinds.K1;
import com.mojang.datafixers.util.Function10;
import com.mojang.datafixers.util.Function11;
import com.mojang.datafixers.util.Function12;
import com.mojang.datafixers.util.Function13;
import com.mojang.datafixers.util.Function14;
import com.mojang.datafixers.util.Function15;
import com.mojang.datafixers.util.Function16;
import com.mojang.datafixers.util.Function3;
import com.mojang.datafixers.util.Function4;
import com.mojang.datafixers.util.Function5;
import com.mojang.datafixers.util.Function6;
import com.mojang.datafixers.util.Function7;
import com.mojang.datafixers.util.Function8;
import com.mojang.datafixers.util.Function9;
import java.util.function.BiFunction;
import java.util.function.Function;

public interface Products {
   static <T1, T2> Products.P2<IdF.Mu, T1, T2> of(T1 t1, T2 t2) {
      return new Products.P2<>(IdF.create(t1), IdF.create(t2));
   }

   record P1<F extends K1, T1>(App<F, T1> t1) {
      public <T2> Products.P2<F, T1, T2> and(App<F, T2> t2) {
         return new Products.P2<>(this.t1, t2);
      }

      public <T2, T3> Products.P3<F, T1, T2, T3> and(Products.P2<F, T2, T3> p) {
         return new Products.P3<>(this.t1, p.t1, p.t2);
      }

      public <T2, T3, T4> Products.P4<F, T1, T2, T3, T4> and(Products.P3<F, T2, T3, T4> p) {
         return new Products.P4<>(this.t1, p.t1, p.t2, p.t3);
      }

      public <T2, T3, T4, T5> Products.P5<F, T1, T2, T3, T4, T5> and(Products.P4<F, T2, T3, T4, T5> p) {
         return new Products.P5<>(this.t1, p.t1, p.t2, p.t3, p.t4);
      }

      public <T2, T3, T4, T5, T6> Products.P6<F, T1, T2, T3, T4, T5, T6> and(Products.P5<F, T2, T3, T4, T5, T6> p) {
         return new Products.P6<>(this.t1, p.t1, p.t2, p.t3, p.t4, p.t5);
      }

      public <T2, T3, T4, T5, T6, T7> Products.P7<F, T1, T2, T3, T4, T5, T6, T7> and(Products.P6<F, T2, T3, T4, T5, T6, T7> p) {
         return new Products.P7<>(this.t1, p.t1, p.t2, p.t3, p.t4, p.t5, p.t6);
      }

      public <T2, T3, T4, T5, T6, T7, T8> Products.P8<F, T1, T2, T3, T4, T5, T6, T7, T8> and(Products.P7<F, T2, T3, T4, T5, T6, T7, T8> p) {
         return new Products.P8<>(this.t1, p.t1, p.t2, p.t3, p.t4, p.t5, p.t6, p.t7);
      }

      public <T2, T3, T4, T5, T6, T7, T8, T9> Products.P9<F, T1, T2, T3, T4, T5, T6, T7, T8, T9> and(Products.P8<F, T2, T3, T4, T5, T6, T7, T8, T9> p) {
         return new Products.P9<>(this.t1, p.t1, p.t2, p.t3, p.t4, p.t5, p.t6, p.t7, p.t8);
      }

      public <T2, T3, T4, T5, T6, T7, T8, T9, T10> Products.P10<F, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> and(
         Products.P9<F, T2, T3, T4, T5, T6, T7, T8, T9, T10> p
      ) {
         return new Products.P10<>(this.t1, p.t1, p.t2, p.t3, p.t4, p.t5, p.t6, p.t7, p.t8, p.t9);
      }

      public <T2, T3, T4, T5, T6, T7, T8, T9, T10, T11> Products.P11<F, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11> and(
         Products.P10<F, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11> p
      ) {
         return new Products.P11<>(this.t1, p.t1, p.t2, p.t3, p.t4, p.t5, p.t6, p.t7, p.t8, p.t9, p.t10);
      }

      public <T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12> Products.P12<F, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12> and(
         Products.P11<F, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12> p
      ) {
         return new Products.P12<>(this.t1, p.t1, p.t2, p.t3, p.t4, p.t5, p.t6, p.t7, p.t8, p.t9, p.t10, p.t11);
      }

      public <T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13> Products.P13<F, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13> and(
         Products.P12<F, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13> p
      ) {
         return new Products.P13<>(this.t1, p.t1, p.t2, p.t3, p.t4, p.t5, p.t6, p.t7, p.t8, p.t9, p.t10, p.t11, p.t12);
      }

      public <T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14> Products.P14<F, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14> and(
         Products.P13<F, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14> p
      ) {
         return new Products.P14<>(this.t1, p.t1, p.t2, p.t3, p.t4, p.t5, p.t6, p.t7, p.t8, p.t9, p.t10, p.t11, p.t12, p.t13);
      }

      public <T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15> Products.P15<F, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15> and(
         Products.P14<F, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15> p
      ) {
         return new Products.P15<>(this.t1, p.t1, p.t2, p.t3, p.t4, p.t5, p.t6, p.t7, p.t8, p.t9, p.t10, p.t11, p.t12, p.t13, p.t14);
      }

      public <T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16> Products.P16<F, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16> and(
         Products.P15<F, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16> p
      ) {
         return new Products.P16<>(this.t1, p.t1, p.t2, p.t3, p.t4, p.t5, p.t6, p.t7, p.t8, p.t9, p.t10, p.t11, p.t12, p.t13, p.t14, p.t15);
      }

      public <R> App<F, R> apply(Applicative<F, ?> instance, Function<T1, R> function) {
         return this.apply(instance, instance.point(function));
      }

      public <R> App<F, R> apply(Applicative<F, ?> instance, App<F, Function<T1, R>> function) {
         return instance.ap(function, this.t1);
      }
   }

   record P10<F extends K1, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10>(
      App<F, T1> t1, App<F, T2> t2, App<F, T3> t3, App<F, T4> t4, App<F, T5> t5, App<F, T6> t6, App<F, T7> t7, App<F, T8> t8, App<F, T9> t9, App<F, T10> t10
   ) {
      public <T11> Products.P11<F, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11> and(App<F, T11> t11) {
         return new Products.P11<>(this.t1, this.t2, this.t3, this.t4, this.t5, this.t6, this.t7, this.t8, this.t9, this.t10, t11);
      }

      public <T11, T12> Products.P12<F, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12> and(Products.P2<F, T11, T12> p) {
         return new Products.P12<>(this.t1, this.t2, this.t3, this.t4, this.t5, this.t6, this.t7, this.t8, this.t9, this.t10, p.t1, p.t2);
      }

      public <T11, T12, T13> Products.P13<F, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13> and(Products.P3<F, T11, T12, T13> p) {
         return new Products.P13<>(this.t1, this.t2, this.t3, this.t4, this.t5, this.t6, this.t7, this.t8, this.t9, this.t10, p.t1, p.t2, p.t3);
      }

      public <T11, T12, T13, T14> Products.P14<F, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14> and(Products.P4<F, T11, T12, T13, T14> p) {
         return new Products.P14<>(this.t1, this.t2, this.t3, this.t4, this.t5, this.t6, this.t7, this.t8, this.t9, this.t10, p.t1, p.t2, p.t3, p.t4);
      }

      public <T11, T12, T13, T14, T15> Products.P15<F, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15> and(
         Products.P5<F, T11, T12, T13, T14, T15> p
      ) {
         return new Products.P15<>(this.t1, this.t2, this.t3, this.t4, this.t5, this.t6, this.t7, this.t8, this.t9, this.t10, p.t1, p.t2, p.t3, p.t4, p.t5);
      }

      public <T11, T12, T13, T14, T15, T16> Products.P16<F, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16> and(
         Products.P6<F, T11, T12, T13, T14, T15, T16> p
      ) {
         return new Products.P16<>(
            this.t1, this.t2, this.t3, this.t4, this.t5, this.t6, this.t7, this.t8, this.t9, this.t10, p.t1, p.t2, p.t3, p.t4, p.t5, p.t6
         );
      }

      public <R> App<F, R> apply(Applicative<F, ?> instance, Function10<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, R> function) {
         return this.apply(instance, instance.point(function));
      }

      public <R> App<F, R> apply(Applicative<F, ?> instance, App<F, Function10<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, R>> function) {
         return instance.ap10(function, this.t1, this.t2, this.t3, this.t4, this.t5, this.t6, this.t7, this.t8, this.t9, this.t10);
      }
   }

   record P11<F extends K1, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11>(
      App<F, T1> t1,
      App<F, T2> t2,
      App<F, T3> t3,
      App<F, T4> t4,
      App<F, T5> t5,
      App<F, T6> t6,
      App<F, T7> t7,
      App<F, T8> t8,
      App<F, T9> t9,
      App<F, T10> t10,
      App<F, T11> t11
   ) {
      public <T12> Products.P12<F, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12> and(App<F, T12> t12) {
         return new Products.P12<>(this.t1, this.t2, this.t3, this.t4, this.t5, this.t6, this.t7, this.t8, this.t9, this.t10, this.t11, t12);
      }

      public <T12, T13> Products.P13<F, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13> and(Products.P2<F, T12, T13> p) {
         return new Products.P13<>(this.t1, this.t2, this.t3, this.t4, this.t5, this.t6, this.t7, this.t8, this.t9, this.t10, this.t11, p.t1, p.t2);
      }

      public <T12, T13, T14> Products.P14<F, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14> and(Products.P3<F, T12, T13, T14> p) {
         return new Products.P14<>(this.t1, this.t2, this.t3, this.t4, this.t5, this.t6, this.t7, this.t8, this.t9, this.t10, this.t11, p.t1, p.t2, p.t3);
      }

      public <T12, T13, T14, T15> Products.P15<F, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15> and(Products.P4<F, T12, T13, T14, T15> p) {
         return new Products.P15<>(this.t1, this.t2, this.t3, this.t4, this.t5, this.t6, this.t7, this.t8, this.t9, this.t10, this.t11, p.t1, p.t2, p.t3, p.t4);
      }

      public <T12, T13, T14, T15, T16> Products.P16<F, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16> and(
         Products.P5<F, T12, T13, T14, T15, T16> p
      ) {
         return new Products.P16<>(
            this.t1, this.t2, this.t3, this.t4, this.t5, this.t6, this.t7, this.t8, this.t9, this.t10, this.t11, p.t1, p.t2, p.t3, p.t4, p.t5
         );
      }

      public <R> App<F, R> apply(Applicative<F, ?> instance, Function11<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, R> function) {
         return this.apply(instance, instance.point(function));
      }

      public <R> App<F, R> apply(Applicative<F, ?> instance, App<F, Function11<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, R>> function) {
         return instance.ap11(function, this.t1, this.t2, this.t3, this.t4, this.t5, this.t6, this.t7, this.t8, this.t9, this.t10, this.t11);
      }
   }

   record P12<F extends K1, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12>(
      App<F, T1> t1,
      App<F, T2> t2,
      App<F, T3> t3,
      App<F, T4> t4,
      App<F, T5> t5,
      App<F, T6> t6,
      App<F, T7> t7,
      App<F, T8> t8,
      App<F, T9> t9,
      App<F, T10> t10,
      App<F, T11> t11,
      App<F, T12> t12
   ) {
      public <T13> Products.P13<F, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13> and(App<F, T13> t13) {
         return new Products.P13<>(this.t1, this.t2, this.t3, this.t4, this.t5, this.t6, this.t7, this.t8, this.t9, this.t10, this.t11, this.t12, t13);
      }

      public <T13, T14> Products.P14<F, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14> and(Products.P2<F, T13, T14> p) {
         return new Products.P14<>(this.t1, this.t2, this.t3, this.t4, this.t5, this.t6, this.t7, this.t8, this.t9, this.t10, this.t11, this.t12, p.t1, p.t2);
      }

      public <T13, T14, T15> Products.P15<F, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15> and(Products.P3<F, T13, T14, T15> p) {
         return new Products.P15<>(
            this.t1, this.t2, this.t3, this.t4, this.t5, this.t6, this.t7, this.t8, this.t9, this.t10, this.t11, this.t12, p.t1, p.t2, p.t3
         );
      }

      public <T13, T14, T15, T16> Products.P16<F, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16> and(
         Products.P4<F, T13, T14, T15, T16> p
      ) {
         return new Products.P16<>(
            this.t1, this.t2, this.t3, this.t4, this.t5, this.t6, this.t7, this.t8, this.t9, this.t10, this.t11, this.t12, p.t1, p.t2, p.t3, p.t4
         );
      }

      public <R> App<F, R> apply(Applicative<F, ?> instance, Function12<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, R> function) {
         return this.apply(instance, instance.point(function));
      }

      public <R> App<F, R> apply(Applicative<F, ?> instance, App<F, Function12<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, R>> function) {
         return instance.ap12(function, this.t1, this.t2, this.t3, this.t4, this.t5, this.t6, this.t7, this.t8, this.t9, this.t10, this.t11, this.t12);
      }
   }

   record P13<F extends K1, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13>(
      App<F, T1> t1,
      App<F, T2> t2,
      App<F, T3> t3,
      App<F, T4> t4,
      App<F, T5> t5,
      App<F, T6> t6,
      App<F, T7> t7,
      App<F, T8> t8,
      App<F, T9> t9,
      App<F, T10> t10,
      App<F, T11> t11,
      App<F, T12> t12,
      App<F, T13> t13
   ) {
      public <T14> Products.P14<F, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14> and(App<F, T14> t14) {
         return new Products.P14<>(this.t1, this.t2, this.t3, this.t4, this.t5, this.t6, this.t7, this.t8, this.t9, this.t10, this.t11, this.t12, this.t13, t14);
      }

      public <T14, T15> Products.P15<F, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15> and(Products.P2<F, T14, T15> p) {
         return new Products.P15<>(
            this.t1, this.t2, this.t3, this.t4, this.t5, this.t6, this.t7, this.t8, this.t9, this.t10, this.t11, this.t12, this.t13, p.t1, p.t2
         );
      }

      public <T14, T15, T16> Products.P16<F, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16> and(Products.P3<F, T14, T15, T16> p) {
         return new Products.P16<>(
            this.t1, this.t2, this.t3, this.t4, this.t5, this.t6, this.t7, this.t8, this.t9, this.t10, this.t11, this.t12, this.t13, p.t1, p.t2, p.t3
         );
      }

      public <R> App<F, R> apply(Applicative<F, ?> instance, Function13<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, R> function) {
         return this.apply(instance, instance.point(function));
      }

      public <R> App<F, R> apply(Applicative<F, ?> instance, App<F, Function13<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, R>> function) {
         return instance.ap13(function, this.t1, this.t2, this.t3, this.t4, this.t5, this.t6, this.t7, this.t8, this.t9, this.t10, this.t11, this.t12, this.t13);
      }
   }

   record P14<F extends K1, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14>(
      App<F, T1> t1,
      App<F, T2> t2,
      App<F, T3> t3,
      App<F, T4> t4,
      App<F, T5> t5,
      App<F, T6> t6,
      App<F, T7> t7,
      App<F, T8> t8,
      App<F, T9> t9,
      App<F, T10> t10,
      App<F, T11> t11,
      App<F, T12> t12,
      App<F, T13> t13,
      App<F, T14> t14
   ) {
      public <T15> Products.P15<F, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15> and(App<F, T15> t15) {
         return new Products.P15<>(
            this.t1, this.t2, this.t3, this.t4, this.t5, this.t6, this.t7, this.t8, this.t9, this.t10, this.t11, this.t12, this.t13, this.t14, t15
         );
      }

      public <T15, T16> Products.P16<F, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16> and(Products.P2<F, T15, T16> p) {
         return new Products.P16<>(
            this.t1, this.t2, this.t3, this.t4, this.t5, this.t6, this.t7, this.t8, this.t9, this.t10, this.t11, this.t12, this.t13, this.t14, p.t1, p.t2
         );
      }

      public <R> App<F, R> apply(Applicative<F, ?> instance, Function14<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, R> function) {
         return this.apply(instance, instance.point(function));
      }

      public <R> App<F, R> apply(Applicative<F, ?> instance, App<F, Function14<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, R>> function) {
         return instance.ap14(
            function, this.t1, this.t2, this.t3, this.t4, this.t5, this.t6, this.t7, this.t8, this.t9, this.t10, this.t11, this.t12, this.t13, this.t14
         );
      }
   }

   record P15<F extends K1, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15>(
      App<F, T1> t1,
      App<F, T2> t2,
      App<F, T3> t3,
      App<F, T4> t4,
      App<F, T5> t5,
      App<F, T6> t6,
      App<F, T7> t7,
      App<F, T8> t8,
      App<F, T9> t9,
      App<F, T10> t10,
      App<F, T11> t11,
      App<F, T12> t12,
      App<F, T13> t13,
      App<F, T14> t14,
      App<F, T15> t15
   ) {
      public <T16> Products.P16<F, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16> and(App<F, T16> t16) {
         return new Products.P16<>(
            this.t1, this.t2, this.t3, this.t4, this.t5, this.t6, this.t7, this.t8, this.t9, this.t10, this.t11, this.t12, this.t13, this.t14, this.t15, t16
         );
      }

      public <R> App<F, R> apply(Applicative<F, ?> instance, Function15<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, R> function) {
         return this.apply(instance, instance.point(function));
      }

      public <R> App<F, R> apply(Applicative<F, ?> instance, App<F, Function15<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, R>> function) {
         return instance.ap15(
            function,
            this.t1,
            this.t2,
            this.t3,
            this.t4,
            this.t5,
            this.t6,
            this.t7,
            this.t8,
            this.t9,
            this.t10,
            this.t11,
            this.t12,
            this.t13,
            this.t14,
            this.t15
         );
      }
   }

   record P16<F extends K1, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16>(
      App<F, T1> t1,
      App<F, T2> t2,
      App<F, T3> t3,
      App<F, T4> t4,
      App<F, T5> t5,
      App<F, T6> t6,
      App<F, T7> t7,
      App<F, T8> t8,
      App<F, T9> t9,
      App<F, T10> t10,
      App<F, T11> t11,
      App<F, T12> t12,
      App<F, T13> t13,
      App<F, T14> t14,
      App<F, T15> t15,
      App<F, T16> t16
   ) {
      public <R> App<F, R> apply(Applicative<F, ?> instance, Function16<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, R> function) {
         return this.apply(instance, instance.point(function));
      }

      public <R> App<F, R> apply(
         Applicative<F, ?> instance, App<F, Function16<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, R>> function
      ) {
         return instance.ap16(
            function,
            this.t1,
            this.t2,
            this.t3,
            this.t4,
            this.t5,
            this.t6,
            this.t7,
            this.t8,
            this.t9,
            this.t10,
            this.t11,
            this.t12,
            this.t13,
            this.t14,
            this.t15,
            this.t16
         );
      }
   }

   record P2<F extends K1, T1, T2>(App<F, T1> t1, App<F, T2> t2) {
      public <T3> Products.P3<F, T1, T2, T3> and(App<F, T3> t3) {
         return new Products.P3<>(this.t1, this.t2, t3);
      }

      public <T3, T4> Products.P4<F, T1, T2, T3, T4> and(Products.P2<F, T3, T4> p) {
         return new Products.P4<>(this.t1, this.t2, p.t1, p.t2);
      }

      public <T3, T4, T5> Products.P5<F, T1, T2, T3, T4, T5> and(Products.P3<F, T3, T4, T5> p) {
         return new Products.P5<>(this.t1, this.t2, p.t1, p.t2, p.t3);
      }

      public <T3, T4, T5, T6> Products.P6<F, T1, T2, T3, T4, T5, T6> and(Products.P4<F, T3, T4, T5, T6> p) {
         return new Products.P6<>(this.t1, this.t2, p.t1, p.t2, p.t3, p.t4);
      }

      public <T3, T4, T5, T6, T7> Products.P7<F, T1, T2, T3, T4, T5, T6, T7> and(Products.P5<F, T3, T4, T5, T6, T7> p) {
         return new Products.P7<>(this.t1, this.t2, p.t1, p.t2, p.t3, p.t4, p.t5);
      }

      public <T3, T4, T5, T6, T7, T8> Products.P8<F, T1, T2, T3, T4, T5, T6, T7, T8> and(Products.P6<F, T3, T4, T5, T6, T7, T8> p) {
         return new Products.P8<>(this.t1, this.t2, p.t1, p.t2, p.t3, p.t4, p.t5, p.t6);
      }

      public <T3, T4, T5, T6, T7, T8, T9> Products.P9<F, T1, T2, T3, T4, T5, T6, T7, T8, T9> and(Products.P7<F, T3, T4, T5, T6, T7, T8, T9> p) {
         return new Products.P9<>(this.t1, this.t2, p.t1, p.t2, p.t3, p.t4, p.t5, p.t6, p.t7);
      }

      public <T3, T4, T5, T6, T7, T8, T9, T10> Products.P10<F, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> and(Products.P8<F, T3, T4, T5, T6, T7, T8, T9, T10> p) {
         return new Products.P10<>(this.t1, this.t2, p.t1, p.t2, p.t3, p.t4, p.t5, p.t6, p.t7, p.t8);
      }

      public <T3, T4, T5, T6, T7, T8, T9, T10, T11> Products.P11<F, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11> and(
         Products.P9<F, T3, T4, T5, T6, T7, T8, T9, T10, T11> p
      ) {
         return new Products.P11<>(this.t1, this.t2, p.t1, p.t2, p.t3, p.t4, p.t5, p.t6, p.t7, p.t8, p.t9);
      }

      public <T3, T4, T5, T6, T7, T8, T9, T10, T11, T12> Products.P12<F, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12> and(
         Products.P10<F, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12> p
      ) {
         return new Products.P12<>(this.t1, this.t2, p.t1, p.t2, p.t3, p.t4, p.t5, p.t6, p.t7, p.t8, p.t9, p.t10);
      }

      public <T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13> Products.P13<F, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13> and(
         Products.P11<F, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13> p
      ) {
         return new Products.P13<>(this.t1, this.t2, p.t1, p.t2, p.t3, p.t4, p.t5, p.t6, p.t7, p.t8, p.t9, p.t10, p.t11);
      }

      public <T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14> Products.P14<F, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14> and(
         Products.P12<F, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14> p
      ) {
         return new Products.P14<>(this.t1, this.t2, p.t1, p.t2, p.t3, p.t4, p.t5, p.t6, p.t7, p.t8, p.t9, p.t10, p.t11, p.t12);
      }

      public <T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15> Products.P15<F, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15> and(
         Products.P13<F, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15> p
      ) {
         return new Products.P15<>(this.t1, this.t2, p.t1, p.t2, p.t3, p.t4, p.t5, p.t6, p.t7, p.t8, p.t9, p.t10, p.t11, p.t12, p.t13);
      }

      public <T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16> Products.P16<F, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16> and(
         Products.P14<F, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16> p
      ) {
         return new Products.P16<>(this.t1, this.t2, p.t1, p.t2, p.t3, p.t4, p.t5, p.t6, p.t7, p.t8, p.t9, p.t10, p.t11, p.t12, p.t13, p.t14);
      }

      public <R> App<F, R> apply(Applicative<F, ?> instance, BiFunction<T1, T2, R> function) {
         return this.apply(instance, instance.point(function));
      }

      public <R> App<F, R> apply(Applicative<F, ?> instance, App<F, BiFunction<T1, T2, R>> function) {
         return instance.ap2(function, this.t1, this.t2);
      }
   }

   record P3<F extends K1, T1, T2, T3>(App<F, T1> t1, App<F, T2> t2, App<F, T3> t3) {
      public <T4> Products.P4<F, T1, T2, T3, T4> and(App<F, T4> t4) {
         return new Products.P4<>(this.t1, this.t2, this.t3, t4);
      }

      public <T4, T5> Products.P5<F, T1, T2, T3, T4, T5> and(Products.P2<F, T4, T5> p) {
         return new Products.P5<>(this.t1, this.t2, this.t3, p.t1, p.t2);
      }

      public <T4, T5, T6> Products.P6<F, T1, T2, T3, T4, T5, T6> and(Products.P3<F, T4, T5, T6> p) {
         return new Products.P6<>(this.t1, this.t2, this.t3, p.t1, p.t2, p.t3);
      }

      public <T4, T5, T6, T7> Products.P7<F, T1, T2, T3, T4, T5, T6, T7> and(Products.P4<F, T4, T5, T6, T7> p) {
         return new Products.P7<>(this.t1, this.t2, this.t3, p.t1, p.t2, p.t3, p.t4);
      }

      public <T4, T5, T6, T7, T8> Products.P8<F, T1, T2, T3, T4, T5, T6, T7, T8> and(Products.P5<F, T4, T5, T6, T7, T8> p) {
         return new Products.P8<>(this.t1, this.t2, this.t3, p.t1, p.t2, p.t3, p.t4, p.t5);
      }

      public <T4, T5, T6, T7, T8, T9> Products.P9<F, T1, T2, T3, T4, T5, T6, T7, T8, T9> and(Products.P6<F, T4, T5, T6, T7, T8, T9> p) {
         return new Products.P9<>(this.t1, this.t2, this.t3, p.t1, p.t2, p.t3, p.t4, p.t5, p.t6);
      }

      public <T4, T5, T6, T7, T8, T9, T10> Products.P10<F, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> and(Products.P7<F, T4, T5, T6, T7, T8, T9, T10> p) {
         return new Products.P10<>(this.t1, this.t2, this.t3, p.t1, p.t2, p.t3, p.t4, p.t5, p.t6, p.t7);
      }

      public <T4, T5, T6, T7, T8, T9, T10, T11> Products.P11<F, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11> and(
         Products.P8<F, T4, T5, T6, T7, T8, T9, T10, T11> p
      ) {
         return new Products.P11<>(this.t1, this.t2, this.t3, p.t1, p.t2, p.t3, p.t4, p.t5, p.t6, p.t7, p.t8);
      }

      public <T4, T5, T6, T7, T8, T9, T10, T11, T12> Products.P12<F, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12> and(
         Products.P9<F, T4, T5, T6, T7, T8, T9, T10, T11, T12> p
      ) {
         return new Products.P12<>(this.t1, this.t2, this.t3, p.t1, p.t2, p.t3, p.t4, p.t5, p.t6, p.t7, p.t8, p.t9);
      }

      public <T4, T5, T6, T7, T8, T9, T10, T11, T12, T13> Products.P13<F, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13> and(
         Products.P10<F, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13> p
      ) {
         return new Products.P13<>(this.t1, this.t2, this.t3, p.t1, p.t2, p.t3, p.t4, p.t5, p.t6, p.t7, p.t8, p.t9, p.t10);
      }

      public <T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14> Products.P14<F, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14> and(
         Products.P11<F, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14> p
      ) {
         return new Products.P14<>(this.t1, this.t2, this.t3, p.t1, p.t2, p.t3, p.t4, p.t5, p.t6, p.t7, p.t8, p.t9, p.t10, p.t11);
      }

      public <T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15> Products.P15<F, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15> and(
         Products.P12<F, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15> p
      ) {
         return new Products.P15<>(this.t1, this.t2, this.t3, p.t1, p.t2, p.t3, p.t4, p.t5, p.t6, p.t7, p.t8, p.t9, p.t10, p.t11, p.t12);
      }

      public <T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16> Products.P16<F, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16> and(
         Products.P13<F, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16> p
      ) {
         return new Products.P16<>(this.t1, this.t2, this.t3, p.t1, p.t2, p.t3, p.t4, p.t5, p.t6, p.t7, p.t8, p.t9, p.t10, p.t11, p.t12, p.t13);
      }

      public <R> App<F, R> apply(Applicative<F, ?> instance, Function3<T1, T2, T3, R> function) {
         return this.apply(instance, instance.point(function));
      }

      public <R> App<F, R> apply(Applicative<F, ?> instance, App<F, Function3<T1, T2, T3, R>> function) {
         return instance.ap3(function, this.t1, this.t2, this.t3);
      }
   }

   record P4<F extends K1, T1, T2, T3, T4>(App<F, T1> t1, App<F, T2> t2, App<F, T3> t3, App<F, T4> t4) {
      public <T5> Products.P5<F, T1, T2, T3, T4, T5> and(App<F, T5> t5) {
         return new Products.P5<>(this.t1, this.t2, this.t3, this.t4, t5);
      }

      public <T5, T6> Products.P6<F, T1, T2, T3, T4, T5, T6> and(Products.P2<F, T5, T6> p) {
         return new Products.P6<>(this.t1, this.t2, this.t3, this.t4, p.t1, p.t2);
      }

      public <T5, T6, T7> Products.P7<F, T1, T2, T3, T4, T5, T6, T7> and(Products.P3<F, T5, T6, T7> p) {
         return new Products.P7<>(this.t1, this.t2, this.t3, this.t4, p.t1, p.t2, p.t3);
      }

      public <T5, T6, T7, T8> Products.P8<F, T1, T2, T3, T4, T5, T6, T7, T8> and(Products.P4<F, T5, T6, T7, T8> p) {
         return new Products.P8<>(this.t1, this.t2, this.t3, this.t4, p.t1, p.t2, p.t3, p.t4);
      }

      public <T5, T6, T7, T8, T9> Products.P9<F, T1, T2, T3, T4, T5, T6, T7, T8, T9> and(Products.P5<F, T5, T6, T7, T8, T9> p) {
         return new Products.P9<>(this.t1, this.t2, this.t3, this.t4, p.t1, p.t2, p.t3, p.t4, p.t5);
      }

      public <T5, T6, T7, T8, T9, T10> Products.P10<F, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> and(Products.P6<F, T5, T6, T7, T8, T9, T10> p) {
         return new Products.P10<>(this.t1, this.t2, this.t3, this.t4, p.t1, p.t2, p.t3, p.t4, p.t5, p.t6);
      }

      public <T5, T6, T7, T8, T9, T10, T11> Products.P11<F, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11> and(Products.P7<F, T5, T6, T7, T8, T9, T10, T11> p) {
         return new Products.P11<>(this.t1, this.t2, this.t3, this.t4, p.t1, p.t2, p.t3, p.t4, p.t5, p.t6, p.t7);
      }

      public <T5, T6, T7, T8, T9, T10, T11, T12> Products.P12<F, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12> and(
         Products.P8<F, T5, T6, T7, T8, T9, T10, T11, T12> p
      ) {
         return new Products.P12<>(this.t1, this.t2, this.t3, this.t4, p.t1, p.t2, p.t3, p.t4, p.t5, p.t6, p.t7, p.t8);
      }

      public <T5, T6, T7, T8, T9, T10, T11, T12, T13> Products.P13<F, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13> and(
         Products.P9<F, T5, T6, T7, T8, T9, T10, T11, T12, T13> p
      ) {
         return new Products.P13<>(this.t1, this.t2, this.t3, this.t4, p.t1, p.t2, p.t3, p.t4, p.t5, p.t6, p.t7, p.t8, p.t9);
      }

      public <T5, T6, T7, T8, T9, T10, T11, T12, T13, T14> Products.P14<F, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14> and(
         Products.P10<F, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14> p
      ) {
         return new Products.P14<>(this.t1, this.t2, this.t3, this.t4, p.t1, p.t2, p.t3, p.t4, p.t5, p.t6, p.t7, p.t8, p.t9, p.t10);
      }

      public <T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15> Products.P15<F, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15> and(
         Products.P11<F, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15> p
      ) {
         return new Products.P15<>(this.t1, this.t2, this.t3, this.t4, p.t1, p.t2, p.t3, p.t4, p.t5, p.t6, p.t7, p.t8, p.t9, p.t10, p.t11);
      }

      public <T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16> Products.P16<F, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16> and(
         Products.P12<F, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16> p
      ) {
         return new Products.P16<>(this.t1, this.t2, this.t3, this.t4, p.t1, p.t2, p.t3, p.t4, p.t5, p.t6, p.t7, p.t8, p.t9, p.t10, p.t11, p.t12);
      }

      public <R> App<F, R> apply(Applicative<F, ?> instance, Function4<T1, T2, T3, T4, R> function) {
         return this.apply(instance, instance.point(function));
      }

      public <R> App<F, R> apply(Applicative<F, ?> instance, App<F, Function4<T1, T2, T3, T4, R>> function) {
         return instance.ap4(function, this.t1, this.t2, this.t3, this.t4);
      }
   }

   record P5<F extends K1, T1, T2, T3, T4, T5>(App<F, T1> t1, App<F, T2> t2, App<F, T3> t3, App<F, T4> t4, App<F, T5> t5) {
      public <T6> Products.P6<F, T1, T2, T3, T4, T5, T6> and(App<F, T6> t6) {
         return new Products.P6<>(this.t1, this.t2, this.t3, this.t4, this.t5, t6);
      }

      public <T6, T7> Products.P7<F, T1, T2, T3, T4, T5, T6, T7> and(Products.P2<F, T6, T7> p) {
         return new Products.P7<>(this.t1, this.t2, this.t3, this.t4, this.t5, p.t1, p.t2);
      }

      public <T6, T7, T8> Products.P8<F, T1, T2, T3, T4, T5, T6, T7, T8> and(Products.P3<F, T6, T7, T8> p) {
         return new Products.P8<>(this.t1, this.t2, this.t3, this.t4, this.t5, p.t1, p.t2, p.t3);
      }

      public <T6, T7, T8, T9> Products.P9<F, T1, T2, T3, T4, T5, T6, T7, T8, T9> and(Products.P4<F, T6, T7, T8, T9> p) {
         return new Products.P9<>(this.t1, this.t2, this.t3, this.t4, this.t5, p.t1, p.t2, p.t3, p.t4);
      }

      public <T6, T7, T8, T9, T10> Products.P10<F, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> and(Products.P5<F, T6, T7, T8, T9, T10> p) {
         return new Products.P10<>(this.t1, this.t2, this.t3, this.t4, this.t5, p.t1, p.t2, p.t3, p.t4, p.t5);
      }

      public <T6, T7, T8, T9, T10, T11> Products.P11<F, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11> and(Products.P6<F, T6, T7, T8, T9, T10, T11> p) {
         return new Products.P11<>(this.t1, this.t2, this.t3, this.t4, this.t5, p.t1, p.t2, p.t3, p.t4, p.t5, p.t6);
      }

      public <T6, T7, T8, T9, T10, T11, T12> Products.P12<F, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12> and(
         Products.P7<F, T6, T7, T8, T9, T10, T11, T12> p
      ) {
         return new Products.P12<>(this.t1, this.t2, this.t3, this.t4, this.t5, p.t1, p.t2, p.t3, p.t4, p.t5, p.t6, p.t7);
      }

      public <T6, T7, T8, T9, T10, T11, T12, T13> Products.P13<F, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13> and(
         Products.P8<F, T6, T7, T8, T9, T10, T11, T12, T13> p
      ) {
         return new Products.P13<>(this.t1, this.t2, this.t3, this.t4, this.t5, p.t1, p.t2, p.t3, p.t4, p.t5, p.t6, p.t7, p.t8);
      }

      public <T6, T7, T8, T9, T10, T11, T12, T13, T14> Products.P14<F, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14> and(
         Products.P9<F, T6, T7, T8, T9, T10, T11, T12, T13, T14> p
      ) {
         return new Products.P14<>(this.t1, this.t2, this.t3, this.t4, this.t5, p.t1, p.t2, p.t3, p.t4, p.t5, p.t6, p.t7, p.t8, p.t9);
      }

      public <T6, T7, T8, T9, T10, T11, T12, T13, T14, T15> Products.P15<F, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15> and(
         Products.P10<F, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15> p
      ) {
         return new Products.P15<>(this.t1, this.t2, this.t3, this.t4, this.t5, p.t1, p.t2, p.t3, p.t4, p.t5, p.t6, p.t7, p.t8, p.t9, p.t10);
      }

      public <T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16> Products.P16<F, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16> and(
         Products.P11<F, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16> p
      ) {
         return new Products.P16<>(this.t1, this.t2, this.t3, this.t4, this.t5, p.t1, p.t2, p.t3, p.t4, p.t5, p.t6, p.t7, p.t8, p.t9, p.t10, p.t11);
      }

      public <R> App<F, R> apply(Applicative<F, ?> instance, Function5<T1, T2, T3, T4, T5, R> function) {
         return this.apply(instance, instance.point(function));
      }

      public <R> App<F, R> apply(Applicative<F, ?> instance, App<F, Function5<T1, T2, T3, T4, T5, R>> function) {
         return instance.ap5(function, this.t1, this.t2, this.t3, this.t4, this.t5);
      }
   }

   record P6<F extends K1, T1, T2, T3, T4, T5, T6>(App<F, T1> t1, App<F, T2> t2, App<F, T3> t3, App<F, T4> t4, App<F, T5> t5, App<F, T6> t6) {
      public <T7> Products.P7<F, T1, T2, T3, T4, T5, T6, T7> and(App<F, T7> t7) {
         return new Products.P7<>(this.t1, this.t2, this.t3, this.t4, this.t5, this.t6, t7);
      }

      public <T7, T8> Products.P8<F, T1, T2, T3, T4, T5, T6, T7, T8> and(Products.P2<F, T7, T8> p) {
         return new Products.P8<>(this.t1, this.t2, this.t3, this.t4, this.t5, this.t6, p.t1, p.t2);
      }

      public <T7, T8, T9> Products.P9<F, T1, T2, T3, T4, T5, T6, T7, T8, T9> and(Products.P3<F, T7, T8, T9> p) {
         return new Products.P9<>(this.t1, this.t2, this.t3, this.t4, this.t5, this.t6, p.t1, p.t2, p.t3);
      }

      public <T7, T8, T9, T10> Products.P10<F, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> and(Products.P4<F, T7, T8, T9, T10> p) {
         return new Products.P10<>(this.t1, this.t2, this.t3, this.t4, this.t5, this.t6, p.t1, p.t2, p.t3, p.t4);
      }

      public <T7, T8, T9, T10, T11> Products.P11<F, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11> and(Products.P5<F, T7, T8, T9, T10, T11> p) {
         return new Products.P11<>(this.t1, this.t2, this.t3, this.t4, this.t5, this.t6, p.t1, p.t2, p.t3, p.t4, p.t5);
      }

      public <T7, T8, T9, T10, T11, T12> Products.P12<F, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12> and(Products.P6<F, T7, T8, T9, T10, T11, T12> p) {
         return new Products.P12<>(this.t1, this.t2, this.t3, this.t4, this.t5, this.t6, p.t1, p.t2, p.t3, p.t4, p.t5, p.t6);
      }

      public <T7, T8, T9, T10, T11, T12, T13> Products.P13<F, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13> and(
         Products.P7<F, T7, T8, T9, T10, T11, T12, T13> p
      ) {
         return new Products.P13<>(this.t1, this.t2, this.t3, this.t4, this.t5, this.t6, p.t1, p.t2, p.t3, p.t4, p.t5, p.t6, p.t7);
      }

      public <T7, T8, T9, T10, T11, T12, T13, T14> Products.P14<F, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14> and(
         Products.P8<F, T7, T8, T9, T10, T11, T12, T13, T14> p
      ) {
         return new Products.P14<>(this.t1, this.t2, this.t3, this.t4, this.t5, this.t6, p.t1, p.t2, p.t3, p.t4, p.t5, p.t6, p.t7, p.t8);
      }

      public <T7, T8, T9, T10, T11, T12, T13, T14, T15> Products.P15<F, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15> and(
         Products.P9<F, T7, T8, T9, T10, T11, T12, T13, T14, T15> p
      ) {
         return new Products.P15<>(this.t1, this.t2, this.t3, this.t4, this.t5, this.t6, p.t1, p.t2, p.t3, p.t4, p.t5, p.t6, p.t7, p.t8, p.t9);
      }

      public <T7, T8, T9, T10, T11, T12, T13, T14, T15, T16> Products.P16<F, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16> and(
         Products.P10<F, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16> p
      ) {
         return new Products.P16<>(this.t1, this.t2, this.t3, this.t4, this.t5, this.t6, p.t1, p.t2, p.t3, p.t4, p.t5, p.t6, p.t7, p.t8, p.t9, p.t10);
      }

      public <R> App<F, R> apply(Applicative<F, ?> instance, Function6<T1, T2, T3, T4, T5, T6, R> function) {
         return this.apply(instance, instance.point(function));
      }

      public <R> App<F, R> apply(Applicative<F, ?> instance, App<F, Function6<T1, T2, T3, T4, T5, T6, R>> function) {
         return instance.ap6(function, this.t1, this.t2, this.t3, this.t4, this.t5, this.t6);
      }
   }

   record P7<F extends K1, T1, T2, T3, T4, T5, T6, T7>(App<F, T1> t1, App<F, T2> t2, App<F, T3> t3, App<F, T4> t4, App<F, T5> t5, App<F, T6> t6, App<F, T7> t7) {
      public <T8> Products.P8<F, T1, T2, T3, T4, T5, T6, T7, T8> and(App<F, T8> t8) {
         return new Products.P8<>(this.t1, this.t2, this.t3, this.t4, this.t5, this.t6, this.t7, t8);
      }

      public <T8, T9> Products.P9<F, T1, T2, T3, T4, T5, T6, T7, T8, T9> and(Products.P2<F, T8, T9> p) {
         return new Products.P9<>(this.t1, this.t2, this.t3, this.t4, this.t5, this.t6, this.t7, p.t1, p.t2);
      }

      public <T8, T9, T10> Products.P10<F, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> and(Products.P3<F, T8, T9, T10> p) {
         return new Products.P10<>(this.t1, this.t2, this.t3, this.t4, this.t5, this.t6, this.t7, p.t1, p.t2, p.t3);
      }

      public <T8, T9, T10, T11> Products.P11<F, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11> and(Products.P4<F, T8, T9, T10, T11> p) {
         return new Products.P11<>(this.t1, this.t2, this.t3, this.t4, this.t5, this.t6, this.t7, p.t1, p.t2, p.t3, p.t4);
      }

      public <T8, T9, T10, T11, T12> Products.P12<F, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12> and(Products.P5<F, T8, T9, T10, T11, T12> p) {
         return new Products.P12<>(this.t1, this.t2, this.t3, this.t4, this.t5, this.t6, this.t7, p.t1, p.t2, p.t3, p.t4, p.t5);
      }

      public <T8, T9, T10, T11, T12, T13> Products.P13<F, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13> and(
         Products.P6<F, T8, T9, T10, T11, T12, T13> p
      ) {
         return new Products.P13<>(this.t1, this.t2, this.t3, this.t4, this.t5, this.t6, this.t7, p.t1, p.t2, p.t3, p.t4, p.t5, p.t6);
      }

      public <T8, T9, T10, T11, T12, T13, T14> Products.P14<F, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14> and(
         Products.P7<F, T8, T9, T10, T11, T12, T13, T14> p
      ) {
         return new Products.P14<>(this.t1, this.t2, this.t3, this.t4, this.t5, this.t6, this.t7, p.t1, p.t2, p.t3, p.t4, p.t5, p.t6, p.t7);
      }

      public <T8, T9, T10, T11, T12, T13, T14, T15> Products.P15<F, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15> and(
         Products.P8<F, T8, T9, T10, T11, T12, T13, T14, T15> p
      ) {
         return new Products.P15<>(this.t1, this.t2, this.t3, this.t4, this.t5, this.t6, this.t7, p.t1, p.t2, p.t3, p.t4, p.t5, p.t6, p.t7, p.t8);
      }

      public <T8, T9, T10, T11, T12, T13, T14, T15, T16> Products.P16<F, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16> and(
         Products.P9<F, T8, T9, T10, T11, T12, T13, T14, T15, T16> p
      ) {
         return new Products.P16<>(this.t1, this.t2, this.t3, this.t4, this.t5, this.t6, this.t7, p.t1, p.t2, p.t3, p.t4, p.t5, p.t6, p.t7, p.t8, p.t9);
      }

      public <R> App<F, R> apply(Applicative<F, ?> instance, Function7<T1, T2, T3, T4, T5, T6, T7, R> function) {
         return this.apply(instance, instance.point(function));
      }

      public <R> App<F, R> apply(Applicative<F, ?> instance, App<F, Function7<T1, T2, T3, T4, T5, T6, T7, R>> function) {
         return instance.ap7(function, this.t1, this.t2, this.t3, this.t4, this.t5, this.t6, this.t7);
      }
   }

   record P8<F extends K1, T1, T2, T3, T4, T5, T6, T7, T8>(
      App<F, T1> t1, App<F, T2> t2, App<F, T3> t3, App<F, T4> t4, App<F, T5> t5, App<F, T6> t6, App<F, T7> t7, App<F, T8> t8
   ) {
      public <T9> Products.P9<F, T1, T2, T3, T4, T5, T6, T7, T8, T9> and(App<F, T9> t9) {
         return new Products.P9<>(this.t1, this.t2, this.t3, this.t4, this.t5, this.t6, this.t7, this.t8, t9);
      }

      public <T9, T10> Products.P10<F, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> and(Products.P2<F, T9, T10> p) {
         return new Products.P10<>(this.t1, this.t2, this.t3, this.t4, this.t5, this.t6, this.t7, this.t8, p.t1, p.t2);
      }

      public <T9, T10, T11> Products.P11<F, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11> and(Products.P3<F, T9, T10, T11> p) {
         return new Products.P11<>(this.t1, this.t2, this.t3, this.t4, this.t5, this.t6, this.t7, this.t8, p.t1, p.t2, p.t3);
      }

      public <T9, T10, T11, T12> Products.P12<F, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12> and(Products.P4<F, T9, T10, T11, T12> p) {
         return new Products.P12<>(this.t1, this.t2, this.t3, this.t4, this.t5, this.t6, this.t7, this.t8, p.t1, p.t2, p.t3, p.t4);
      }

      public <T9, T10, T11, T12, T13> Products.P13<F, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13> and(Products.P5<F, T9, T10, T11, T12, T13> p) {
         return new Products.P13<>(this.t1, this.t2, this.t3, this.t4, this.t5, this.t6, this.t7, this.t8, p.t1, p.t2, p.t3, p.t4, p.t5);
      }

      public <T9, T10, T11, T12, T13, T14> Products.P14<F, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14> and(
         Products.P6<F, T9, T10, T11, T12, T13, T14> p
      ) {
         return new Products.P14<>(this.t1, this.t2, this.t3, this.t4, this.t5, this.t6, this.t7, this.t8, p.t1, p.t2, p.t3, p.t4, p.t5, p.t6);
      }

      public <T9, T10, T11, T12, T13, T14, T15> Products.P15<F, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15> and(
         Products.P7<F, T9, T10, T11, T12, T13, T14, T15> p
      ) {
         return new Products.P15<>(this.t1, this.t2, this.t3, this.t4, this.t5, this.t6, this.t7, this.t8, p.t1, p.t2, p.t3, p.t4, p.t5, p.t6, p.t7);
      }

      public <T9, T10, T11, T12, T13, T14, T15, T16> Products.P16<F, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16> and(
         Products.P8<F, T9, T10, T11, T12, T13, T14, T15, T16> p
      ) {
         return new Products.P16<>(this.t1, this.t2, this.t3, this.t4, this.t5, this.t6, this.t7, this.t8, p.t1, p.t2, p.t3, p.t4, p.t5, p.t6, p.t7, p.t8);
      }

      public <R> App<F, R> apply(Applicative<F, ?> instance, Function8<T1, T2, T3, T4, T5, T6, T7, T8, R> function) {
         return this.apply(instance, instance.point(function));
      }

      public <R> App<F, R> apply(Applicative<F, ?> instance, App<F, Function8<T1, T2, T3, T4, T5, T6, T7, T8, R>> function) {
         return instance.ap8(function, this.t1, this.t2, this.t3, this.t4, this.t5, this.t6, this.t7, this.t8);
      }
   }

   record P9<F extends K1, T1, T2, T3, T4, T5, T6, T7, T8, T9>(
      App<F, T1> t1, App<F, T2> t2, App<F, T3> t3, App<F, T4> t4, App<F, T5> t5, App<F, T6> t6, App<F, T7> t7, App<F, T8> t8, App<F, T9> t9
   ) {
      public <T10> Products.P10<F, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> and(App<F, T10> t10) {
         return new Products.P10<>(this.t1, this.t2, this.t3, this.t4, this.t5, this.t6, this.t7, this.t8, this.t9, t10);
      }

      public <T10, T11> Products.P11<F, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11> and(Products.P2<F, T10, T11> p) {
         return new Products.P11<>(this.t1, this.t2, this.t3, this.t4, this.t5, this.t6, this.t7, this.t8, this.t9, p.t1, p.t2);
      }

      public <T10, T11, T12> Products.P12<F, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12> and(Products.P3<F, T10, T11, T12> p) {
         return new Products.P12<>(this.t1, this.t2, this.t3, this.t4, this.t5, this.t6, this.t7, this.t8, this.t9, p.t1, p.t2, p.t3);
      }

      public <T10, T11, T12, T13> Products.P13<F, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13> and(Products.P4<F, T10, T11, T12, T13> p) {
         return new Products.P13<>(this.t1, this.t2, this.t3, this.t4, this.t5, this.t6, this.t7, this.t8, this.t9, p.t1, p.t2, p.t3, p.t4);
      }

      public <T10, T11, T12, T13, T14> Products.P14<F, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14> and(
         Products.P5<F, T10, T11, T12, T13, T14> p
      ) {
         return new Products.P14<>(this.t1, this.t2, this.t3, this.t4, this.t5, this.t6, this.t7, this.t8, this.t9, p.t1, p.t2, p.t3, p.t4, p.t5);
      }

      public <T10, T11, T12, T13, T14, T15> Products.P15<F, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15> and(
         Products.P6<F, T10, T11, T12, T13, T14, T15> p
      ) {
         return new Products.P15<>(this.t1, this.t2, this.t3, this.t4, this.t5, this.t6, this.t7, this.t8, this.t9, p.t1, p.t2, p.t3, p.t4, p.t5, p.t6);
      }

      public <T10, T11, T12, T13, T14, T15, T16> Products.P16<F, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16> and(
         Products.P7<F, T10, T11, T12, T13, T14, T15, T16> p
      ) {
         return new Products.P16<>(this.t1, this.t2, this.t3, this.t4, this.t5, this.t6, this.t7, this.t8, this.t9, p.t1, p.t2, p.t3, p.t4, p.t5, p.t6, p.t7);
      }

      public <R> App<F, R> apply(Applicative<F, ?> instance, Function9<T1, T2, T3, T4, T5, T6, T7, T8, T9, R> function) {
         return this.apply(instance, instance.point(function));
      }

      public <R> App<F, R> apply(Applicative<F, ?> instance, App<F, Function9<T1, T2, T3, T4, T5, T6, T7, T8, T9, R>> function) {
         return instance.ap9(function, this.t1, this.t2, this.t3, this.t4, this.t5, this.t6, this.t7, this.t8, this.t9);
      }
   }
}
