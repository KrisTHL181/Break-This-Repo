package com.mojang.realmsclient.dto;

import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.mojang.util.UUIDTypeAdapter;
import java.util.UUID;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class PlayerInfo extends ValueObject implements ReflectionBasedSerialization {
   @SerializedName("name")
   public final String name;
   @SerializedName("uuid")
   @JsonAdapter(UUIDTypeAdapter.class)
   public final UUID uuid;
   @SerializedName("operator")
   public boolean operator;
   @SerializedName("accepted")
   public final boolean accepted;
   @SerializedName("online")
   public final boolean online;

   public PlayerInfo(String p_455580_, UUID p_459136_, boolean p_454452_, boolean p_460258_, boolean p_458622_) {
      this.name = p_455580_;
      this.uuid = p_459136_;
      this.operator = p_454452_;
      this.accepted = p_460258_;
      this.online = p_458622_;
   }
}
