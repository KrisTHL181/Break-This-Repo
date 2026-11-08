package com.elfmcys.yesstevemodel.model.format;

import com.elfmcys.yesstevemodel.resource.models.Metadata;
import com.elfmcys.yesstevemodel.resource.models.ModelProperties;
import com.elfmcys.yesstevemodel.util.FileTypeUtil;
import com.elfmcys.yesstevemodel.resource.models.MainModelInfo;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class ServerModelInfo {

    @Nullable
    private final Metadata metadata;

    private final ModelProperties modelProperties;

    private final MainModelInfo mainModelInfo;

    private final int formatVersion; // .ysm的内部的format版本号 没有就是65535

    private final String modelHash;

    private final String extra; // 备注

    private final long timestamp;

    private final String rand;

    private final int hashId;

    private final Map<String, Map<String, String>> translations;

    public ServerModelInfo(@Nullable Metadata metadata, ModelProperties modelProperties, MainModelInfo mainModelInfo, int formatVersion, String modelHash, String extra, long timestamp, String rand, Map<String, Map<String, String>> translations) {
        this.metadata = metadata;
        this.modelProperties = modelProperties;
        this.mainModelInfo = mainModelInfo;
        this.formatVersion = formatVersion;
        this.modelHash = modelHash;
        this.extra = extra;
        this.timestamp = timestamp;
        this.rand = rand;
        this.hashId = FileTypeUtil.parseHexId(modelHash);
        LinkedHashMap<String, Map<String, String>> copy = new LinkedHashMap<>();
        if (translations != null) {
            translations.forEach((locale, values) -> copy.put(locale, values == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(values))));
        }
        this.translations = Collections.unmodifiableMap(copy);
    }

    @Nullable
    public Metadata getExtraInfo() {
        return this.metadata;
    }

    public ModelProperties getModelProperties() {
        return this.modelProperties;
    }

    public MainModelInfo getMainModelInfo() {
        return this.mainModelInfo;
    }

    public int getFormatVersion() {
        return this.formatVersion;
    }

    public String getModelHash() {
        return this.modelHash;
    }

    public String getExtra() {
        return this.extra;
    }

    public long getTimestamp() {
        return this.timestamp;
    }

    public String getRand() {
        return this.rand;
    }

    public int getHashId() {
        return this.hashId;
    }

    public Map<String, Map<String, String>> getTranslations() {
        return this.translations;
    }
}
