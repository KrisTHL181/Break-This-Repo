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
      new RegionFileVersion(1, null, in -> new FastBufferedInputStream(new GZIPInputStream(in)), out -> new BufferedOutputStream(new GZIPOutputStream(out)))
   );
   public static final RegionFileVersion VERSION_DEFLATE = register(
      new RegionFileVersion(
         2, "deflate", in -> new FastBufferedInputStream(new InflaterInputStream(in)), out -> new BufferedOutputStream(new DeflaterOutputStream(out))
      )
   );
   public static final RegionFileVersion VERSION_NONE = register(new RegionFileVersion(3, "none", FastBufferedInputStream::new, BufferedOutputStream::new));
   public static final RegionFileVersion VERSION_LZ4 = register(
      new RegionFileVersion(
         4, "lz4", in -> new FastBufferedInputStream(new LZ4BlockInputStream(in)), out -> new BufferedOutputStream(new LZ4BlockOutputStream(out))
      )
   );
   public static final RegionFileVersion VERSION_CUSTOM = register(new RegionFileVersion(127, null, in -> {
      throw new UnsupportedOperationException();
   }, out -> {
      throw new UnsupportedOperationException();
   }));
   public static final RegionFileVersion DEFAULT = VERSION_DEFLATE;
   private static volatile RegionFileVersion selected = DEFAULT;
   private final int id;
   private final @Nullable String optionName;
   private final RegionFileVersion.StreamWrapper<InputStream> inputWrapper;
   private final RegionFileVersion.StreamWrapper<OutputStream> outputWrapper;

   private RegionFileVersion(
      final int id,
      final @Nullable String optionName,
      final RegionFileVersion.StreamWrapper<InputStream> inputWrapper,
      final RegionFileVersion.StreamWrapper<OutputStream> outputWrapper
   ) {
      this.id = id;
      this.optionName = optionName;
      this.inputWrapper = inputWrapper;
      this.outputWrapper = outputWrapper;
   }

   private static RegionFileVersion register(final RegionFileVersion version) {
      VERSIONS.put(version.id, version);
      if (version.optionName != null) {
         VERSIONS_BY_NAME.put(version.optionName, version);
      }

      return version;
   }

   public static @Nullable RegionFileVersion fromId(final int id) {
      return (RegionFileVersion)VERSIONS.get(id);
   }

   public static void configure(final String optionName) {
      RegionFileVersion version = (RegionFileVersion)VERSIONS_BY_NAME.get(optionName);
      if (version != null) {
         selected = version;
      } else {
         LOGGER.error(
            "Invalid `region-file-compression` value `{}` in server.properties. Please use one of: {}",
            optionName,
            String.join(", ", VERSIONS_BY_NAME.keySet())
         );
      }
   }

   public static RegionFileVersion getSelected() {
      return selected;
   }

   public static boolean isValidVersion(final int version) {
      return VERSIONS.containsKey(version);
   }

   public int getId() {
      return this.id;
   }

   public OutputStream wrap(final OutputStream is) throws IOException {
      return this.outputWrapper.wrap(is);
   }

   public InputStream wrap(final InputStream is) throws IOException {
      return this.inputWrapper.wrap(is);
   }

   @FunctionalInterface
   private interface StreamWrapper<O> {
      O wrap(O stream) throws IOException;
   }
}
