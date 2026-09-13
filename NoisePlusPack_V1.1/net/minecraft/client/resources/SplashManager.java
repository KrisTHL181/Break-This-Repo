package net.minecraft.client.resources;

import java.io.BufferedReader;
import java.io.IOException;
import java.time.MonthDay;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.client.gui.components.SplashRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.RandomSource;
import net.minecraft.util.SpecialDates;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class SplashManager extends SimplePreparableReloadListener<List<Component>> {
   private static final Style DEFAULT_STYLE = Style.EMPTY.withColor(-256);
   public static final Component CHRISTMAS = literalSplash("Merry X-mas!");
   public static final Component NEW_YEAR = literalSplash("Happy new year!");
   public static final Component HALLOWEEN = literalSplash("OOoooOOOoooo! Spooky!");
   private static final Identifier SPLASHES_LOCATION = Identifier.withDefaultNamespace("texts/splashes.txt");
   private static final RandomSource RANDOM = RandomSource.create();
   private List<Component> splashes = List.of();
   private final User user;

   public SplashManager(User p_118866_) {
      this.user = p_118866_;
   }

   private static Component literalSplash(String p_451980_) {
      return Component.literal(p_451980_).setStyle(DEFAULT_STYLE);
   }

   protected List<Component> prepare(ResourceManager p_118869_, ProfilerFiller p_118870_) {
      try (BufferedReader bufferedreader = Minecraft.getInstance().getResourceManager().openAsReader(SPLASHES_LOCATION)) {
         return bufferedreader.lines().map(String::trim).filter(p_118876_ -> p_118876_.hashCode() != 125780783).map(SplashManager::literalSplash).toList();
      } catch (IOException ioexception) {
         return List.of();
      }
   }

   protected void apply(List<Component> p_118878_, ResourceManager p_118879_, ProfilerFiller p_118880_) {
      this.splashes = List.copyOf(p_118878_);
   }

   public @Nullable SplashRenderer getSplash() {
      MonthDay monthday = SpecialDates.dayNow();
      if (monthday.equals(SpecialDates.CHRISTMAS)) {
         return SplashRenderer.CHRISTMAS;
      } else if (monthday.equals(SpecialDates.NEW_YEAR)) {
         return SplashRenderer.NEW_YEAR;
      } else if (monthday.equals(SpecialDates.HALLOWEEN)) {
         return SplashRenderer.HALLOWEEN;
      } else if (this.splashes.isEmpty()) {
         return null;
      } else {
         return this.user != null && RANDOM.nextInt(this.splashes.size()) == 42
            ? new SplashRenderer(literalSplash(this.user.getName().toUpperCase(Locale.ROOT) + " IS YOU"))
            : new SplashRenderer(this.splashes.get(RANDOM.nextInt(this.splashes.size())));
      }
   }
}
