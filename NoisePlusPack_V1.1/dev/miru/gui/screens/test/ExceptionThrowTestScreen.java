package dev.miru.gui.screens.test;

import dev.miru.gui.screens.MessageScreen;
import dev.miru.helper.KitUtil;
import dev.miru.main.ModMain;
import java.lang.reflect.Constructor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ExceptionThrowTestScreen extends Screen {
   private final Screen parent;
   private Button throwButton;
   private EditBox classPathInputBox;

   public ExceptionThrowTestScreen(Screen parent) {
      super(Component.literal(ModMain.getI18N("screen.exception_throw_test.title")));
      this.parent = parent;
   }

   @Override
   public void init() {
      EditBox classPathInputBox;
      this.classPathInputBox = classPathInputBox = KitUtil.editbox(
         this.font,
         "",
         Component.literal(ModMain.getI18N("screen.exception_throw_test.input.full_name.hint")),
         300,
         20,
         this.width / 2 - 150,
         this.height / 2 - 35
      );
      this.classPathInputBox.setResponder(text -> {
         this.throwButton.setTooltip(Tooltip.create(Component.literal(ModMain.getI18N("screen.exception_throw_test.throw.tooltip", text))));
         if (this.isExceptionClass(this.classPathInputBox.getValue())) {
            this.classPathInputBox.setTextColor(-65536);
         } else {
            this.classPathInputBox.setTextColor(-1);
         }
      });
      EditBox messageInputBox = KitUtil.editbox(
         this.font,
         "",
         Component.literal(ModMain.getI18N("screen.exception_throw_test.input.message.hint")),
         300,
         20,
         this.width / 2 - 150,
         this.height / 2 - 10
      );
      Button throwButton;
      this.throwButton = throwButton = KitUtil.button(
         Component.literal(ModMain.getI18N("screen.exception_throw_test.throw")),
         Component.literal(ModMain.getI18N("screen.exception_throw_test.throw.empty_name")),
         btn -> this.actionThrow(classPathInputBox.getValue(), messageInputBox.getValue()),
         145,
         20,
         this.width / 2 - 150,
         this.height / 2 + 15
      );
      Button backButton = KitUtil.button(
         Component.literal(ModMain.getI18N("screen.exception_throw_test.back")),
         Component.literal(""),
         btn -> this.minecraft.setScreen(this.parent),
         145,
         20,
         this.width / 2 + 5,
         this.height / 2 + 15
      );
      this.addRenderableWidget(classPathInputBox);
      this.addRenderableWidget(messageInputBox);
      this.addRenderableWidget(throwButton);
      this.addRenderableWidget(backButton);
   }

   private void actionThrow(String name, String message) {
      Throwable exception;
      try {
         Class<?> targetClass = Class.forName(name);
         Constructor<?> constructor = targetClass.getConstructor(String.class);
         exception = (Throwable)constructor.newInstance(message);
      } catch (Exception throwable) {
         this.minecraft.setScreen(new MessageScreen(ModMain.getI18N("screen.exception_throw_test.error.caught", throwable), this));
         return;
      }

      throw new RuntimeException(exception);
   }

   private boolean isExceptionClass(String path) {
      try {
         return Class.forName(path).isAssignableFrom(Throwable.class);
      } catch (ClassNotFoundException e) {
         return false;
      }
   }

   @Override
   public void tick() {
      if (this.throwButton != null && !this.classPathInputBox.getValue().isBlank()) {
         this.throwButton.active = true;
      } else if (this.throwButton != null) {
         this.throwButton.active = false;
      }
   }
}
