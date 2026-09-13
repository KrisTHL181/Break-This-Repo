package com.mojang.realmsclient.dto;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public enum RealmsRegion {
   AUSTRALIA_EAST("AustraliaEast", "realms.configuration.region.australia_east"),
   AUSTRALIA_SOUTHEAST("AustraliaSoutheast", "realms.configuration.region.australia_southeast"),
   BRAZIL_SOUTH("BrazilSouth", "realms.configuration.region.brazil_south"),
   CENTRAL_INDIA("CentralIndia", "realms.configuration.region.central_india"),
   CENTRAL_US("CentralUs", "realms.configuration.region.central_us"),
   EAST_ASIA("EastAsia", "realms.configuration.region.east_asia"),
   EAST_US("EastUs", "realms.configuration.region.east_us"),
   EAST_US_2("EastUs2", "realms.configuration.region.east_us_2"),
   FRANCE_CENTRAL("FranceCentral", "realms.configuration.region.france_central"),
   JAPAN_EAST("JapanEast", "realms.configuration.region.japan_east"),
   JAPAN_WEST("JapanWest", "realms.configuration.region.japan_west"),
   KOREA_CENTRAL("KoreaCentral", "realms.configuration.region.korea_central"),
   NORTH_CENTRAL_US("NorthCentralUs", "realms.configuration.region.north_central_us"),
   NORTH_EUROPE("NorthEurope", "realms.configuration.region.north_europe"),
   SOUTH_CENTRAL_US("SouthCentralUs", "realms.configuration.region.south_central_us"),
   SOUTHEAST_ASIA("SoutheastAsia", "realms.configuration.region.southeast_asia"),
   SWEDEN_CENTRAL("SwedenCentral", "realms.configuration.region.sweden_central"),
   UAE_NORTH("UAENorth", "realms.configuration.region.uae_north"),
   UK_SOUTH("UKSouth", "realms.configuration.region.uk_south"),
   WEST_CENTRAL_US("WestCentralUs", "realms.configuration.region.west_central_us"),
   WEST_EUROPE("WestEurope", "realms.configuration.region.west_europe"),
   WEST_US("WestUs", "realms.configuration.region.west_us"),
   WEST_US_2("WestUs2", "realms.configuration.region.west_us_2"),
   INVALID_REGION("invalid", "");

   public final String nameId;
   public final String translationKey;

   RealmsRegion(final String nameId, final String translationKey) {
      this.nameId = nameId;
      this.translationKey = translationKey;
   }

   public static @Nullable RealmsRegion findByNameId(final String nameIdStr) {
      for (RealmsRegion value : values()) {
         if (value.nameId.equals(nameIdStr)) {
            return value;
         }
      }

      return null;
   }

   public static class RealmsRegionJsonAdapter extends TypeAdapter<RealmsRegion> {
      private static final Logger LOGGER = LogUtils.getLogger();

      public void write(final JsonWriter jsonWriter, final RealmsRegion realmsRegion) throws IOException {
         jsonWriter.value(realmsRegion.nameId);
      }

      public RealmsRegion read(final JsonReader jsonReader) throws IOException {
         String nameId = jsonReader.nextString();
         RealmsRegion realmsRegion = RealmsRegion.findByNameId(nameId);
         if (realmsRegion == null) {
            LOGGER.warn("Unsupported RealmsRegion {}", nameId);
            return RealmsRegion.INVALID_REGION;
         } else {
            return realmsRegion;
         }
      }
   }
}
