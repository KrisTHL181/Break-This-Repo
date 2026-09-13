package net.minecraft;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.logging.LogUtils;
import net.ModMetadata.ModMetadata;
import net.minecraft.server.packs.metadata.pack.PackFormat;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.storage.DataVersion;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.UUID;

public class DetectedVersion {
    private static final Logger LOGGER = LogUtils.getLogger();

    // ✅ 恢复 BUILT_IN，指向 ModMetadata 的版本，避免循环依赖
    public static final WorldVersion BUILT_IN = ModMetadata.VERSION;

    // ❌ 删除 createBuiltIn 方法（已不再使用）
    // ❌ 删除 createBuiltIn 的重载

    private static WorldVersion createFromJson(final JsonObject root) {
        JsonObject packVersion = GsonHelper.getAsJsonObject(root, "pack_version");
        return new WorldVersion.Simple(
                GsonHelper.getAsString(root, "id"),
                GsonHelper.getAsString(root, "name"),
                new DataVersion(GsonHelper.getAsInt(root, "world_version"), GsonHelper.getAsString(root, "series_id", "main")),
                GsonHelper.getAsInt(root, "protocol_version"),
                PackFormat.of(GsonHelper.getAsInt(packVersion, "resource_major"), GsonHelper.getAsInt(packVersion, "resource_minor")),
                PackFormat.of(GsonHelper.getAsInt(packVersion, "data_major"), GsonHelper.getAsInt(packVersion, "data_minor")),
                Date.from(ZonedDateTime.parse(GsonHelper.getAsString(root, "build_time")).toInstant()),
                GsonHelper.getAsBoolean(root, "stable")
        );
    }

    public static WorldVersion tryDetectVersion() {
        LOGGER.info("🚀 MCRe ModMetadata loaded: {}", ModMetadata.VERSION.id());
        return ModMetadata.VERSION;
    }
}