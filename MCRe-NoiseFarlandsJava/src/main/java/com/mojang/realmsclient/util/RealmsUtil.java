package com.mojang.realmsclient.util;

import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.exception.RealmsServiceException;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class RealmsUtil {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Component RIGHT_NOW = Component.translatable("mco.util.time.now");
    private static final int MINUTES = 60;
    private static final int HOURS = 3600;
    private static final int DAYS = 86400;

    public static Component convertToAgePresentation(final long timeDiff) {
        if (timeDiff < 0L) {
            return RIGHT_NOW;
        } else {
            long timeDiffInSeconds = timeDiff / 1000L;
            if (timeDiffInSeconds < 60L) {
                return Component.translatable("mco.time.secondsAgo", timeDiffInSeconds);
            } else if (timeDiffInSeconds < 3600L) {
                long minutes = timeDiffInSeconds / 60L;
                return Component.translatable("mco.time.minutesAgo", minutes);
            } else if (timeDiffInSeconds < 86400L) {
                long hours = timeDiffInSeconds / 3600L;
                return Component.translatable("mco.time.hoursAgo", hours);
            } else {
                long days = timeDiffInSeconds / 86400L;
                return Component.translatable("mco.time.daysAgo", days);
            }
        }
    }

    public static Component convertToAgePresentationFromInstant(final Instant date) {
        return convertToAgePresentation(System.currentTimeMillis() - date.toEpochMilli());
    }

    public static void extractPlayerFace(final GuiGraphicsExtractor graphics, final int x, final int y, final int size, final UUID playerId) {
        PlayerFaceExtractor.extractRenderState(graphics, ResolvableProfile.createUnresolved(playerId), x, y, size);
    }

    public static <T> CompletableFuture<T> supplyAsync(
        final RealmsUtil.RealmsIoFunction<T> function, final @Nullable Consumer<RealmsServiceException> onFailure
    ) {
        return CompletableFuture.supplyAsync(() -> {
            RealmsClient client = RealmsClient.getOrCreate();

            try {
                return function.apply(client);
            } catch (Throwable t) {
                if (t instanceof RealmsServiceException e) {
                    if (onFailure != null) {
                        onFailure.accept(e);
                    }
                } else {
                    LOGGER.error("Unhandled exception", t);
                }

                throw new RuntimeException(t);
            }
        }, Util.nonCriticalIoPool());
    }

    public static CompletableFuture<Void> runAsync(final RealmsUtil.RealmsIoConsumer function, final @Nullable Consumer<RealmsServiceException> onFailure) {
        return supplyAsync(function, onFailure);
    }

    public static Consumer<RealmsServiceException> openScreenOnFailure(final Function<RealmsServiceException, Screen> errorScreen) {
        Minecraft minecraft = Minecraft.getInstance();
        return e -> minecraft.execute(() -> minecraft.gui.setScreen(errorScreen.apply(e)));
    }

    public static Consumer<RealmsServiceException> openScreenAndLogOnFailure(
        final Function<RealmsServiceException, Screen> errorScreen, final String errorMessage
    ) {
        return openScreenOnFailure(errorScreen).andThen(e -> LOGGER.error(errorMessage, e));
    }

    @FunctionalInterface
    @OnlyIn(Dist.CLIENT)
    public interface RealmsIoConsumer extends RealmsUtil.RealmsIoFunction<Void> {
        void accept(final RealmsClient client) throws RealmsServiceException;

        default Void apply(final RealmsClient client) throws RealmsServiceException {
            this.accept(client);
            return null;
        }
    }

    @FunctionalInterface
    @OnlyIn(Dist.CLIENT)
    public interface RealmsIoFunction<T> {
        T apply(final RealmsClient client) throws RealmsServiceException;
    }
}