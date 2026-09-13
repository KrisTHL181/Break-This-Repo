package net.minecraft.network.codec;

import com.google.common.base.Suppliers;
import com.mojang.datafixers.util.Function10;
import com.mojang.datafixers.util.Function11;
import com.mojang.datafixers.util.Function12;
import com.mojang.datafixers.util.Function3;
import com.mojang.datafixers.util.Function4;
import com.mojang.datafixers.util.Function5;
import com.mojang.datafixers.util.Function6;
import com.mojang.datafixers.util.Function7;
import com.mojang.datafixers.util.Function8;
import com.mojang.datafixers.util.Function9;
import io.netty.buffer.ByteBuf;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

public interface StreamCodec<B, V> extends StreamDecoder<B, V>, StreamEncoder<B, V> {
   static <B, V> StreamCodec<B, V> of(final StreamEncoder<B, V> p_328457_, final StreamDecoder<B, V> p_332601_) {
      return new StreamCodec<B, V>() {
         @Override
         public V decode(B p_335513_) {
            return p_332601_.decode(p_335513_);
         }

         @Override
         public void encode(B p_333998_, V p_335122_) {
            p_328457_.encode(p_333998_, p_335122_);
         }
      };
   }

   static <B, V> StreamCodec<B, V> ofMember(final StreamMemberEncoder<B, V> p_330640_, final StreamDecoder<B, V> p_327818_) {
      return new StreamCodec<B, V>() {
         @Override
         public V decode(B p_331033_) {
            return p_327818_.decode(p_331033_);
         }

         @Override
         public void encode(B p_329484_, V p_332289_) {
            p_330640_.encode(p_332289_, p_329484_);
         }
      };
   }

   static <B, V> StreamCodec<B, V> unit(final V p_336240_) {
      return new StreamCodec<B, V>() {
         @Override
         public V decode(B p_328164_) {
            return p_336240_;
         }

         @Override
         public void encode(B p_336022_, V p_333291_) {
            if (!p_333291_.equals(p_336240_)) {
               throw new IllegalStateException("Can't encode '" + p_333291_ + "', expected '" + p_336240_ + "'");
            }
         }
      };
   }

   default <O> StreamCodec<B, O> apply(StreamCodec.CodecOperation<B, V, O> p_335614_) {
      return p_335614_.apply(this);
   }

   default <O> StreamCodec<B, O> map(final Function<? super V, ? extends O> p_327720_, final Function<? super O, ? extends V> p_330478_) {
      return new StreamCodec<B, O>() {
         @Override
         public O decode(B p_328614_) {
            return (O)p_327720_.apply(StreamCodec.this.decode(p_328614_));
         }

         @Override
         public void encode(B p_336327_, O p_331146_) {
            StreamCodec.this.encode(p_336327_, (V)p_330478_.apply(p_331146_));
         }
      };
   }

   default <O extends ByteBuf> StreamCodec<O, V> mapStream(final Function<O, ? extends B> p_332075_) {
      return new StreamCodec<O, V>() {
         public V decode(O p_331759_) {
            B b = (B)p_332075_.apply(p_331759_);
            return StreamCodec.this.decode(b);
         }

         public void encode(O p_334335_, V p_336271_) {
            B b = (B)p_332075_.apply(p_334335_);
            StreamCodec.this.encode(b, p_336271_);
         }
      };
   }

   default <U> StreamCodec<B, U> dispatch(
      final Function<? super U, ? extends V> p_333836_, final Function<? super V, ? extends StreamCodec<? super B, ? extends U>> p_335469_
   ) {
      return new StreamCodec<B, U>() {
         @Override
         public U decode(B p_333769_) {
            V v = StreamCodec.this.decode(p_333769_);
            StreamCodec<? super B, ? extends U> streamcodec = (StreamCodec<? super B, ? extends U>)p_335469_.apply(v);
            return (U)streamcodec.decode(p_333769_);
         }

         @Override
         public void encode(B p_331493_, U p_333683_) {
            V v = (V)p_333836_.apply(p_333683_);
            StreamCodec<B, U> streamcodec = (StreamCodec<B, U>)p_335469_.apply(v);
            StreamCodec.this.encode(p_331493_, v);
            streamcodec.encode(p_331493_, p_333683_);
         }
      };
   }

   static <B, C, T1> StreamCodec<B, C> composite(final StreamCodec<? super B, T1> p_332516_, final Function<C, T1> p_335276_, final Function<T1, C> p_330982_) {
      return new StreamCodec<B, C>() {
         @Override
         public C decode(B p_331843_) {
            T1 t1 = p_332516_.decode(p_331843_);
            return p_330982_.apply(t1);
         }

         @Override
         public void encode(B p_330937_, C p_333579_) {
            p_332516_.encode(p_330937_, p_335276_.apply(p_333579_));
         }
      };
   }

   static <B, C, T1, T2> StreamCodec<B, C> composite(
      final StreamCodec<? super B, T1> p_329724_,
      final Function<C, T1> p_329438_,
      final StreamCodec<? super B, T2> p_328233_,
      final Function<C, T2> p_328617_,
      final BiFunction<T1, T2, C> p_334409_
   ) {
      return new StreamCodec<B, C>() {
         @Override
         public C decode(B p_331897_) {
            T1 t1 = p_329724_.decode(p_331897_);
            T2 t2 = p_328233_.decode(p_331897_);
            return p_334409_.apply(t1, t2);
         }

         @Override
         public void encode(B p_334266_, C p_331042_) {
            p_329724_.encode(p_334266_, p_329438_.apply(p_331042_));
            p_328233_.encode(p_334266_, p_328617_.apply(p_331042_));
         }
      };
   }

   static <B, C, T1, T2, T3> StreamCodec<B, C> composite(
      final StreamCodec<? super B, T1> p_329473_,
      final Function<C, T1> p_334404_,
      final StreamCodec<? super B, T2> p_327967_,
      final Function<C, T2> p_330724_,
      final StreamCodec<? super B, T3> p_328162_,
      final Function<C, T3> p_333383_,
      final Function3<T1, T2, T3, C> p_334421_
   ) {
      return new StreamCodec<B, C>() {
         @Override
         public C decode(B p_331065_) {
            T1 t1 = p_329473_.decode(p_331065_);
            T2 t2 = p_327967_.decode(p_331065_);
            T3 t3 = p_328162_.decode(p_331065_);
            return (C)p_334421_.apply(t1, t2, t3);
         }

         @Override
         public void encode(B p_333137_, C p_328354_) {
            p_329473_.encode(p_333137_, p_334404_.apply(p_328354_));
            p_327967_.encode(p_333137_, p_330724_.apply(p_328354_));
            p_328162_.encode(p_333137_, p_333383_.apply(p_328354_));
         }
      };
   }

   static <B, C, T1, T2, T3, T4> StreamCodec<B, C> composite(
      final StreamCodec<? super B, T1> p_331397_,
      final Function<C, T1> p_331210_,
      final StreamCodec<? super B, T2> p_332449_,
      final Function<C, T2> p_329970_,
      final StreamCodec<? super B, T3> p_328015_,
      final Function<C, T3> p_333423_,
      final StreamCodec<? super B, T4> p_332358_,
      final Function<C, T4> p_331597_,
      final Function4<T1, T2, T3, T4, C> p_332476_
   ) {
      return new StreamCodec<B, C>() {
         @Override
         public C decode(B p_334517_) {
            T1 t1 = p_331397_.decode(p_334517_);
            T2 t2 = p_332449_.decode(p_334517_);
            T3 t3 = p_328015_.decode(p_334517_);
            T4 t4 = p_332358_.decode(p_334517_);
            return (C)p_332476_.apply(t1, t2, t3, t4);
         }

         @Override
         public void encode(B p_336185_, C p_330170_) {
            p_331397_.encode(p_336185_, p_331210_.apply(p_330170_));
            p_332449_.encode(p_336185_, p_329970_.apply(p_330170_));
            p_328015_.encode(p_336185_, p_333423_.apply(p_330170_));
            p_332358_.encode(p_336185_, p_331597_.apply(p_330170_));
         }
      };
   }

   static <B, C, T1, T2, T3, T4, T5> StreamCodec<B, C> composite(
      final StreamCodec<? super B, T1> p_332680_,
      final Function<C, T1> p_336312_,
      final StreamCodec<? super B, T2> p_328131_,
      final Function<C, T2> p_332283_,
      final StreamCodec<? super B, T3> p_330440_,
      final Function<C, T3> p_333147_,
      final StreamCodec<? super B, T4> p_329904_,
      final Function<C, T4> p_330832_,
      final StreamCodec<? super B, T5> p_335857_,
      final Function<C, T5> p_333237_,
      final Function5<T1, T2, T3, T4, T5, C> p_328623_
   ) {
      return new StreamCodec<B, C>() {
         @Override
         public C decode(B p_328956_) {
            T1 t1 = p_332680_.decode(p_328956_);
            T2 t2 = p_328131_.decode(p_328956_);
            T3 t3 = p_330440_.decode(p_328956_);
            T4 t4 = p_329904_.decode(p_328956_);
            T5 t5 = p_335857_.decode(p_328956_);
            return (C)p_328623_.apply(t1, t2, t3, t4, t5);
         }

         @Override
         public void encode(B p_328899_, C p_328944_) {
            p_332680_.encode(p_328899_, p_336312_.apply(p_328944_));
            p_328131_.encode(p_328899_, p_332283_.apply(p_328944_));
            p_330440_.encode(p_328899_, p_333147_.apply(p_328944_));
            p_329904_.encode(p_328899_, p_330832_.apply(p_328944_));
            p_335857_.encode(p_328899_, p_333237_.apply(p_328944_));
         }
      };
   }

   static <B, C, T1, T2, T3, T4, T5, T6> StreamCodec<B, C> composite(
      final StreamCodec<? super B, T1> p_333401_,
      final Function<C, T1> p_329450_,
      final StreamCodec<? super B, T2> p_330884_,
      final Function<C, T2> p_328085_,
      final StreamCodec<? super B, T3> p_332808_,
      final Function<C, T3> p_327867_,
      final StreamCodec<? super B, T4> p_335472_,
      final Function<C, T4> p_328511_,
      final StreamCodec<? super B, T5> p_333318_,
      final Function<C, T5> p_330123_,
      final StreamCodec<? super B, T6> p_332458_,
      final Function<C, T6> p_328086_,
      final Function6<T1, T2, T3, T4, T5, T6, C> p_329947_
   ) {
      return new StreamCodec<B, C>() {
         @Override
         public C decode(B p_330564_) {
            T1 t1 = p_333401_.decode(p_330564_);
            T2 t2 = p_330884_.decode(p_330564_);
            T3 t3 = p_332808_.decode(p_330564_);
            T4 t4 = p_335472_.decode(p_330564_);
            T5 t5 = p_333318_.decode(p_330564_);
            T6 t6 = p_332458_.decode(p_330564_);
            return (C)p_329947_.apply(t1, t2, t3, t4, t5, t6);
         }

         @Override
         public void encode(B p_328016_, C p_331911_) {
            p_333401_.encode(p_328016_, p_329450_.apply(p_331911_));
            p_330884_.encode(p_328016_, p_328085_.apply(p_331911_));
            p_332808_.encode(p_328016_, p_327867_.apply(p_331911_));
            p_335472_.encode(p_328016_, p_328511_.apply(p_331911_));
            p_333318_.encode(p_328016_, p_330123_.apply(p_331911_));
            p_332458_.encode(p_328016_, p_328086_.apply(p_331911_));
         }
      };
   }

   static <B, C, T1, T2, T3, T4, T5, T6, T7> StreamCodec<B, C> composite(
      final StreamCodec<? super B, T1> p_362813_,
      final Function<C, T1> p_366161_,
      final StreamCodec<? super B, T2> p_363894_,
      final Function<C, T2> p_362001_,
      final StreamCodec<? super B, T3> p_363671_,
      final Function<C, T3> p_367051_,
      final StreamCodec<? super B, T4> p_366641_,
      final Function<C, T4> p_367961_,
      final StreamCodec<? super B, T5> p_369011_,
      final Function<C, T5> p_368271_,
      final StreamCodec<? super B, T6> p_364705_,
      final Function<C, T6> p_363391_,
      final StreamCodec<? super B, T7> p_369569_,
      final Function<C, T7> p_365688_,
      final Function7<T1, T2, T3, T4, T5, T6, T7, C> p_370204_
   ) {
      return new StreamCodec<B, C>() {
         @Override
         public C decode(B p_330854_) {
            T1 t1 = p_362813_.decode(p_330854_);
            T2 t2 = p_363894_.decode(p_330854_);
            T3 t3 = p_363671_.decode(p_330854_);
            T4 t4 = p_366641_.decode(p_330854_);
            T5 t5 = p_369011_.decode(p_330854_);
            T6 t6 = p_364705_.decode(p_330854_);
            T7 t7 = p_369569_.decode(p_330854_);
            return (C)p_370204_.apply(t1, t2, t3, t4, t5, t6, t7);
         }

         @Override
         public void encode(B p_332524_, C p_336367_) {
            p_362813_.encode(p_332524_, p_366161_.apply(p_336367_));
            p_363894_.encode(p_332524_, p_362001_.apply(p_336367_));
            p_363671_.encode(p_332524_, p_367051_.apply(p_336367_));
            p_366641_.encode(p_332524_, p_367961_.apply(p_336367_));
            p_369011_.encode(p_332524_, p_368271_.apply(p_336367_));
            p_364705_.encode(p_332524_, p_363391_.apply(p_336367_));
            p_369569_.encode(p_332524_, p_365688_.apply(p_336367_));
         }
      };
   }

   static <B, C, T1, T2, T3, T4, T5, T6, T7, T8> StreamCodec<B, C> composite(
      final StreamCodec<? super B, T1> p_367373_,
      final Function<C, T1> p_369557_,
      final StreamCodec<? super B, T2> p_368011_,
      final Function<C, T2> p_363664_,
      final StreamCodec<? super B, T3> p_367205_,
      final Function<C, T3> p_364055_,
      final StreamCodec<? super B, T4> p_361203_,
      final Function<C, T4> p_365006_,
      final StreamCodec<? super B, T5> p_362409_,
      final Function<C, T5> p_367771_,
      final StreamCodec<? super B, T6> p_362282_,
      final Function<C, T6> p_365852_,
      final StreamCodec<? super B, T7> p_361750_,
      final Function<C, T7> p_368272_,
      final StreamCodec<? super B, T8> p_367402_,
      final Function<C, T8> p_369297_,
      final Function8<T1, T2, T3, T4, T5, T6, T7, T8, C> p_365425_
   ) {
      return new StreamCodec<B, C>() {
         @Override
         public C decode(B p_362416_) {
            T1 t1 = p_367373_.decode(p_362416_);
            T2 t2 = p_368011_.decode(p_362416_);
            T3 t3 = p_367205_.decode(p_362416_);
            T4 t4 = p_361203_.decode(p_362416_);
            T5 t5 = p_362409_.decode(p_362416_);
            T6 t6 = p_362282_.decode(p_362416_);
            T7 t7 = p_361750_.decode(p_362416_);
            T8 t8 = p_367402_.decode(p_362416_);
            return (C)p_365425_.apply(t1, t2, t3, t4, t5, t6, t7, t8);
         }

         @Override
         public void encode(B p_366041_, C p_365657_) {
            p_367373_.encode(p_366041_, p_369557_.apply(p_365657_));
            p_368011_.encode(p_366041_, p_363664_.apply(p_365657_));
            p_367205_.encode(p_366041_, p_364055_.apply(p_365657_));
            p_361203_.encode(p_366041_, p_365006_.apply(p_365657_));
            p_362409_.encode(p_366041_, p_367771_.apply(p_365657_));
            p_362282_.encode(p_366041_, p_365852_.apply(p_365657_));
            p_361750_.encode(p_366041_, p_368272_.apply(p_365657_));
            p_367402_.encode(p_366041_, p_369297_.apply(p_365657_));
         }
      };
   }

   static <B, C, T1, T2, T3, T4, T5, T6, T7, T8, T9> StreamCodec<B, C> composite(
      final StreamCodec<? super B, T1> p_396599_,
      final Function<C, T1> p_394059_,
      final StreamCodec<? super B, T2> p_391853_,
      final Function<C, T2> p_397231_,
      final StreamCodec<? super B, T3> p_397084_,
      final Function<C, T3> p_396685_,
      final StreamCodec<? super B, T4> p_396284_,
      final Function<C, T4> p_396853_,
      final StreamCodec<? super B, T5> p_396318_,
      final Function<C, T5> p_397637_,
      final StreamCodec<? super B, T6> p_392835_,
      final Function<C, T6> p_395094_,
      final StreamCodec<? super B, T7> p_395281_,
      final Function<C, T7> p_392689_,
      final StreamCodec<? super B, T8> p_395884_,
      final Function<C, T8> p_391389_,
      final StreamCodec<? super B, T9> p_394579_,
      final Function<C, T9> p_391273_,
      final Function9<T1, T2, T3, T4, T5, T6, T7, T8, T9, C> p_396269_
   ) {
      return new StreamCodec<B, C>() {
         @Override
         public C decode(B p_366688_) {
            T1 t1 = p_396599_.decode(p_366688_);
            T2 t2 = p_391853_.decode(p_366688_);
            T3 t3 = p_397084_.decode(p_366688_);
            T4 t4 = p_396284_.decode(p_366688_);
            T5 t5 = p_396318_.decode(p_366688_);
            T6 t6 = p_392835_.decode(p_366688_);
            T7 t7 = p_395281_.decode(p_366688_);
            T8 t8 = p_395884_.decode(p_366688_);
            T9 t9 = p_394579_.decode(p_366688_);
            return (C)p_396269_.apply(t1, t2, t3, t4, t5, t6, t7, t8, t9);
         }

         @Override
         public void encode(B p_364543_, C p_364761_) {
            p_396599_.encode(p_364543_, p_394059_.apply(p_364761_));
            p_391853_.encode(p_364543_, p_397231_.apply(p_364761_));
            p_397084_.encode(p_364543_, p_396685_.apply(p_364761_));
            p_396284_.encode(p_364543_, p_396853_.apply(p_364761_));
            p_396318_.encode(p_364543_, p_397637_.apply(p_364761_));
            p_392835_.encode(p_364543_, p_395094_.apply(p_364761_));
            p_395281_.encode(p_364543_, p_392689_.apply(p_364761_));
            p_395884_.encode(p_364543_, p_391389_.apply(p_364761_));
            p_394579_.encode(p_364543_, p_391273_.apply(p_364761_));
         }
      };
   }

   static <B, C, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> StreamCodec<B, C> composite(
      final StreamCodec<? super B, T1> p_454003_,
      final Function<C, T1> p_454229_,
      final StreamCodec<? super B, T2> p_456138_,
      final Function<C, T2> p_458801_,
      final StreamCodec<? super B, T3> p_459447_,
      final Function<C, T3> p_454357_,
      final StreamCodec<? super B, T4> p_459731_,
      final Function<C, T4> p_452314_,
      final StreamCodec<? super B, T5> p_451666_,
      final Function<C, T5> p_450818_,
      final StreamCodec<? super B, T6> p_451093_,
      final Function<C, T6> p_452440_,
      final StreamCodec<? super B, T7> p_460608_,
      final Function<C, T7> p_456876_,
      final StreamCodec<? super B, T8> p_458078_,
      final Function<C, T8> p_454423_,
      final StreamCodec<? super B, T9> p_460942_,
      final Function<C, T9> p_456621_,
      final StreamCodec<? super B, T10> p_451884_,
      final Function<C, T10> p_453899_,
      final Function10<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, C> p_453527_
   ) {
      return new StreamCodec<B, C>() {
         @Override
         public C decode(B p_397382_) {
            T1 t1 = p_454003_.decode(p_397382_);
            T2 t2 = p_456138_.decode(p_397382_);
            T3 t3 = p_459447_.decode(p_397382_);
            T4 t4 = p_459731_.decode(p_397382_);
            T5 t5 = p_451666_.decode(p_397382_);
            T6 t6 = p_451093_.decode(p_397382_);
            T7 t7 = p_460608_.decode(p_397382_);
            T8 t8 = p_458078_.decode(p_397382_);
            T9 t9 = p_460942_.decode(p_397382_);
            T10 t10 = p_451884_.decode(p_397382_);
            return (C)p_453527_.apply(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10);
         }

         @Override
         public void encode(B p_396343_, C p_395545_) {
            p_454003_.encode(p_396343_, p_454229_.apply(p_395545_));
            p_456138_.encode(p_396343_, p_458801_.apply(p_395545_));
            p_459447_.encode(p_396343_, p_454357_.apply(p_395545_));
            p_459731_.encode(p_396343_, p_452314_.apply(p_395545_));
            p_451666_.encode(p_396343_, p_450818_.apply(p_395545_));
            p_451093_.encode(p_396343_, p_452440_.apply(p_395545_));
            p_460608_.encode(p_396343_, p_456876_.apply(p_395545_));
            p_458078_.encode(p_396343_, p_454423_.apply(p_395545_));
            p_460942_.encode(p_396343_, p_456621_.apply(p_395545_));
            p_451884_.encode(p_396343_, p_453899_.apply(p_395545_));
         }
      };
   }

   static <B, C, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11> StreamCodec<B, C> composite(
      final StreamCodec<? super B, T1> p_409776_,
      final Function<C, T1> p_407759_,
      final StreamCodec<? super B, T2> p_409570_,
      final Function<C, T2> p_409883_,
      final StreamCodec<? super B, T3> p_406711_,
      final Function<C, T3> p_407045_,
      final StreamCodec<? super B, T4> p_409342_,
      final Function<C, T4> p_406909_,
      final StreamCodec<? super B, T5> p_409279_,
      final Function<C, T5> p_406008_,
      final StreamCodec<? super B, T6> p_406410_,
      final Function<C, T6> p_408119_,
      final StreamCodec<? super B, T7> p_405948_,
      final Function<C, T7> p_407885_,
      final StreamCodec<? super B, T8> p_407345_,
      final Function<C, T8> p_405989_,
      final StreamCodec<? super B, T9> p_406674_,
      final Function<C, T9> p_409372_,
      final StreamCodec<? super B, T10> p_408986_,
      final Function<C, T10> p_408528_,
      final StreamCodec<? super B, T11> p_408946_,
      final Function<C, T11> p_407134_,
      final Function11<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, C> p_407962_
   ) {
      return new StreamCodec<B, C>() {
         @Override
         public C decode(B p_407981_) {
            T1 t1 = p_409776_.decode(p_407981_);
            T2 t2 = p_409570_.decode(p_407981_);
            T3 t3 = p_406711_.decode(p_407981_);
            T4 t4 = p_409342_.decode(p_407981_);
            T5 t5 = p_409279_.decode(p_407981_);
            T6 t6 = p_406410_.decode(p_407981_);
            T7 t7 = p_405948_.decode(p_407981_);
            T8 t8 = p_407345_.decode(p_407981_);
            T9 t9 = p_406674_.decode(p_407981_);
            T10 t10 = p_408986_.decode(p_407981_);
            T11 t11 = p_408946_.decode(p_407981_);
            return (C)p_407962_.apply(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11);
         }

         @Override
         public void encode(B p_408401_, C p_410054_) {
            p_409776_.encode(p_408401_, p_407759_.apply(p_410054_));
            p_409570_.encode(p_408401_, p_409883_.apply(p_410054_));
            p_406711_.encode(p_408401_, p_407045_.apply(p_410054_));
            p_409342_.encode(p_408401_, p_406909_.apply(p_410054_));
            p_409279_.encode(p_408401_, p_406008_.apply(p_410054_));
            p_406410_.encode(p_408401_, p_408119_.apply(p_410054_));
            p_405948_.encode(p_408401_, p_407885_.apply(p_410054_));
            p_407345_.encode(p_408401_, p_405989_.apply(p_410054_));
            p_406674_.encode(p_408401_, p_409372_.apply(p_410054_));
            p_408986_.encode(p_408401_, p_408528_.apply(p_410054_));
            p_408946_.encode(p_408401_, p_407134_.apply(p_410054_));
         }
      };
   }

   static <B, C, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12> StreamCodec<B, C> composite(
      final StreamCodec<? super B, T1> p_458378_,
      final Function<C, T1> p_453541_,
      final StreamCodec<? super B, T2> p_456000_,
      final Function<C, T2> p_456302_,
      final StreamCodec<? super B, T3> p_451624_,
      final Function<C, T3> p_455650_,
      final StreamCodec<? super B, T4> p_453475_,
      final Function<C, T4> p_453203_,
      final StreamCodec<? super B, T5> p_453809_,
      final Function<C, T5> p_456740_,
      final StreamCodec<? super B, T6> p_450701_,
      final Function<C, T6> p_456188_,
      final StreamCodec<? super B, T7> p_458252_,
      final Function<C, T7> p_458819_,
      final StreamCodec<? super B, T8> p_456208_,
      final Function<C, T8> p_453119_,
      final StreamCodec<? super B, T9> p_455786_,
      final Function<C, T9> p_454342_,
      final StreamCodec<? super B, T10> p_460726_,
      final Function<C, T10> p_456439_,
      final StreamCodec<? super B, T11> p_460366_,
      final Function<C, T11> p_450933_,
      final StreamCodec<? super B, T12> p_454504_,
      final Function<C, T12> p_454812_,
      final Function12<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, C> p_452964_
   ) {
      return new StreamCodec<B, C>() {
         @Override
         public C decode(B p_458560_) {
            T1 t1 = p_458378_.decode(p_458560_);
            T2 t2 = p_456000_.decode(p_458560_);
            T3 t3 = p_451624_.decode(p_458560_);
            T4 t4 = p_453475_.decode(p_458560_);
            T5 t5 = p_453809_.decode(p_458560_);
            T6 t6 = p_450701_.decode(p_458560_);
            T7 t7 = p_458252_.decode(p_458560_);
            T8 t8 = p_456208_.decode(p_458560_);
            T9 t9 = p_455786_.decode(p_458560_);
            T10 t10 = p_460726_.decode(p_458560_);
            T11 t11 = p_460366_.decode(p_458560_);
            T12 t12 = p_454504_.decode(p_458560_);
            return (C)p_452964_.apply(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12);
         }

         @Override
         public void encode(B p_451776_, C p_458198_) {
            p_458378_.encode(p_451776_, p_453541_.apply(p_458198_));
            p_456000_.encode(p_451776_, p_456302_.apply(p_458198_));
            p_451624_.encode(p_451776_, p_455650_.apply(p_458198_));
            p_453475_.encode(p_451776_, p_453203_.apply(p_458198_));
            p_453809_.encode(p_451776_, p_456740_.apply(p_458198_));
            p_450701_.encode(p_451776_, p_456188_.apply(p_458198_));
            p_458252_.encode(p_451776_, p_458819_.apply(p_458198_));
            p_456208_.encode(p_451776_, p_453119_.apply(p_458198_));
            p_455786_.encode(p_451776_, p_454342_.apply(p_458198_));
            p_460726_.encode(p_451776_, p_456439_.apply(p_458198_));
            p_460366_.encode(p_451776_, p_450933_.apply(p_458198_));
            p_454504_.encode(p_451776_, p_454812_.apply(p_458198_));
         }
      };
   }

   static <B, T> StreamCodec<B, T> recursive(final UnaryOperator<StreamCodec<B, T>> p_336362_) {
      return new StreamCodec<B, T>() {
         private final Supplier<StreamCodec<B, T>> inner = Suppliers.memoize(() -> p_336362_.apply(this));

         @Override
         public T decode(B p_456319_) {
            return this.inner.get().decode(p_456319_);
         }

         @Override
         public void encode(B p_457830_, T p_457968_) {
            this.inner.get().encode(p_457830_, p_457968_);
         }
      };
   }

   default <S extends B> StreamCodec<S, V> cast() {
      return this;
   }

   @FunctionalInterface
   interface CodecOperation<B, S, T> {
      StreamCodec<B, T> apply(StreamCodec<B, S> var1);
   }
}
