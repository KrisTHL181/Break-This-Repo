package net.minecraft.client.renderer.texture;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.gui.screens.AddRealmPopupScreen;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class TextureManager implements PreparableReloadListener, AutoCloseable {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final Identifier INTENTIONAL_MISSING_TEXTURE = Identifier.withDefaultNamespace("");
   private final Map<Identifier, AbstractTexture> byPath = new HashMap<>();
   private final Set<TickableTexture> tickableTextures = new HashSet<>();
   private final ResourceManager resourceManager;

   public TextureManager(ResourceManager p_118474_) {
      this.resourceManager = p_118474_;
      NativeImage nativeimage = MissingTextureAtlasSprite.generateMissingImage();
      this.register(MissingTextureAtlasSprite.getLocation(), new DynamicTexture(() -> "(intentionally-)Missing Texture", nativeimage));
   }

   public void registerAndLoad(Identifier p_450653_, ReloadableTexture p_376843_) {
      try {
         p_376843_.apply(this.loadContentsSafe(p_450653_, p_376843_));
      } catch (Throwable throwable) {
         CrashReport crashreport = CrashReport.forThrowable(throwable, "Uploading texture");
         CrashReportCategory crashreportcategory = crashreport.addCategory("Uploaded texture");
         crashreportcategory.setDetail("Resource location", p_376843_.resourceId());
         crashreportcategory.setDetail("Texture id", p_450653_);
         throw new ReportedException(crashreport);
      }

      this.register(p_450653_, p_376843_);
   }

   private TextureContents loadContentsSafe(Identifier p_455216_, ReloadableTexture p_378623_) {
      try {
         return loadContents(this.resourceManager, p_455216_, p_378623_);
      } catch (Exception exception) {
         LOGGER.error("Failed to load texture {} into slot {}", new Object[]{p_378623_.resourceId(), p_455216_, exception});
         return TextureContents.createMissing();
      }
   }

   public void registerForNextReload(Identifier p_453887_) {
      this.register(p_453887_, new SimpleTexture(p_453887_));
   }

   public void register(Identifier p_461040_, AbstractTexture p_118497_) {
      AbstractTexture abstracttexture = this.byPath.put(p_461040_, p_118497_);
      if (abstracttexture != p_118497_) {
         if (abstracttexture != null) {
            this.safeClose(p_461040_, abstracttexture);
         }

         if (p_118497_ instanceof TickableTexture tickabletexture) {
            this.tickableTextures.add(tickabletexture);
         }
      }
   }

   private void safeClose(Identifier p_456017_, AbstractTexture p_118510_) {
      this.tickableTextures.remove(p_118510_);

      try {
         p_118510_.close();
      } catch (Exception exception) {
         LOGGER.warn("Failed to close texture {}", p_456017_, exception);
      }
   }

   public AbstractTexture getTexture(Identifier p_453290_) {
      AbstractTexture abstracttexture = this.byPath.get(p_453290_);
      if (abstracttexture != null) {
         return abstracttexture;
      }

      SimpleTexture simpletexture = new SimpleTexture(p_453290_);
      this.registerAndLoad(p_453290_, simpletexture);
      return simpletexture;
   }

   public void tick() {
      for (TickableTexture tickabletexture : this.tickableTextures) {
         tickabletexture.tick();
      }
   }

   public void release(Identifier p_460182_) {
      AbstractTexture abstracttexture = this.byPath.remove(p_460182_);
      if (abstracttexture != null) {
         this.safeClose(p_460182_, abstracttexture);
      }
   }

   @Override
   public void close() {
      this.byPath.forEach(this::safeClose);
      this.byPath.clear();
      this.tickableTextures.clear();
   }

   @Override
   public CompletableFuture<Void> reload(
      PreparableReloadListener.SharedState p_427249_, Executor p_118480_, PreparableReloadListener.PreparationBarrier p_118476_, Executor p_118481_
   ) {
      ResourceManager resourcemanager = p_427249_.resourceManager();
      List<TextureManager.PendingReload> list = new ArrayList<>();
      this.byPath.forEach((p_448398_, p_448399_) -> {
         if (p_448399_ instanceof ReloadableTexture reloadabletexture) {
            list.add(scheduleLoad(resourcemanager, p_448398_, reloadabletexture, p_118480_));
         }
      });
      return CompletableFuture.allOf(list.stream().map(TextureManager.PendingReload::newContents).toArray(CompletableFuture[]::new))
         .thenCompose(p_118476_::wait)
         .thenAcceptAsync(p_374677_ -> {
            AddRealmPopupScreen.updateCarouselImages(this.resourceManager);

            for (TextureManager.PendingReload texturemanager$pendingreload : list) {
               texturemanager$pendingreload.texture.apply(texturemanager$pendingreload.newContents.join());
            }
         }, p_118481_);
   }

   public void dumpAllSheets(Path p_276129_) {
      try {
         Files.createDirectories(p_276129_);
      } catch (IOException ioexception) {
         LOGGER.error("Failed to create directory {}", p_276129_, ioexception);
         return;
      }

      this.byPath.forEach((p_448393_, p_448394_) -> {
         if (p_448394_ instanceof Dumpable dumpable) {
            try {
               dumpable.dumpContents(p_448393_, p_276129_);
            } catch (Exception exception) {
               LOGGER.error("Failed to dump texture {}", p_448393_, exception);
            }
         }
      });
   }

   private static TextureContents loadContents(ResourceManager p_375654_, Identifier p_451339_, ReloadableTexture p_377917_) throws IOException {
      try {
         return p_377917_.loadContents(p_375654_);
      } catch (FileNotFoundException filenotfoundexception) {
         if (p_451339_ != INTENTIONAL_MISSING_TEXTURE) {
            LOGGER.warn("Missing resource {} referenced from {}", p_377917_.resourceId(), p_451339_);
         }

         return TextureContents.createMissing();
      }
   }

   private static TextureManager.PendingReload scheduleLoad(ResourceManager p_377119_, Identifier p_454798_, ReloadableTexture p_377978_, Executor p_376135_) {
      return new TextureManager.PendingReload(p_377978_, CompletableFuture.supplyAsync(() -> {
         try {
            return loadContents(p_377119_, p_454798_, p_377978_);
         } catch (IOException ioexception) {
            throw new UncheckedIOException(ioexception);
         }
      }, p_376135_));
   }

   @OnlyIn(Dist.CLIENT)
   record PendingReload(ReloadableTexture texture, CompletableFuture<TextureContents> newContents) {
   }
}
