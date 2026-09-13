package net.minecraft.client.renderer;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.Reader;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.StrictJsonParser;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.util.profiling.Zone;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class GpuWarnlistManager extends SimplePreparableReloadListener<GpuWarnlistManager.Preparations> {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Identifier GPU_WARNLIST_LOCATION = Identifier.withDefaultNamespace("gpu_warnlist.json");
   private ImmutableMap<String, String> warnings = ImmutableMap.of();
   private boolean showWarning;
   private boolean warningDismissed;

   public boolean hasWarnings() {
      return !this.warnings.isEmpty();
   }

   public boolean willShowWarning() {
      return this.hasWarnings() && !this.warningDismissed;
   }

   public void showWarning() {
      this.showWarning = true;
   }

   public void dismissWarning() {
      this.warningDismissed = true;
   }

   public boolean isShowingWarning() {
      return this.showWarning && !this.warningDismissed;
   }

   public void resetWarnings() {
      this.showWarning = false;
      this.warningDismissed = false;
   }

   public @Nullable String getRendererWarnings() {
      return (String)this.warnings.get("renderer");
   }

   public @Nullable String getVersionWarnings() {
      return (String)this.warnings.get("version");
   }

   public @Nullable String getVendorWarnings() {
      return (String)this.warnings.get("vendor");
   }

   public @Nullable String getAllWarnings() {
      StringBuilder stringbuilder = new StringBuilder();
      this.warnings.forEach((p_109235_, p_109236_) -> stringbuilder.append(p_109235_).append(": ").append(p_109236_));
      return stringbuilder.isEmpty() ? null : stringbuilder.toString();
   }

   protected GpuWarnlistManager.Preparations prepare(ResourceManager p_109220_, ProfilerFiller p_109221_) {
      List<Pattern> list = Lists.newArrayList();
      List<Pattern> list1 = Lists.newArrayList();
      List<Pattern> list2 = Lists.newArrayList();
      JsonObject jsonobject = parseJson(p_109220_, p_109221_);
      if (jsonobject != null) {
         try (Zone zone = p_109221_.zone("compile_regex")) {
            compilePatterns(jsonobject.getAsJsonArray("renderer"), list);
            compilePatterns(jsonobject.getAsJsonArray("version"), list1);
            compilePatterns(jsonobject.getAsJsonArray("vendor"), list2);
         }
      }

      return new GpuWarnlistManager.Preparations(list, list1, list2);
   }

   protected void apply(GpuWarnlistManager.Preparations p_109226_, ResourceManager p_109227_, ProfilerFiller p_109228_) {
      this.warnings = p_109226_.apply();
   }

   private static void compilePatterns(JsonArray p_109223_, List<Pattern> p_109224_) {
      p_109223_.forEach(p_109239_ -> p_109224_.add(Pattern.compile(p_109239_.getAsString(), 2)));
   }

   private static @Nullable JsonObject parseJson(ResourceManager p_109245_, ProfilerFiller p_109246_) {
      try (
         Zone zone = p_109246_.zone("parse_json");
         Reader reader = p_109245_.openAsReader(GPU_WARNLIST_LOCATION);
      ) {
         return StrictJsonParser.parse(reader).getAsJsonObject();
      } catch (JsonSyntaxException | IOException ioexception) {
         LOGGER.warn("Failed to load GPU warnlist", ioexception);
         return null;
      }
   }

   @OnlyIn(Dist.CLIENT)
   protected static final class Preparations {
      private final List<Pattern> rendererPatterns;
      private final List<Pattern> versionPatterns;
      private final List<Pattern> vendorPatterns;

      Preparations(List<Pattern> p_109261_, List<Pattern> p_109262_, List<Pattern> p_109263_) {
         this.rendererPatterns = p_109261_;
         this.versionPatterns = p_109262_;
         this.vendorPatterns = p_109263_;
      }

      private static String matchAny(List<Pattern> p_109273_, String p_109274_) {
         List<String> list = Lists.newArrayList();

         for (Pattern pattern : p_109273_) {
            Matcher matcher = pattern.matcher(p_109274_);

            while (matcher.find()) {
               list.add(matcher.group());
            }
         }

         return String.join(", ", list);
      }

      ImmutableMap<String, String> apply() {
         Builder<String, String> builder = new Builder();
         GpuDevice gpudevice = RenderSystem.getDevice();
         if (gpudevice.getBackendName().equals("OpenGL")) {
            String s = matchAny(this.rendererPatterns, gpudevice.getRenderer());
            if (!s.isEmpty()) {
               builder.put("renderer", s);
            }

            String s1 = matchAny(this.versionPatterns, gpudevice.getVersion());
            if (!s1.isEmpty()) {
               builder.put("version", s1);
            }

            String s2 = matchAny(this.vendorPatterns, gpudevice.getVendor());
            if (!s2.isEmpty()) {
               builder.put("vendor", s2);
            }
         }

         return builder.build();
      }
   }
}
