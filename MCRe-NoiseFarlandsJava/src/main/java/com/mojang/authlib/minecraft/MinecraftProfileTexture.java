package com.mojang.authlib.minecraft;

import com.google.gson.annotations.SerializedName;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;

import javax.annotation.Nullable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

public class MinecraftProfileTexture {
    public enum Type {
        SKIN,
        CAPE,
        ELYTRA
        ;
    }

    public static final int PROFILE_TEXTURE_COUNT = Type.values().length;

    @SerializedName("url")
    private final String url;
    @SerializedName("metadata")
    private final Map<String, String> metadata;

    public MinecraftProfileTexture(final String url, final Map<String, String> metadata) {
        this.url = url;
        this.metadata = metadata;
    }

    public String getUrl() {
        return url;
    }

    @Nullable
    public String getMetadata(final String key) {
        if (metadata == null) {
            return null;
        }
        return metadata.get(key);
    }

    public String getHash() {
        try {
            return FilenameUtils.getBaseName(new URL(url).getPath());
        } catch (final MalformedURLException exception) {
            throw new IllegalArgumentException("Invalid profile texture url");
        }
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
            .append("url", url)
            .append("hash", getHash())
            .toString();
    }
}
