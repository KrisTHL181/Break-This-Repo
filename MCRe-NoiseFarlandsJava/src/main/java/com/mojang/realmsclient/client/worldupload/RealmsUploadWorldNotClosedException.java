package com.mojang.realmsclient.client.worldupload;

import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RealmsUploadWorldNotClosedException extends RealmsUploadException {
    @Override
    public Component getStatusMessage() {
        return Component.translatable("mco.upload.close.failure");
    }
}