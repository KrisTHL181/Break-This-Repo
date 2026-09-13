package com.mojang.realmsclient.client.worldupload;

import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RealmsUploadCanceledException extends RealmsUploadException {
    private static final Component UPLOAD_CANCELED = Component.translatable("mco.upload.cancelled");

    @Override
    public Component getStatusMessage() {
        return UPLOAD_CANCELED;
    }
}