package com.mojang.realmsclient.dto;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public enum ServiceQuality {
   GREAT(1, "icon/ping_5"),
   GOOD(2, "icon/ping_4"),
   OKAY(3, "icon/ping_3"),
   POOR(4, "icon/ping_2"),
   UNKNOWN(5, "icon/ping_unknown");

   private final int value;
   private final Identifier icon;

   ServiceQuality(final int value, final String iconPath) {
      this.value = value;
      this.icon = Identifier.withDefaultNamespace(iconPath);
   }

   public static @Nullable ServiceQuality byValue(final int value) {
      for (ServiceQuality quality : values()) {
         if (quality.getValue() == value) {
            return quality;
         }
      }

      return null;
   }

   public int getValue() {
      return this.value;
   }

   public Identifier getIcon() {
      return this.icon;
   }

   public static class RealmsServiceQualityJsonAdapter extends TypeAdapter<ServiceQuality> {
      private static final Logger LOGGER = LogUtils.getLogger();

      public void write(final JsonWriter jsonWriter, final ServiceQuality quality) throws IOException {
         jsonWriter.value(quality.value);
      }

      public ServiceQuality read(final JsonReader jsonReader) throws IOException {
         int value = jsonReader.nextInt();
         ServiceQuality quality = ServiceQuality.byValue(value);
         if (quality == null) {
            LOGGER.warn("Unsupported ServiceQuality {}", value);
            return ServiceQuality.UNKNOWN;
         } else {
            return quality;
         }
      }
   }
}
