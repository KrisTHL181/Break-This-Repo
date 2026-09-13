package net.minecraft.server.packs;

import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.util.GsonHelper;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public abstract class AbstractPackResources implements PackResources {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final PackLocationInfo location;

   protected AbstractPackResources(PackLocationInfo p_332936_) {
      this.location = p_332936_;
   }

   @Override
   public <T> @Nullable T getMetadataSection(MetadataSectionType<T> p_375504_) throws IOException {
      IoSupplier<InputStream> iosupplier = this.getRootResource("pack.mcmeta");
      if (iosupplier == null) {
         return null;
      }

      try (InputStream inputstream = iosupplier.get()) {
         return getMetadataFromStream(p_375504_, inputstream, this.location);
      }
   }

   public static <T> @Nullable T getMetadataFromStream(MetadataSectionType<T> p_375667_, InputStream p_10216_, PackLocationInfo p_424288_) {
      JsonObject jsonobject;
      try (BufferedReader bufferedreader = new BufferedReader(new InputStreamReader(p_10216_, StandardCharsets.UTF_8))) {
         jsonobject = GsonHelper.parse(bufferedreader);
      } catch (Exception exception) {
         LOGGER.error("Couldn't load {} {} metadata: {}", new Object[]{p_424288_.id(), p_375667_.name(), exception.getMessage()});
         return null;
      }

      return (T)(!jsonobject.has(p_375667_.name())
         ? null
         : p_375667_.codec()
            .parse(JsonOps.INSTANCE, jsonobject.get(p_375667_.name()))
            .ifError(p_421498_ -> LOGGER.error("Couldn't load {} {} metadata: {}", new Object[]{p_424288_.id(), p_375667_.name(), p_421498_.message()}))
            .result()
            .orElse(null));
   }

   @Override
   public PackLocationInfo location() {
      return this.location;
   }
}
