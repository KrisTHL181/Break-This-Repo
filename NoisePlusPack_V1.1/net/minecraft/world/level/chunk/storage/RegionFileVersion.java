package net.minecraft.world.level.chunk.storage;

import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.InflaterInputStream;
import net.jpountz.lz4.LZ4BlockInputStream;
import net.jpountz.lz4.LZ4BlockOutputStream;
import net.minecraft.util.FastBufferedInputStream;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class RegionFileVersion {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Int2ObjectMap<RegionFileVersion> VERSIONS = new Int2ObjectOpenHashMap();
   private static final Object2ObjectMap<String, RegionFileVersion> VERSIONS_BY_NAME = new Object2ObjectOpenHashMap();
   public static final RegionFileVersion VERSION_GZIP = register(
      new RegionFileVersion(
         1, null, p_63767_ -> new FastBufferedInputStream(new GZIPInputStream(p_63767_)), p_63769_ -> new BufferedOutputStream(new GZIPOutputStream(p_63769_))
      )
   );
   public static final RegionFileVersion VERSION_DEFLATE = register(
      new RegionFileVersion(
         2,
         "deflate",
         p_196964_ -> new FastBufferedInputStream(new InflaterInputStream(p_196964_)),
         p_196966_ -> new BufferedOutputStream(new DeflaterOutputStream(p_196966_))
      )
   );
   public static final RegionFileVersion VERSION_NONE = register(new RegionFileVersion(3, "none", FastBufferedInputStream::new, BufferedOutputStream::new));
   public static final RegionFileVersion VERSION_LZ4 = register(
      new RegionFileVersion(
         4,
         "lz4",
         p_327422_ -> new FastBufferedInputStream(new LZ4BlockInputStream(p_327422_)),
         p_327421_ -> new BufferedOutputStream(new LZ4BlockOutputStream(p_327421_))
      )
   );
   public static final RegionFileVersion VERSION_CUSTOM = register(new RegionFileVersion(127, null, p_327423_ -> {
      throw new UnsupportedOperationException();
   }, p_327424_ -> {
      throw new UnsupportedOperationException();
   }));
   public static final RegionFileVersion DEFAULT = VERSION_DEFLATE;
   private static volatile RegionFileVersion selected = DEFAULT;
   private final int id;
   private final @Nullable String optionName;
   private final RegionFileVersion.StreamWrapper<InputStream> inputWrapper;
   private final RegionFileVersion.StreamWrapper<OutputStream> outputWrapper;

   private RegionFileVersion(
      int p_63752_, @Nullable String p_336103_, RegionFileVersion.StreamWrapper<InputStream> p_63753_, RegionFileVersion.StreamWrapper<OutputStream> p_63754_
   ) {
      this.id = p_63752_;
      this.optionName = p_336103_;
      this.inputWrapper = p_63753_;
      this.outputWrapper = p_63754_;
   }

   private static RegionFileVersion register(RegionFileVersion p_63759_) {
      VERSIONS.put(p_63759_.id, p_63759_);
      if (p_63759_.optionName != null) {
         VERSIONS_BY_NAME.put(p_63759_.optionName, p_63759_);
      }

      return p_63759_;
   }

   public static @Nullable RegionFileVersion fromId(int p_63757_) {
      return (RegionFileVersion)VERSIONS.get(p_63757_);
   }

   public static void configure(String p_335730_) {
      RegionFileVersion regionfileversion = (RegionFileVersion)VERSIONS_BY_NAME.get(p_335730_);
      if (regionfileversion != null) {
         selected = regionfileversion;
      } else {
         LOGGER.error(
            "Invalid `region-file-compression` value `{}` in server.properties. Please use one of: {}", p_335730_, String.join(", ", VERSIONS_BY_NAME.keySet())
         );
      }
   }

   public static RegionFileVersion getSelected() {
      return selected;
   }

   public static boolean isValidVersion(int p_63765_) {
      return VERSIONS.containsKey(p_63765_);
   }

   public int getId() {
      return this.id;
   }

   public OutputStream wrap(OutputStream p_63763_) throws IOException {
      return this.outputWrapper.wrap(p_63763_);
   }

   public InputStream wrap(InputStream p_63761_) throws IOException {
      return this.inputWrapper.wrap(p_63761_);
   }

   @FunctionalInterface
   interface StreamWrapper<O> {
      O wrap(O var1) throws IOException;
   }
}
