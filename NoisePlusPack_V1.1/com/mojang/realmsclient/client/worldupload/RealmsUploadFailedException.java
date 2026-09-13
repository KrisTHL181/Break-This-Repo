package com.mojang.realmsclient.client.worldupload;

import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RealmsUploadFailedException extends RealmsUploadException {
   private final Component errorMessage;

   public RealmsUploadFailedException(Component p_364964_) {
      this.errorMessage = p_364964_;
   }

   public RealmsUploadFailedException(String p_362207_) {
      this(Component.literal(p_362207_));
   }

   @Override
   public Component getStatusMessage() {
      return Component.translatable("mco.upload.failed", this.errorMessage);
   }
}
