package net.minecraft.world.entity.ai.behavior.declarative;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.datafixers.kinds.Const;
import com.mojang.datafixers.kinds.IdF;
import com.mojang.datafixers.kinds.K1;
import com.mojang.datafixers.kinds.OptionalBox;
import com.mojang.datafixers.util.Function3;
import com.mojang.datafixers.util.Function4;
import com.mojang.datafixers.util.Unit;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
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

    public static <E extends LivingEntity, M> BehaviorBuilder<E, M> unbox(final App<BehaviorBuilder.Mu<E>, M> box) {
        return (BehaviorBuilder<E, M>)box;
    }

    public static <E extends LivingEntity> BehaviorBuilder.Instance<E> instance() {
        return new BehaviorBuilder.Instance<>();
    }

    public static <E extends LivingEntity> OneShot<E> create(
        final Function<BehaviorBuilder.Instance<E>, ? extends App<BehaviorBuilder.Mu<E>, Trigger<E>>> builder
    ) {
        final BehaviorBuilder.TriggerWithResult<E, Trigger<E>> resolvedBuilder = get((App<BehaviorBuilder.Mu<E>, Trigger<E>>)builder.apply(instance()));
        return new OneShot<E>() {
            @Override
            public boolean trigger(final ServerLevel level, final E body, final long timestamp) {
                Trigger<E> trigger = resolvedBuilder.tryTrigger(level, body, timestamp);
                return trigger == null ? false : trigger.trigger(level, body, timestamp);
            }

            @Override
            public Set<MemoryModuleType<?>> getRequiredMemories() {
                return resolvedBuilder.memories();
            }

            @Override
            public String debugString() {
                return "OneShot[" + resolvedBuilder.debugString() + "]";
            }

            @Override
            public String toString() {
                return this.debugString();
            }
        };
    }

    public static <E extends LivingEntity> OneShot<E> sequence(final Trigger<? super E> first, final OneShot<? super E> second) {
        final OneShot<E> wrapped = create(i -> i.group(i.ifTriggered(first)).apply(i, var1x -> second::trigger));
        return new OneShot<E>() {
            @Override
            public boolean trigger(final ServerLevel level, final E body, final long timestamp) {
                return wrapped.trigger(level, body, timestamp);
            }

            @Override
            public Set<MemoryModuleType<?>> getRequiredMemories() {
                Set<MemoryModuleType<?>> memories = new HashSet<>();
                memories.addAll(wrapped.getRequiredMemories());
                memories.addAll(second.getRequiredMemories());
                return memories;
            }

            @Override
            public String debugString() {
                return "OneShot[stuff]";
            }

            @Override
            public String toString() {
                return this.debugString();
            }
        };
    }

    public static <E extends LivingEntity> OneShot<E> triggerIf(final Predicate<E> predicate, final OneShot<? super E> behavior) {
        return sequence(triggerIf(predicate), behavior);
    }

    public static <E extends LivingEntity> OneShot<E> triggerIf(final Predicate<E> predicate) {
        return create(i -> i.point((level, body, timestamp) -> predicate.test(body)));
    }

    public static <E extends LivingEntity> OneShot<E> triggerIf(final BiPredicate<ServerLevel, E> predicate) {
        return create(i -> i.point((level, body, timestamp) -> predicate.test(level, body)));
    }

    private static <E extends LivingEntity, M> BehaviorBuilder.TriggerWithResult<E, M> get(final App<BehaviorBuilder.Mu<E>, M> box) {
        return unbox(box).trigger;
    }

    private BehaviorBuilder(final BehaviorBuilder.TriggerWithResult<E, M> trigger) {
        this.trigger = trigger;
    }

    private static <E extends LivingEntity, M> BehaviorBuilder<E, M> create(final BehaviorBuilder.TriggerWithResult<E, M> instanceFactory) {
        return new BehaviorBuilder<>(instanceFactory);
    }

    private static final class Constant<E extends LivingEntity, A> extends BehaviorBuilder<E, A> {
        private Constant(final A a) {
            this(a, () -> "C[" + a + "]");
        }

        private Constant(final A a, final Supplier<String> debugString) {
            super(new BehaviorBuilder.TriggerWithResult<E, A>() {
                @Override
                public A tryTrigger(final ServerLevel level, final E body, final long timestamp) {
                    return a;
                }

                @Override
                public Set<MemoryModuleType<?>> memories() {
                    return Set.of();
                }

                @Override
                public String debugString() {
                    return debugString.get();
                }

                @Override
                public String toString() {
                    return this.debugString();
                }
            });
        }
    }

    public static final class Instance<E extends LivingEntity> implements Applicative<BehaviorBuilder.Mu<E>, BehaviorBuilder.Instance.Mu<E>> {
        public <Value> Optional<Value> tryGet(final MemoryAccessor<OptionalBox.Mu, Value> box) {
            return OptionalBox.unbox(box.value());
        }

        public <Value> Value get(final MemoryAccessor<IdF.Mu, Value> box) {
            return IdF.get(box.value());
        }

        public <Value> BehaviorBuilder<E, MemoryAccessor<OptionalBox.Mu, Value>> registered(final MemoryModuleType<Value> memory) {
            return new BehaviorBuilder.PureMemory<>(new MemoryCondition.Registered<>(memory));
        }

        public <Value> BehaviorBuilder<E, MemoryAccessor<IdF.Mu, Value>> present(final MemoryModuleType<Value> memory) {
            return new BehaviorBuilder.PureMemory<>(new MemoryCondition.Present<>(memory));
        }

        public <Value> BehaviorBuilder<E, MemoryAccessor<Const.Mu<Unit>, Value>> absent(final MemoryModuleType<Value> memory) {
            return new BehaviorBuilder.PureMemory<>(new MemoryCondition.Absent<>(memory));
        }

        public BehaviorBuilder<E, Unit> ifTriggered(final Trigger<? super E> dependentTrigger) {
            return new BehaviorBuilder.TriggerWrapper<>(dependentTrigger);
        }

        public <A> BehaviorBuilder<E, A> point(final A a) {
            return new BehaviorBuilder.Constant<>(a);
        }

        public <A> BehaviorBuilder<E, A> point(final Supplier<String> debugString, final A a) {
            return new BehaviorBuilder.Constant<>(a, debugString);
        }

        @Override
        public <A, R> Function<App<BehaviorBuilder.Mu<E>, A>, App<BehaviorBuilder.Mu<E>, R>> lift1(final App<BehaviorBuilder.Mu<E>, Function<A, R>> function) {
            return a -> {
                final BehaviorBuilder.TriggerWithResult<E, A> aTrigger = BehaviorBuilder.get(a);
                final BehaviorBuilder.TriggerWithResult<E, Function<A, R>> fTrigger = BehaviorBuilder.get(function);
                return BehaviorBuilder.create(new BehaviorBuilder.TriggerWithResult<E, R>() {
                    @Override
                    public R tryTrigger(final ServerLevel level, final E body, final long timestamp) {
                        A ra = (A)aTrigger.tryTrigger(level, body, timestamp);
                        if (ra == null) {
                            return null;
                        }

                        Function<A, R> rf = (Function<A, R>)fTrigger.tryTrigger(level, body, timestamp);
                        return (R)(rf == null ? null : rf.apply(ra));
                    }

                    @Override
                    public Set<MemoryModuleType<?>> memories() {
                        Set<MemoryModuleType<?>> memories = new HashSet<>();
                        memories.addAll(aTrigger.memories());
                        memories.addAll(fTrigger.memories());
                        return memories;
                    }

                    @Override
                    public String debugString() {
                        return fTrigger.debugString() + " * " + aTrigger.debugString();
                    }

                    @Override
                    public String toString() {
                        return this.debugString();
                    }
                });
            };
        }

        public <T, R> BehaviorBuilder<E, R> map(final Function<? super T, ? extends R> func, final App<BehaviorBuilder.Mu<E>, T> ts) {
            final BehaviorBuilder.TriggerWithResult<E, T> tTrigger = BehaviorBuilder.get(ts);
            return BehaviorBuilder.create(new BehaviorBuilder.TriggerWithResult<E, R>() {
                @Override
                public R tryTrigger(final ServerLevel level, final E body, final long timestamp) {
                    T t = tTrigger.tryTrigger(level, body, timestamp);
                    return (R)(t == null ? null : func.apply(t));
                }

                @Override
                public Set<MemoryModuleType<?>> memories() {
                    return tTrigger.memories();
                }

                @Override
                public String debugString() {
                    return tTrigger.debugString() + ".map[" + func + "]";
                }

                @Override
                public String toString() {
                    return this.debugString();
                }
            });
        }

        public <A, B, R> BehaviorBuilder<E, R> ap2(
            final App<BehaviorBuilder.Mu<E>, BiFunction<A, B, R>> func, final App<BehaviorBuilder.Mu<E>, A> a, final App<BehaviorBuilder.Mu<E>, B> b
        ) {
            final BehaviorBuilder.TriggerWithResult<E, A> aTrigger = BehaviorBuilder.get(a);
            final BehaviorBuilder.TriggerWithResult<E, B> bTrigger = BehaviorBuilder.get(b);
            final BehaviorBuilder.TriggerWithResult<E, BiFunction<A, B, R>> fTrigger = BehaviorBuilder.get(func);
            return BehaviorBuilder.create(new BehaviorBuilder.TriggerWithResult<E, R>() {
                @Override
                public R tryTrigger(final ServerLevel level, final E body, final long timestamp) {
                    A ra = aTrigger.tryTrigger(level, body, timestamp);
                    if (ra == null) {
                        return null;
                    }

                    B rb = bTrigger.tryTrigger(level, body, timestamp);
                    if (rb == null) {
                        return null;
                    }

                    BiFunction<A, B, R> fr = fTrigger.tryTrigger(level, body, timestamp);
                    return fr == null ? null : fr.apply(ra, rb);
                }

                @Override
                public Set<MemoryModuleType<?>> memories() {
                    Set<MemoryModuleType<?>> memories = new HashSet<>();
                    memories.addAll(aTrigger.memories());
                    memories.addAll(bTrigger.memories());
                    memories.addAll(fTrigger.memories());
                    return memories;
                }

                @Override
                public String debugString() {
                    return fTrigger.debugString() + " * " + aTrigger.debugString() + " * " + bTrigger.debugString();
                }

                @Override
                public String toString() {
                    return this.debugString();
                }
            });
        }

        public <T1, T2, T3, R> BehaviorBuilder<E, R> ap3(
            final App<BehaviorBuilder.Mu<E>, Function3<T1, T2, T3, R>> func,
            final App<BehaviorBuilder.Mu<E>, T1> t1,
            final App<BehaviorBuilder.Mu<E>, T2> t2,
            final App<BehaviorBuilder.Mu<E>, T3> t3
        ) {
            final BehaviorBuilder.TriggerWithResult<E, T1> t1Trigger = BehaviorBuilder.get(t1);
            final BehaviorBuilder.TriggerWithResult<E, T2> t2Trigger = BehaviorBuilder.get(t2);
            final BehaviorBuilder.TriggerWithResult<E, T3> t3Trigger = BehaviorBuilder.get(t3);
            final BehaviorBuilder.TriggerWithResult<E, Function3<T1, T2, T3, R>> fTrigger = BehaviorBuilder.get(func);
            return BehaviorBuilder.create(new BehaviorBuilder.TriggerWithResult<E, R>() {
                @Override
                public R tryTrigger(final ServerLevel level, final E body, final long timestamp) {
                    T1 r1 = t1Trigger.tryTrigger(level, body, timestamp);
                    if (r1 == null) {
                        return null;
                    }

                    T2 r2 = t2Trigger.tryTrigger(level, body, timestamp);
                    if (r2 == null) {
                        return null;
                    }

                    T3 r3 = t3Trigger.tryTrigger(level, body, timestamp);
                    if (r3 == null) {
                        return null;
                    }

                    Function3<T1, T2, T3, R> rf = fTrigger.tryTrigger(level, body, timestamp);
                    return rf == null ? null : rf.apply(r1, r2, r3);
                }

                @Override
                public Set<MemoryModuleType<?>> memories() {
                    Set<MemoryModuleType<?>> memories = new HashSet<>();
                    memories.addAll(t1Trigger.memories());
                    memories.addAll(t2Trigger.memories());
                    memories.addAll(t3Trigger.memories());
                    memories.addAll(fTrigger.memories());
                    return memories;
                }

                @Override
                public String debugString() {
                    return fTrigger.debugString() + " * " + t1Trigger.debugString() + " * " + t2Trigger.debugString() + " * " + t3Trigger.debugString();
                }

                @Override
                public String toString() {
                    return this.debugString();
                }
            });
        }

        public <T1, T2, T3, T4, R> BehaviorBuilder<E, R> ap4(
            final App<BehaviorBuilder.Mu<E>, Function4<T1, T2, T3, T4, R>> func,
            final App<BehaviorBuilder.Mu<E>, T1> t1,
            final App<BehaviorBuilder.Mu<E>, T2> t2,
            final App<BehaviorBuilder.Mu<E>, T3> t3,
            final App<BehaviorBuilder.Mu<E>, T4> t4
        ) {
            final BehaviorBuilder.TriggerWithResult<E, T1> t1Trigger = BehaviorBuilder.get(t1);
            final BehaviorBuilder.TriggerWithResult<E, T2> t2Trigger = BehaviorBuilder.get(t2);
            final BehaviorBuilder.TriggerWithResult<E, T3> t3Trigger = BehaviorBuilder.get(t3);
            final BehaviorBuilder.TriggerWithResult<E, T4> t4Trigger = BehaviorBuilder.get(t4);
            final BehaviorBuilder.TriggerWithResult<E, Function4<T1, T2, T3, T4, R>> fTrigger = BehaviorBuilder.get(func);
            return BehaviorBuilder.create(
                new BehaviorBuilder.TriggerWithResult<E, R>() {
                    @Override
                    public R tryTrigger(final ServerLevel level, final E body, final long timestamp) {
                        T1 r1 = t1Trigger.tryTrigger(level, body, timestamp);
                        if (r1 == null) {
                            return null;
                        }

                        T2 r2 = t2Trigger.tryTrigger(level, body, timestamp);
                        if (r2 == null) {
                            return null;
                        }

                        T3 r3 = t3Trigger.tryTrigger(level, body, timestamp);
                        if (r3 == null) {
                            return null;
                        }

                        T4 r4 = t4Trigger.tryTrigger(level, body, timestamp);
                        if (r4 == null) {
                            return null;
                        }

                        Function4<T1, T2, T3, T4, R> rf = fTrigger.tryTrigger(level, body, timestamp);
                        return rf == null ? null : rf.apply(r1, r2, r3, r4);
                    }

                    @Override
                    public Set<MemoryModuleType<?>> memories() {
                        Set<MemoryModuleType<?>> memories = new HashSet<>();
                        memories.addAll(t1Trigger.memories());
                        memories.addAll(t2Trigger.memories());
                        memories.addAll(t3Trigger.memories());
                        memories.addAll(t4Trigger.memories());
                        memories.addAll(fTrigger.memories());
                        return memories;
                    }

                    @Override
                    public String debugString() {
                        return fTrigger.debugString()
                            + " * "
                            + t1Trigger.debugString()
                            + " * "
                            + t2Trigger.debugString()
                            + " * "
                            + t3Trigger.debugString()
                            + " * "
                            + t4Trigger.debugString();
                    }

                    @Override
                    public String toString() {
                        return this.debugString();
                    }
                }
            );
        }

        private static final class Mu<E extends LivingEntity> implements Applicative.Mu {
        }
    }

    public static final class Mu<E extends LivingEntity> implements K1 {
    }

    private static final class PureMemory<E extends LivingEntity, F extends K1, Value> extends BehaviorBuilder<E, MemoryAccessor<F, Value>> {
        private PureMemory(final MemoryCondition<F, Value> condition) {
            super(new BehaviorBuilder.TriggerWithResult<E, MemoryAccessor<F, Value>>() {
                public @Nullable MemoryAccessor<F, Value> tryTrigger(final ServerLevel level, final E body, final long timestamp) {
                    Brain<?> brain = body.getBrain();
                    Optional<Value> value = brain.getMemoryInternal(condition.memory());
                    return value == null ? null : condition.createAccessor(brain, value);
                }

                @Override
                public Set<MemoryModuleType<?>> memories() {
                    return Set.of(condition.memory());
                }

                @Override
                public String debugString() {
                    return "M[" + condition + "]";
                }

                @Override
                public String toString() {
                    return this.debugString();
                }
            });
        }
    }

    private interface TriggerWithResult<E extends LivingEntity, R> {
        @Nullable R tryTrigger(final ServerLevel level, final E body, final long timestamp);

        Set<MemoryModuleType<?>> memories();

        String debugString();
    }

    private static final class TriggerWrapper<E extends LivingEntity> extends BehaviorBuilder<E, Unit> {
        private TriggerWrapper(final Trigger<? super E> dependentTrigger) {
            super(new BehaviorBuilder.TriggerWithResult<E, Unit>() {
                public @Nullable Unit tryTrigger(final ServerLevel level, final E body, final long timestamp) {
                    return dependentTrigger.trigger(level, body, timestamp) ? Unit.INSTANCE : null;
                }

                @Override
                public Set<MemoryModuleType<?>> memories() {
                    return Set.of();
                }

                @Override
                public String debugString() {
                    return "T[" + dependentTrigger + "]";
                }
            });
        }
    }
}