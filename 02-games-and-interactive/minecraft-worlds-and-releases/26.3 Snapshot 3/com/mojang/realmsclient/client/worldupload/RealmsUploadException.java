package com.mojang.realmsclient.client.worldupload;

import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

public abstract class RealmsUploadException extends RuntimeException {
   public @Nullable Component getStatusMessage() {
      return null;
   }

   public Component @Nullable [] getErrorMessages() {
      return null;
   }
}
