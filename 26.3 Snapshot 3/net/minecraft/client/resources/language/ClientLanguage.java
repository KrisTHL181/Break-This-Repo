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
import org.slf4j.Logger;

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
               LOGGER.warn("Skipped language file: {}:{} ({})", new Object[]{namespace, path, e.toString()});
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
            LOGGER.warn("Failed to load translations for {} from pack {}", new Object[]{languageCode, resource.sourcePackId(), e});
         }
      }
   }

   @Override
   public String getOrDefault(final String key, final String defaultValue) {
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
