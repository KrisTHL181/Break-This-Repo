package com.mojang.realmsclient.dto;

import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.mojang.util.UUIDTypeAdapter;
import java.util.UUID;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class OutboundPlayer implements ReflectionBasedSerialization {
    @SerializedName("name")
    public @Nullable String name;
    @SerializedName("uuid")
    @JsonAdapter(UUIDTypeAdapter.class)
    public @Nullable UUID uuid;
}