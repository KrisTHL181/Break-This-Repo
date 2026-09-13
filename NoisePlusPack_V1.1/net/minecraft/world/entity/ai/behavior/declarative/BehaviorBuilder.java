package net.minecraft.world.entity.ai.behavior.declarative;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.datafixers.kinds.IdF;
import com.mojang.datafixers.kinds.K1;
import com.mojang.datafixers.kinds.OptionalBox;
import com.mojang.datafixers.util.Function3;
import com.mojang.datafixers.util.Function4;
import com.mojang.datafixers.util.Unit;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.OneShot;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import org.jspecify.annotations.Nullable;

public class BehaviorBuilder<E extends LivingEntity, M> implements App<BehaviorBuilder.Mu<E>, M> {
   private final BehaviorBuilder.TriggerWithResult<E, M> trigger;

   public static <E extends LivingEntity, M> BehaviorBuilder<E, M> unbox(App<BehaviorBuilder.Mu<E>, M> p_259593_) {
      return (BehaviorBuilder<E, M>)p_259593_;
   }

   public static <E extends LivingEntity> BehaviorBuilder.Instance<E> instance() {
      return new BehaviorBuilder.Instance<>();
   }

   public static <E extends LivingEntity> OneShot<E> create(Function<BehaviorBuilder.Instance<E>, ? extends App<BehaviorBuilder.Mu<E>, Trigger<E>>> p_259386_) {
      final BehaviorBuilder.TriggerWithResult<E, Trigger<E>> triggerwithresult = get((App<BehaviorBuilder.Mu<E>, Trigger<E>>)p_259386_.apply(instance()));
      return new OneShot<E>() {
         @Override
         public boolean trigger(ServerLevel p_259385_, E p_260003_, long p_259194_) {
            Trigger<E> trigger = triggerwithresult.tryTrigger(p_259385_, p_260003_, p_259194_);
            return trigger == null ? false : trigger.trigger(p_259385_, p_260003_, p_259194_);
         }

         @Override
         public String debugString() {
            return "OneShot[" + triggerwithresult.debugString() + "]";
         }

         @Override
         public String toString() {
            return this.debugString();
         }
      };
   }

   public static <E extends LivingEntity> OneShot<E> sequence(Trigger<? super E> p_260174_, Trigger<? super E> p_259134_) {
      return create(p_259495_ -> p_259495_.group(p_259495_.ifTriggered(p_260174_)).apply(p_259495_, p_260322_ -> p_259134_::trigger));
   }

   public static <E extends LivingEntity> OneShot<E> triggerIf(Predicate<E> p_260059_, OneShot<? super E> p_259640_) {
      return sequence(triggerIf(p_260059_), p_259640_);
   }

   public static <E extends LivingEntity> OneShot<E> triggerIf(Predicate<E> p_260112_) {
      return create(p_260353_ -> p_260353_.point((p_259280_, p_259428_, p_259845_) -> p_260112_.test(p_259428_)));
   }

   public static <E extends LivingEntity> OneShot<E> triggerIf(BiPredicate<ServerLevel, E> p_259227_) {
      return create(p_260191_ -> p_260191_.point((p_259079_, p_259093_, p_260140_) -> p_259227_.test(p_259079_, p_259093_)));
   }

   static <E extends LivingEntity, M> BehaviorBuilder.TriggerWithResult<E, M> get(App<BehaviorBuilder.Mu<E>, M> p_259615_) {
      return unbox(p_259615_).trigger;
   }

   BehaviorBuilder(BehaviorBuilder.TriggerWithResult<E, M> p_260164_) {
      this.trigger = p_260164_;
   }

   static <E extends LivingEntity, M> BehaviorBuilder<E, M> create(BehaviorBuilder.TriggerWithResult<E, M> p_259575_) {
      return new BehaviorBuilder<>(p_259575_);
   }

   static final class Constant<E extends LivingEntity, A> extends BehaviorBuilder<E, A> {
      Constant(A p_259906_) {
         this(p_259906_, () -> "C[" + p_259906_ + "]");
      }

      Constant(final A p_259514_, final Supplier<String> p_259950_) {
         super(new BehaviorBuilder.TriggerWithResult<E, A>() {
            @Override
            public A tryTrigger(ServerLevel p_259561_, E p_259467_, long p_259297_) {
               return p_259514_;
            }

            @Override
            public String debugString() {
               return p_259950_.get();
            }

            @Override
            public String toString() {
               return this.debugString();
            }
         });
      }
   }

   public static final class Instance<E extends LivingEntity> implements Applicative<BehaviorBuilder.Mu<E>, BehaviorBuilder.Instance.Mu<E>> {
      public <Value> Optional<Value> tryGet(MemoryAccessor<com.mojang.datafixers.kinds.OptionalBox.Mu, Value> p_259352_) {
         return OptionalBox.unbox(p_259352_.value());
      }

      public <Value> Value get(MemoryAccessor<com.mojang.datafixers.kinds.IdF.Mu, Value> p_259206_) {
         return (Value)IdF.get(p_259206_.value());
      }

      public <Value> BehaviorBuilder<E, MemoryAccessor<com.mojang.datafixers.kinds.OptionalBox.Mu, Value>> registered(MemoryModuleType<Value> p_259477_) {
         return new BehaviorBuilder.PureMemory<>(new MemoryCondition.Registered<>(p_259477_));
      }

      public <Value> BehaviorBuilder<E, MemoryAccessor<com.mojang.datafixers.kinds.IdF.Mu, Value>> present(MemoryModuleType<Value> p_259673_) {
         return new BehaviorBuilder.PureMemory<>(new MemoryCondition.Present<>(p_259673_));
      }

      public <Value> BehaviorBuilder<E, MemoryAccessor<com.mojang.datafixers.kinds.Const.Mu<Unit>, Value>> absent(MemoryModuleType<Value> p_260198_) {
         return new BehaviorBuilder.PureMemory<>(new MemoryCondition.Absent<>(p_260198_));
      }

      public BehaviorBuilder<E, Unit> ifTriggered(Trigger<? super E> p_260247_) {
         return new BehaviorBuilder.TriggerWrapper<>(p_260247_);
      }

      public <A> BehaviorBuilder<E, A> point(A p_259634_) {
         return new BehaviorBuilder.Constant<>(p_259634_);
      }

      public <A> BehaviorBuilder<E, A> point(Supplier<String> p_260070_, A p_260295_) {
         return new BehaviorBuilder.Constant<>(p_260295_, p_260070_);
      }

      public <A, R> Function<App<BehaviorBuilder.Mu<E>, A>, App<BehaviorBuilder.Mu<E>, R>> lift1(App<BehaviorBuilder.Mu<E>, Function<A, R>> p_259294_) {
         return p_259751_ -> {
            final BehaviorBuilder.TriggerWithResult<E, A> triggerwithresult = (BehaviorBuilder.TriggerWithResult<E, A>)BehaviorBuilder.get(
               (App<BehaviorBuilder.Mu<E>, M>)p_259751_
            );
            final BehaviorBuilder.TriggerWithResult<E, Function<A, R>> triggerwithresult1 = BehaviorBuilder.get(p_259294_);
            return BehaviorBuilder.create(new BehaviorBuilder.TriggerWithResult<E, R>() {
               @Override
               public R tryTrigger(ServerLevel p_259603_, E p_260233_, long p_259654_) {
                  A a = (A)triggerwithresult.tryTrigger(p_259603_, p_260233_, p_259654_);
                  if (a == null) {
                     return null;
                  }

                  Function<A, R> function = (Function<A, R>)triggerwithresult1.tryTrigger(p_259603_, p_260233_, p_259654_);
                  return (R)(function == null ? null : function.apply(a));
               }

               @Override
               public String debugString() {
                  return triggerwithresult1.debugString() + " * " + triggerwithresult.debugString();
               }

               @Override
               public String toString() {
                  return this.debugString();
               }
            });
         };
      }

      public <T, R> BehaviorBuilder<E, R> map(final Function<? super T, ? extends R> p_259963_, App<BehaviorBuilder.Mu<E>, T> p_260355_) {
         final BehaviorBuilder.TriggerWithResult<E, T> triggerwithresult = (BehaviorBuilder.TriggerWithResult<E, T>)BehaviorBuilder.get(
            (App<BehaviorBuilder.Mu<E>, M>)p_260355_
         );
         return BehaviorBuilder.create(new BehaviorBuilder.TriggerWithResult<E, R>() {
            @Override
            public R tryTrigger(ServerLevel p_259755_, E p_259656_, long p_259300_) {
               T t = triggerwithresult.tryTrigger(p_259755_, p_259656_, p_259300_);
               return (R)(t == null ? null : p_259963_.apply(t));
            }

            @Override
            public String debugString() {
               return triggerwithresult.debugString() + ".map[" + p_259963_ + "]";
            }

            @Override
            public String toString() {
               return this.debugString();
            }
         });
      }

      public <A, B, R> BehaviorBuilder<E, R> ap2(
         App<BehaviorBuilder.Mu<E>, BiFunction<A, B, R>> p_259535_, App<BehaviorBuilder.Mu<E>, A> p_259162_, App<BehaviorBuilder.Mu<E>, B> p_259733_
      ) {
         final BehaviorBuilder.TriggerWithResult<E, A> triggerwithresult = (BehaviorBuilder.TriggerWithResult<E, A>)BehaviorBuilder.get(
            (App<BehaviorBuilder.Mu<E>, M>)p_259162_
         );
         final BehaviorBuilder.TriggerWithResult<E, B> triggerwithresult1 = (BehaviorBuilder.TriggerWithResult<E, B>)BehaviorBuilder.get(
            (App<BehaviorBuilder.Mu<E>, M>)p_259733_
         );
         final BehaviorBuilder.TriggerWithResult<E, BiFunction<A, B, R>> triggerwithresult2 = BehaviorBuilder.get(p_259535_);
         return BehaviorBuilder.create(new BehaviorBuilder.TriggerWithResult<E, R>() {
            @Override
            public R tryTrigger(ServerLevel p_259274_, E p_259817_, long p_259820_) {
               A a = triggerwithresult.tryTrigger(p_259274_, p_259817_, p_259820_);
               if (a == null) {
                  return null;
               }

               B b = triggerwithresult1.tryTrigger(p_259274_, p_259817_, p_259820_);
               if (b == null) {
                  return null;
               }

               BiFunction<A, B, R> bifunction = triggerwithresult2.tryTrigger(p_259274_, p_259817_, p_259820_);
               return bifunction == null ? null : bifunction.apply(a, b);
            }

            @Override
            public String debugString() {
               return triggerwithresult2.debugString() + " * " + triggerwithresult.debugString() + " * " + triggerwithresult1.debugString();
            }

            @Override
            public String toString() {
               return this.debugString();
            }
         });
      }

      public <T1, T2, T3, R> BehaviorBuilder<E, R> ap3(
         App<BehaviorBuilder.Mu<E>, Function3<T1, T2, T3, R>> p_260239_,
         App<BehaviorBuilder.Mu<E>, T1> p_259239_,
         App<BehaviorBuilder.Mu<E>, T2> p_259638_,
         App<BehaviorBuilder.Mu<E>, T3> p_259969_
      ) {
         final BehaviorBuilder.TriggerWithResult<E, T1> triggerwithresult = (BehaviorBuilder.TriggerWithResult<E, T1>)BehaviorBuilder.get(
            (App<BehaviorBuilder.Mu<E>, M>)p_259239_
         );
         final BehaviorBuilder.TriggerWithResult<E, T2> triggerwithresult1 = (BehaviorBuilder.TriggerWithResult<E, T2>)BehaviorBuilder.get(
            (App<BehaviorBuilder.Mu<E>, M>)p_259638_
         );
         final BehaviorBuilder.TriggerWithResult<E, T3> triggerwithresult2 = (BehaviorBuilder.TriggerWithResult<E, T3>)BehaviorBuilder.get(
            (App<BehaviorBuilder.Mu<E>, M>)p_259969_
         );
         final BehaviorBuilder.TriggerWithResult<E, Function3<T1, T2, T3, R>> triggerwithresult3 = BehaviorBuilder.get(p_260239_);
         return BehaviorBuilder.create(
            new BehaviorBuilder.TriggerWithResult<E, R>() {
               @Override
               public R tryTrigger(ServerLevel p_259096_, E p_260221_, long p_259035_) {
                  T1 t1 = triggerwithresult.tryTrigger(p_259096_, p_260221_, p_259035_);
                  if (t1 == null) {
                     return null;
                  }

                  T2 t2 = triggerwithresult1.tryTrigger(p_259096_, p_260221_, p_259035_);
                  if (t2 == null) {
                     return null;
                  }

                  T3 t3 = triggerwithresult2.tryTrigger(p_259096_, p_260221_, p_259035_);
                  if (t3 == null) {
                     return null;
                  }

                  Function3<T1, T2, T3, R> function3 = triggerwithresult3.tryTrigger(p_259096_, p_260221_, p_259035_);
                  return (R)(function3 == null ? null : function3.apply(t1, t2, t3));
               }

               @Override
               public String debugString() {
                  return triggerwithresult3.debugString()
                     + " * "
                     + triggerwithresult.debugString()
                     + " * "
                     + triggerwithresult1.debugString()
                     + " * "
                     + triggerwithresult2.debugString();
               }

               @Override
               public String toString() {
                  return this.debugString();
               }
            }
         );
      }

      public <T1, T2, T3, T4, R> BehaviorBuilder<E, R> ap4(
         App<BehaviorBuilder.Mu<E>, Function4<T1, T2, T3, T4, R>> p_259519_,
         App<BehaviorBuilder.Mu<E>, T1> p_259829_,
         App<BehaviorBuilder.Mu<E>, T2> p_259314_,
         App<BehaviorBuilder.Mu<E>, T3> p_260089_,
         App<BehaviorBuilder.Mu<E>, T4> p_259136_
      ) {
         final BehaviorBuilder.TriggerWithResult<E, T1> triggerwithresult = (BehaviorBuilder.TriggerWithResult<E, T1>)BehaviorBuilder.get(
            (App<BehaviorBuilder.Mu<E>, M>)p_259829_
         );
         final BehaviorBuilder.TriggerWithResult<E, T2> triggerwithresult1 = (BehaviorBuilder.TriggerWithResult<E, T2>)BehaviorBuilder.get(
            (App<BehaviorBuilder.Mu<E>, M>)p_259314_
         );
         final BehaviorBuilder.TriggerWithResult<E, T3> triggerwithresult2 = (BehaviorBuilder.TriggerWithResult<E, T3>)BehaviorBuilder.get(
            (App<BehaviorBuilder.Mu<E>, M>)p_260089_
         );
         final BehaviorBuilder.TriggerWithResult<E, T4> triggerwithresult3 = (BehaviorBuilder.TriggerWithResult<E, T4>)BehaviorBuilder.get(
            (App<BehaviorBuilder.Mu<E>, M>)p_259136_
         );
         final BehaviorBuilder.TriggerWithResult<E, Function4<T1, T2, T3, T4, R>> triggerwithresult4 = BehaviorBuilder.get(p_259519_);
         return BehaviorBuilder.create(
            new BehaviorBuilder.TriggerWithResult<E, R>() {
               @Override
               public R tryTrigger(ServerLevel p_259537_, E p_259581_, long p_259423_) {
                  T1 t1 = triggerwithresult.tryTrigger(p_259537_, p_259581_, p_259423_);
                  if (t1 == null) {
                     return null;
                  }

                  T2 t2 = triggerwithresult1.tryTrigger(p_259537_, p_259581_, p_259423_);
                  if (t2 == null) {
                     return null;
                  }

                  T3 t3 = triggerwithresult2.tryTrigger(p_259537_, p_259581_, p_259423_);
                  if (t3 == null) {
                     return null;
                  }

                  T4 t4 = triggerwithresult3.tryTrigger(p_259537_, p_259581_, p_259423_);
                  if (t4 == null) {
                     return null;
                  }

                  Function4<T1, T2, T3, T4, R> function4 = triggerwithresult4.tryTrigger(p_259537_, p_259581_, p_259423_);
                  return (R)(function4 == null ? null : function4.apply(t1, t2, t3, t4));
               }

               @Override
               public String debugString() {
                  return triggerwithresult4.debugString()
                     + " * "
                     + triggerwithresult.debugString()
                     + " * "
                     + triggerwithresult1.debugString()
                     + " * "
                     + triggerwithresult2.debugString()
                     + " * "
                     + triggerwithresult3.debugString();
               }

               @Override
               public String toString() {
                  return this.debugString();
               }
            }
         );
      }

      static final class Mu<E extends LivingEntity> implements com.mojang.datafixers.kinds.Applicative.Mu {
         private Mu() {
         }
      }
   }

   public static final class Mu<E extends LivingEntity> implements K1 {
   }

   static final class PureMemory<E extends LivingEntity, F extends K1, Value> extends BehaviorBuilder<E, MemoryAccessor<F, Value>> {
      PureMemory(final MemoryCondition<F, Value> p_259776_) {
         super(new BehaviorBuilder.TriggerWithResult<E, MemoryAccessor<F, Value>>() {
            public @Nullable MemoryAccessor<F, Value> tryTrigger(ServerLevel p_259899_, E p_259558_, long p_259793_) {
               Brain<?> brain = p_259558_.getBrain();
               Optional<Value> optional = brain.getMemoryInternal(p_259776_.memory());
               return optional == null ? null : p_259776_.createAccessor(brain, optional);
            }

            @Override
            public String debugString() {
               return "M[" + p_259776_ + "]";
            }

            @Override
            public String toString() {
               return this.debugString();
            }
         });
      }
   }

   interface TriggerWithResult<E extends LivingEntity, R> {
      @Nullable R tryTrigger(ServerLevel var1, E var2, long var3);

      String debugString();
   }

   static final class TriggerWrapper<E extends LivingEntity> extends BehaviorBuilder<E, Unit> {
      TriggerWrapper(final Trigger<? super E> p_259310_) {
         super(new BehaviorBuilder.TriggerWithResult<E, Unit>() {
            public @Nullable Unit tryTrigger(ServerLevel p_259397_, E p_260169_, long p_259155_) {
               return p_259310_.trigger(p_259397_, p_260169_, p_259155_) ? Unit.INSTANCE : null;
            }

            @Override
            public String debugString() {
               return "T[" + p_259310_ + "]";
            }
         });
      }
   }
}
