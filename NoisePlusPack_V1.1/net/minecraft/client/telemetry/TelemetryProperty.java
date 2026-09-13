package net.minecraft.client.telemetry;

import com.mojang.authlib.minecraft.TelemetryPropertyContainer;
import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.minecraft.client.telemetry.events.GameLoadTimesEvent;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.StringRepresentable;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record TelemetryProperty<T>(String id, String exportKey, Codec<T> codec, TelemetryProperty.Exporter<T> exporter) {
   private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneId.from(ZoneOffset.UTC));
   public static final TelemetryProperty<String> USER_ID = string("user_id", "userId");
   public static final TelemetryProperty<String> CLIENT_ID = string("client_id", "clientId");
   public static final TelemetryProperty<UUID> MINECRAFT_SESSION_ID = uuid("minecraft_session_id", "deviceSessionId");
   public static final TelemetryProperty<String> GAME_VERSION = string("game_version", "buildDisplayName");
   public static final TelemetryProperty<String> OPERATING_SYSTEM = string("operating_system", "buildPlatform");
   public static final TelemetryProperty<String> PLATFORM = string("platform", "platform");
   public static final TelemetryProperty<Boolean> CLIENT_MODDED = bool("client_modded", "clientModded");
   public static final TelemetryProperty<String> LAUNCHER_NAME = string("launcher_name", "launcherName");
   public static final TelemetryProperty<UUID> WORLD_SESSION_ID = uuid("world_session_id", "worldSessionId");
   public static final TelemetryProperty<Boolean> SERVER_MODDED = bool("server_modded", "serverModded");
   public static final TelemetryProperty<TelemetryProperty.ServerType> SERVER_TYPE = create(
      "server_type",
      "serverType",
      TelemetryProperty.ServerType.CODEC,
      (p_261518_, p_262138_, p_262085_) -> p_261518_.addProperty(p_262138_, p_262085_.getSerializedName())
   );
   public static final TelemetryProperty<Boolean> OPT_IN = bool("opt_in", "isOptional");
   public static final TelemetryProperty<Instant> EVENT_TIMESTAMP_UTC = create(
      "event_timestamp_utc",
      "eventTimestampUtc",
      ExtraCodecs.INSTANT_ISO8601,
      (p_261517_, p_261626_, p_261868_) -> p_261517_.addProperty(p_261626_, TIMESTAMP_FORMATTER.format(p_261868_))
   );
   public static final TelemetryProperty<TelemetryProperty.GameMode> GAME_MODE = create(
      "game_mode", "playerGameMode", TelemetryProperty.GameMode.CODEC, (p_261849_, p_262092_, p_261574_) -> p_261849_.addProperty(p_262092_, p_261574_.id())
   );
   public static final TelemetryProperty<String> REALMS_MAP_CONTENT = string("realms_map_content", "realmsMapContent");
   public static final TelemetryProperty<Integer> SECONDS_SINCE_LOAD = integer("seconds_since_load", "secondsSinceLoad");
   public static final TelemetryProperty<Integer> TICKS_SINCE_LOAD = integer("ticks_since_load", "ticksSinceLoad");
   public static final TelemetryProperty<LongList> FRAME_RATE_SAMPLES = longSamples("frame_rate_samples", "serializedFpsSamples");
   public static final TelemetryProperty<LongList> RENDER_TIME_SAMPLES = longSamples("render_time_samples", "serializedRenderTimeSamples");
   public static final TelemetryProperty<LongList> USED_MEMORY_SAMPLES = longSamples("used_memory_samples", "serializedUsedMemoryKbSamples");
   public static final TelemetryProperty<Integer> NUMBER_OF_SAMPLES = integer("number_of_samples", "numSamples");
   public static final TelemetryProperty<Integer> RENDER_DISTANCE = integer("render_distance", "renderDistance");
   public static final TelemetryProperty<Integer> DEDICATED_MEMORY_KB = integer("dedicated_memory_kb", "dedicatedMemoryKb");
   public static final TelemetryProperty<Integer> WORLD_LOAD_TIME_MS = integer("world_load_time_ms", "worldLoadTimeMs");
   public static final TelemetryProperty<Boolean> NEW_WORLD = bool("new_world", "newWorld");
   public static final TelemetryProperty<GameLoadTimesEvent.Measurement> LOAD_TIME_TOTAL_TIME_MS = gameLoadMeasurement(
      "load_time_total_time_ms", "loadTimeTotalTimeMs"
   );
   public static final TelemetryProperty<GameLoadTimesEvent.Measurement> LOAD_TIME_PRE_WINDOW_MS = gameLoadMeasurement(
      "load_time_pre_window_ms", "loadTimePreWindowMs"
   );
   public static final TelemetryProperty<GameLoadTimesEvent.Measurement> LOAD_TIME_BOOTSTRAP_MS = gameLoadMeasurement(
      "load_time_bootstrap_ms", "loadTimeBootstrapMs"
   );
   public static final TelemetryProperty<GameLoadTimesEvent.Measurement> LOAD_TIME_LOADING_OVERLAY_MS = gameLoadMeasurement(
      "load_time_loading_overlay_ms", "loadTimeLoadingOverlayMs"
   );
   public static final TelemetryProperty<String> ADVANCEMENT_ID = string("advancement_id", "advancementId");
   public static final TelemetryProperty<Long> ADVANCEMENT_GAME_TIME = makeLong("advancement_game_time", "advancementGameTime");

   public static <T> TelemetryProperty<T> create(String p_262052_, String p_261851_, Codec<T> p_261617_, TelemetryProperty.Exporter<T> p_261478_) {
      return new TelemetryProperty<>(p_262052_, p_261851_, p_261617_, p_261478_);
   }

   public static TelemetryProperty<Boolean> bool(String p_261605_, String p_262064_) {
      return create(p_261605_, p_262064_, Codec.BOOL, TelemetryPropertyContainer::addProperty);
   }

   public static TelemetryProperty<String> string(String p_261570_, String p_261611_) {
      return create(p_261570_, p_261611_, Codec.STRING, TelemetryPropertyContainer::addProperty);
   }

   public static TelemetryProperty<Integer> integer(String p_262077_, String p_261580_) {
      return create(p_262077_, p_261580_, Codec.INT, TelemetryPropertyContainer::addProperty);
   }

   public static TelemetryProperty<Long> makeLong(String p_286489_, String p_286616_) {
      return create(p_286489_, p_286616_, Codec.LONG, TelemetryPropertyContainer::addProperty);
   }

   public static TelemetryProperty<UUID> uuid(String p_261558_, String p_261707_) {
      return create(p_261558_, p_261707_, UUIDUtil.STRING_CODEC, (p_261704_, p_261590_, p_261975_) -> p_261704_.addProperty(p_261590_, p_261975_.toString()));
   }

   public static TelemetryProperty<GameLoadTimesEvent.Measurement> gameLoadMeasurement(String p_286636_, String p_286769_) {
      return create(
         p_286636_, p_286769_, GameLoadTimesEvent.Measurement.CODEC, (p_286179_, p_286180_, p_286181_) -> p_286179_.addProperty(p_286180_, p_286181_.millis())
      );
   }

   public static TelemetryProperty<LongList> longSamples(String p_262055_, String p_261726_) {
      return create(
         p_262055_,
         p_261726_,
         Codec.LONG.listOf().xmap(LongArrayList::new, Function.identity()),
         (p_261674_, p_262049_, p_262118_) -> p_261674_.addProperty(
            p_262049_, p_262118_.longStream().mapToObj(String::valueOf).collect(Collectors.joining(";"))
         )
      );
   }

   public void export(TelemetryPropertyMap p_262111_, TelemetryPropertyContainer p_262082_) {
      T t = p_262111_.get(this);
      if (t != null) {
         this.exporter.apply(p_262082_, this.exportKey, t);
      } else {
         p_262082_.addNullProperty(this.exportKey);
      }
   }

   public MutableComponent title() {
      return Component.translatable("telemetry.property." + this.id + ".title");
   }

   @Override
   public String toString() {
      return "TelemetryProperty[" + this.id + "]";
   }

   @OnlyIn(Dist.CLIENT)
   public interface Exporter<T> {
      void apply(TelemetryPropertyContainer var1, String var2, T var3);
   }

   @OnlyIn(Dist.CLIENT)
   public enum GameMode implements StringRepresentable {
      SURVIVAL("survival", 0),
      CREATIVE("creative", 1),
      ADVENTURE("adventure", 2),
      SPECTATOR("spectator", 6),
      HARDCORE("hardcore", 99);

      public static final Codec<TelemetryProperty.GameMode> CODEC = StringRepresentable.fromEnum(TelemetryProperty.GameMode::values);
      private final String key;
      private final int id;

      GameMode(final String p_261661_, final int p_261545_) {
         this.key = p_261661_;
         this.id = p_261545_;
      }

      public int id() {
         return this.id;
      }

      @Override
      public String getSerializedName() {
         return this.key;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public enum ServerType implements StringRepresentable {
      REALM("realm"),
      LOCAL("local"),
      OTHER("server");

      public static final Codec<TelemetryProperty.ServerType> CODEC = StringRepresentable.fromEnum(TelemetryProperty.ServerType::values);
      private final String key;

      ServerType(final String p_261499_) {
         this.key = p_261499_;
      }

      @Override
      public String getSerializedName() {
         return this.key;
      }
   }
}
