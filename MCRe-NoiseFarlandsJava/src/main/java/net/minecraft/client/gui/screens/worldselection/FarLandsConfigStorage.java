package net.minecraft.client.gui.screens.worldselection;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.logging.LogUtils;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

/**
 * 🔧 MCRe：FarLands 全局配置持久化（方案 B —— 轻量 Gson JSON）
 *
 * <p>配置文件保存在 options.txt 同目录（游戏主目录）下的 farlands_config.json，
 * 属于全局配置（非按世界），固定 26.2 无跨版本需求，因此直接整类序列化，
 * 不引入 options.txt 那套 OptionInstance/Codec/dataFix 机制。
 *
 * @author MCRe Ultimate Scaler
 */
@OnlyIn(Dist.CLIENT)
public final class FarLandsConfigStorage {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String FILE_NAME = "farlands_config.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private FarLandsConfigStorage() {
    }

    /** 配置文件路径：与 options.txt 同目录 */
    public static File configFile(final File gameDirectory) {
        return new File(gameDirectory, FILE_NAME);
    }

    /** 从文件加载配置；文件不存在时先按默认配置保存一次再返回默认（写保护） */
    public static WorldMainSettingScreen.FarLandsConfigData load(final File gameDirectory) {
        File file = configFile(gameDirectory);
        if (!file.exists()) {
            // 🔧 MCRe：游戏目录无配置文件 → 写入一份默认配置，避免反复重建
            WorldMainSettingScreen.FarLandsConfigData defaults = new WorldMainSettingScreen.FarLandsConfigData();
            save(gameDirectory, defaults);
            return defaults;
        }

        try (FileReader reader = new FileReader(file)) {
            WorldMainSettingScreen.FarLandsConfigData config =
                    GSON.fromJson(reader, WorldMainSettingScreen.FarLandsConfigData.class);
            if (config == null) {
                LOGGER.warn("Empty FarLands config, using defaults");
                return new WorldMainSettingScreen.FarLandsConfigData();
            }
            return config;
        } catch (IOException | com.google.gson.JsonParseException e) {
            LOGGER.error("Failed to load FarLands config from {}, using defaults", file, e);
            return new WorldMainSettingScreen.FarLandsConfigData();
        }
    }

    /** 保存配置到文件 */
    public static void save(final File gameDirectory, final WorldMainSettingScreen.FarLandsConfigData config) {
        File file = configFile(gameDirectory);
        try (FileWriter writer = new FileWriter(file)) {
            GSON.toJson(config, writer);
        } catch (IOException e) {
            LOGGER.error("Failed to save FarLands config to {}", file, e);
        }
    }
}
