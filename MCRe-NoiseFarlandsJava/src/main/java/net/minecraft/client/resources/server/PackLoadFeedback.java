package net.minecraft.client.resources.server;

import java.util.UUID;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface PackLoadFeedback {
    void reportUpdate(UUID id, PackLoadFeedback.Update result);

    void reportFinalResult(UUID id, PackLoadFeedback.FinalResult result);

    @OnlyIn(Dist.CLIENT)
    enum FinalResult {
        DECLINED,
        APPLIED,
        DISCARDED,
        DOWNLOAD_FAILED,
        ACTIVATION_FAILED;
    }

    @OnlyIn(Dist.CLIENT)
    enum Update {
        ACCEPTED,
        DOWNLOADED;
    }
}