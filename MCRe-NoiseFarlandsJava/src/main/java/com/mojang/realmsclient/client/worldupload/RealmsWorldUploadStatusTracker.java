package com.mojang.realmsclient.client.worldupload;

import com.mojang.realmsclient.client.UploadStatus;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface RealmsWorldUploadStatusTracker {
    UploadStatus getUploadStatus();

    void setUploading();

    static RealmsWorldUploadStatusTracker noOp() {
        return new RealmsWorldUploadStatusTracker() {
            private final UploadStatus uploadStatus = new UploadStatus();

            @Override
            public UploadStatus getUploadStatus() {
                return this.uploadStatus;
            }

            @Override
            public void setUploading() {
            }
        };
    }
}