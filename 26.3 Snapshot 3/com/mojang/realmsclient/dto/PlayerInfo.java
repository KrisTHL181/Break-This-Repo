package com.mojang.realmsclient.dto;

import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.mojang.util.UUIDTypeAdapter;
import java.util.UUID;

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

   public PlayerInfo(final String name, final UUID uuid, final boolean operator, final boolean accepted, final boolean online) {
      this.name = name;
      this.uuid = uuid;
      this.operator = operator;
      this.accepted = accepted;
      this.online = online;
   }
}
