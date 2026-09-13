package net.ModMetadata;

import net.minecraft.WorldVersion;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.pack.PackFormat;
import net.minecraft.world.level.storage.DataVersion;
import net.minecraft.DetectedVersion;

import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 🔥 MCRe 硬编码版本元数据 —— 完全替代 version.json
 *
 * <p>所有数据均在类中以常量形式定义，无需读取任何外部文件。 直接使用 {@link #WORLD_VERSION} 即可获得完整的 WorldVersion 实例。
 *
 * @author MCRe Ultimate Scaler
 * @since 2026-08-03
 */
public final class ModMetadata {

    // ==================== 原版 version.json 字段（硬编码） ====================
    private static final String ID = "26.2";
    private static final String NAME = "26.2 [Permanent]";
    private static final int WORLD_VERSION = 4903;
    private static final String SERIES_ID = "main";
    public static final int PROTOCOL_VERSION = 776; // ← 改为 public
    private static final int RESOURCE_MAJOR = 88;
    private static final int RESOURCE_MINOR = 0;
    private static final int DATA_MAJOR = 107;
    private static final int DATA_MINOR = 1;
    private static final Date BUILD_TIME = Date.from(ZonedDateTime.parse("2026-09-05T04:40:57.186803751Z").toInstant());
    private static final boolean STABLE = true;

    // ==================== MCRe Mod 扩展字段（硬编码） ====================
    private static final String MOD_ID = "MCRe_NoiseFarlandsJava__";
    private static final String MOD_VERSION = "1.0.0 - 32bit";
    private static final String DISPLAY_NAME = "MCRe NoiseFarlandsJava";
    private static final List<String> AUTHORS = List.of("MFSCelebrate_", "More.....");
    private static final String DESCRIPTION = "A Farlands Mod, designed to fully overcome the limitations of 32-bit and 64-bit integers.";
    private static final Map<String, String> DEPENDENCIES = Map.of("minecraft", "26.2");
    private static final boolean IS_64BIT_READY = false;

    // ==================== 获取构建时间（供外部调用） ====================
    public static Date getBuildTime() {
        return BUILD_TIME;
    }
    
    // ==================== 对外暴露的 WorldVersion 单例 ====================
    public static final WorldVersion VERSION = new WorldVersion() {
        @Override
        public DataVersion dataVersion() {
            return new DataVersion(WORLD_VERSION, SERIES_ID); // 这里还是用 int 常量
        }

        // ... 其他方法保持不变

        @Override
        public String id() {
            return ID;
        }

        @Override
        public String name() {
            return NAME;
        }

        @Override
        public int protocolVersion() {
            return PROTOCOL_VERSION;
        }

        @Override
        public PackFormat packVersion(PackType packType) {
            return switch (packType) {
                case CLIENT_RESOURCES -> PackFormat.of(RESOURCE_MAJOR, RESOURCE_MINOR);
                case SERVER_DATA -> PackFormat.of(DATA_MAJOR, DATA_MINOR);
            };
        }

        @Override
        public Date buildTime() {
            return BUILD_TIME;
        }

        @Override
        public boolean stable() {
            return STABLE;
        }

        @Override
        public String toString() {
            return String.format("MCRe_WorldVersion{id='%s', name='%s', modId='%s', 64bit=%s}",
                    id(), name(), MOD_ID, IS_64BIT_READY);
        }
    };

    // ==================== 可选：获取 Mod 元数据（非 WorldVersion 部分） ====================
    public static String getModId() {
        return MOD_ID;
    }

    public static String getModVersion() {
        return MOD_VERSION;
    }

    public static String getDisplayName() {
        return DISPLAY_NAME;
    }

    public static List<String> getAuthors() {
        return AUTHORS;
    }

    public static String getDescription() {
        return DESCRIPTION;
    }

    public static Map<String, String> getDependencies() {
        return DEPENDENCIES;
    }

    public static boolean is64BitReady() {
        return IS_64BIT_READY;
    }

    // 私有构造，防止实例化
    private ModMetadata() {}
}