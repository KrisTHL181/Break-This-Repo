package com.mojang.realmsclient.dto;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import net.minecraft.resources.Identifier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public enum ServiceQuality {
   GREAT(1, "icon/ping_5"),
   GOOD(2, "icon/ping_4"),
   OKAY(3, "icon/ping_3"),
   POOR(4, "icon/ping_2"),
   UNKNOWN(5, "icon/ping_unknown");

   final int value;
   private final Identifier icon;

   ServiceQuality(final int p_409518_, final String p_410427_) {
      this.value = p_409518_;
      this.icon = Identifier.withDefaultNamespace(p_410427_);
   }

   public static @Nullable ServiceQuality byValue(int p_408869_) {
      for (ServiceQuality servicequality : values()) {
         if (servicequality.getValue() == p_408869_) {
            return servicequality;
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

   @OnlyIn(Dist.CLIENT)
   public static class RealmsServiceQualityJsonAdapter extends TypeAdapter<ServiceQuality> {
      private static final Logger LOGGER = LogUtils.getLogger();

      public void write(JsonWriter p_406695_, ServiceQuality p_407766_) throws IOException {
         p_406695_.value(p_407766_.value);
      }

      public ServiceQuality read(JsonReader p_407194_) throws IOException {
         int i = p_407194_.nextInt();
         ServiceQuality servicequality = ServiceQuality.byValue(i);
         if (servicequality == null) {
            LOGGER.warn("Unsupported ServiceQuality {}", i);
            return ServiceQuality.UNKNOWN;
         } else {
            return servicequality;
         }
      }
   }
}
