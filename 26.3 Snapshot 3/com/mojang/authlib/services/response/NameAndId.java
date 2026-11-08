package com.mojang.authlib.services.response;

import com.google.gson.annotations.SerializedName;
import java.util.UUID;

public record NameAndId(@SerializedName("id") UUID id, @SerializedName("name") String name) {
}
