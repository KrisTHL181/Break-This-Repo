package dev.miru.gui.screens;

import dev.miru.gui.screens.config.GeneralSettingScreen;
import dev.miru.main.ModMain;
import java.util.function.Supplier;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.toasts.NowPlayingToast;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.ShareToLanScreen;
import net.minecraft.client.gui.screens.achievement.StatsScreen;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.social.SocialInteractionsScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class EnhantedPauseScreen extends Screen {
   private static final Identifier DRAFT_REPORT_SPRITE = Identifier.withDefaultNamespace("icon/draft_report");
   private static final Component RETURN_TO_GAME = Component.translatable("menu.returnToGame");
   private static final Component ADVANCEMENTS = Component.translatable("gui.advancements");
   private static final Component STATS = Component.translatable("gui.stats");
   private static final Component OPTIONS = Component.translatable("menu.options");
   private static final Component SHARE_TO_LAN = Component.translatable("menu.shareToLan");
   private static final Component PLAYER_REPORTING = Component.translatable("menu.playerReporting");
   private static final Component GAME = Component.translatable("menu.game");
   private static final Component PAUSED = Component.translatable("menu.paused");
   private final boolean showPauseMenu;
   private @Nullable Button disconnectButton;

   public EnhantedPauseScreen(boolean p_96308_) {
      super(p_96308_ ? GAME : PAUSED);
      this.showPauseMenu = p_96308_;
   }

   public EnhantedPauseScreen() {
      super(GAME);
      this.showPauseMenu = true;
   }

   @Override
   protected void init() {
      if (this.showPauseMenu) {
         this.createPauseMenu();
      }

      int i = this.font.width(this.title);
      this.addRenderableWidget(new StringWidget(this.width / 2 - i / 2, this.showPauseMenu ? 40 : 10, i, 9, this.title, this.font));
   }

   private void createPauseMenu() {
      GridLayout gridlayout = new GridLayout();
      gridlayout.defaultCellSetting().padding(4, 4, 4, 0);
      GridLayout.RowHelper gridlayout$rowhelper = gridlayout.createRowHelper(2);
      gridlayout$rowhelper.addChild(Button.builder(RETURN_TO_GAME, p_280814_ -> {
         this.minecraft.setScreen(null);
         this.minecraft.mouseHandler.grabMouse();
      }).width(204).build(), 2, gridlayout.newCellSettings().paddingTop(50));
      gridlayout$rowhelper.addChild(this.openScreenButton(ADVANCEMENTS, () -> new AdvancementsScreen(this.minecraft.player.connection.getAdvancements(), this)));
      gridlayout$rowhelper.addChild(this.openScreenButton(STATS, () -> new StatsScreen(this, this.minecraft.player.getStats())));
      gridlayout$rowhelper.addChild(this.openScreenButton(OPTIONS, () -> new OptionsScreen(this, this.minecraft.options)));
      if (this.minecraft.hasSingleplayerServer() && !this.minecraft.getSingleplayerServer().isPublished()) {
         gridlayout$rowhelper.addChild(this.openScreenButton(SHARE_TO_LAN, () -> new ShareToLanScreen(this)));
      } else {
         gridlayout$rowhelper.addChild(this.openScreenButton(PLAYER_REPORTING, () -> new SocialInteractionsScreen(this)));
      }

      gridlayout$rowhelper.addChild(
         this.openScreenButton(Component.literal(ModMain.getI18N("screen.pause.tpp_menu")), () -> new GeneralSettingScreen(this, ModMain.getOptions()))
      );
      this.disconnectButton = gridlayout$rowhelper.addChild(
         Button.builder(
               CommonComponents.disconnectButtonLabel(this.minecraft.isLocalServer()),
               p_280815_ -> {
                  p_280815_.active = false;
                  this.minecraft
                     .getReportingContext()
                     .draftReportHandled(this.minecraft, this, () -> this.minecraft.disconnectFromWorld(ClientLevel.DEFAULT_QUIT_MESSAGE), true);
               }
            )
            .width(204)
            .build(),
         2
      );
      gridlayout.arrangeElements();
      FrameLayout.alignInRectangle(gridlayout, 0, 0, this.width, this.height, 0.5F, 0.25F);
      gridlayout.visitWidgets(this::addRenderableWidget);
   }

   @Override
   public void tick() {
      if (this.rendersNowPlayingToast()) {
         NowPlayingToast.tickMusicNotes();
      }
   }

   @Override
   public void render(GuiGraphics p_281899_, int p_281431_, int p_283183_, float p_281435_) {
      super.render(p_281899_, p_281431_, p_283183_, p_281435_);
      if (this.rendersNowPlayingToast()) {
         NowPlayingToast.renderToast(p_281899_, this.font);
      }

      if (this.showPauseMenu && this.minecraft.getReportingContext().hasDraftReport() && this.disconnectButton != null) {
         p_281899_.blitSprite(
            RenderPipelines.GUI_TEXTURED,
            DRAFT_REPORT_SPRITE,
            this.disconnectButton.getX() + this.disconnectButton.getWidth() - 17,
            this.disconnectButton.getY() + 3,
            15,
            15
         );
      }
   }

   @Override
   public void renderBackground(GuiGraphics p_299656_, int p_297892_, int p_299995_, float p_300532_) {
      if (this.showPauseMenu) {
         super.renderBackground(p_299656_, p_297892_, p_299995_, p_300532_);
      }
   }

   public boolean rendersNowPlayingToast() {
      Options options = this.minecraft.options;
      return options.musicToast().get().renderInPauseScreen() && options.getFinalSoundSourceVolume(SoundSource.MUSIC) > 0.0F && this.showPauseMenu;
   }

   private Button openScreenButton(Component p_262567_, Supplier<Screen> p_262581_) {
      return Button.builder(p_262567_, p_280817_ -> this.minecraft.setScreen(p_262581_.get())).width(98).build();
   }
}
