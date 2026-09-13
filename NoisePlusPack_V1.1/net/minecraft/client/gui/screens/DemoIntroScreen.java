package net.minecraft.client.gui.screens;

import net.minecraft.client.Options;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.TextAlignment;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.util.CommonLinks;
import net.minecraft.util.Util;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class DemoIntroScreen extends Screen {
   private static final Identifier DEMO_BACKGROUND_LOCATION = Identifier.withDefaultNamespace("textures/gui/demo_background.png");
   private static final int BACKGROUND_TEXTURE_WIDTH = 256;
   private static final int BACKGROUND_TEXTURE_HEIGHT = 256;
   private static final int TEXT_COLOR = -14737633;
   private MultiLineLabel movementMessage = MultiLineLabel.EMPTY;
   private MultiLineLabel durationMessage = MultiLineLabel.EMPTY;

   public DemoIntroScreen() {
      super(Component.translatable("demo.help.title"));
   }

   @Override
   protected void init() {
      int i = -16;
      this.addRenderableWidget(Button.builder(Component.translatable("demo.help.buy"), p_448017_ -> {
         p_448017_.active = false;
         Util.getPlatform().openUri(CommonLinks.BUY_MINECRAFT_JAVA);
      }).bounds(this.width / 2 - 116, this.height / 2 + 62 + -16, 114, 20).build());
      this.addRenderableWidget(Button.builder(Component.translatable("demo.help.later"), p_280798_ -> {
         this.minecraft.setScreen(null);
         this.minecraft.mouseHandler.grabMouse();
      }).bounds(this.width / 2 + 2, this.height / 2 + 62 + -16, 114, 20).build());
      Options options = this.minecraft.options;
      this.movementMessage = MultiLineLabel.create(
         this.font,
         this.movementMessage(
            Component.translatable(
               "demo.help.movementShort",
               options.keyUp.getTranslatedKeyMessage(),
               options.keyLeft.getTranslatedKeyMessage(),
               options.keyDown.getTranslatedKeyMessage(),
               options.keyRight.getTranslatedKeyMessage()
            )
         ),
         this.movementMessage(Component.translatable("demo.help.movementMouse")),
         this.movementMessage(Component.translatable("demo.help.jump", options.keyJump.getTranslatedKeyMessage())),
         this.movementMessage(Component.translatable("demo.help.inventory", options.keyInventory.getTranslatedKeyMessage()))
      );
      this.durationMessage = MultiLineLabel.create(this.font, Component.translatable("demo.help.fullWrapped").withoutShadow().withColor(-14737633), 218);
   }

   private Component movementMessage(MutableComponent p_459297_) {
      return p_459297_.withoutShadow().withColor(-11579569);
   }

   @Override
   public void renderBackground(GuiGraphics p_283391_, int p_299907_, int p_301194_, float p_297228_) {
      super.renderBackground(p_283391_, p_299907_, p_301194_, p_297228_);
      int i = (this.width - 248) / 2;
      int j = (this.height - 166) / 2;
      p_283391_.blit(RenderPipelines.GUI_TEXTURED, DEMO_BACKGROUND_LOCATION, i, j, 0.0F, 0.0F, 248, 166, 256, 256);
   }

   @Override
   public void render(GuiGraphics p_281247_, int p_281844_, int p_283693_, float p_281842_) {
      super.render(p_281247_, p_281844_, p_283693_, p_281842_);
      int i = (this.width - 248) / 2 + 10;
      int j = (this.height - 166) / 2 + 8;
      ActiveTextCollector activetextcollector = p_281247_.textRenderer();
      p_281247_.drawString(this.font, this.title, i, j, -14737633, false);
      j = this.movementMessage.visitLines(TextAlignment.LEFT, i, j + 12, 12, activetextcollector);
      this.durationMessage.visitLines(TextAlignment.LEFT, i, j + 20, 9, activetextcollector);
   }
}
