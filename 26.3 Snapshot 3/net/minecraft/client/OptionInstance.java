package net.minecraft.client;

import com.google.common.collect.ImmutableList;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleFunction;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.stream.IntStream;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractOptionSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.ResettableOptionWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public final class OptionInstance<T> {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final OptionInstance.Enum<Boolean> BOOLEAN_VALUES = new OptionInstance.Enum<>(ImmutableList.of(Boolean.TRUE, Boolean.FALSE), Codec.BOOL);
   public static final OptionInstance.CaptionBasedToString<Boolean> BOOLEAN_TO_STRING = (var0, b) -> b
      ? CommonComponents.OPTION_ON
      : CommonComponents.OPTION_OFF;
   public static final OptionInstance.ValueUpdateListener<Object> NO_ACTION = var0 -> {};
   private final OptionInstance.TooltipSupplier<T> tooltip;
   private final Function<T, Component> toString;
   private final OptionInstance.ValueSet<T> values;
   private final Codec<T> codec;
   private final T initialValue;
   private final OptionInstance.ValueUpdateListener<? super T> onValueUpdate;
   private final Component caption;
   private T value;

   public static OptionInstance<Boolean> createBoolean(
      final String captionId, final boolean initialValue, final OptionInstance.ValueUpdateListener<? super Boolean> onValueUpdate
   ) {
      return createBoolean(captionId, noTooltip(), initialValue, onValueUpdate);
   }

   public static OptionInstance<Boolean> createBoolean(final String captionId, final boolean initialValue) {
      return createBoolean(captionId, noTooltip(), initialValue, NO_ACTION);
   }

   public static OptionInstance<Boolean> createBoolean(
      final String captionId, final OptionInstance.TooltipSupplier<Boolean> tooltip, final boolean initialValue
   ) {
      return createBoolean(captionId, tooltip, initialValue, NO_ACTION);
   }

   public static OptionInstance<Boolean> createBoolean(
      final String captionId,
      final OptionInstance.TooltipSupplier<Boolean> tooltip,
      final boolean initialValue,
      final OptionInstance.ValueUpdateListener<? super Boolean> onValueUpdate
   ) {
      return createBoolean(captionId, tooltip, BOOLEAN_TO_STRING, initialValue, onValueUpdate);
   }

   public static OptionInstance<Boolean> createBoolean(
      final String captionId,
      final OptionInstance.TooltipSupplier<Boolean> tooltip,
      final OptionInstance.CaptionBasedToString<Boolean> toString,
      final boolean initialValue,
      final OptionInstance.ValueUpdateListener<? super Boolean> onValueUpdate
   ) {
      return new OptionInstance<>(captionId, tooltip, toString, BOOLEAN_VALUES, initialValue, onValueUpdate);
   }

   public OptionInstance(
      final String captionId,
      final OptionInstance.TooltipSupplier<T> tooltip,
      final OptionInstance.CaptionBasedToString<T> toString,
      final OptionInstance.ValueSet<T> values,
      final T initialValue,
      final OptionInstance.ValueUpdateListener<? super T> onValueUpdate
   ) {
      this(captionId, tooltip, toString, values, values.codec(), initialValue, onValueUpdate);
   }

   public OptionInstance(
      final String captionId,
      final OptionInstance.TooltipSupplier<T> tooltip,
      final OptionInstance.CaptionBasedToString<T> toString,
      final OptionInstance.ValueSet<T> values,
      final Codec<T> codec,
      final T initialValue,
      final OptionInstance.ValueUpdateListener<? super T> onValueUpdate
   ) {
      this.caption = Component.translatable(captionId);
      this.tooltip = tooltip;
      this.toString = value -> toString.toString(this.caption, value);
      this.values = values;
      this.codec = codec;
      this.initialValue = initialValue;
      this.onValueUpdate = onValueUpdate;
      this.value = this.initialValue;
   }

   public static <T> OptionInstance.TooltipSupplier<T> noTooltip() {
      return var0 -> null;
   }

   public static <T> OptionInstance.TooltipSupplier<T> cachedConstantTooltip(final Component tooltipComponent) {
      return var1 -> Tooltip.create(tooltipComponent);
   }

   public AbstractWidget createButton(final Options options) {
      return this.createButton(options, 0, 0, 150);
   }

   public AbstractWidget createButton(final Options options, final int x, final int y, final int width) {
      return this.createButton(options, x, y, width, NO_ACTION);
   }

   public AbstractWidget createButton(
      final Options options, final int x, final int y, final int width, final OptionInstance.ValueUpdateListener<? super T> onValueChanged
   ) {
      return this.values.createButton(this.tooltip, options, x, y, width, onValueChanged).apply(this);
   }

   public T get() {
      return this.value;
   }

   public Codec<T> codec() {
      return this.codec;
   }

   @Override
   public String toString() {
      return this.caption.getString();
   }

   public void set(final T value) {
      T newValue = this.values.validateValue(value).orElseGet(() -> {
         LOGGER.error("Illegal option value {} for {}", value, this.caption.getString());
         return this.initialValue;
      });
      if (!Minecraft.getInstance().isRunning()) {
         this.value = newValue;
      } else {
         if (!Objects.equals(this.value, newValue)) {
            this.value = newValue;
            this.onValueUpdate.valueChanged(newValue);
         }
      }
   }

   public OptionInstance.ValueSet<T> values() {
      return this.values;
   }

   public record AltEnum<T>(
      List<T> values, List<T> altValues, BooleanSupplier altCondition, OptionInstance.CycleableValueSet.ValueSetter<T> valueSetter, Codec<T> codec
   ) implements OptionInstance.CycleableValueSet<T> {
      @Override
      public CycleButton.ValueListSupplier<T> valueListSupplier() {
         return CycleButton.ValueListSupplier.create(this.altCondition, this.values, this.altValues);
      }

      @Override
      public Optional<T> validateValue(final T value) {
         return (this.altCondition.getAsBoolean() ? this.altValues : this.values).contains(value) ? Optional.of(value) : Optional.empty();
      }
   }

   @FunctionalInterface
   public interface CaptionBasedToString<T> {
      Component toString(Component caption, T value);
   }

   public record ClampingLazyMaxIntRange(int minInclusive, IntSupplier maxSupplier, int encodableMaxInclusive)
      implements OptionInstance.IntRangeBase,
      OptionInstance.SliderableOrCyclableValueSet<Integer> {
      public Optional<Integer> validateValue(final Integer value) {
         return Optional.of(Mth.clamp(value, this.minInclusive(), this.maxInclusive()));
      }

      @Override
      public int maxInclusive() {
         return this.maxSupplier.getAsInt();
      }

      @Override
      public Codec<Integer> codec() {
         return Codec.INT
            .validate(
               value -> {
                  int maxExclusive = this.encodableMaxInclusive + 1;
                  return value.compareTo(this.minInclusive) >= 0 && value.compareTo(maxExclusive) <= 0
                     ? DataResult.success(value)
                     : DataResult.error(() -> "Value " + value + " outside of range [" + this.minInclusive + ":" + maxExclusive + "]", value);
               }
            );
      }

      @Override
      public boolean createCycleButton() {
         return true;
      }

      @Override
      public CycleButton.ValueListSupplier<Integer> valueListSupplier() {
         return CycleButton.ValueListSupplier.create(IntStream.range(this.minInclusive, this.maxInclusive() + 1).boxed().toList());
      }
   }

   public interface CycleableValueSet<T> extends OptionInstance.ValueSet<T> {
      CycleButton.ValueListSupplier<T> valueListSupplier();

      default OptionInstance.CycleableValueSet.ValueSetter<T> valueSetter() {
         return OptionInstance::set;
      }

      @Override
      default Function<OptionInstance<T>, AbstractWidget> createButton(
         final OptionInstance.TooltipSupplier<T> tooltip,
         final Options options,
         final int x,
         final int y,
         final int width,
         final OptionInstance.ValueUpdateListener<? super T> onValueChanged
      ) {
         return instance -> CycleButton.builder(instance.toString, instance::get)
            .withValues(this.valueListSupplier())
            .withTooltip(tooltip)
            .create(x, y, width, 20, instance.caption, (var4x, value) -> {
               this.valueSetter().set(instance, value);
               options.save();
               onValueChanged.valueChanged(value);
            });
      }

      interface ValueSetter<T> {
         void set(final OptionInstance<T> instance, final T value);
      }
   }

   public record Enum<T>(List<T> values, Codec<T> codec) implements OptionInstance.CycleableValueSet<T> {
      @Override
      public Optional<T> validateValue(final T value) {
         return this.values.contains(value) ? Optional.of(value) : Optional.empty();
      }

      @Override
      public CycleButton.ValueListSupplier<T> valueListSupplier() {
         return CycleButton.ValueListSupplier.create(this.values);
      }
   }

   public record IntRange(int minInclusive, int maxInclusive, boolean applyValueImmediately) implements OptionInstance.IntRangeBase {
      public IntRange(final int minInclusive, final int maxInclusive) {
         this(minInclusive, maxInclusive, true);
      }

      public Optional<Integer> validateValue(final Integer value) {
         return value.compareTo(this.minInclusive()) >= 0 && value.compareTo(this.maxInclusive()) <= 0 ? Optional.of(value) : Optional.empty();
      }

      @Override
      public Codec<Integer> codec() {
         return Codec.intRange(this.minInclusive, this.maxInclusive + 1);
      }
   }

   public interface IntRangeBase extends OptionInstance.SliderableValueSet<Integer> {
      int minInclusive();

      int maxInclusive();

      default Optional<Integer> next(final Integer current) {
         return Optional.of(current + 1);
      }

      default Optional<Integer> previous(final Integer current) {
         return Optional.of(current - 1);
      }

      default double toSliderValue(final Integer value) {
         if (value == this.minInclusive()) {
            return 0.0;
         } else {
            return value == this.maxInclusive() ? 1.0 : Mth.map(value.intValue() + 0.5, this.minInclusive(), this.maxInclusive() + 1.0, 0.0, 1.0);
         }
      }

      default Integer fromSliderValue(double slider) {
         if (slider >= 1.0) {
            slider = 0.99999F;
         }

         return Mth.floor(Mth.map(slider, 0.0, 1.0, this.minInclusive(), this.maxInclusive() + 1.0));
      }

      default <R> OptionInstance.SliderableValueSet<R> xmap(final IntFunction<? extends R> to, final ToIntFunction<? super R> from, final boolean discrete) {
         return new OptionInstance.SliderableValueSet<R>() {
            @Override
            public Optional<R> validateValue(final R value) {
               return IntRangeBase.this.validateValue(from.applyAsInt(value)).map(to::apply);
            }

            @Override
            public double toSliderValue(final R value) {
               return IntRangeBase.this.toSliderValue(from.applyAsInt(value));
            }

            @Override
            public Optional<R> next(final R current) {
               if (!discrete) {
                  return Optional.empty();
               }

               int currentIndex = from.applyAsInt(current);
               return (Optional<R>)Optional.of(to.apply(IntRangeBase.this.validateValue(currentIndex + 1).orElse(currentIndex)));
            }

            @Override
            public Optional<R> previous(final R current) {
               if (!discrete) {
                  return Optional.empty();
               }

               int currentIndex = from.applyAsInt(current);
               return (Optional<R>)Optional.of(to.apply(IntRangeBase.this.validateValue(currentIndex - 1).orElse(currentIndex)));
            }

            @Override
            public R fromSliderValue(final double slider) {
               return (R)to.apply(IntRangeBase.this.fromSliderValue(slider));
            }

            @Override
            public Codec<R> codec() {
               return IntRangeBase.this.codec().xmap(to::apply, from::applyAsInt);
            }
         };
      }
   }

   public record LazyEnum<T>(Supplier<List<T>> values, Function<T, Optional<T>> validateValue, Codec<T> codec) implements OptionInstance.CycleableValueSet<T> {
      @Override
      public Optional<T> validateValue(final T value) {
         return this.validateValue.apply(value);
      }

      @Override
      public CycleButton.ValueListSupplier<T> valueListSupplier() {
         return CycleButton.ValueListSupplier.create(this.values.get());
      }
   }

   public static final class OptionInstanceSliderButton<N> extends AbstractOptionSliderButton implements ResettableOptionWidget {
      private final OptionInstance<N> instance;
      private final OptionInstance.SliderableValueSet<N> values;
      private final OptionInstance.TooltipSupplier<N> tooltipSupplier;
      private final OptionInstance.ValueUpdateListener<? super N> onValueChanged;
      private @Nullable Long delayedApplyAt;
      private final boolean applyValueImmediately;

      private OptionInstanceSliderButton(
         final Options options,
         final int x,
         final int y,
         final int width,
         final int height,
         final OptionInstance<N> instance,
         final OptionInstance.SliderableValueSet<N> values,
         final OptionInstance.TooltipSupplier<N> tooltipSupplier,
         final OptionInstance.ValueUpdateListener<? super N> onValueChanged,
         final boolean applyValueImmediately
      ) {
         super(options, x, y, width, height, values.toSliderValue(instance.get()));
         this.instance = instance;
         this.values = values;
         this.tooltipSupplier = tooltipSupplier;
         this.onValueChanged = onValueChanged;
         this.applyValueImmediately = applyValueImmediately;
         this.updateMessage();
      }

      @Override
      protected void updateMessage() {
         this.setMessage(this.instance.toString.apply(this.values.fromSliderValue(this.value)));
         this.setTooltip(this.tooltipSupplier.apply(this.values.fromSliderValue(this.value)));
      }

      @Override
      protected void applyValue() {
         if (this.applyValueImmediately) {
            this.applyUnsavedValue();
         } else {
            this.delayedApplyAt = Util.getMillis() + 600L;
         }
      }

      public void applyUnsavedValue() {
         N sliderValue = this.values.fromSliderValue(this.value);
         if (!Objects.equals(sliderValue, this.instance.get())) {
            this.instance.set(sliderValue);
            this.onValueChanged.valueChanged(this.instance.get());
         }
      }

      @Override
      public void resetValue() {
         if (this.value != this.values.toSliderValue(this.instance.get())) {
            this.value = this.values.toSliderValue(this.instance.get());
            this.delayedApplyAt = null;
            this.updateMessage();
         }
      }

      @Override
      public void extractWidgetRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
         super.extractWidgetRenderState(graphics, mouseX, mouseY, a);
         if (this.delayedApplyAt != null && Util.getMillis() >= this.delayedApplyAt) {
            this.delayedApplyAt = null;
            this.applyUnsavedValue();
            this.resetValue();
         }
      }

      @Override
      public void onRelease(final MouseButtonEvent event) {
         super.onRelease(event);
         if (this.applyValueImmediately) {
            this.resetValue();
         }
      }

      @Override
      public boolean keyPressed(final KeyEvent event) {
         if (event.isSelection()) {
            this.canChangeValue = !this.canChangeValue;
            return true;
         }

         if (this.canChangeValue) {
            boolean left = event.isLeft();
            boolean right = event.isRight();
            if (left) {
               Optional<N> previous = this.values.previous(this.values.fromSliderValue(this.value));
               if (previous.isPresent()) {
                  this.setValue(this.values.toSliderValue(previous.get()));
                  return true;
               }
            }

            if (right) {
               Optional<N> next = this.values.next(this.values.fromSliderValue(this.value));
               if (next.isPresent()) {
                  this.setValue(this.values.toSliderValue(next.get()));
                  return true;
               }
            }

            if (left || right) {
               float direction = left ? -1.0F : 1.0F;
               this.setValue(this.value + direction / (this.width - 8));
               return true;
            }
         }

         return false;
      }
   }

   public record SliderableEnum<T>(List<T> values, Codec<T> codec) implements OptionInstance.SliderableValueSet<T> {
      @Override
      public double toSliderValue(final T value) {
         if (value == this.values.getFirst()) {
            return 0.0;
         } else {
            return value == this.values.getLast() ? 1.0 : Mth.map(this.values.indexOf(value), 0.0, this.values.size() - 1, 0.0, 1.0);
         }
      }

      @Override
      public Optional<T> next(final T current) {
         int currentIntex = this.values.indexOf(current);
         int nextIndex = Mth.clamp(currentIntex + 1, 0, this.values.size() - 1);
         return Optional.of(this.values.get(nextIndex));
      }

      @Override
      public Optional<T> previous(final T current) {
         int currentIntex = this.values.indexOf(current);
         int previousIndex = Mth.clamp(currentIntex - 1, 0, this.values.size() - 1);
         return Optional.of(this.values.get(previousIndex));
      }

      @Override
      public T fromSliderValue(double slider) {
         if (slider >= 1.0) {
            slider = 0.99999F;
         }

         int index = Mth.floor(Mth.map(slider, 0.0, 1.0, 0.0, this.values.size()));
         return this.values.get(Mth.clamp(index, 0, this.values.size() - 1));
      }

      @Override
      public Optional<T> validateValue(final T value) {
         int index = this.values.indexOf(value);
         return index > -1 ? Optional.of(value) : Optional.empty();
      }
   }

   public interface SliderableOrCyclableValueSet<T> extends OptionInstance.SliderableValueSet<T>, OptionInstance.CycleableValueSet<T> {
      boolean createCycleButton();

      @Override
      default Function<OptionInstance<T>, AbstractWidget> createButton(
         final OptionInstance.TooltipSupplier<T> tooltip,
         final Options options,
         final int x,
         final int y,
         final int width,
         final OptionInstance.ValueUpdateListener<? super T> onValueChanged
      ) {
         return this.createCycleButton()
            ? OptionInstance.CycleableValueSet.super.createButton(tooltip, options, x, y, width, onValueChanged)
            : OptionInstance.SliderableValueSet.super.createButton(tooltip, options, x, y, width, onValueChanged);
      }
   }

   public interface SliderableValueSet<T> extends OptionInstance.ValueSet<T> {
      double toSliderValue(final T value);

      default Optional<T> next(final T current) {
         return Optional.empty();
      }

      default Optional<T> previous(final T current) {
         return Optional.empty();
      }

      T fromSliderValue(final double slider);

      default boolean applyValueImmediately() {
         return true;
      }

      @Override
      default Function<OptionInstance<T>, AbstractWidget> createButton(
         final OptionInstance.TooltipSupplier<T> tooltip,
         final Options options,
         final int x,
         final int y,
         final int width,
         final OptionInstance.ValueUpdateListener<? super T> onValueChanged
      ) {
         return instance -> new OptionInstance.OptionInstanceSliderButton<>(
            options, x, y, width, 20, instance, this, tooltip, onValueChanged, this.applyValueImmediately()
         );
      }
   }

   @FunctionalInterface
   public interface TooltipSupplier<T> {
      @Nullable Tooltip apply(T value);
   }

   public enum UnitDouble implements OptionInstance.SliderableValueSet<Double> {
      INSTANCE;

      public Optional<Double> validateValue(final Double value) {
         return value >= 0.0 && value <= 1.0 ? Optional.of(value) : Optional.empty();
      }

      public double toSliderValue(final Double value) {
         return value;
      }

      public Double fromSliderValue(final double slider) {
         return slider;
      }

      public <R> OptionInstance.SliderableValueSet<R> xmap(final DoubleFunction<? extends R> to, final ToDoubleFunction<? super R> from) {
         return new OptionInstance.SliderableValueSet<R>() {
            @Override
            public Optional<R> validateValue(final R value) {
               return UnitDouble.this.validateValue(from.applyAsDouble(value)).map(to::apply);
            }

            @Override
            public double toSliderValue(final R value) {
               return UnitDouble.this.toSliderValue(from.applyAsDouble(value));
            }

            @Override
            public R fromSliderValue(final double slider) {
               return (R)to.apply(UnitDouble.this.fromSliderValue(slider));
            }

            @Override
            public Codec<R> codec() {
               return UnitDouble.this.codec().xmap(to::apply, from::applyAsDouble);
            }
         };
      }

      @Override
      public Codec<Double> codec() {
         return Codec.withAlternative(Codec.doubleRange(0.0, 1.0), Codec.BOOL, b -> b ? 1.0 : 0.0);
      }
   }

   public interface ValueSet<T> {
      Function<OptionInstance<T>, AbstractWidget> createButton(
         final OptionInstance.TooltipSupplier<T> tooltip,
         Options options,
         final int x,
         final int y,
         final int width,
         final OptionInstance.ValueUpdateListener<? super T> onValueChanged
      );

      Optional<T> validateValue(final T value);

      Codec<T> codec();
   }

   @FunctionalInterface
   public interface ValueUpdateListener<T> {
      void valueChanged(T newValue);
   }
}
