package net.minecraft.client;

import com.google.common.collect.ImmutableMap;
import com.google.common.math.LongMath;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2BooleanFunction;
import java.io.Reader;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.StrictJsonParser;
import net.minecraft.util.Util;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class PeriodicNotificationManager
   extends SimplePreparableReloadListener<Map<String, List<PeriodicNotificationManager.Notification>>>
   implements AutoCloseable {
   // ===== 修改：显式指定 Codec.unboundedMap 类型参数，以及 RecordCodecBuilder.create 类型参数 =====
   private static final Codec<Map<String, List<PeriodicNotificationManager.Notification>>> CODEC = Codec.<String, List<PeriodicNotificationManager.Notification>>unboundedMap(
      Codec.STRING,
      RecordCodecBuilder.<PeriodicNotificationManager.Notification>create(
            i -> i.group(
                  Codec.LONG.optionalFieldOf("delay", 0L).forGetter(PeriodicNotificationManager.Notification::delay),
                  Codec.LONG.fieldOf("period").forGetter(PeriodicNotificationManager.Notification::period),
                  Codec.STRING.fieldOf("title").forGetter(PeriodicNotificationManager.Notification::title),
                  Codec.STRING.fieldOf("message").forGetter(PeriodicNotificationManager.Notification::message)
               )
               .apply(i, PeriodicNotificationManager.Notification::new)
         )
         .listOf()
   );
   private static final Logger LOGGER = LogUtils.getLogger();
   private final Identifier notifications;
   private final Object2BooleanFunction<String> selector;
   private @Nullable Timer timer;
   private PeriodicNotificationManager.@Nullable NotificationTask notificationTask;

   public PeriodicNotificationManager(final Identifier notifications, final Object2BooleanFunction<String> selector) {
      this.notifications = notifications;
      this.selector = selector;
   }

   protected Map<String, List<PeriodicNotificationManager.Notification>> prepare(final ResourceManager manager, final ProfilerFiller profiler) {
      try (Reader reader = manager.openAsReader(this.notifications)) {
         return (Map<String, List<PeriodicNotificationManager.Notification>>)CODEC.parse(JsonOps.INSTANCE, StrictJsonParser.parse(reader))
            .result()
            .orElseThrow();
      } catch (Exception e) {
         LOGGER.warn("Failed to load {}", this.notifications, e);
         return ImmutableMap.of();
      }
   }

   protected void apply(
      final Map<String, List<PeriodicNotificationManager.Notification>> preparations, final ResourceManager manager, final ProfilerFiller profiler
   ) {
      List<PeriodicNotificationManager.Notification> notifications = preparations.entrySet()
         .stream()
         .filter(e -> (Boolean)this.selector.apply(e.getKey()))
         .map(Entry::getValue)
         .flatMap(Collection::stream)
         .collect(Collectors.toList());
      if (notifications.isEmpty()) {
         this.stopTimer();
      } else if (notifications.stream().anyMatch(n -> n.period == 0L)) {
         Util.logAndPauseIfInIde("A periodic notification in " + this.notifications + " has a period of zero minutes");
         this.stopTimer();
      } else {
         long delay = this.calculateInitialDelay(notifications);
         long period = this.calculateOptimalPeriod(notifications, delay);
         if (this.timer == null) {
            this.timer = new Timer();
         }

         if (this.notificationTask == null) {
            this.notificationTask = new PeriodicNotificationManager.NotificationTask(notifications, delay, period);
         } else {
            this.notificationTask = this.notificationTask.reset(notifications, period);
         }

         this.timer.scheduleAtFixedRate(this.notificationTask, TimeUnit.MINUTES.toMillis(delay), TimeUnit.MINUTES.toMillis(period));
      }
   }

   @Override
   public void close() {
      this.stopTimer();
   }

   private void stopTimer() {
      if (this.timer != null) {
         this.timer.cancel();
      }
   }

   private long calculateOptimalPeriod(final List<PeriodicNotificationManager.Notification> notifications, final long initialDelay) {
      return notifications.stream().mapToLong(c -> {
         long delayPeriods = c.delay - initialDelay;
         return LongMath.gcd(delayPeriods, c.period);
      }).reduce(LongMath::gcd).orElseThrow(() -> new IllegalStateException("Empty notifications from: " + this.notifications));
   }

   private long calculateInitialDelay(final List<PeriodicNotificationManager.Notification> notifications) {
      return notifications.stream().mapToLong(c -> c.delay).min().orElse(0L);
   }

   public record Notification(long delay, long period, String title, String message) {
   }

   private static class NotificationTask extends TimerTask {
      private final Minecraft minecraft = Minecraft.getInstance();
      private final List<PeriodicNotificationManager.Notification> notifications;
      private final long period;
      private final AtomicLong elapsed;

      public NotificationTask(final List<PeriodicNotificationManager.Notification> notifications, final long elapsed, final long period) {
         this.notifications = notifications;
         this.period = period;
         this.elapsed = new AtomicLong(elapsed);
      }

      public PeriodicNotificationManager.NotificationTask reset(final List<PeriodicNotificationManager.Notification> notifications, final long period) {
         this.cancel();
         return new PeriodicNotificationManager.NotificationTask(notifications, this.elapsed.get(), period);
      }

      @Override
      public void run() {
         long currentMinute = this.elapsed.getAndAdd(this.period);
         long nextMinute = this.elapsed.get();

         for (PeriodicNotificationManager.Notification notification : this.notifications) {
            if (currentMinute >= notification.delay) {
               long elapsedPeriods = currentMinute / notification.period;
               long currentPeriods = nextMinute / notification.period;
               if (elapsedPeriods != currentPeriods) {
                  this.minecraft
                     .execute(
                        () -> SystemToast.add(
                           Minecraft.getInstance().gui.toastManager(),
                           SystemToast.SystemToastId.PERIODIC_NOTIFICATION,
                           Component.translatable(notification.title, elapsedPeriods),
                           Component.translatable(notification.message, elapsedPeriods)
                        )
                     );
                  return;
               }
            }
         }
      }
   }
}