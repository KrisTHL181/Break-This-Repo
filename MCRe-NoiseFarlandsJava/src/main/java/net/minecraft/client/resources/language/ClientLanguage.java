package net.minecraft.client.resources.language;

import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.locale.DeprecatedTranslationsInfo;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.ModMetadata.I18N.ModTranslateResources;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class ClientLanguage extends Language {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final Map<String, String> storage;
    private final boolean defaultRightToLeft;

    private ClientLanguage(final Map<String, String> storage, final boolean defaultRightToLeft) {
        this.storage = storage;
        this.defaultRightToLeft = defaultRightToLeft;
    }

    public static ClientLanguage loadFrom(final ResourceManager resourceManager, final List<String> languageStack, final boolean defaultRightToLeft) {
        Map<String, String> translations = new HashMap<>();

        for (String languageCode : languageStack) {
            String path = String.format(Locale.ROOT, "lang/%s.json", languageCode);

            for (String namespace : resourceManager.getNamespaces()) {
                try {
                    Identifier location = Identifier.fromNamespaceAndPath(namespace, path);
                    appendFrom(languageCode, resourceManager.getResourceStack(location), translations);
                } catch (Exception e) {
                    LOGGER.warn("Skipped language file: {}:{} ({})", namespace, path, e.toString());
                }
            }
        }

        DeprecatedTranslationsInfo.loadFromDefaultResource().applyToMap(translations);
        return new ClientLanguage(Map.copyOf(translations), defaultRightToLeft);
    }

    private static void appendFrom(final String languageCode, final List<Resource> resources, final Map<String, String> translations) {
        for (Resource resource : resources) {
            try (InputStream inputStream = resource.open()) {
                Language.loadFromJson(inputStream, translations::put);
            } catch (IOException e) {
                LOGGER.warn("Failed to load translations for {} from pack {}", languageCode, resource.sourcePackId(), e);
            }
        }
    }

    @Override
    public String getOrDefault(final String key, final String defaultValue) {
        // 🔧 MCRe：自定义 I18N 桥接 —— 命中自定义表优先返回，否则走原版语言表。
        // 这样世界类型（generator.minecraft.*）等 Component.translatable 也能吃到自定义翻译，
        // 无需改动散列资源加载优先级。
        String custom = ModTranslateResources.getCustomTranslation(key);
        if (custom != null) {
            return custom;
        }
        return this.storage.getOrDefault(key, defaultValue);
    }

    @Override
    public boolean has(final String key) {
        return this.storage.containsKey(key);
    }

    @Override
    public boolean isDefaultRightToLeft() {
        return this.defaultRightToLeft;
    }

    @Override
    public FormattedCharSequence getVisualOrder(final FormattedText logicalOrderText) {
        return FormattedBidiReorder.reorder(logicalOrderText, this.defaultRightToLeft);
    }
}