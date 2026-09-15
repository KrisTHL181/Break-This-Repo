package com.mojang.realmsclient.client.worldupload;

import net.minecraft.network.chat.Component;

public class RealmsUploadCanceledException extends RealmsUploadException {
   private static final Component UPLOAD_CANCELED = Component.translatable("mco.upload.cancelled");

   @Override
   public Component getStatusMessage() {
      return UPLOAD_CANCELED;
   }
}
