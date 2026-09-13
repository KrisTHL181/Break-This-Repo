package com.mojang.realmsclient.dto;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record RealmsSetting(@SerializedName("name") String name, @SerializedName("value") String value) implements ReflectionBasedSerialization {
   public static RealmsSetting hardcoreSetting(boolean p_409901_) {
      return new RealmsSetting("hardcore", Boolean.toString(p_409901_));
   }

   public static boolean isHardcore(List<RealmsSetting> p_406429_) {
      for (RealmsSetting realmssetting : p_406429_) {
         if (realmssetting.name().equals("hardcore")) {
            return Boolean.parseBoolean(realmssetting.value());
         }
      }

      return false;
   }
}
