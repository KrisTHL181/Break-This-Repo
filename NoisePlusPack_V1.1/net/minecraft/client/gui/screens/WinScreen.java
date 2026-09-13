package net.minecraft.client.gui.screens;

import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.LogoRenderer;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.blockentity.AbstractEndPortalRenderer;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.Music;
import net.minecraft.sounds.Musics;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class WinScreen extends Screen {
   private static final Identifier VIGNETTE_LOCATION = Identifier.withDefaultNamespace("textures/misc/credits_vignette.png");
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Component SECTION_HEADING = Component.literal("============").withStyle(ChatFormatting.WHITE);
   private static final String NAME_PREFIX = "           ";
   private static final String OBFUSCATE_TOKEN = "" + ChatFormatting.WHITE + ChatFormatting.OBFUSCATED + ChatFormatting.GREEN + ChatFormatting.AQUA;
   private static final float SPEEDUP_FACTOR = 5.0F;
   private static final float SPEEDUP_FACTOR_FAST = 15.0F;
   private static final Identifier END_POEM_LOCATION = Identifier.withDefaultNamespace("texts/end.txt");
   private static final Identifier CREDITS_LOCATION = Identifier.withDefaultNamespace("texts/credits.json");
   private static final Identifier POSTCREDITS_LOCATION = Identifier.withDefaultNamespace("texts/postcredits.txt");
   private final boolean poem;
   private final Runnable onFinished;
   private float scroll;
   private List<FormattedCharSequence> lines;
   private List<Component> narratorComponents;
   private IntSet centeredLines;
   private int totalScrollLength;
   private boolean speedupActive;
   private final IntSet speedupModifiers = new IntOpenHashSet();
   private float scrollSpeed;
   private final float unmodifiedScrollSpeed;
   private int direction;
   private final LogoRenderer logoRenderer = new LogoRenderer(false);

   public WinScreen(boolean p_276286_, Runnable p_276294_) {
      super(GameNarrator.NO_TITLE);
      this.poem = p_276286_;
      this.onFinished = p_276294_;
      if (!p_276286_) {
         this.unmodifiedScrollSpeed = 0.75F;
      } else {
         this.unmodifiedScrollSpeed = 0.5F;
      }

      this.direction = 1;
      this.scrollSpeed = this.unmodifiedScrollSpeed;
   }

   private float calculateScrollSpeed() {
      return this.speedupActive
         ? this.unmodifiedScrollSpeed * (5.0F + this.speedupModifiers.size() * 15.0F) * this.direction
         : this.unmodifiedScrollSpeed * this.direction;
   }

   @Override
   public void tick() {
      this.minecraft.getMusicManager().tick();
      this.minecraft.getSoundManager().tick(false);
      float f = this.totalScrollLength + this.height + this.height + 24;
      if (this.scroll > f) {
         this.respawn();
      }
   }

   @Override
   public boolean keyPressed(KeyEvent p_425603_) {
      if (p_425603_.isUp()) {
         this.direction = -1;
      } else if (p_425603_.key() == 341 || p_425603_.key() == 345) {
         this.speedupModifiers.add(p_425603_.key());
      } else if (p_425603_.key() == 32) {
         this.speedupActive = true;
      }

      this.scrollSpeed = this.calculateScrollSpeed();
      return super.keyPressed(p_425603_);
   }

   @Override
   public boolean keyReleased(KeyEvent p_422509_) {
      if (p_422509_.isUp()) {
         this.direction = 1;
      }

      if (p_422509_.key() == 32) {
         this.speedupActive = false;
      } else if (p_422509_.key() == 341 || p_422509_.key() == 345) {
         this.speedupModifiers.remove(p_422509_.key());
      }

      this.scrollSpeed = this.calculateScrollSpeed();
      return super.keyReleased(p_422509_);
   }

   @Override
   public void onClose() {
      this.respawn();
   }

   private void respawn() {
      this.onFinished.run();
   }

   @Override
   protected void init() {
      if (this.lines == null) {
         this.lines = Lists.newArrayList();
         this.narratorComponents = Lists.newArrayList();
         this.centeredLines = new IntOpenHashSet();
         if (this.poem) {
            this.wrapCreditsIO(END_POEM_LOCATION, this::addPoemFile);
         }

         this.wrapCreditsIO(CREDITS_LOCATION, this::addCreditsFile);
         if (this.poem) {
            this.wrapCreditsIO(POSTCREDITS_LOCATION, this::addPoemFile);
         }

         this.totalScrollLength = this.lines.size() * 12;
      }
   }

   @Override
   public Component getNarrationMessage() {
      return CommonComponents.joinForNarration(this.narratorComponents.toArray(Component[]::new));
   }

   private void wrapCreditsIO(Identifier p_460734_, WinScreen.CreditsReader p_197400_) {
      try (Reader reader = this.minecraft.getResourceManager().openAsReader(p_460734_)) {
         p_197400_.read(reader);
      } catch (Exception exception) {
         LOGGER.error("Couldn't load credits from file {}", p_460734_, exception);
      }
   }

   private void addPoemFile(Reader p_232818_) throws IOException {
      BufferedReader bufferedreader = new BufferedReader(p_232818_);
      RandomSource randomsource = RandomSource.create(8124371L);

      String s;
      while ((s = bufferedreader.readLine()) != null) {
         s = s.replaceAll("PLAYERNAME", this.minecraft.getUser().getName());

         int i;
         while ((i = s.indexOf(OBFUSCATE_TOKEN)) != -1) {
            String s1 = s.substring(0, i);
            String s2 = s.substring(i + OBFUSCATE_TOKEN.length());
            s = s1 + ChatFormatting.WHITE + ChatFormatting.OBFUSCATED + "XXXXXXXX".substring(0, randomsource.nextInt(4) + 3) + s2;
         }

         this.addPoemLines(s);
         this.addEmptyLine();
      }

      for (int j = 0; j < 8; j++) {
         this.addEmptyLine();
      }
   }

   private void addCreditsFile(Reader p_232820_) {
      for (JsonElement jsonelement : GsonHelper.parseArray(p_232820_)) {
         JsonObject jsonobject = jsonelement.getAsJsonObject();
         String s = jsonobject.get("section").getAsString();
         this.addCreditsLine(SECTION_HEADING, true, false);
         this.addCreditsLine(Component.literal(s).withStyle(ChatFormatting.YELLOW), true, true);
         this.addCreditsLine(SECTION_HEADING, true, false);
         this.addEmptyLine();
         this.addEmptyLine();

         for (JsonElement jsonelement1 : jsonobject.getAsJsonArray("disciplines")) {
            JsonObject jsonobject1 = jsonelement1.getAsJsonObject();
            String s1 = jsonobject1.get("discipline").getAsString();
            if (StringUtils.isNotEmpty(s1)) {
               this.addCreditsLine(Component.literal(s1).withStyle(ChatFormatting.YELLOW), true, true);
               this.addEmptyLine();
               this.addEmptyLine();
            }

            for (JsonElement jsonelement2 : jsonobject1.getAsJsonArray("titles")) {
               JsonObject jsonobject2 = jsonelement2.getAsJsonObject();
               String s2 = jsonobject2.get("title").getAsString();
               JsonArray jsonarray = jsonobject2.getAsJsonArray("names");
               this.addCreditsLine(Component.literal(s2).withStyle(ChatFormatting.GRAY), false, true);

               for (JsonElement jsonelement3 : jsonarray) {
                  String s3 = jsonelement3.getAsString();
                  this.addCreditsLine(Component.literal("           ").append(s3).withStyle(ChatFormatting.WHITE), false, true);
               }

               this.addEmptyLine();
               this.addEmptyLine();
            }
         }
      }
   }

   private void addEmptyLine() {
      this.lines.add(FormattedCharSequence.EMPTY);
      this.narratorComponents.add(CommonComponents.EMPTY);
   }

   private void addPoemLines(String p_181398_) {
      Component component = Component.literal(p_181398_);
      this.lines.addAll(this.minecraft.font.split(component, 256));
      this.narratorComponents.add(component);
   }

   private void addCreditsLine(Component p_169473_, boolean p_169474_, boolean p_406104_) {
      if (p_169474_) {
         this.centeredLines.add(this.lines.size());
      }

      this.lines.add(p_169473_.getVisualOrderText());
      if (p_406104_) {
         this.narratorComponents.add(p_169473_);
      }
   }

   @Override
   public void render(GuiGraphics p_281907_, int p_282364_, int p_282696_, float p_281316_) {
      super.render(p_281907_, p_282364_, p_282696_, p_281316_);
      this.renderVignette(p_281907_);
      this.scroll = Math.max(0.0F, this.scroll + p_281316_ * this.scrollSpeed);
      int i = this.width / 2 - 128;
      int j = this.height + 50;
      float f = -this.scroll;
      p_281907_.pose().pushMatrix();
      p_281907_.pose().translate(0.0F, f);
      p_281907_.nextStratum();
      this.logoRenderer.renderLogo(p_281907_, this.width, 1.0F, j);
      int k = j + 100;

      for (int l = 0; l < this.lines.size(); l++) {
         if (l == this.lines.size() - 1) {
            float f1 = k + f - (this.height / 2 - 6);
            if (f1 < 0.0F) {
               p_281907_.pose().translate(0.0F, -f1);
            }
         }

         if (k + f + 12.0F + 8.0F > 0.0F && k + f < this.height) {
            FormattedCharSequence formattedcharsequence = this.lines.get(l);
            if (this.centeredLines.contains(l)) {
               p_281907_.drawCenteredString(this.font, formattedcharsequence, i + 128, k, -1);
            } else {
               p_281907_.drawString(this.font, formattedcharsequence, i, k, -1);
            }
         }

         k += 12;
      }

      p_281907_.pose().popMatrix();
   }

   private void renderVignette(GuiGraphics p_330215_) {
      p_330215_.blit(RenderPipelines.VIGNETTE, VIGNETTE_LOCATION, 0, 0, 0.0F, 0.0F, this.width, this.height, this.width, this.height);
   }

   @Override
   public void renderBackground(GuiGraphics p_282239_, int p_298581_, int p_297654_, float p_300743_) {
      if (this.poem) {
         TextureManager texturemanager = Minecraft.getInstance().getTextureManager();
         AbstractTexture abstracttexture = texturemanager.getTexture(AbstractEndPortalRenderer.END_SKY_LOCATION);
         AbstractTexture abstracttexture1 = texturemanager.getTexture(AbstractEndPortalRenderer.END_PORTAL_LOCATION);
         TextureSetup texturesetup = TextureSetup.doubleTexture(
            abstracttexture.getTextureView(), abstracttexture.getSampler(), abstracttexture1.getTextureView(), abstracttexture1.getSampler()
         );
         p_282239_.fill(RenderPipelines.END_PORTAL, texturesetup, 0, 0, this.width, this.height);
      } else {
         super.renderBackground(p_282239_, p_298581_, p_297654_, p_300743_);
      }
   }

   @Override
   protected void renderMenuBackground(GuiGraphics p_335873_, int p_330470_, int p_334347_, int p_334935_, int p_329161_) {
      float f = this.scroll * 0.5F;
      Screen.renderMenuBackgroundTexture(p_335873_, Screen.MENU_BACKGROUND, 0, 0, 0.0F, f, p_334935_, p_329161_);
   }

   @Override
   public boolean isPauseScreen() {
      return !this.poem;
   }

   @Override
   public boolean isAllowedInPortal() {
      return true;
   }

   @Override
   public void removed() {
      this.minecraft.getMusicManager().stopPlaying(Musics.CREDITS);
   }

   @Override
   public Music getBackgroundMusic() {
      return Musics.CREDITS;
   }

   @FunctionalInterface
   @OnlyIn(Dist.CLIENT)
   interface CreditsReader {
      void read(Reader var1) throws IOException;
   }
}
