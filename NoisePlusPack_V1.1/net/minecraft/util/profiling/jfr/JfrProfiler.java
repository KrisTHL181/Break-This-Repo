package net.minecraft.util.profiling.jfr;

import com.mojang.logging.LogUtils;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.SocketAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import jdk.jfr.Configuration;
import jdk.jfr.Event;
import jdk.jfr.FlightRecorder;
import jdk.jfr.FlightRecorderListener;
import jdk.jfr.Recording;
import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.FileUtil;
import net.minecraft.util.Util;
import net.minecraft.util.profiling.jfr.callback.ProfiledDuration;
import net.minecraft.util.profiling.jfr.event.ChunkGenerationEvent;
import net.minecraft.util.profiling.jfr.event.ChunkRegionReadEvent;
import net.minecraft.util.profiling.jfr.event.ChunkRegionWriteEvent;
import net.minecraft.util.profiling.jfr.event.ClientFpsEvent;
import net.minecraft.util.profiling.jfr.event.NetworkSummaryEvent;
import net.minecraft.util.profiling.jfr.event.PacketReceivedEvent;
import net.minecraft.util.profiling.jfr.event.PacketSentEvent;
import net.minecraft.util.profiling.jfr.event.ServerTickTimeEvent;
import net.minecraft.util.profiling.jfr.event.StructureGenerationEvent;
import net.minecraft.util.profiling.jfr.event.WorldLoadFinishedEvent;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.storage.RegionFileVersion;
import net.minecraft.world.level.chunk.storage.RegionStorageInfo;
import net.minecraft.world.level.levelgen.structure.Structure;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class JfrProfiler implements JvmProfiler {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final String ROOT_CATEGORY = "Minecraft";
   public static final String WORLD_GEN_CATEGORY = "World Generation";
   public static final String TICK_CATEGORY = "Ticking";
   public static final String NETWORK_CATEGORY = "Network";
   public static final String STORAGE_CATEGORY = "Storage";
   private static final List<Class<? extends Event>> CUSTOM_EVENTS = List.of(
      ChunkGenerationEvent.class,
      ChunkRegionReadEvent.class,
      ChunkRegionWriteEvent.class,
      PacketReceivedEvent.class,
      PacketSentEvent.class,
      NetworkSummaryEvent.class,
      ServerTickTimeEvent.class,
      ClientFpsEvent.class,
      StructureGenerationEvent.class,
      WorldLoadFinishedEvent.class
   );
   private static final String FLIGHT_RECORDER_CONFIG = "/flightrecorder-config.jfc";
   private static final DateTimeFormatter DATE_TIME_FORMATTER = new DateTimeFormatterBuilder()
      .appendPattern("yyyy-MM-dd-HHmmss")
      .toFormatter(Locale.ROOT)
      .withZone(ZoneId.systemDefault());
   private static final JfrProfiler INSTANCE = new JfrProfiler();
   @Nullable Recording recording;
   private int currentFPS;
   private float currentAverageTickTimeServer;
   private final Map<String, NetworkSummaryEvent.SumAggregation> networkTrafficByAddress = new ConcurrentHashMap<>();
   private final Runnable periodicClientFps = () -> new ClientFpsEvent(this.currentFPS).commit();
   private final Runnable periodicServerTickTime = () -> new ServerTickTimeEvent(this.currentAverageTickTimeServer).commit();
   private final Runnable periodicNetworkSummary = () -> {
      Iterator<NetworkSummaryEvent.SumAggregation> iterator = this.networkTrafficByAddress.values().iterator();

      while (iterator.hasNext()) {
         iterator.next().commitEvent();
         iterator.remove();
      }
   };

   private JfrProfiler() {
      CUSTOM_EVENTS.forEach(FlightRecorder::register);
      this.registerPeriodicEvents();
      FlightRecorder.addListener(new FlightRecorderListener() {
         @Override
         public void recordingStateChanged(Recording p_185339_) {
            switch (p_185339_.getState()) {
               case STOPPED:
                  JfrProfiler.this.registerPeriodicEvents();
               case NEW:
               case DELAYED:
               case RUNNING:
               case CLOSED:
            }
         }
      });
   }

   void registerPeriodicEvents() {
      addPeriodicEvent(ClientFpsEvent.class, this.periodicClientFps);
      addPeriodicEvent(ServerTickTimeEvent.class, this.periodicServerTickTime);
      addPeriodicEvent(NetworkSummaryEvent.class, this.periodicNetworkSummary);
   }

   private static void addPeriodicEvent(Class<? extends Event> p_458888_, Runnable p_454666_) {
      FlightRecorder.removePeriodicEvent(p_454666_);
      FlightRecorder.addPeriodicEvent(p_458888_, p_454666_);
   }

   public static JfrProfiler getInstance() {
      return INSTANCE;
   }

   @Override
   public boolean start(Environment p_185307_) {
      URL url = JfrProfiler.class.getResource("/flightrecorder-config.jfc");
      if (url == null) {
         LOGGER.warn("Could not find default flight recorder config at {}", "/flightrecorder-config.jfc");
         return false;
      }

      try (BufferedReader bufferedreader = new BufferedReader(new InputStreamReader(url.openStream(), StandardCharsets.UTF_8))) {
         return this.start(bufferedreader, p_185307_);
      } catch (IOException ioexception) {
         LOGGER.warn("Failed to start flight recorder using configuration at {}", url, ioexception);
         return false;
      }
   }

   @Override
   public Path stop() {
      if (this.recording == null) {
         throw new IllegalStateException("Not currently profiling");
      }

      this.networkTrafficByAddress.clear();
      Path path = this.recording.getDestination();
      this.recording.stop();
      return path;
   }

   @Override
   public boolean isRunning() {
      return this.recording != null;
   }

   @Override
   public boolean isAvailable() {
      return FlightRecorder.isAvailable();
   }

   private boolean start(Reader p_185317_, Environment p_185318_) {
      if (this.isRunning()) {
         LOGGER.warn("Profiling already in progress");
         return false;
      }

      try {
         Configuration configuration = Configuration.create(p_185317_);
         String s = DATE_TIME_FORMATTER.format(Instant.now());
         this.recording = Util.make(new Recording(configuration), p_405260_ -> {
            CUSTOM_EVENTS.forEach(p_405260_::enable);
            p_405260_.setDumpOnExit(true);
            p_405260_.setToDisk(true);
            p_405260_.setName(String.format(Locale.ROOT, "%s-%s-%s", p_185318_.getDescription(), SharedConstants.getCurrentVersion().name(), s));
         });
         Path path = Paths.get(String.format(Locale.ROOT, "debug/%s-%s.jfr", p_185318_.getDescription(), s));
         FileUtil.createDirectoriesSafe(path.getParent());
         this.recording.setDestination(path);
         this.recording.start();
         this.setupSummaryListener();
      } catch (ParseException | IOException ioexception) {
         LOGGER.warn("Failed to start jfr profiling", ioexception);
         return false;
      }

      LOGGER.info(
         "Started flight recorder profiling id({}):name({}) - will dump to {} on exit or stop command",
         new Object[]{this.recording.getId(), this.recording.getName(), this.recording.getDestination()}
      );
      return true;
   }

   private void setupSummaryListener() {
      FlightRecorder.addListener(new FlightRecorderListener() {
         final SummaryReporter summaryReporter = new SummaryReporter(() -> JfrProfiler.this.recording = null);

         @Override
         public void recordingStateChanged(Recording p_459441_) {
            if (p_459441_ == JfrProfiler.this.recording) {
               switch (p_459441_.getState()) {
                  case STOPPED:
                     this.summaryReporter.recordingStopped(p_459441_.getDestination());
                     FlightRecorder.removeListener(this);
                  case NEW:
                  case DELAYED:
                  case RUNNING:
                  case CLOSED:
               }
            }
         }
      });
   }

   @Override
   public void onClientTick(int p_458125_) {
      if (ClientFpsEvent.TYPE.isEnabled()) {
         this.currentFPS = p_458125_;
      }
   }

   @Override
   public void onServerTick(float p_185300_) {
      if (ServerTickTimeEvent.TYPE.isEnabled()) {
         this.currentAverageTickTimeServer = p_185300_;
      }
   }

   @Override
   public void onPacketReceived(ConnectionProtocol p_300094_, PacketType<?> p_335626_, SocketAddress p_185304_, int p_185302_) {
      if (PacketReceivedEvent.TYPE.isEnabled()) {
         new PacketReceivedEvent(p_300094_.id(), p_335626_.flow().id(), p_335626_.id().toString(), p_185304_, p_185302_).commit();
      }

      if (NetworkSummaryEvent.TYPE.isEnabled()) {
         this.networkStatFor(p_185304_).trackReceivedPacket(p_185302_);
      }
   }

   @Override
   public void onPacketSent(ConnectionProtocol p_299489_, PacketType<?> p_334491_, SocketAddress p_185325_, int p_185323_) {
      if (PacketSentEvent.TYPE.isEnabled()) {
         new PacketSentEvent(p_299489_.id(), p_334491_.flow().id(), p_334491_.id().toString(), p_185325_, p_185323_).commit();
      }

      if (NetworkSummaryEvent.TYPE.isEnabled()) {
         this.networkStatFor(p_185325_).trackSentPacket(p_185323_);
      }
   }

   private NetworkSummaryEvent.SumAggregation networkStatFor(SocketAddress p_185320_) {
      return this.networkTrafficByAddress.computeIfAbsent(p_185320_.toString(), NetworkSummaryEvent.SumAggregation::new);
   }

   @Override
   public void onRegionFileRead(RegionStorageInfo p_332602_, ChunkPos p_331074_, RegionFileVersion p_332565_, int p_334299_) {
      if (ChunkRegionReadEvent.TYPE.isEnabled()) {
         new ChunkRegionReadEvent(p_332602_, p_331074_, p_332565_, p_334299_).commit();
      }
   }

   @Override
   public void onRegionFileWrite(RegionStorageInfo p_334895_, ChunkPos p_330898_, RegionFileVersion p_334334_, int p_334429_) {
      if (ChunkRegionWriteEvent.TYPE.isEnabled()) {
         new ChunkRegionWriteEvent(p_334895_, p_330898_, p_334334_, p_334429_).commit();
      }
   }

   @Override
   public @Nullable ProfiledDuration onWorldLoadedStarted() {
      if (!WorldLoadFinishedEvent.TYPE.isEnabled()) {
         return null;
      }

      WorldLoadFinishedEvent worldloadfinishedevent = new WorldLoadFinishedEvent();
      worldloadfinishedevent.begin();
      return p_374913_ -> worldloadfinishedevent.commit();
   }

   @Override
   public @Nullable ProfiledDuration onChunkGenerate(ChunkPos p_185313_, ResourceKey<Level> p_185314_, String p_185315_) {
      if (!ChunkGenerationEvent.TYPE.isEnabled()) {
         return null;
      }

      ChunkGenerationEvent chunkgenerationevent = new ChunkGenerationEvent(p_185313_, p_185314_, p_185315_);
      chunkgenerationevent.begin();
      return p_374911_ -> chunkgenerationevent.commit();
   }

   @Override
   public @Nullable ProfiledDuration onStructureGenerate(ChunkPos p_376237_, ResourceKey<Level> p_376556_, Holder<Structure> p_377441_) {
      if (!StructureGenerationEvent.TYPE.isEnabled()) {
         return null;
      }

      StructureGenerationEvent structuregenerationevent = new StructureGenerationEvent(p_376237_, p_377441_, p_376556_);
      structuregenerationevent.begin();
      return p_374909_ -> {
         structuregenerationevent.success = p_374909_;
         structuregenerationevent.commit();
      };
   }
}
