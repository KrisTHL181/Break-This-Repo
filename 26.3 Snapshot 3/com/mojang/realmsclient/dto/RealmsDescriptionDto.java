package com.mojang.realmsclient.dto;

import com.google.gson.annotations.SerializedName;
import org.jspecify.annotations.Nullable;

public record RealmsDescriptionDto(@SerializedName("name") @Nullable String name, @SerializedName("description") String description)
   implements ReflectionBasedSerialization {
}
