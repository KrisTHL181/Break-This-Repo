package net.minecraft.client.data.models.blockstates;

import com.mojang.datafixers.util.Function3;
import com.mojang.datafixers.util.Function4;
import com.mojang.datafixers.util.Function5;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.client.renderer.block.model.VariantMutator;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class PropertyDispatch<V> {
   private final Map<PropertyValueList, V> values = new HashMap<>();

   protected void putValue(PropertyValueList p_391165_, V p_391702_) {
      V v = this.values.put(p_391165_, p_391702_);
      if (v != null) {
         throw new IllegalStateException("Value " + p_391165_ + " is already defined");
      }
   }

   Map<PropertyValueList, V> getEntries() {
      this.verifyComplete();
      return Map.copyOf(this.values);
   }

   private void verifyComplete() {
      List<Property<?>> list = this.getDefinedProperties();
      Stream<PropertyValueList> stream = Stream.of(PropertyValueList.EMPTY);

      for (Property<?> property : list) {
         stream = stream.flatMap(p_396264_ -> property.getAllValues().map(p_396264_::extend));
      }

      List<PropertyValueList> list1 = stream.filter(p_394003_ -> !this.values.containsKey(p_394003_)).toList();
      if (!list1.isEmpty()) {
         throw new IllegalStateException("Missing definition for properties: " + list1);
      }
   }

   abstract List<Property<?>> getDefinedProperties();

   public static <T1 extends Comparable<T1>> PropertyDispatch.C1<MultiVariant, T1> initial(Property<T1> p_375693_) {
      return new PropertyDispatch.C1<>(p_375693_);
   }

   public static <T1 extends Comparable<T1>, T2 extends Comparable<T2>> PropertyDispatch.C2<MultiVariant, T1, T2> initial(
      Property<T1> p_378486_, Property<T2> p_376121_
   ) {
      return new PropertyDispatch.C2<>(p_378486_, p_376121_);
   }

   public static <T1 extends Comparable<T1>, T2 extends Comparable<T2>, T3 extends Comparable<T3>> PropertyDispatch.C3<MultiVariant, T1, T2, T3> initial(
      Property<T1> p_394539_, Property<T2> p_393348_, Property<T3> p_396231_
   ) {
      return new PropertyDispatch.C3<>(p_394539_, p_393348_, p_396231_);
   }

   public static <T1 extends Comparable<T1>, T2 extends Comparable<T2>, T3 extends Comparable<T3>, T4 extends Comparable<T4>> PropertyDispatch.C4<MultiVariant, T1, T2, T3, T4> initial(
      Property<T1> p_396442_, Property<T2> p_392333_, Property<T3> p_396555_, Property<T4> p_393660_
   ) {
      return new PropertyDispatch.C4<>(p_396442_, p_392333_, p_396555_, p_393660_);
   }

   public static <T1 extends Comparable<T1>, T2 extends Comparable<T2>, T3 extends Comparable<T3>, T4 extends Comparable<T4>, T5 extends Comparable<T5>> PropertyDispatch.C5<MultiVariant, T1, T2, T3, T4, T5> initial(
      Property<T1> p_378288_, Property<T2> p_376698_, Property<T3> p_375794_, Property<T4> p_377627_, Property<T5> p_377745_
   ) {
      return new PropertyDispatch.C5<>(p_378288_, p_376698_, p_375794_, p_377627_, p_377745_);
   }

   public static <T1 extends Comparable<T1>> PropertyDispatch.C1<VariantMutator, T1> modify(Property<T1> p_392002_) {
      return new PropertyDispatch.C1<>(p_392002_);
   }

   public static <T1 extends Comparable<T1>, T2 extends Comparable<T2>> PropertyDispatch.C2<VariantMutator, T1, T2> modify(
      Property<T1> p_391326_, Property<T2> p_393658_
   ) {
      return new PropertyDispatch.C2<>(p_391326_, p_393658_);
   }

   public static <T1 extends Comparable<T1>, T2 extends Comparable<T2>, T3 extends Comparable<T3>> PropertyDispatch.C3<VariantMutator, T1, T2, T3> modify(
      Property<T1> p_378219_, Property<T2> p_376157_, Property<T3> p_377920_
   ) {
      return new PropertyDispatch.C3<>(p_378219_, p_376157_, p_377920_);
   }

   public static <T1 extends Comparable<T1>, T2 extends Comparable<T2>, T3 extends Comparable<T3>, T4 extends Comparable<T4>> PropertyDispatch.C4<VariantMutator, T1, T2, T3, T4> modify(
      Property<T1> p_376975_, Property<T2> p_376597_, Property<T3> p_375517_, Property<T4> p_375767_
   ) {
      return new PropertyDispatch.C4<>(p_376975_, p_376597_, p_375517_, p_375767_);
   }

   public static <T1 extends Comparable<T1>, T2 extends Comparable<T2>, T3 extends Comparable<T3>, T4 extends Comparable<T4>, T5 extends Comparable<T5>> PropertyDispatch.C5<VariantMutator, T1, T2, T3, T4, T5> modify(
      Property<T1> p_392424_, Property<T2> p_393722_, Property<T3> p_394175_, Property<T4> p_395776_, Property<T5> p_393427_
   ) {
      return new PropertyDispatch.C5<>(p_392424_, p_393722_, p_394175_, p_395776_, p_393427_);
   }

   @OnlyIn(Dist.CLIENT)
   public static class C1<V, T1 extends Comparable<T1>> extends PropertyDispatch<V> {
      private final Property<T1> property1;

      C1(Property<T1> p_377319_) {
         this.property1 = p_377319_;
      }

      @Override
      public List<Property<?>> getDefinedProperties() {
         return List.of(this.property1);
      }

      public PropertyDispatch.C1<V, T1> select(T1 p_377138_, V p_391518_) {
         PropertyValueList propertyvaluelist = PropertyValueList.of(this.property1.value(p_377138_));
         this.putValue(propertyvaluelist, p_391518_);
         return this;
      }

      public PropertyDispatch<V> generate(Function<T1, V> p_376293_) {
         this.property1.getPossibleValues().forEach(p_389286_ -> this.select((T1)p_389286_, p_376293_.apply((T1)p_389286_)));
         return this;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class C2<V, T1 extends Comparable<T1>, T2 extends Comparable<T2>> extends PropertyDispatch<V> {
      private final Property<T1> property1;
      private final Property<T2> property2;

      C2(Property<T1> p_377098_, Property<T2> p_375939_) {
         this.property1 = p_377098_;
         this.property2 = p_375939_;
      }

      @Override
      public List<Property<?>> getDefinedProperties() {
         return List.of(this.property1, this.property2);
      }

      public PropertyDispatch.C2<V, T1, T2> select(T1 p_375979_, T2 p_375490_, V p_392168_) {
         PropertyValueList propertyvaluelist = PropertyValueList.of(this.property1.value(p_375979_), this.property2.value(p_375490_));
         this.putValue(propertyvaluelist, p_392168_);
         return this;
      }

      public PropertyDispatch<V> generate(BiFunction<T1, T2, V> p_376615_) {
         this.property1
            .getPossibleValues()
            .forEach(
               p_377154_ -> this.property2
                  .getPossibleValues()
                  .forEach(p_389289_ -> this.select((T1)p_377154_, (T2)p_389289_, p_376615_.apply((T1)p_377154_, (T2)p_389289_)))
            );
         return this;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class C3<V, T1 extends Comparable<T1>, T2 extends Comparable<T2>, T3 extends Comparable<T3>> extends PropertyDispatch<V> {
      private final Property<T1> property1;
      private final Property<T2> property2;
      private final Property<T3> property3;

      C3(Property<T1> p_378639_, Property<T2> p_378424_, Property<T3> p_376367_) {
         this.property1 = p_378639_;
         this.property2 = p_378424_;
         this.property3 = p_376367_;
      }

      @Override
      public List<Property<?>> getDefinedProperties() {
         return List.of(this.property1, this.property2, this.property3);
      }

      public PropertyDispatch.C3<V, T1, T2, T3> select(T1 p_375963_, T2 p_376963_, T3 p_376668_, V p_396210_) {
         PropertyValueList propertyvaluelist = PropertyValueList.of(
            this.property1.value(p_375963_), this.property2.value(p_376963_), this.property3.value(p_376668_)
         );
         this.putValue(propertyvaluelist, p_396210_);
         return this;
      }

      public PropertyDispatch<V> generate(Function3<T1, T2, T3, V> p_392989_) {
         this.property1
            .getPossibleValues()
            .forEach(
               p_377047_ -> this.property2
                  .getPossibleValues()
                  .forEach(
                     p_377231_ -> this.property3
                        .getPossibleValues()
                        .forEach(p_389293_ -> this.select((T1)p_377047_, (T2)p_377231_, (T3)p_389293_, (V)p_392989_.apply(p_377047_, p_377231_, p_389293_)))
                  )
            );
         return this;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class C4<V, T1 extends Comparable<T1>, T2 extends Comparable<T2>, T3 extends Comparable<T3>, T4 extends Comparable<T4>>
      extends PropertyDispatch<V> {
      private final Property<T1> property1;
      private final Property<T2> property2;
      private final Property<T3> property3;
      private final Property<T4> property4;

      C4(Property<T1> p_377852_, Property<T2> p_377209_, Property<T3> p_378386_, Property<T4> p_376113_) {
         this.property1 = p_377852_;
         this.property2 = p_377209_;
         this.property3 = p_378386_;
         this.property4 = p_376113_;
      }

      @Override
      public List<Property<?>> getDefinedProperties() {
         return List.of(this.property1, this.property2, this.property3, this.property4);
      }

      public PropertyDispatch.C4<V, T1, T2, T3, T4> select(T1 p_378307_, T2 p_376465_, T3 p_377599_, T4 p_378302_, V p_395358_) {
         PropertyValueList propertyvaluelist = PropertyValueList.of(
            this.property1.value(p_378307_), this.property2.value(p_376465_), this.property3.value(p_377599_), this.property4.value(p_378302_)
         );
         this.putValue(propertyvaluelist, p_395358_);
         return this;
      }

      public PropertyDispatch<V> generate(Function4<T1, T2, T3, T4, V> p_392096_) {
         this.property1
            .getPossibleValues()
            .forEach(
               p_376254_ -> this.property2
                  .getPossibleValues()
                  .forEach(
                     p_375541_ -> this.property3
                        .getPossibleValues()
                        .forEach(
                           p_376281_ -> this.property4
                              .getPossibleValues()
                              .forEach(
                                 p_389298_ -> this.select(
                                    (T1)p_376254_, (T2)p_375541_, (T3)p_376281_, (T4)p_389298_, (V)p_392096_.apply(p_376254_, p_375541_, p_376281_, p_389298_)
                                 )
                              )
                        )
                  )
            );
         return this;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class C5<V, T1 extends Comparable<T1>, T2 extends Comparable<T2>, T3 extends Comparable<T3>, T4 extends Comparable<T4>, T5 extends Comparable<T5>>
      extends PropertyDispatch<V> {
      private final Property<T1> property1;
      private final Property<T2> property2;
      private final Property<T3> property3;
      private final Property<T4> property4;
      private final Property<T5> property5;

      C5(Property<T1> p_375447_, Property<T2> p_377052_, Property<T3> p_378060_, Property<T4> p_376870_, Property<T5> p_375803_) {
         this.property1 = p_375447_;
         this.property2 = p_377052_;
         this.property3 = p_378060_;
         this.property4 = p_376870_;
         this.property5 = p_375803_;
      }

      @Override
      public List<Property<?>> getDefinedProperties() {
         return List.of(this.property1, this.property2, this.property3, this.property4, this.property5);
      }

      public PropertyDispatch.C5<V, T1, T2, T3, T4, T5> select(T1 p_378643_, T2 p_377480_, T3 p_376302_, T4 p_375916_, T5 p_378810_, V p_393152_) {
         PropertyValueList propertyvaluelist = PropertyValueList.of(
            this.property1.value(p_378643_),
            this.property2.value(p_377480_),
            this.property3.value(p_376302_),
            this.property4.value(p_375916_),
            this.property5.value(p_378810_)
         );
         this.putValue(propertyvaluelist, p_393152_);
         return this;
      }

      public PropertyDispatch<V> generate(Function5<T1, T2, T3, T4, T5, V> p_396465_) {
         this.property1
            .getPossibleValues()
            .forEach(
               p_376257_ -> this.property2
                  .getPossibleValues()
                  .forEach(
                     p_378211_ -> this.property3
                        .getPossibleValues()
                        .forEach(
                           p_376810_ -> this.property4
                              .getPossibleValues()
                              .forEach(
                                 p_378107_ -> this.property5
                                    .getPossibleValues()
                                    .forEach(
                                       p_389304_ -> this.select(
                                          (T1)p_376257_,
                                          (T2)p_378211_,
                                          (T3)p_376810_,
                                          (T4)p_378107_,
                                          (T5)p_389304_,
                                          (V)p_396465_.apply(p_376257_, p_378211_, p_376810_, p_378107_, p_389304_)
                                       )
                                    )
                              )
                        )
                  )
            );
         return this;
      }
   }
}
