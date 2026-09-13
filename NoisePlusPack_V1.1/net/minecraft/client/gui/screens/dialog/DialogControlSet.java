package net.minecraft.client.gui.screens.dialog;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.screens.dialog.input.InputControlHandlers;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.server.dialog.ActionButton;
import net.minecraft.server.dialog.CommonButtonData;
import net.minecraft.server.dialog.Input;
import net.minecraft.server.dialog.action.Action;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class DialogControlSet {
   public static final Supplier<Optional<ClickEvent>> EMPTY_ACTION = Optional::empty;
   private final DialogScreen<?> screen;
   private final Map<String, Action.ValueGetter> valueGetters = new HashMap<>();

   public DialogControlSet(DialogScreen<?> p_405896_) {
      this.screen = p_405896_;
   }

   public void addInput(Input p_407891_, Consumer<LayoutElement> p_407015_) {
      String s = p_407891_.key();
      InputControlHandlers.createHandler(p_407891_.control(), this.screen, (p_410319_, p_406391_) -> {
         this.valueGetters.put(s, p_406391_);
         p_407015_.accept(p_410319_);
      });
   }

   private static Button.Builder createDialogButton(CommonButtonData p_410010_, Button.OnPress p_409832_) {
      Button.Builder button$builder = Button.builder(p_410010_.label(), p_409832_);
      button$builder.width(p_410010_.width());
      if (p_410010_.tooltip().isPresent()) {
         button$builder = button$builder.tooltip(Tooltip.create(p_410010_.tooltip().get()));
      }

      return button$builder;
   }

   public Supplier<Optional<ClickEvent>> bindAction(Optional<Action> p_406921_) {
      if (p_406921_.isPresent()) {
         Action action = p_406921_.get();
         return () -> action.createAction(this.valueGetters);
      } else {
         return EMPTY_ACTION;
      }
   }

   public Button.Builder createActionButton(ActionButton p_407975_) {
      Supplier<Optional<ClickEvent>> supplier = this.bindAction(p_407975_.action());
      return createDialogButton(p_407975_.button(), p_406838_ -> this.screen.runAction(supplier.get()));
   }
}
