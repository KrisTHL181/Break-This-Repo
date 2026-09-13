package net.minecraft.client.resources.language;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.stream.Stream;
import net.minecraft.client.resources.metadata.language.LanguageMetadataSection;
import net.minecraft.locale.Language;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class LanguageManager implements ResourceManagerReloadListener {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final LanguageInfo DEFAULT_LANGUAGE = new LanguageInfo("US", "English", false);
   private Map<String, LanguageInfo> languages = ImmutableMap.of("en_us", DEFAULT_LANGUAGE);
   private String currentCode;
   private final Consumer<ClientLanguage> reloadCallback;

   public LanguageManager(String p_118971_, Consumer<ClientLanguage> p_342376_) {
      this.currentCode = p_118971_;
      this.reloadCallback = p_342376_;
   }

   private static Map<String, LanguageInfo> extractLanguages(Stream<PackResources> p_118982_) {
      Map<String, LanguageInfo> map = Maps.newHashMap();
      p_118982_.forEach(p_374687_ -> {
         try {
            LanguageMetadataSection languagemetadatasection = p_374687_.getMetadataSection(LanguageMetadataSection.TYPE);
            if (languagemetadatasection != null) {
               languagemetadatasection.languages().forEach(map::putIfAbsent);
            }
         } catch (IOException | RuntimeException runtimeexception) {
            LOGGER.warn("Unable to parse language metadata section of resourcepack: {}", p_374687_.packId(), runtimeexception);
         }
      });
      return ImmutableMap.copyOf(map);
   }

   @Override
   public void onResourceManagerReload(ResourceManager p_118973_) {
      this.languages = extractLanguages(p_118973_.listPacks());
      List<String> list = new ArrayList<>(2);
      boolean flag = DEFAULT_LANGUAGE.bidirectional();
      list.add("en_us");
      if (!this.currentCode.equals("en_us")) {
         LanguageInfo languageinfo = this.languages.get(this.currentCode);
         if (languageinfo != null) {
            list.add(this.currentCode);
            flag = languageinfo.bidirectional();
         }
      }

      ClientLanguage clientlanguage = ClientLanguage.loadFrom(p_118973_, list, flag);
      I18n.setLanguage(clientlanguage);
      Language.inject(clientlanguage);
      this.reloadCallback.accept(clientlanguage);
   }

   public void setSelected(String p_265224_) {
      this.currentCode = p_265224_;
   }

   public String getSelected() {
      return this.currentCode;
   }

   public SortedMap<String, LanguageInfo> getLanguages() {
      return new TreeMap<>(this.languages);
   }

   public @Nullable LanguageInfo getLanguage(String p_118977_) {
      return this.languages.get(p_118977_);
   }
}
