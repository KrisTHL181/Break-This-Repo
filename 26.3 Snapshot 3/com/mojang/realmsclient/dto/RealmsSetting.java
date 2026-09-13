package com.mojang.realmsclient.dto;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public record RealmsSetting(@SerializedName("name") String name, @SerializedName("value") String value) implements ReflectionBasedSerialization {
   public static RealmsSetting hardcoreSetting(final boolean hardcore) {
      return new RealmsSetting("hardcore", Boolean.toString(hardcore));
   }

   public static boolean isHardcore(final List<RealmsSetting> settings) {
      for (RealmsSetting setting : settings) {
         if (setting.name().equals("hardcore")) {
            return Boolean.parseBoolean(setting.value());
         }
      }

      return false;
   }
}
