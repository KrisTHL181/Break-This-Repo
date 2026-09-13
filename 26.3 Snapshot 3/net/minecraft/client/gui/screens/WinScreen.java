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
import net.minecraft.client.gui.GuiGraphicsExtractor;
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
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

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

   public WinScreen(final boolean poem, final Runnable onFinished) {
      super(GameNarrator.NO_TITLE);
      this.poem = poem;
      this.onFinished = onFinished;
      if (!poem) {
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
      float maxScroll = this.totalScrollLength + this.height + this.height + 24;
      if (this.scroll > maxScroll) {
         this.respawn();
      }
   }

   @Override
   public boolean keyPressed(final KeyEvent event) {
      if (event.isUp()) {
         this.direction = -1;
      } else if (event.key() == 341 || event.key() == 345) {
         this.speedupModifiers.add(event.key());
      } else if (event.key() == 32) {
         this.speedupActive = true;
      }

      this.scrollSpeed = this.calculateScrollSpeed();
      return super.keyPressed(event);
   }

   @Override
   public boolean keyReleased(final KeyEvent event) {
      if (event.isUp()) {
         this.direction = 1;
      }

      if (event.key() == 32) {
         this.speedupActive = false;
      } else if (event.key() == 341 || event.key() == 345) {
         this.speedupModifiers.remove(event.key());
      }

      this.scrollSpeed = this.calculateScrollSpeed();
      return super.keyReleased(event);
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

   private void wrapCreditsIO(final Identifier file, final WinScreen.CreditsReader creditsReader) {
      try (Reader resource = this.minecraft.getResourceManager().openAsReader(file)) {
         creditsReader.read(resource);
      } catch (Exception e) {
         LOGGER.error("Couldn't load credits from file {}", file, e);
      }
   }

   private void addPoemFile(final Reader inputReader) throws IOException {
      BufferedReader reader = new BufferedReader(inputReader);
      RandomSource random = RandomSource.createThreadLocalInstance(8124371L);

      String line;
      while ((line = reader.readLine()) != null) {
         line = line.replaceAll("PLAYERNAME", this.minecraft.getUser().getName());

         int pos;
         while ((pos = line.indexOf(OBFUSCATE_TOKEN)) != -1) {
            String before = line.substring(0, pos);
            String after = line.substring(pos + OBFUSCATE_TOKEN.length());
            line = before + ChatFormatting.WHITE + ChatFormatting.OBFUSCATED + "XXXXXXXX".substring(0, random.nextInt(4) + 3) + after;
         }

         this.addPoemLines(line);
         this.addEmptyLine();
      }

      for (int i = 0; i < 8; i++) {
         this.addEmptyLine();
      }
   }

   private void addCreditsFile(final Reader inputReader) {
      for (JsonElement sectionElement : GsonHelper.parseArray(inputReader)) {
         JsonObject section = sectionElement.getAsJsonObject();
         String sectionName = section.get("section").getAsString();
         this.addCreditsLine(SECTION_HEADING, true, false);
         this.addCreditsLine(Component.literal(sectionName).withStyle(ChatFormatting.YELLOW), true, true);
         this.addCreditsLine(SECTION_HEADING, true, false);
         this.addEmptyLine();
         this.addEmptyLine();

         for (JsonElement disciplineElement : section.getAsJsonArray("disciplines")) {
            JsonObject discipline = disciplineElement.getAsJsonObject();
            String disciplineName = discipline.get("discipline").getAsString();
            if (StringUtils.isNotEmpty(disciplineName)) {
               this.addCreditsLine(Component.literal(disciplineName).withStyle(ChatFormatting.YELLOW), true, true);
               this.addEmptyLine();
               this.addEmptyLine();
            }

            for (JsonElement titleElement : discipline.getAsJsonArray("titles")) {
               JsonObject title = titleElement.getAsJsonObject();
               String titleName = title.get("title").getAsString();
               JsonArray names = title.getAsJsonArray("names");
               this.addCreditsLine(Component.literal(titleName).withStyle(ChatFormatting.GRAY), false, true);

               for (JsonElement nameElement : names) {
                  String name = nameElement.getAsString();
                  this.addCreditsLine(Component.literal("           ").append(name).withStyle(ChatFormatting.WHITE), false, true);
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

   private void addPoemLines(final String line) {
      Component component = Component.literal(line);
      this.lines.addAll(this.minecraft.font.split(component, 256));
      this.narratorComponents.add(component);
   }

   private void addCreditsLine(final Component line, final boolean centered, final boolean narrated) {
      if (centered) {
         this.centeredLines.add(this.lines.size());
      }

      this.lines.add(line.getVisualOrderText());
      if (narrated) {
         this.narratorComponents.add(line);
      }
   }

   @Override
   public void extractRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
      super.extractRenderState(graphics, mouseX, mouseY, a);
      this.extractVignette(graphics);
      this.scroll = Math.max(0.0F, this.scroll + a * this.scrollSpeed);
      int logoX = this.width / 2 - 128;
      int logoY = this.height + 50;
      float yOffs = -this.scroll;
      graphics.pose().pushMatrix();
      graphics.pose().translate(0.0F, yOffs);
      graphics.nextStratum();
      this.logoRenderer.extractRenderState(graphics, this.width, 1.0F, logoY);
      int yPos = logoY + 100;

      for (int i = 0; i < this.lines.size(); i++) {
         if (i == this.lines.size() - 1) {
            float diff = yPos + yOffs - (this.height / 2 - 6);
            if (diff < 0.0F) {
               graphics.pose().translate(0.0F, -diff);
            }
         }

         if (yPos + yOffs + 12.0F + 8.0F > 0.0F && yPos + yOffs < this.height) {
            FormattedCharSequence line = this.lines.get(i);
            if (this.centeredLines.contains(i)) {
               graphics.centeredText(this.font, line, logoX + 128, yPos, -1);
            } else {
               graphics.text(this.font, line, logoX, yPos, -1);
            }
         }

         yPos += 12;
      }

      graphics.pose().popMatrix();
   }

   private void extractVignette(final GuiGraphicsExtractor graphics) {
      graphics.blit(RenderPipelines.VIGNETTE, VIGNETTE_LOCATION, 0, 0, 0.0F, 0.0F, this.width, this.height, this.width, this.height);
   }

   @Override
   public void extractBackground(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
      if (this.poem) {
         TextureManager textureManager = Minecraft.getInstance().getTextureManager();
         AbstractTexture skyTexture = textureManager.getTexture(AbstractEndPortalRenderer.END_SKY_LOCATION);
         AbstractTexture portalTexture = textureManager.getTexture(AbstractEndPortalRenderer.END_PORTAL_LOCATION);
         TextureSetup textureSetup = TextureSetup.doubleTexture(
            skyTexture.getTextureView(), skyTexture.getSampler(), portalTexture.getTextureView(), portalTexture.getSampler()
         );
         graphics.fill(RenderPipelines.END_PORTAL, textureSetup, 0, 0, this.width, this.height);
      } else {
         super.extractBackground(graphics, mouseX, mouseY, a);
      }
   }

   @Override
   protected void extractMenuBackground(final GuiGraphicsExtractor graphics, final int x, final int y, final int width, final int height) {
      float v = this.scroll * 0.5F;
      Screen.extractMenuBackgroundTexture(graphics, Screen.MENU_BACKGROUND, 0, 0, 0.0F, v, width, height);
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
   private interface CreditsReader {
      void read(final Reader reader) throws IOException;
   }
}
