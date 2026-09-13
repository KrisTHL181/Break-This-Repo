package com.mojang.realmsclient.dto;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
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

   RealmsRegion(final String p_406279_, final String p_405867_) {
      this.nameId = p_406279_;
      this.translationKey = p_405867_;
   }

   public static @Nullable RealmsRegion findByNameId(String p_406292_) {
      for (RealmsRegion realmsregion : values()) {
         if (realmsregion.nameId.equals(p_406292_)) {
            return realmsregion;
         }
      }

      return null;
   }

   @OnlyIn(Dist.CLIENT)
   public static class RealmsRegionJsonAdapter extends TypeAdapter<RealmsRegion> {
      private static final Logger LOGGER = LogUtils.getLogger();

      public void write(JsonWriter p_407897_, RealmsRegion p_407545_) throws IOException {
         p_407897_.value(p_407545_.nameId);
      }

      public RealmsRegion read(JsonReader p_409020_) throws IOException {
         String s = p_409020_.nextString();
         RealmsRegion realmsregion = RealmsRegion.findByNameId(s);
         if (realmsregion == null) {
            LOGGER.warn("Unsupported RealmsRegion {}", s);
            return RealmsRegion.INVALID_REGION;
         } else {
            return realmsregion;
         }
      }
   }
}
