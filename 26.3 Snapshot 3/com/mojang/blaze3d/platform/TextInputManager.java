package com.mojang.blaze3d.platform;

import org.lwjgl.glfw.GLFW;

public class TextInputManager {
   private final Window window;
   private boolean textInputEnabled;
   private boolean imeRequested;
   private volatile boolean imeStatusChanged = true;
   private boolean cachedIMEStatus;

   public TextInputManager(final Window window) {
      this.window = window;
   }

   public void setTextInputArea(final int x0, final int y0, final int x1, final int y1) {
      int guiScale = this.window.getGuiScale();
      GLFW.glfwSetPreeditCursorRectangle(this.window.handle(), x0 * guiScale, y0 * guiScale, (x1 - x0) * guiScale, (y1 - y0) * guiScale);
   }

   public void notifyIMEChanged() {
      this.imeStatusChanged = true;
   }

   public void tick() {
      if (this.textInputEnabled) {
         this.tickDuringTextInput();
      } else {
         this.tickOutsideTextInput();
      }
   }

   private boolean getIMEStatus() {
      if (this.imeStatusChanged) {
         this.imeStatusChanged = false;
         this.cachedIMEStatus = GLFW.glfwGetInputMode(this.window.handle(), 208903) == 1;
      }

      return this.cachedIMEStatus;
   }

   private void tickOutsideTextInput() {
      if (this.window.isFocused() && this.getIMEStatus()) {
         this.setIMEInputMode(false);
      }
   }

   private void tickDuringTextInput() {
      this.imeRequested = this.getIMEStatus();
   }

   public void startTextInput() {
      this.textInputEnabled = true;
      if (this.imeRequested) {
         this.setIMEInputMode(true);
      }
   }

   public void stopTextInput() {
      this.textInputEnabled = false;
   }

   public void onTextInputFocusChange(final boolean focused) {
      if (focused) {
         this.startTextInput();
      } else {
         this.stopTextInput();
      }
   }

   private void setIMEInputMode(final boolean value) {
      GLFW.glfwSetInputMode(this.window.handle(), 208903, GLX.glfwBool(value));
   }
}
