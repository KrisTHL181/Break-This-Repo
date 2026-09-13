package net.minecraft.client.resources.server;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface PackReloadConfig {
    void scheduleReload(PackReloadConfig.Callbacks callbacks);

    @OnlyIn(Dist.CLIENT)
    interface Callbacks {
        void onSuccess();

        void onFailure(boolean isRecovery);

        List<PackReloadConfig.IdAndPath> packsToLoad();
    }

    @OnlyIn(Dist.CLIENT)
    record IdAndPath(UUID id, Path path) {
    }
}