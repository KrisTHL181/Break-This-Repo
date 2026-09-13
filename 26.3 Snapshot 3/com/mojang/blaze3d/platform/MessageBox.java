package com.mojang.blaze3d.platform;

import org.lwjgl.util.tinyfd.TinyFileDialogs;

public class MessageBox {
   private static final String DEFAULT_TITLE = "Minecraft";
   public static final String TYPE_OK = "ok";
   public static final String TYPE_OK_CANCEL = "okcancel";
   public static final String TYPE_YES_NO = "yesno";
   public static final String TYPE_YES_NO_CANCEL = "yesnocancel";
   public static final String ICON_INFO = "info";
   public static final String ICON_WARNING = "warning";
   public static final String ICON_ERROR = "error";
   public static final String ICON_QUESTION = "question";
   public static final int BUTTON_CANCEL_OR_NO = 0;
   public static final int BUTTON_OK_OR_YES = 1;
   public static final int BUTTON_NO = 2;

   public static void error(final String message) {
      TinyFileDialogs.tinyfd_messageBox("Minecraft", message, "ok", "error", 1);
   }

   public static boolean errorWithContinue(final String message) {
      return TinyFileDialogs.tinyfd_messageBox("Minecraft", message, "yesno", "error", 1) == 1;
   }
}
