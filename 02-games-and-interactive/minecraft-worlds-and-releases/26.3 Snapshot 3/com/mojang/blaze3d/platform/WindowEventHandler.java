package com.mojang.blaze3d.platform;

public interface WindowEventHandler {
   void framebufferSizeChanged();

   void resizeGui();

   void cursorEntered();
}
