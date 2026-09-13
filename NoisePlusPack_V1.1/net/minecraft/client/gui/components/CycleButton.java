package net.minecraft.client.gui.components;

import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class CycleButton<T> extends AbstractButton implements ResettableOptionWidget {
   public static final BooleanSupplier DEFAULT_ALT_LIST_SELECTOR = () -> Minecraft.getInstance().hasAltDown();
   private static final List<Boolean> BOOLEAN_OPTIONS = ImmutableList.of(Boolean.TRUE, Boolean.FALSE);
   private final Supplier<T> defaultValueSupplier;
   private final Component name;
   private int index;
   private T value;
   private final CycleButton.ValueListSupplier<T> values;
   private final Function<T, Component> valueStringifier;
   private final Function<CycleButton<T>, MutableComponent> narrationProvider;
   private final CycleButton.OnValueChange<T> onValueChange;
   private final CycleButton.DisplayState displayState;
   private final OptionInstance.TooltipSupplier<T> tooltipSupplier;
   private final CycleButton.SpriteSupplier<T> spriteSupplier;

   CycleButton(
      int p_232484_,
      int p_232485_,
      int p_232486_,
      int p_232487_,
      Component p_232488_,
      Component p_232489_,
      int p_232490_,
      T p_232491_,
      Supplier<T> p_457487_,
      CycleButton.ValueListSupplier<T> p_232492_,
      Function<T, Component> p_232493_,
      Function<CycleButton<T>, MutableComponent> p_232494_,
      CycleButton.OnValueChange<T> p_232495_,
      OptionInstance.TooltipSupplier<T> p_232496_,
      CycleButton.DisplayState p_454340_,
      CycleButton.SpriteSupplier<T> p_454413_
   ) {
      super(p_232484_, p_232485_, p_232486_, p_232487_, p_232488_);
      this.name = p_232489_;
      this.index = p_232490_;
      this.defaultValueSupplier = p_457487_;
      this.value = p_232491_;
      this.values = p_232492_;
      this.valueStringifier = p_232493_;
      this.narrationProvider = p_232494_;
      this.onValueChange = p_232495_;
      this.displayState = p_454340_;
      this.tooltipSupplier = p_232496_;
      this.spriteSupplier = p_454413_;
      this.updateTooltip();
   }

   @Override
   protected void renderContents(GuiGraphics p_456835_, int p_454494_, int p_454886_, float p_450515_) {
      Identifier identifier = this.spriteSupplier.apply(this, this.getValue());
      if (identifier != null) {
         p_456835_.blitSprite(RenderPipelines.GUI_TEXTURED, identifier, this.getX(), this.getY(), this.getWidth(), this.getHeight());
      } else {
         this.renderDefaultSprite(p_456835_);
      }

      if (this.displayState != CycleButton.DisplayState.HIDE) {
         this.renderDefaultLabel(p_456835_.textRendererForWidget(this, GuiGraphics.HoveredTextEffects.NONE));
      }
   }

   private void updateTooltip() {
      this.setTooltip(this.tooltipSupplier.apply(this.value));
   }

   @Override
   public void onPress(InputWithModifiers p_425965_) {
      if (p_425965_.hasShiftDown()) {
         this.cycleValue(-1);
      } else {
         this.cycleValue(1);
      }
   }

   private void cycleValue(int p_168909_) {
      List<T> list = this.values.getSelectedList();
      this.index = Mth.positiveModulo(this.index + p_168909_, list.size());
      T t = list.get(this.index);
      this.updateValue(t);
      this.onValueChange.onValueChange(this, t);
   }

   private T getCycledValue(int p_168915_) {
      List<T> list = this.values.getSelectedList();
      return list.get(Mth.positiveModulo(this.index + p_168915_, list.size()));
   }

   @Override
   public boolean mouseScrolled(double p_168885_, double p_168886_, double p_168887_, double p_300536_) {
      if (p_300536_ > 0.0) {
         this.cycleValue(-1);
      } else if (p_300536_ < 0.0) {
         this.cycleValue(1);
      }

      return true;
   }

   public void setValue(T p_168893_) {
      List<T> list = this.values.getSelectedList();
      int i = list.indexOf(p_168893_);
      if (i != -1) {
         this.index = i;
      }

      this.updateValue(p_168893_);
   }

   @Override
   public void resetValue() {
      this.setValue(this.defaultValueSupplier.get());
   }

   private void updateValue(T p_168906_) {
      Component component = this.createLabelForValue(p_168906_);
      this.setMessage(component);
      this.value = p_168906_;
      this.updateTooltip();
   }

   private Component createLabelForValue(T p_168911_) {
      return this.displayState == CycleButton.DisplayState.VALUE ? this.valueStringifier.apply(p_168911_) : this.createFullName(p_168911_);
   }

   private MutableComponent createFullName(T p_168913_) {
      return CommonComponents.optionNameValue(this.name, this.valueStringifier.apply(p_168913_));
   }

   public T getValue() {
      return this.value;
   }

   @Override
   protected MutableComponent createNarrationMessage() {
      return this.narrationProvider.apply(this);
   }

   @Override
   public void updateWidgetNarration(NarrationElementOutput p_168889_) {
      p_168889_.add(NarratedElementType.TITLE, this.createNarrationMessage());
      if (this.active) {
         T t = this.getCycledValue(1);
         Component component = this.createLabelForValue(t);
         if (this.isFocused()) {
            p_168889_.add(NarratedElementType.USAGE, Component.translatable("narration.cycle_button.usage.focused", component));
         } else {
            p_168889_.add(NarratedElementType.USAGE, Component.translatable("narration.cycle_button.usage.hovered", component));
         }
      }
   }

   public MutableComponent createDefaultNarrationMessage() {
      return wrapDefaultNarrationMessage(this.displayState == CycleButton.DisplayState.VALUE ? this.createFullName(this.value) : this.getMessage());
   }

   public static <T> CycleButton.Builder<T> builder(Function<T, Component> p_168895_, Supplier<T> p_453650_) {
      return new CycleButton.Builder<>(p_168895_, p_453650_);
   }

   public static <T> CycleButton.Builder<T> builder(Function<T, Component> p_457614_, T p_457615_) {
      return new CycleButton.Builder<>(p_457614_, () -> p_457615_);
   }

   public static CycleButton.Builder<Boolean> booleanBuilder(Component p_168897_, Component p_168898_, boolean p_453738_) {
      return new CycleButton.Builder<>(p_447970_ -> p_447970_ == Boolean.TRUE ? p_168897_ : p_168898_, () -> p_453738_).withValues(BOOLEAN_OPTIONS);
   }

   public static CycleButton.Builder<Boolean> onOffBuilder(boolean p_168917_) {
      return new CycleButton.Builder<>(p_447967_ -> p_447967_ == Boolean.TRUE ? CommonComponents.OPTION_ON : CommonComponents.OPTION_OFF, () -> p_168917_)
         .withValues(BOOLEAN_OPTIONS);
   }

   @OnlyIn(Dist.CLIENT)
   public static class Builder<T> {
      private final Supplier<T> defaultValueSupplier;
      private final Function<T, Component> valueStringifier;
      private OptionInstance.TooltipSupplier<T> tooltipSupplier = p_168964_ -> null;
      private CycleButton.SpriteSupplier<T> spriteSupplier = (p_447974_, p_447975_) -> null;
      private Function<CycleButton<T>, MutableComponent> narrationProvider = CycleButton::createDefaultNarrationMessage;
      private CycleButton.ValueListSupplier<T> values = CycleButton.ValueListSupplier.create(ImmutableList.of());
      private CycleButton.DisplayState displayState = CycleButton.DisplayState.NAME_AND_VALUE;

      public Builder(Function<T, Component> p_168928_, Supplier<T> p_456533_) {
         this.valueStringifier = p_168928_;
         this.defaultValueSupplier = p_456533_;
      }

      public CycleButton.Builder<T> withValues(Collection<T> p_232503_) {
         return this.withValues(CycleButton.ValueListSupplier.create(p_232503_));
      }

      @SafeVarargs
      public final CycleButton.Builder<T> withValues(T... p_168962_) {
         return this.withValues(ImmutableList.copyOf(p_168962_));
      }

      public CycleButton.Builder<T> withValues(List<T> p_168953_, List<T> p_168954_) {
         return this.withValues(CycleButton.ValueListSupplier.create(CycleButton.DEFAULT_ALT_LIST_SELECTOR, p_168953_, p_168954_));
      }

      public CycleButton.Builder<T> withValues(BooleanSupplier p_168956_, List<T> p_168957_, List<T> p_168958_) {
         return this.withValues(CycleButton.ValueListSupplier.create(p_168956_, p_168957_, p_168958_));
      }

      public CycleButton.Builder<T> withValues(CycleButton.ValueListSupplier<T> p_232501_) {
         this.values = p_232501_;
         return this;
      }

      public CycleButton.Builder<T> withTooltip(OptionInstance.TooltipSupplier<T> p_232499_) {
         this.tooltipSupplier = p_232499_;
         return this;
      }

      public CycleButton.Builder<T> withCustomNarration(Function<CycleButton<T>, MutableComponent> p_168960_) {
         this.narrationProvider = p_168960_;
         return this;
      }

      public CycleButton.Builder<T> withSprite(CycleButton.SpriteSupplier<T> p_456551_) {
         this.spriteSupplier = p_456551_;
         return this;
      }

      public CycleButton.Builder<T> displayState(CycleButton.DisplayState p_458174_) {
         this.displayState = p_458174_;
         return this;
      }

      public CycleButton.Builder<T> displayOnlyValue() {
         return this.displayState(CycleButton.DisplayState.VALUE);
      }

      public CycleButton<T> create(Component p_331414_, CycleButton.OnValueChange<T> p_335090_) {
         return this.create(0, 0, 150, 20, p_331414_, p_335090_);
      }

      public CycleButton<T> create(int p_168931_, int p_168932_, int p_168933_, int p_168934_, Component p_168935_) {
         return this.create(p_168931_, p_168932_, p_168933_, p_168934_, p_168935_, (p_168946_, p_168947_) -> {});
      }

      public CycleButton<T> create(int p_168937_, int p_168938_, int p_168939_, int p_168940_, Component p_168941_, CycleButton.OnValueChange<T> p_168942_) {
         List<T> list = this.values.getDefaultList();
         if (list.isEmpty()) {
            throw new IllegalStateException("No values for cycle button");
         }

         T t = this.defaultValueSupplier.get();
         int i = list.indexOf(t);
         Component component = this.valueStringifier.apply(t);
         Component component1 = this.displayState == CycleButton.DisplayState.VALUE ? component : CommonComponents.optionNameValue(p_168941_, component);
         return new CycleButton<>(
            p_168937_,
            p_168938_,
            p_168939_,
            p_168940_,
            component1,
            p_168941_,
            i,
            t,
            this.defaultValueSupplier,
            this.values,
            this.valueStringifier,
            this.narrationProvider,
            p_168942_,
            this.tooltipSupplier,
            this.displayState,
            this.spriteSupplier
         );
      }
   }

   @OnlyIn(Dist.CLIENT)
   public enum DisplayState {
      NAME_AND_VALUE,
      VALUE,
      HIDE;
   }

   @FunctionalInterface
   @OnlyIn(Dist.CLIENT)
   public interface OnValueChange<T> {
      void onValueChange(CycleButton<T> var1, T var2);
   }

   @FunctionalInterface
   @OnlyIn(Dist.CLIENT)
   public interface SpriteSupplier<T> {
      @Nullable Identifier apply(CycleButton<T> var1, T var2);
   }

   @OnlyIn(Dist.CLIENT)
   public interface ValueListSupplier<T> {
      List<T> getSelectedList();

      List<T> getDefaultList();

      static <T> CycleButton.ValueListSupplier<T> create(Collection<T> p_232505_) {
         final List<T> list = ImmutableList.copyOf(p_232505_);
         return new CycleButton.ValueListSupplier<T>() {
            @Override
            public List<T> getSelectedList() {
               return list;
            }

            @Override
            public List<T> getDefaultList() {
               return list;
            }
         };
      }

      static <T> CycleButton.ValueListSupplier<T> create(final BooleanSupplier p_168971_, List<T> p_168972_, List<T> p_168973_) {
         final List<T> list = ImmutableList.copyOf(p_168972_);
         final List<T> list1 = ImmutableList.copyOf(p_168973_);
         return new CycleButton.ValueListSupplier<T>() {
            @Override
            public List<T> getSelectedList() {
               return p_168971_.getAsBoolean() ? list1 : list;
            }

            @Override
            public List<T> getDefaultList() {
               return list;
            }
         };
      }
   }
}
