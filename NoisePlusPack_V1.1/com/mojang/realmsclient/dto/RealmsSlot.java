package com.mojang.realmsclient.dto;

import com.google.gson.TypeAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class RealmsSlot implements ReflectionBasedSerialization {
   @SerializedName("slotId")
   public int slotId;
   @SerializedName("options")
   @JsonAdapter(RealmsSlot.RealmsWorldOptionsJsonAdapter.class)
   public RealmsWorldOptions options;
   @SerializedName("settings")
   public List<RealmsSetting> settings;

   public RealmsSlot(int p_408591_, RealmsWorldOptions p_408181_, List<RealmsSetting> p_406723_) {
      this.slotId = p_408591_;
      this.options = p_408181_;
      this.settings = p_406723_;
   }

   public static RealmsSlot defaults(int p_407048_) {
      return new RealmsSlot(p_407048_, RealmsWorldOptions.createEmptyDefaults(), List.of(RealmsSetting.hardcoreSetting(false)));
   }

   public RealmsSlot copy() {
      return new RealmsSlot(this.slotId, this.options.copy(), new ArrayList<>(this.settings));
   }

   public boolean isHardcore() {
      return RealmsSetting.isHardcore(this.settings);
   }

   @OnlyIn(Dist.CLIENT)
   static class RealmsWorldOptionsJsonAdapter extends TypeAdapter<RealmsWorldOptions> {
      private RealmsWorldOptionsJsonAdapter() {
      }

      public void write(JsonWriter p_407195_, RealmsWorldOptions p_408041_) throws IOException {
         p_407195_.jsonValue(new GuardedSerializer().toJson(p_408041_));
      }

      public RealmsWorldOptions read(JsonReader p_406478_) throws IOException {
         String s = p_406478_.nextString();
         return RealmsWorldOptions.parse(new GuardedSerializer(), s);
      }
   }
}
