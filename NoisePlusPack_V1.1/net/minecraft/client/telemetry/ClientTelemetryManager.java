package net.minecraft.client.telemetry;

import com.google.common.base.Suppliers;
import com.mojang.authlib.minecraft.TelemetrySession;
import com.mojang.authlib.minecraft.UserApiService;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.util.Util;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class ClientTelemetryManager implements AutoCloseable {
   private static final AtomicInteger THREAD_COUNT = new AtomicInteger(1);
   private static final Executor EXECUTOR = Executors.newSingleThreadExecutor(p_261485_ -> {
      Thread thread = new Thread(p_261485_);
      thread.setName("Telemetry-Sender-#" + THREAD_COUNT.getAndIncrement());
      return thread;
   });
   private final Minecraft minecraft;
   private final UserApiService userApiService;
   private final TelemetryPropertyMap deviceSessionProperties;
   private final Path logDirectory;
   private final CompletableFuture<Optional<TelemetryLogManager>> logManager;
   private final Supplier<TelemetryEventSender> outsideSessionSender = Suppliers.memoize(this::createEventSender);

   public ClientTelemetryManager(Minecraft p_261610_, UserApiService p_261552_, User p_262159_) {
      this.minecraft = p_261610_;
      this.userApiService = p_261552_;
      TelemetryPropertyMap.Builder telemetrypropertymap$builder = TelemetryPropertyMap.builder();
      p_262159_.getXuid().ifPresent(p_261810_ -> telemetrypropertymap$builder.put(TelemetryProperty.USER_ID, p_261810_));
      p_262159_.getClientId().ifPresent(p_261690_ -> telemetrypropertymap$builder.put(TelemetryProperty.CLIENT_ID, p_261690_));
      telemetrypropertymap$builder.put(TelemetryProperty.MINECRAFT_SESSION_ID, UUID.randomUUID());
      telemetrypropertymap$builder.put(TelemetryProperty.GAME_VERSION, SharedConstants.getCurrentVersion().id());
      telemetrypropertymap$builder.put(TelemetryProperty.OPERATING_SYSTEM, Util.getPlatform().telemetryName());
      telemetrypropertymap$builder.put(TelemetryProperty.PLATFORM, System.getProperty("os.name"));
      telemetrypropertymap$builder.put(TelemetryProperty.CLIENT_MODDED, Minecraft.checkModStatus().shouldReportAsModified());
      telemetrypropertymap$builder.putIfNotNull(TelemetryProperty.LAUNCHER_NAME, Minecraft.getLauncherBrand());
      this.deviceSessionProperties = telemetrypropertymap$builder.build();
      this.logDirectory = p_261610_.gameDirectory.toPath().resolve("logs/telemetry");
      this.logManager = TelemetryLogManager.open(this.logDirectory);
   }

   public WorldSessionTelemetryManager createWorldSessionManager(boolean p_286373_, @Nullable Duration p_286752_, @Nullable String p_286568_) {
      return new WorldSessionTelemetryManager(this.createEventSender(), p_286373_, p_286752_, p_286568_);
   }

   public TelemetryEventSender getOutsideSessionSender() {
      return this.outsideSessionSender.get();
   }

   private TelemetryEventSender createEventSender() {
      if (!this.minecraft.allowsTelemetry()) {
         return TelemetryEventSender.DISABLED;
      }

      TelemetrySession telemetrysession = this.userApiService.newTelemetrySession(EXECUTOR);
      if (!telemetrysession.isEnabled()) {
         return TelemetryEventSender.DISABLED;
      }

      CompletableFuture<Optional<TelemetryEventLogger>> completablefuture = this.logManager
         .thenCompose(p_261737_ -> p_261737_.map(TelemetryLogManager::openLogger).orElseGet(() -> CompletableFuture.completedFuture(Optional.empty())));
      return (p_261827_, p_261818_) -> {
         if (!p_261827_.isOptIn() || Minecraft.getInstance().telemetryOptInExtra()) {
            TelemetryPropertyMap.Builder telemetrypropertymap$builder = TelemetryPropertyMap.builder();
            telemetrypropertymap$builder.putAll(this.deviceSessionProperties);
            telemetrypropertymap$builder.put(TelemetryProperty.EVENT_TIMESTAMP_UTC, Instant.now());
            telemetrypropertymap$builder.put(TelemetryProperty.OPT_IN, p_261827_.isOptIn());
            p_261818_.accept(telemetrypropertymap$builder);
            TelemetryEventInstance telemetryeventinstance = new TelemetryEventInstance(p_261827_, telemetrypropertymap$builder.build());
            completablefuture.thenAccept(p_421087_ -> {
               if (!p_421087_.isEmpty()) {
                  p_421087_.get().log(telemetryeventinstance);
                  if (!SharedConstants.IS_RUNNING_IN_IDE || !SharedConstants.DEBUG_DONT_SEND_TELEMETRY_TO_BACKEND) {
                     telemetryeventinstance.export(telemetrysession).send();
                  }
               }
            });
         }
      };
   }

   public Path getLogDirectory() {
      return this.logDirectory;
   }

   @Override
   public void close() {
      this.logManager.thenAccept(p_261643_ -> p_261643_.ifPresent(TelemetryLogManager::close));
   }
}
