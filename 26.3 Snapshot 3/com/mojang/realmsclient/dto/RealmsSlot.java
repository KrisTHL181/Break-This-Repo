package com.mojang.realmsclient.dto;

import com.google.gson.TypeAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class RealmsSlot implements ReflectionBasedSerialization {
   @SerializedName("slotId")
   public int slotId;
   @SerializedName("options")
   @JsonAdapter(RealmsSlot.RealmsWorldOptionsJsonAdapter.class)
   public RealmsWorldOptions options;
   @SerializedName("settings")
   public List<RealmsSetting> settings;

   public RealmsSlot(final int slotId, final RealmsWorldOptions options, final List<RealmsSetting> settings) {
      this.slotId = slotId;
      this.options = options;
      this.settings = settings;
   }

   public static RealmsSlot defaults(final int slotId) {
      return new RealmsSlot(slotId, RealmsWorldOptions.createEmptyDefaults(), List.of(RealmsSetting.hardcoreSetting(false)));
   }

   public RealmsSlot copy() {
      return new RealmsSlot(this.slotId, this.options.copy(), new ArrayList<>(this.settings));
   }

   public boolean isHardcore() {
      return RealmsSetting.isHardcore(this.settings);
   }

   private static class RealmsWorldOptionsJsonAdapter extends TypeAdapter<RealmsWorldOptions> {
      public void write(final JsonWriter jsonWriter, final RealmsWorldOptions realmsSlotOptions) throws IOException {
         jsonWriter.jsonValue(new GuardedSerializer().toJson(realmsSlotOptions));
      }

      public RealmsWorldOptions read(final JsonReader jsonReader) throws IOException {
         String json = jsonReader.nextString();
         return RealmsWorldOptions.parse(new GuardedSerializer(), json);
      }
   }
}
