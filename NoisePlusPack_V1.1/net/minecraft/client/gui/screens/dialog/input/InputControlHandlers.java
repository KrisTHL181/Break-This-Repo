package net.minecraft.client.gui.screens.dialog.input;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.layouts.CommonLayouts;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.dialog.action.Action;
import net.minecraft.server.dialog.input.BooleanInput;
import net.minecraft.server.dialog.input.InputControl;
import net.minecraft.server.dialog.input.NumberRangeInput;
import net.minecraft.server.dialog.input.SingleOptionInput;
import net.minecraft.server.dialog.input.TextInput;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class InputControlHandlers {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Map<MapCodec<? extends InputControl>, InputControlHandler<?>> HANDLERS = new HashMap<>();

   private static <T extends InputControl> void register(MapCodec<T> p_407482_, InputControlHandler<? super T> p_406406_) {
      HANDLERS.put(p_407482_, p_406406_);
   }

   private static <T extends InputControl> @Nullable InputControlHandler<T> get(T p_406934_) {
      return (InputControlHandler<T>)HANDLERS.get(p_406934_.mapCodec());
   }

   public static <T extends InputControl> void createHandler(T p_408090_, Screen p_407348_, InputControlHandler.Output p_409707_) {
      InputControlHandler<T> inputcontrolhandler = get(p_408090_);
      if (inputcontrolhandler == null) {
         LOGGER.warn("Unrecognized input control {}", p_408090_);
      } else {
         inputcontrolhandler.addControl(p_408090_, p_407348_, p_409707_);
      }
   }

   public static void bootstrap() {
      register(TextInput.MAP_CODEC, new InputControlHandlers.TextInputHandler());
      register(SingleOptionInput.MAP_CODEC, new InputControlHandlers.SingleOptionHandler());
      register(BooleanInput.MAP_CODEC, new InputControlHandlers.BooleanHandler());
      register(NumberRangeInput.MAP_CODEC, new InputControlHandlers.NumberRangeHandler());
   }

   @OnlyIn(Dist.CLIENT)
   static class BooleanHandler implements InputControlHandler<BooleanInput> {
      public void addControl(final BooleanInput p_409564_, Screen p_409847_, InputControlHandler.Output p_409802_) {
         Font font = p_409847_.getFont();
         final Checkbox checkbox = Checkbox.builder(p_409564_.label(), font).selected(p_409564_.initial()).build();
         p_409802_.accept(checkbox, new Action.ValueGetter() {
            @Override
            public String asTemplateSubstitution() {
               return checkbox.selected() ? p_409564_.onTrue() : p_409564_.onFalse();
            }

            @Override
            public Tag asTag() {
               return ByteTag.valueOf(checkbox.selected());
            }
         });
      }
   }

   @OnlyIn(Dist.CLIENT)
   static class NumberRangeHandler implements InputControlHandler<NumberRangeInput> {
      public void addControl(NumberRangeInput p_408461_, Screen p_406549_, InputControlHandler.Output p_407361_) {
         float f = p_408461_.rangeInfo().initialSliderValue();
         final InputControlHandlers.NumberRangeHandler.SliderImpl inputcontrolhandlers$numberrangehandler$sliderimpl = new InputControlHandlers.NumberRangeHandler.SliderImpl(
            p_408461_, f
         );
         p_407361_.accept(inputcontrolhandlers$numberrangehandler$sliderimpl, new Action.ValueGetter() {
            @Override
            public String asTemplateSubstitution() {
               return inputcontrolhandlers$numberrangehandler$sliderimpl.stringValueToSend();
            }

            @Override
            public Tag asTag() {
               return FloatTag.valueOf(inputcontrolhandlers$numberrangehandler$sliderimpl.floatValueToSend());
            }
         });
      }

      @OnlyIn(Dist.CLIENT)
      static class SliderImpl extends AbstractSliderButton {
         private final NumberRangeInput input;

         SliderImpl(NumberRangeInput p_408873_, double p_407140_) {
            super(0, 0, p_408873_.width(), 20, computeMessage(p_408873_, p_407140_), p_407140_);
            this.input = p_408873_;
         }

         @Override
         protected void updateMessage() {
            this.setMessage(computeMessage(this.input, this.value));
         }

         @Override
         protected void applyValue() {
         }

         public String stringValueToSend() {
            return sliderValueToString(this.input, this.value);
         }

         public float floatValueToSend() {
            return scaledValue(this.input, this.value);
         }

         private static float scaledValue(NumberRangeInput p_409440_, double p_407596_) {
            return p_409440_.rangeInfo().computeScaledValue((float)p_407596_);
         }

         private static String sliderValueToString(NumberRangeInput p_408201_, double p_410113_) {
            return valueToString(scaledValue(p_408201_, p_410113_));
         }

         private static Component computeMessage(NumberRangeInput p_408035_, double p_408501_) {
            return p_408035_.computeLabel(sliderValueToString(p_408035_, p_408501_));
         }

         private static String valueToString(float p_407623_) {
            int i = (int)p_407623_;
            return i == p_407623_ ? Integer.toString(i) : Float.toString(p_407623_);
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   static class SingleOptionHandler implements InputControlHandler<SingleOptionInput> {
      public void addControl(SingleOptionInput p_407094_, Screen p_406176_, InputControlHandler.Output p_410623_) {
         SingleOptionInput.Entry singleoptioninput$entry = p_407094_.initial().orElse(p_407094_.entries().getFirst());
         CycleButton.Builder<SingleOptionInput.Entry> builder = CycleButton.builder(SingleOptionInput.Entry::displayOrDefault, singleoptioninput$entry)
            .withValues(p_407094_.entries())
            .displayState(!p_407094_.labelVisible() ? CycleButton.DisplayState.VALUE : CycleButton.DisplayState.NAME_AND_VALUE);
         CycleButton<SingleOptionInput.Entry> cyclebutton = builder.create(0, 0, p_407094_.width(), 20, p_407094_.label());
         p_410623_.accept(cyclebutton, Action.ValueGetter.of(() -> cyclebutton.getValue().id()));
      }
   }

   @OnlyIn(Dist.CLIENT)
   static class TextInputHandler implements InputControlHandler<TextInput> {
      public void addControl(TextInput p_410124_, Screen p_408290_, InputControlHandler.Output p_406869_) {
         Font font = p_408290_.getFont();
         LayoutElement layoutelement;
         final Supplier<String> supplier;
         if (p_410124_.multiline().isPresent()) {
            TextInput.MultilineOptions textinput$multilineoptions = p_410124_.multiline().get();
            int i = textinput$multilineoptions.height().orElseGet(() -> {
               int j = textinput$multilineoptions.maxLines().orElse(4);
               return Math.min(9 * j + 8, 512);
            });
            MultiLineEditBox multilineeditbox = MultiLineEditBox.builder().build(font, p_410124_.width(), i, CommonComponents.EMPTY);
            multilineeditbox.setCharacterLimit(p_410124_.maxLength());
            textinput$multilineoptions.maxLines().ifPresent(multilineeditbox::setLineLimit);
            multilineeditbox.setValue(p_410124_.initial());
            layoutelement = multilineeditbox;
            supplier = multilineeditbox::getValue;
         } else {
            EditBox editbox = new EditBox(font, p_410124_.width(), 20, p_410124_.label());
            editbox.setMaxLength(p_410124_.maxLength());
            editbox.setValue(p_410124_.initial());
            layoutelement = editbox;
            supplier = editbox::getValue;
         }

         LayoutElement layoutelement1 = p_410124_.labelVisible() ? CommonLayouts.labeledElement(font, layoutelement, p_410124_.label()) : layoutelement;
         p_406869_.accept(layoutelement1, new Action.ValueGetter() {
            @Override
            public String asTemplateSubstitution() {
               return StringTag.escapeWithoutQuotes(supplier.get());
            }

            @Override
            public Tag asTag() {
               return StringTag.valueOf(supplier.get());
            }
         });
      }
   }
}
