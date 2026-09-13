package com.mojang.realmsclient.dto;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public enum RegionSelectionPreference {
   AUTOMATIC_PLAYER(0, "realms.configuration.region_preference.automatic_player"),
   AUTOMATIC_OWNER(1, "realms.configuration.region_preference.automatic_owner"),
   MANUAL(2, "");

   public static final RegionSelectionPreference DEFAULT_SELECTION = AUTOMATIC_PLAYER;
   public final int id;
   public final String translationKey;

   RegionSelectionPreference(final int p_408115_, final String p_408712_) {
      this.id = p_408115_;
      this.translationKey = p_408712_;
   }

   @OnlyIn(Dist.CLIENT)
   public static class RegionSelectionPreferenceJsonAdapter extends TypeAdapter<RegionSelectionPreference> {
      private static final Logger LOGGER = LogUtils.getLogger();

      public void write(JsonWriter p_406740_, RegionSelectionPreference p_407229_) throws IOException {
         p_406740_.value(p_407229_.id);
      }

      public RegionSelectionPreference read(JsonReader p_406745_) throws IOException {
         int i = p_406745_.nextInt();

         for (RegionSelectionPreference regionselectionpreference : RegionSelectionPreference.values()) {
            if (regionselectionpreference.id == i) {
               return regionselectionpreference;
            }
         }

         LOGGER.warn("Unsupported RegionSelectionPreference {}", i);
         return RegionSelectionPreference.DEFAULT_SELECTION;
      }
   }
}
