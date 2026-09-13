package net.minecraft.util;

import com.google.common.base.Ticker;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.MoreExecutors;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.DSL.TypeReference;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Pair;
import com.mojang.jtracy.TracyClient;
import com.mojang.jtracy.Zone;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectLists;
import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceImmutableList;
import it.unimi.dsi.fastutil.objects.ReferenceList;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.spi.FileSystemProvider;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;
import java.util.function.UnaryOperator;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import net.minecraft.CharPredicate;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.DefaultUncaughtExceptionHandler;
import net.minecraft.ReportType;
import net.minecraft.ReportedException;
import net.minecraft.SharedConstants;
import net.minecraft.SuppressForbidden;
import net.minecraft.TracingExecutor;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.world.level.block.state.properties.Property;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class Util {
   static final Logger LOGGER = LogUtils.getLogger();
   private static final int DEFAULT_MAX_THREADS = 255;
   private static final int DEFAULT_SAFE_FILE_OPERATION_RETRIES = 10;
   private static final String MAX_THREADS_SYSTEM_PROPERTY = "max.bg.threads";
   private static final TracingExecutor BACKGROUND_EXECUTOR = makeExecutor("Main");
   private static final TracingExecutor IO_POOL = makeIoExecutor("IO-Worker-", false);
   private static final TracingExecutor DOWNLOAD_POOL = makeIoExecutor("Download-", true);
   private static final DateTimeFormatter FILENAME_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH.mm.ss", Locale.ROOT);
   public static final int LINEAR_LOOKUP_THRESHOLD = 8;
   private static final Set<String> ALLOWED_UNTRUSTED_LINK_PROTOCOLS = Set.of("http", "https");
   public static final long NANOS_PER_MILLI = 1000000L;
   public static TimeSource.NanoTimeSource timeSource = System::nanoTime;
   public static final Ticker TICKER = new Ticker() {
      public long read() {
         return Util.timeSource.getAsLong();
      }
   };
   public static final UUID NIL_UUID = new UUID(0L, 0L);
   public static final FileSystemProvider ZIP_FILE_SYSTEM_PROVIDER = FileSystemProvider.installedProviders()
      .stream()
      .filter(p_457025_ -> p_457025_.getScheme().equalsIgnoreCase("jar"))
      .findFirst()
      .orElseThrow(() -> new IllegalStateException("No jar file system provider found"));
   private static Consumer<String> thePauser = p_458324_ -> {};

   public static <K, V> Collector<Entry<? extends K, ? extends V>, ?, Map<K, V>> toMap() {
      return Collectors.toMap(Entry::getKey, Entry::getValue);
   }

   public static <T> Collector<T, ?, List<T>> toMutableList() {
      return Collectors.toCollection(Lists::newArrayList);
   }

   public static <T extends Comparable<T>> String getPropertyName(Property<T> p_460833_, Object p_457400_) {
      return p_460833_.getName((T)p_457400_);
   }

   public static String makeDescriptionId(String p_455958_, @Nullable Identifier p_457044_) {
      return p_457044_ == null ? p_455958_ + ".unregistered_sadface" : p_455958_ + "." + p_457044_.getNamespace() + "." + p_457044_.getPath().replace('/', '.');
   }

   public static long getMillis() {
      return getNanos() / 1000000L;
   }

   public static long getNanos() {
      return timeSource.getAsLong();
   }

   public static long getEpochMillis() {
      return Instant.now().toEpochMilli();
   }

   public static String getFilenameFormattedDateTime() {
      return FILENAME_DATE_TIME_FORMATTER.format(ZonedDateTime.now());
   }

   private static TracingExecutor makeExecutor(String p_453187_) {
      int i = maxAllowedExecutorThreads();
      ExecutorService executorservice;
      if (i <= 0) {
         executorservice = MoreExecutors.newDirectExecutorService();
      } else {
         AtomicInteger atomicinteger = new AtomicInteger(1);
         executorservice = new ForkJoinPool(i, p_453197_ -> {
            final String s = "Worker-" + p_453187_ + "-" + atomicinteger.getAndIncrement();
            ForkJoinWorkerThread forkjoinworkerthread = new ForkJoinWorkerThread(p_453197_) {
               @Override
               protected void onStart() {
                  TracyClient.setThreadName(s, p_453187_.hashCode());
                  super.onStart();
               }

               @Override
               protected void onTermination(@Nullable Throwable p_460306_) {
                  if (p_460306_ != null) {
                     Util.LOGGER.warn("{} died", this.getName(), p_460306_);
                  } else {
                     Util.LOGGER.debug("{} shutdown", this.getName());
                  }

                  super.onTermination(p_460306_);
               }
            };
            forkjoinworkerthread.setName(s);
            return forkjoinworkerthread;
         }, Util::onThreadException, true);
      }

      return new TracingExecutor(executorservice);
   }

   public static int maxAllowedExecutorThreads() {
      return Mth.clamp(Runtime.getRuntime().availableProcessors() - 1, 1, getMaxThreads());
   }

   private static int getMaxThreads() {
      String s = System.getProperty("max.bg.threads");
      if (s != null) {
         try {
            int i = Integer.parseInt(s);
            if (i >= 1 && i <= 255) {
               return i;
            }

            LOGGER.error("Wrong {} property value '{}'. Should be an integer value between 1 and {}.", new Object[]{"max.bg.threads", s, 255});
         } catch (NumberFormatException numberformatexception) {
            LOGGER.error("Could not parse {} property value '{}'. Should be an integer value between 1 and {}.", new Object[]{"max.bg.threads", s, 255});
         }
      }

      return 255;
   }

   public static TracingExecutor backgroundExecutor() {
      return BACKGROUND_EXECUTOR;
   }

   public static TracingExecutor ioPool() {
      return IO_POOL;
   }

   public static TracingExecutor nonCriticalIoPool() {
      return DOWNLOAD_POOL;
   }

   public static void shutdownExecutors() {
      BACKGROUND_EXECUTOR.shutdownAndAwait(3L, TimeUnit.SECONDS);
      IO_POOL.shutdownAndAwait(3L, TimeUnit.SECONDS);
   }

   private static TracingExecutor makeIoExecutor(String p_457359_, boolean p_459469_) {
      AtomicInteger atomicinteger = new AtomicInteger(1);
      return new TracingExecutor(Executors.newCachedThreadPool(p_450370_ -> {
         Thread thread = new Thread(p_450370_);
         String s = p_457359_ + atomicinteger.getAndIncrement();
         TracyClient.setThreadName(s, p_457359_.hashCode());
         thread.setName(s);
         thread.setDaemon(p_459469_);
         thread.setUncaughtExceptionHandler(Util::onThreadException);
         return thread;
      }));
   }

   public static void throwAsRuntime(Throwable p_460606_) {
      throw p_460606_ instanceof RuntimeException ? (RuntimeException)p_460606_ : new RuntimeException(p_460606_);
   }

   private static void onThreadException(Thread p_456265_, Throwable p_453090_) {
      pauseInIde(p_453090_);
      if (p_453090_ instanceof CompletionException) {
         p_453090_ = p_453090_.getCause();
      }

      if (p_453090_ instanceof ReportedException reportedexception) {
         Bootstrap.realStdoutPrintln(reportedexception.getReport().getFriendlyReport(ReportType.CRASH));
         System.exit(-1);
      }

      LOGGER.error("Caught exception in thread {}", p_456265_, p_453090_);
   }

   public static @Nullable Type<?> fetchChoiceType(TypeReference p_459367_, String p_459346_) {
      return !SharedConstants.CHECK_DATA_FIXER_SCHEMA ? null : doFetchChoiceType(p_459367_, p_459346_);
   }

   private static @Nullable Type<?> doFetchChoiceType(TypeReference p_457223_, String p_460132_) {
      Type<?> type = null;

      try {
         type = DataFixers.getDataFixer()
            .getSchema(DataFixUtils.makeKey(SharedConstants.getCurrentVersion().dataVersion().version()))
            .getChoiceType(p_457223_, p_460132_);
      } catch (IllegalArgumentException illegalargumentexception) {
         LOGGER.error("No data fixer registered for {}", p_460132_);
         if (SharedConstants.IS_RUNNING_IN_IDE) {
            throw illegalargumentexception;
         }
      }

      return type;
   }

   public static void runNamed(Runnable p_460297_, String p_459159_) {
      if (SharedConstants.IS_RUNNING_IN_IDE) {
         Thread thread = Thread.currentThread();
         String s = thread.getName();
         thread.setName(p_459159_);

         try {
            Zone zone = TracyClient.beginZone(p_459159_, SharedConstants.IS_RUNNING_IN_IDE);

            try {
               p_460297_.run();
            } catch (Throwable var16) {
               if (zone != null) {
                  try {
                     zone.close();
                  } catch (Throwable var14) {
                     var16.addSuppressed(var14);
                  }
               }

               throw var16;
            }

            if (zone != null) {
               zone.close();
            }
         } finally {
            thread.setName(s);
         }
      } else {
         Zone zone1 = TracyClient.beginZone(p_459159_, SharedConstants.IS_RUNNING_IN_IDE);

         try {
            p_460297_.run();
         } catch (Throwable var15) {
            if (zone1 != null) {
               try {
                  zone1.close();
               } catch (Throwable var13) {
                  var15.addSuppressed(var13);
               }
            }

            throw var15;
         }

         if (zone1 != null) {
            zone1.close();
         }
      }
   }

   public static <T> String getRegisteredName(Registry<T> p_460292_, T p_459811_) {
      Identifier identifier = p_460292_.getKey(p_459811_);
      return identifier == null ? "[unregistered]" : identifier.toString();
   }

   public static <T> Predicate<T> allOf() {
      return p_460613_ -> true;
   }

   public static <T> Predicate<T> allOf(Predicate<? super T> p_456091_) {
      return (Predicate<T>)p_456091_;
   }

   public static <T> Predicate<T> allOf(Predicate<? super T> p_459674_, Predicate<? super T> p_453390_) {
      return p_451013_ -> p_459674_.test(p_451013_) && p_453390_.test(p_451013_);
   }

   public static <T> Predicate<T> allOf(Predicate<? super T> p_454735_, Predicate<? super T> p_460074_, Predicate<? super T> p_457366_) {
      return p_453285_ -> p_454735_.test(p_453285_) && p_460074_.test(p_453285_) && p_457366_.test(p_453285_);
   }

   public static <T> Predicate<T> allOf(
      Predicate<? super T> p_456296_, Predicate<? super T> p_458470_, Predicate<? super T> p_458595_, Predicate<? super T> p_455733_
   ) {
      return p_450585_ -> p_456296_.test(p_450585_) && p_458470_.test(p_450585_) && p_458595_.test(p_450585_) && p_455733_.test(p_450585_);
   }

   public static <T> Predicate<T> allOf(
      Predicate<? super T> p_459141_,
      Predicate<? super T> p_454958_,
      Predicate<? super T> p_460515_,
      Predicate<? super T> p_453827_,
      Predicate<? super T> p_457799_
   ) {
      return p_456408_ -> p_459141_.test(p_456408_)
         && p_454958_.test(p_456408_)
         && p_460515_.test(p_456408_)
         && p_453827_.test(p_456408_)
         && p_457799_.test(p_456408_);
   }

   @SafeVarargs
   public static <T> Predicate<T> allOf(Predicate<? super T>... p_451245_) {
      return p_451148_ -> {
         for (Predicate<? super T> predicate : p_451245_) {
            if (!predicate.test(p_451148_)) {
               return false;
            }
         }

         return true;
      };
   }

   public static <T> Predicate<T> allOf(List<? extends Predicate<? super T>> p_453627_) {
      return switch (p_453627_.size()) {
         case 0 -> allOf();
         case 1 -> allOf((Predicate<? super T>)p_453627_.get(0));
         case 2 -> allOf((Predicate<? super T>)p_453627_.get(0), (Predicate<? super T>)p_453627_.get(1));
         case 3 -> allOf((Predicate<? super T>)p_453627_.get(0), (Predicate<? super T>)p_453627_.get(1), (Predicate<? super T>)p_453627_.get(2));
         case 4 -> allOf(
            (Predicate<? super T>)p_453627_.get(0),
            (Predicate<? super T>)p_453627_.get(1),
            (Predicate<? super T>)p_453627_.get(2),
            (Predicate<? super T>)p_453627_.get(3)
         );
         case 5 -> allOf(
            (Predicate<? super T>)p_453627_.get(0),
            (Predicate<? super T>)p_453627_.get(1),
            (Predicate<? super T>)p_453627_.get(2),
            (Predicate<? super T>)p_453627_.get(3),
            (Predicate<? super T>)p_453627_.get(4)
         );
         default -> {
            Predicate<? super T>[] predicate = p_453627_.toArray(Predicate[]::new);
            yield allOf(predicate);
         }
      };
   }

   public static <T> Predicate<T> anyOf() {
      return p_454278_ -> false;
   }

   public static <T> Predicate<T> anyOf(Predicate<? super T> p_459967_) {
      return (Predicate<T>)p_459967_;
   }

   public static <T> Predicate<T> anyOf(Predicate<? super T> p_454209_, Predicate<? super T> p_454901_) {
      return p_450364_ -> p_454209_.test(p_450364_) || p_454901_.test(p_450364_);
   }

   public static <T> Predicate<T> anyOf(Predicate<? super T> p_453617_, Predicate<? super T> p_459231_, Predicate<? super T> p_452772_) {
      return p_457958_ -> p_453617_.test(p_457958_) || p_459231_.test(p_457958_) || p_452772_.test(p_457958_);
   }

   public static <T> Predicate<T> anyOf(
      Predicate<? super T> p_458592_, Predicate<? super T> p_454675_, Predicate<? super T> p_451205_, Predicate<? super T> p_456001_
   ) {
      return p_454136_ -> p_458592_.test(p_454136_) || p_454675_.test(p_454136_) || p_451205_.test(p_454136_) || p_456001_.test(p_454136_);
   }

   public static <T> Predicate<T> anyOf(
      Predicate<? super T> p_458851_,
      Predicate<? super T> p_458142_,
      Predicate<? super T> p_456603_,
      Predicate<? super T> p_456932_,
      Predicate<? super T> p_453934_
   ) {
      return p_452244_ -> p_458851_.test(p_452244_)
         || p_458142_.test(p_452244_)
         || p_456603_.test(p_452244_)
         || p_456932_.test(p_452244_)
         || p_453934_.test(p_452244_);
   }

   @SafeVarargs
   public static <T> Predicate<T> anyOf(Predicate<? super T>... p_458772_) {
      return p_455169_ -> {
         for (Predicate<? super T> predicate : p_458772_) {
            if (predicate.test(p_455169_)) {
               return true;
            }
         }

         return false;
      };
   }

   public static <T> Predicate<T> anyOf(List<? extends Predicate<? super T>> p_452843_) {
      return switch (p_452843_.size()) {
         case 0 -> anyOf();
         case 1 -> anyOf((Predicate<? super T>)p_452843_.get(0));
         case 2 -> anyOf((Predicate<? super T>)p_452843_.get(0), (Predicate<? super T>)p_452843_.get(1));
         case 3 -> anyOf((Predicate<? super T>)p_452843_.get(0), (Predicate<? super T>)p_452843_.get(1), (Predicate<? super T>)p_452843_.get(2));
         case 4 -> anyOf(
            (Predicate<? super T>)p_452843_.get(0),
            (Predicate<? super T>)p_452843_.get(1),
            (Predicate<? super T>)p_452843_.get(2),
            (Predicate<? super T>)p_452843_.get(3)
         );
         case 5 -> anyOf(
            (Predicate<? super T>)p_452843_.get(0),
            (Predicate<? super T>)p_452843_.get(1),
            (Predicate<? super T>)p_452843_.get(2),
            (Predicate<? super T>)p_452843_.get(3),
            (Predicate<? super T>)p_452843_.get(4)
         );
         default -> {
            Predicate<? super T>[] predicate = p_452843_.toArray(Predicate[]::new);
            yield anyOf(predicate);
         }
      };
   }

   public static <T> boolean isSymmetrical(int p_451830_, int p_452583_, List<T> p_459170_) {
      if (p_451830_ == 1) {
         return true;
      }

      int i = p_451830_ / 2;

      for (int j = 0; j < p_452583_; j++) {
         for (int k = 0; k < i; k++) {
            int l = p_451830_ - 1 - k;
            T t = p_459170_.get(k + j * p_451830_);
            T t1 = p_459170_.get(l + j * p_451830_);
            if (!t.equals(t1)) {
               return false;
            }
         }
      }

      return true;
   }

   public static int growByHalf(int p_451690_, int p_454011_) {
      return (int)Math.max(Math.min((long)p_451690_ + (p_451690_ >> 1), 2147483639L), p_454011_);
   }

   @SuppressForbidden(reason = "Intentional use of default locale for user-visible date")
   public static DateTimeFormatter localizedDateFormatter(FormatStyle p_454931_) {
      return DateTimeFormatter.ofLocalizedDateTime(p_454931_);
   }

   public static Util.OS getPlatform() {
      String s = System.getProperty("os.name").toLowerCase(Locale.ROOT);
      if (s.contains("win")) {
         return Util.OS.WINDOWS;
      } else if (s.contains("mac")) {
         return Util.OS.OSX;
      } else if (s.contains("solaris")) {
         return Util.OS.SOLARIS;
      } else if (s.contains("sunos")) {
         return Util.OS.SOLARIS;
      } else if (s.contains("linux")) {
         return Util.OS.LINUX;
      } else {
         return s.contains("unix") ? Util.OS.LINUX : Util.OS.UNKNOWN;
      }
   }

   public static boolean isAarch64() {
      String s = System.getProperty("os.arch").toLowerCase(Locale.ROOT);
      return s.equals("aarch64");
   }

   public static URI parseAndValidateUntrustedUri(String p_460748_) throws URISyntaxException {
      URI uri = new URI(p_460748_);
      String s = uri.getScheme();
      if (s == null) {
         throw new URISyntaxException(p_460748_, "Missing protocol in URI: " + p_460748_);
      } else {
         String s1 = s.toLowerCase(Locale.ROOT);
         if (!ALLOWED_UNTRUSTED_LINK_PROTOCOLS.contains(s1)) {
            throw new URISyntaxException(p_460748_, "Unsupported protocol in URI: " + p_460748_);
         } else {
            return uri;
         }
      }
   }

   public static <T> T findNextInIterable(Iterable<T> p_453662_, @Nullable T p_456366_) {
      Iterator<T> iterator = p_453662_.iterator();
      T t = iterator.next();
      if (p_456366_ != null) {
         T t1 = t;

         while (t1 != p_456366_) {
            if (iterator.hasNext()) {
               t1 = iterator.next();
            }
         }

         if (iterator.hasNext()) {
            return iterator.next();
         }
      }

      return t;
   }

   public static <T> T findPreviousInIterable(Iterable<T> p_453330_, @Nullable T p_458682_) {
      Iterator<T> iterator = p_453330_.iterator();
      T t = null;

      while (iterator.hasNext()) {
         T t1 = iterator.next();
         if (t1 == p_458682_) {
            if (t == null) {
               t = (T)(iterator.hasNext() ? Iterators.getLast(iterator) : p_458682_);
            }
            break;
         }

         t = t1;
      }

      return t;
   }

   public static <T> T make(Supplier<T> p_454184_) {
      return p_454184_.get();
   }

   public static <T> T make(T p_453624_, Consumer<? super T> p_460189_) {
      p_460189_.accept(p_453624_);
      return p_453624_;
   }

   public static <K extends Enum<K>, V> Map<K, V> makeEnumMap(Class<K> p_451083_, Function<K, V> p_450941_) {
      EnumMap<K, V> enummap = new EnumMap<>(p_451083_);

      for (K k : (Enum[])p_451083_.getEnumConstants()) {
         enummap.put(k, p_450941_.apply(k));
      }

      return enummap;
   }

   public static <K, V1, V2> Map<K, V2> mapValues(Map<K, V1> p_458645_, Function<? super V1, V2> p_457319_) {
      return p_458645_.entrySet().stream().collect(Collectors.toMap(Entry::getKey, p_456831_ -> p_457319_.apply(p_456831_.getValue())));
   }

   public static <K, V1, V2> Map<K, V2> mapValuesLazy(Map<K, V1> p_457888_, com.google.common.base.Function<V1, V2> p_450746_) {
      return Maps.transformValues(p_457888_, p_450746_);
   }

   public static <V> CompletableFuture<List<V>> sequence(List<? extends CompletableFuture<V>> p_457744_) {
      if (p_457744_.isEmpty()) {
         return CompletableFuture.completedFuture(List.of());
      }

      if (p_457744_.size() == 1) {
         return p_457744_.getFirst().thenApply(ObjectLists::singleton);
      }

      CompletableFuture<Void> completablefuture = CompletableFuture.allOf(p_457744_.toArray(new CompletableFuture[0]));
      return completablefuture.thenApply(p_460444_ -> p_457744_.stream().map(CompletableFuture::join).toList());
   }

   public static <V> CompletableFuture<List<V>> sequenceFailFast(List<? extends CompletableFuture<? extends V>> p_457938_) {
      CompletableFuture<List<V>> completablefuture = new CompletableFuture<>();
      return fallibleSequence(p_457938_, completablefuture::completeExceptionally).applyToEither(completablefuture, Function.identity());
   }

   public static <V> CompletableFuture<List<V>> sequenceFailFastAndCancel(List<? extends CompletableFuture<? extends V>> p_451859_) {
      CompletableFuture<List<V>> completablefuture = new CompletableFuture<>();
      return fallibleSequence(p_451859_, p_454158_ -> {
         if (completablefuture.completeExceptionally(p_454158_)) {
            for (CompletableFuture<? extends V> completablefuture1 : p_451859_) {
               completablefuture1.cancel(true);
            }
         }
      }).applyToEither(completablefuture, Function.identity());
   }

   private static <V> CompletableFuture<List<V>> fallibleSequence(List<? extends CompletableFuture<? extends V>> p_453437_, Consumer<Throwable> p_454507_) {
      ObjectArrayList<V> objectarraylist = new ObjectArrayList();
      objectarraylist.size(p_453437_.size());
      CompletableFuture<?>[] completablefuture = new CompletableFuture[p_453437_.size()];

      for (int i = 0; i < p_453437_.size(); i++) {
         int j = i;
         completablefuture[i] = p_453437_.get(i).whenComplete((p_454806_, p_451823_) -> {
            if (p_451823_ != null) {
               p_454507_.accept(p_451823_);
            } else {
               objectarraylist.set(j, p_454806_);
            }
         });
      }

      return CompletableFuture.allOf(completablefuture).thenApply(p_454949_ -> objectarraylist);
   }

   public static <T> Optional<T> ifElse(Optional<T> p_450337_, Consumer<T> p_456863_, Runnable p_460016_) {
      if (p_450337_.isPresent()) {
         p_456863_.accept(p_450337_.get());
      } else {
         p_460016_.run();
      }

      return p_450337_;
   }

   public static <T> Supplier<T> name(final Supplier<T> p_457987_, Supplier<String> p_452190_) {
      if (SharedConstants.DEBUG_NAMED_RUNNABLES) {
         final String s = p_452190_.get();
         return new Supplier<T>() {
            @Override
            public T get() {
               return p_457987_.get();
            }

            @Override
            public String toString() {
               return s;
            }
         };
      } else {
         return p_457987_;
      }
   }

   public static Runnable name(final Runnable p_455186_, Supplier<String> p_451819_) {
      if (SharedConstants.DEBUG_NAMED_RUNNABLES) {
         final String s = p_451819_.get();
         return new Runnable() {
            @Override
            public void run() {
               p_455186_.run();
            }

            @Override
            public String toString() {
               return s;
            }
         };
      } else {
         return p_455186_;
      }
   }

   public static void logAndPauseIfInIde(String p_450728_) {
      LOGGER.error(p_450728_);
      if (SharedConstants.IS_RUNNING_IN_IDE) {
         doPause(p_450728_);
      }
   }

   public static void logAndPauseIfInIde(String p_456282_, Throwable p_453319_) {
      LOGGER.error(p_456282_, p_453319_);
      if (SharedConstants.IS_RUNNING_IN_IDE) {
         doPause(p_456282_);
      }
   }

   public static <T extends Throwable> T pauseInIde(T p_453939_) {
      if (SharedConstants.IS_RUNNING_IN_IDE) {
         LOGGER.error("Trying to throw a fatal exception, pausing in IDE", p_453939_);
         doPause(p_453939_.getMessage());
      }

      return p_453939_;
   }

   public static void setPause(Consumer<String> p_454914_) {
      thePauser = p_454914_;
   }

   private static void doPause(String p_455163_) {
      Instant instant = Instant.now();
      LOGGER.warn("Did you remember to set a breakpoint here?");
      boolean flag = Duration.between(instant, Instant.now()).toMillis() > 500L;
      if (!flag) {
         thePauser.accept(p_455163_);
      }
   }

   public static String describeError(Throwable p_452068_) {
      if (p_452068_.getCause() != null) {
         return describeError(p_452068_.getCause());
      } else {
         return p_452068_.getMessage() != null ? p_452068_.getMessage() : p_452068_.toString();
      }
   }

   public static <T> T getRandom(T[] p_456745_, RandomSource p_460847_) {
      return p_456745_[p_460847_.nextInt(p_456745_.length)];
   }

   public static int getRandom(int[] p_457211_, RandomSource p_451763_) {
      return p_457211_[p_451763_.nextInt(p_457211_.length)];
   }

   public static <T> T getRandom(List<T> p_459817_, RandomSource p_451874_) {
      return p_459817_.get(p_451874_.nextInt(p_459817_.size()));
   }

   public static <T> Optional<T> getRandomSafe(List<T> p_457599_, RandomSource p_456380_) {
      return p_457599_.isEmpty() ? Optional.empty() : Optional.of(getRandom(p_457599_, p_456380_));
   }

   private static BooleanSupplier createRenamer(final Path p_458038_, final Path p_450200_) {
      return new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            try {
               Files.move(p_458038_, p_450200_);
               return true;
            } catch (IOException ioexception) {
               Util.LOGGER.error("Failed to rename", ioexception);
               return false;
            }
         }

         @Override
         public String toString() {
            return "rename " + p_458038_ + " to " + p_450200_;
         }
      };
   }

   private static BooleanSupplier createDeleter(final Path p_452747_) {
      return new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            try {
               Files.deleteIfExists(p_452747_);
               return true;
            } catch (IOException ioexception) {
               Util.LOGGER.warn("Failed to delete", ioexception);
               return false;
            }
         }

         @Override
         public String toString() {
            return "delete old " + p_452747_;
         }
      };
   }

   private static BooleanSupplier createFileDeletedCheck(final Path p_450326_) {
      return new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return !Files.exists(p_450326_);
         }

         @Override
         public String toString() {
            return "verify that " + p_450326_ + " is deleted";
         }
      };
   }

   private static BooleanSupplier createFileCreatedCheck(final Path p_452555_) {
      return new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return Files.isRegularFile(p_452555_);
         }

         @Override
         public String toString() {
            return "verify that " + p_452555_ + " is present";
         }
      };
   }

   private static boolean executeInSequence(BooleanSupplier... p_452780_) {
      for (BooleanSupplier booleansupplier : p_452780_) {
         if (!booleansupplier.getAsBoolean()) {
            LOGGER.warn("Failed to execute {}", booleansupplier);
            return false;
         }
      }

      return true;
   }

   private static boolean runWithRetries(int p_460382_, String p_457314_, BooleanSupplier... p_450183_) {
      for (int i = 0; i < p_460382_; i++) {
         if (executeInSequence(p_450183_)) {
            return true;
         }

         LOGGER.error("Failed to {}, retrying {}/{}", new Object[]{p_457314_, i, p_460382_});
      }

      LOGGER.error("Failed to {}, aborting, progress might be lost", p_457314_);
      return false;
   }

   public static void safeReplaceFile(Path p_460239_, Path p_457728_, Path p_453877_) {
      safeReplaceOrMoveFile(p_460239_, p_457728_, p_453877_, false);
   }

   public static boolean safeReplaceOrMoveFile(Path p_457996_, Path p_458052_, Path p_456075_, boolean p_454950_) {
      if (Files.exists(p_457996_)
         && !runWithRetries(10, "create backup " + p_456075_, createDeleter(p_456075_), createRenamer(p_457996_, p_456075_), createFileCreatedCheck(p_456075_))
         )
       {
         return false;
      } else if (!runWithRetries(10, "remove old " + p_457996_, createDeleter(p_457996_), createFileDeletedCheck(p_457996_))) {
         return false;
      } else if (!runWithRetries(10, "replace " + p_457996_ + " with " + p_458052_, createRenamer(p_458052_, p_457996_), createFileCreatedCheck(p_457996_))
         && !p_454950_) {
         runWithRetries(10, "restore " + p_457996_ + " from " + p_456075_, createRenamer(p_456075_, p_457996_), createFileCreatedCheck(p_457996_));
         return false;
      } else {
         return true;
      }
   }

   public static int offsetByCodepoints(String p_461058_, int p_457317_, int p_451873_) {
      int i = p_461058_.length();
      if (p_451873_ >= 0) {
         for (int j = 0; p_457317_ < i && j < p_451873_; j++) {
            if (Character.isHighSurrogate(p_461058_.charAt(p_457317_++)) && p_457317_ < i && Character.isLowSurrogate(p_461058_.charAt(p_457317_))) {
               p_457317_++;
            }
         }
      } else {
         for (int k = p_451873_; p_457317_ > 0 && k < 0; k++) {
            p_457317_--;
            if (Character.isLowSurrogate(p_461058_.charAt(p_457317_)) && p_457317_ > 0 && Character.isHighSurrogate(p_461058_.charAt(p_457317_ - 1))) {
               p_457317_--;
            }
         }
      }

      return p_457317_;
   }

   public static Consumer<String> prefix(String p_451533_, Consumer<String> p_458100_) {
      return p_453355_ -> p_458100_.accept(p_451533_ + p_453355_);
   }

   public static DataResult<int[]> fixedSize(IntStream p_453929_, int p_457720_) {
      int[] aint = p_453929_.limit(p_457720_ + 1).toArray();
      if (aint.length != p_457720_) {
         Supplier<String> supplier = () -> "Input is not a list of " + p_457720_ + " ints";
         return aint.length >= p_457720_ ? DataResult.error(supplier, Arrays.copyOf(aint, p_457720_)) : DataResult.error(supplier);
      } else {
         return DataResult.success(aint);
      }
   }

   public static DataResult<long[]> fixedSize(LongStream p_451891_, int p_450260_) {
      long[] along = p_451891_.limit(p_450260_ + 1).toArray();
      if (along.length != p_450260_) {
         Supplier<String> supplier = () -> "Input is not a list of " + p_450260_ + " longs";
         return along.length >= p_450260_ ? DataResult.error(supplier, Arrays.copyOf(along, p_450260_)) : DataResult.error(supplier);
      } else {
         return DataResult.success(along);
      }
   }

   public static <T> DataResult<List<T>> fixedSize(List<T> p_453371_, int p_451062_) {
      if (p_453371_.size() != p_451062_) {
         Supplier<String> supplier = () -> "Input is not a list of " + p_451062_ + " elements";
         return p_453371_.size() >= p_451062_ ? DataResult.error(supplier, p_453371_.subList(0, p_451062_)) : DataResult.error(supplier);
      } else {
         return DataResult.success(p_453371_);
      }
   }

   public static void startTimerHackThread() {
      Thread thread = new Thread("Timer hack thread") {
         @Override
         public void run() {
            while (true) {
               try {
                  Thread.sleep(2147483647L);
               } catch (InterruptedException interruptedexception) {
                  Util.LOGGER.warn("Timer hack thread interrupted, that really should not happen");
                  return;
               }
            }
         }
      };
      thread.setDaemon(true);
      thread.setUncaughtExceptionHandler(new DefaultUncaughtExceptionHandler(LOGGER));
      thread.start();
   }

   public static void copyBetweenDirs(Path p_458211_, Path p_452717_, Path p_455751_) throws IOException {
      Path path = p_458211_.relativize(p_455751_);
      Path path1 = p_452717_.resolve(path);
      Files.copy(p_455751_, path1);
   }

   public static String sanitizeName(String p_453785_, CharPredicate p_454706_) {
      return p_453785_.toLowerCase(Locale.ROOT)
         .chars()
         .mapToObj(p_453810_ -> p_454706_.test((char)p_453810_) ? Character.toString((char)p_453810_) : "_")
         .collect(Collectors.joining());
   }

   public static <K, V> SingleKeyCache<K, V> singleKeyCache(Function<K, V> p_453833_) {
      return new SingleKeyCache<>(p_453833_);
   }

   public static <T, R> Function<T, R> memoize(final Function<T, R> p_453423_) {
      return new Function<T, R>() {
         private final Map<T, R> cache = new ConcurrentHashMap<>();

         @Override
         public R apply(T p_454211_) {
            return this.cache.computeIfAbsent(p_454211_, p_453423_);
         }

         @Override
         public String toString() {
            return "memoize/1[function=" + p_453423_ + ", size=" + this.cache.size() + "]";
         }
      };
   }

   public static <T, U, R> BiFunction<T, U, R> memoize(final BiFunction<T, U, R> p_458223_) {
      return new BiFunction<T, U, R>() {
         private final Map<Pair<T, U>, R> cache = new ConcurrentHashMap<>();

         @Override
         public R apply(T p_456202_, U p_454939_) {
            return this.cache.computeIfAbsent(Pair.of(p_456202_, p_454939_), p_459666_ -> p_458223_.apply((T)p_459666_.getFirst(), (U)p_459666_.getSecond()));
         }

         @Override
         public String toString() {
            return "memoize/2[function=" + p_458223_ + ", size=" + this.cache.size() + "]";
         }
      };
   }

   public static <T> List<T> toShuffledList(Stream<T> p_458187_, RandomSource p_460883_) {
      ObjectArrayList<T> objectarraylist = p_458187_.collect(ObjectArrayList.toList());
      shuffle(objectarraylist, p_460883_);
      return objectarraylist;
   }

   public static IntArrayList toShuffledList(IntStream p_452049_, RandomSource p_453443_) {
      IntArrayList intarraylist = IntArrayList.wrap(p_452049_.toArray());
      int i = intarraylist.size();

      for (int j = i; j > 1; j--) {
         int k = p_453443_.nextInt(j);
         intarraylist.set(j - 1, intarraylist.set(k, intarraylist.getInt(j - 1)));
      }

      return intarraylist;
   }

   public static <T> List<T> shuffledCopy(T[] p_457399_, RandomSource p_456239_) {
      ObjectArrayList<T> objectarraylist = new ObjectArrayList(p_457399_);
      shuffle(objectarraylist, p_456239_);
      return objectarraylist;
   }

   public static <T> List<T> shuffledCopy(ObjectArrayList<T> p_450699_, RandomSource p_457748_) {
      ObjectArrayList<T> objectarraylist = new ObjectArrayList(p_450699_);
      shuffle(objectarraylist, p_457748_);
      return objectarraylist;
   }

   public static <T> void shuffle(List<T> p_453692_, RandomSource p_451596_) {
      int i = p_453692_.size();

      for (int j = i; j > 1; j--) {
         int k = p_451596_.nextInt(j);
         p_453692_.set(j - 1, p_453692_.set(k, p_453692_.get(j - 1)));
      }
   }

   public static <T> CompletableFuture<T> blockUntilDone(Function<Executor, CompletableFuture<T>> p_456655_) {
      return blockUntilDone(p_456655_, CompletableFuture::isDone);
   }

   public static <T> T blockUntilDone(Function<Executor, T> p_459519_, Predicate<T> p_451994_) {
      BlockingQueue<Runnable> blockingqueue = new LinkedBlockingQueue<>();
      T t = p_459519_.apply(blockingqueue::add);

      while (!p_451994_.test(t)) {
         try {
            Runnable runnable = blockingqueue.poll(100L, TimeUnit.MILLISECONDS);
            if (runnable != null) {
               runnable.run();
            }
         } catch (InterruptedException interruptedexception) {
            LOGGER.warn("Interrupted wait");
            break;
         }
      }

      int i = blockingqueue.size();
      if (i > 0) {
         LOGGER.warn("Tasks left in queue: {}", i);
      }

      return t;
   }

   public static <T> ToIntFunction<T> createIndexLookup(List<T> p_456122_) {
      int i = p_456122_.size();
      if (i < 8) {
         return p_456122_::indexOf;
      }

      Object2IntMap<T> object2intmap = new Object2IntOpenHashMap(i);
      object2intmap.defaultReturnValue(-1);

      for (int j = 0; j < i; j++) {
         object2intmap.put(p_456122_.get(j), j);
      }

      return object2intmap;
   }

   public static <T> ToIntFunction<T> createIndexIdentityLookup(List<T> p_451785_) {
      int i = p_451785_.size();
      if (i < 8) {
         ReferenceList<T> referencelist = new ReferenceImmutableList(p_451785_);
         return referencelist::indexOf;
      }

      Reference2IntMap<T> reference2intmap = new Reference2IntOpenHashMap(i);
      reference2intmap.defaultReturnValue(-1);

      for (int j = 0; j < i; j++) {
         reference2intmap.put(p_451785_.get(j), j);
      }

      return reference2intmap;
   }

   public static <A, B> Typed<B> writeAndReadTypedOrThrow(Typed<A> p_459616_, Type<B> p_460842_, UnaryOperator<Dynamic<?>> p_450843_) {
      Dynamic<?> dynamic = (Dynamic<?>)p_459616_.write().getOrThrow();
      return readTypedOrThrow(p_460842_, p_450843_.apply(dynamic), true);
   }

   public static <T> Typed<T> readTypedOrThrow(Type<T> p_454183_, Dynamic<?> p_451467_) {
      return readTypedOrThrow(p_454183_, p_451467_, false);
   }

   public static <T> Typed<T> readTypedOrThrow(Type<T> p_460265_, Dynamic<?> p_459602_, boolean p_451868_) {
      DataResult<Typed<T>> dataresult = p_460265_.readTyped(p_459602_).map(Pair::getFirst);

      try {
         return p_451868_ ? (Typed)dataresult.getPartialOrThrow(IllegalStateException::new) : (Typed)dataresult.getOrThrow(IllegalStateException::new);
      } catch (IllegalStateException illegalstateexception) {
         CrashReport crashreport = CrashReport.forThrowable(illegalstateexception, "Reading type");
         CrashReportCategory crashreportcategory = crashreport.addCategory("Info");
         crashreportcategory.setDetail("Data", p_459602_);
         crashreportcategory.setDetail("Type", p_460265_);
         throw new ReportedException(crashreport);
      }
   }

   public static <T> List<T> copyAndAdd(List<T> p_457738_, T p_455569_) {
      return ImmutableList.builderWithExpectedSize(p_457738_.size() + 1).addAll(p_457738_).add(p_455569_).build();
   }

   public static <T> List<T> copyAndAdd(T p_452732_, List<T> p_450851_) {
      return ImmutableList.builderWithExpectedSize(p_450851_.size() + 1).add(p_452732_).addAll(p_450851_).build();
   }

   public static <K, V> Map<K, V> copyAndPut(Map<K, V> p_455692_, K p_458646_, V p_457520_) {
      return ImmutableMap.builderWithExpectedSize(p_455692_.size() + 1).putAll(p_455692_).put(p_458646_, p_457520_).buildKeepingLast();
   }

   public enum OS {
      LINUX("linux"),
      SOLARIS("solaris"),
      WINDOWS("windows") {
         @Override
         protected String[] getOpenUriArguments(URI p_450756_) {
            return new String[]{"rundll32", "url.dll,FileProtocolHandler", p_450756_.toString()};
         }
      },
      OSX("mac") {
         @Override
         protected String[] getOpenUriArguments(URI p_454749_) {
            return new String[]{"open", p_454749_.toString()};
         }
      },
      UNKNOWN("unknown");

      private final String telemetryName;

      OS(final String p_455489_) {
         this.telemetryName = p_455489_;
      }

      public void openUri(URI p_452323_) {
         try {
            Process process = Runtime.getRuntime().exec(this.getOpenUriArguments(p_452323_));
            process.getInputStream().close();
            process.getErrorStream().close();
            process.getOutputStream().close();
         } catch (IOException ioexception) {
            Util.LOGGER.error("Couldn't open location '{}'", p_452323_, ioexception);
         }
      }

      public void openFile(File p_459471_) {
         this.openUri(p_459471_.toURI());
      }

      public void openPath(Path p_458178_) {
         this.openUri(p_458178_.toUri());
      }

      protected String[] getOpenUriArguments(URI p_450706_) {
         String s = p_450706_.toString();
         if ("file".equals(p_450706_.getScheme())) {
            s = s.replace("file:", "file://");
         }

         return new String[]{"xdg-open", s};
      }

      public void openUri(String p_451210_) {
         try {
            this.openUri(new URI(p_451210_));
         } catch (IllegalArgumentException | URISyntaxException urisyntaxexception) {
            Util.LOGGER.error("Couldn't open uri '{}'", p_451210_, urisyntaxexception);
         }
      }

      public String telemetryName() {
         return this.telemetryName;
      }
   }
}
