package net.minecraft.client.gui.screens;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import dev.miru.gui.renderer.DebugOverlayRenderer;
import dev.miru.gui.toolbar.ToolbarBuilder;
import dev.miru.helper.HardwareInfoCache;
import dev.miru.helper.KitUtil;
import dev.miru.helper.ScreenChecker;
import dev.miru.main.ModMain;
import java.net.URI;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import net.minecraft.ChatFormatting;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.NarratorStatus;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.TabOrderedElement;
import net.minecraft.client.gui.components.events.AbstractContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.narration.ScreenNarrationCollector;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ServerboundCustomClickActionPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.Music;
import net.minecraft.util.Util;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public abstract class Screen extends AbstractContainerEventHandler implements Renderable {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Component USAGE_NARRATION = Component.translatable("narrator.screen.usage");
   public static final Identifier MENU_BACKGROUND = Identifier.withDefaultNamespace("textures/gui/menu_background.png");
   public static final Identifier HEADER_SEPARATOR = Identifier.withDefaultNamespace("textures/gui/header_separator.png");
   public static final Identifier FOOTER_SEPARATOR = Identifier.withDefaultNamespace("textures/gui/footer_separator.png");
   private static final Identifier INWORLD_MENU_BACKGROUND = Identifier.withDefaultNamespace("textures/gui/inworld_menu_background.png");
   public static final Identifier INWORLD_HEADER_SEPARATOR = Identifier.withDefaultNamespace("textures/gui/inworld_header_separator.png");
   public static final Identifier INWORLD_FOOTER_SEPARATOR = Identifier.withDefaultNamespace("textures/gui/inworld_footer_separator.png");
   protected static final float FADE_IN_TIME = 2000.0F;
   protected final Component title;
   private final List<GuiEventListener> children = Lists.newArrayList();
   private final List<NarratableEntry> narratables = Lists.newArrayList();
   protected final Minecraft minecraft;
   private boolean initialized;
   public int width;
   public int height;
   public final List<Renderable> renderables = Lists.newArrayList();
   protected final Font font;
   private static final long NARRATE_SUPPRESS_AFTER_INIT_TIME = TimeUnit.SECONDS.toMillis(2L);
   private static final long NARRATE_DELAY_NARRATOR_ENABLED = NARRATE_SUPPRESS_AFTER_INIT_TIME;
   private final ScreenNarrationCollector narrationState = new ScreenNarrationCollector();
   private long narrationSuppressTime = Long.MIN_VALUE;
   private long nextNarrationTime = Long.MAX_VALUE;
   protected @Nullable CycleButton<NarratorStatus> narratorButton;
   private @Nullable NarratableEntry lastNarratable;
   protected final Executor screenExecutor;
   private HardwareInfoCache hardwareInfoCache;
   private ScreenChecker screenChecker;
   public static boolean toolBarExpanded = true;
   private ToolbarBuilder toolbarBuilder;

   protected Screen(Component p_96550_) {
      this(Minecraft.getInstance(), Minecraft.getInstance().font, p_96550_);
   }

   protected Screen(Minecraft p_457548_, Font p_458138_, Component p_451539_) {
      this.minecraft = p_457548_;
      this.font = p_458138_;
      this.title = p_451539_;
      this.screenExecutor = p_448031_ -> p_457548_.execute(() -> {
         if (p_457548_.screen == this) {
            p_448031_.run();
         }
      });
      this.toolbarBuilder = new ToolbarBuilder(this, this.minecraft);
      this.hardwareInfoCache = new HardwareInfoCache();
      this.screenChecker = new ScreenChecker(this);
      if (ModMain.getOptions().getExpd_enableOperateMenuValue() && !this.screenChecker.isTypeableScreen()) {
         this.addRenderableWidget(
            KitUtil.button(
               Component.literal("Back to title screen(T)"),
               Component.literal("Focus open title screen"),
               btn -> this.minecraft.setScreen(new TitleScreen()),
               200,
               20,
               10,
               30
            )
         );
         this.addRenderableWidget(
            KitUtil.button(
               Component.literal("Focus quit game(Q)"), Component.literal("Exit game through System.exit(0)"), btn -> System.exit(0), 200, 20, 10, 50
            )
         );
      }
   }

   public Component getTitle() {
      return this.title;
   }

   public Component getNarrationMessage() {
      return this.getTitle();
   }

   public final void renderWithTooltipAndSubtitles(GuiGraphics p_430249_, int p_430834_, int p_428863_, float p_429482_) {
      p_430249_.nextStratum();
      this.renderBackground(p_430249_, p_430834_, p_428863_, p_429482_);
      p_430249_.nextStratum();
      this.render(p_430249_, p_430834_, p_428863_, p_429482_);
      p_430249_.renderDeferredElements();
   }

   @Override
   public void render(GuiGraphics p_281549_, int p_281550_, int p_282878_, float p_282465_) {
      int count = -1;
      boolean showDebugOverlay = ModMain.getOptions().getExpd_debugOverlayValue();
      if (showDebugOverlay) {
         p_281549_.drawString(
            this.font,
            this.getClass().getName().replaceAll("net.minecraft.client.gui.screens.", "Minecraft::").replaceAll("dev.miru.gui.screens.", "TerrainPlusPack::")
               + "(Typeable:%s)".formatted(this.screenChecker.isTypeableScreen()),
            10,
            10,
            -256
         );
         DebugOverlayRenderer debugOverlayRenderer = new DebugOverlayRenderer(p_281549_, this.minecraft);
         debugOverlayRenderer.renderMousePositionTenLine(p_281550_, p_282878_, -16711936, 2);
         debugOverlayRenderer.renderHardWareInfo(this.width - 400, 10, 10, 0, -1, -11513776, this.hardwareInfoCache);
      }

      for (Renderable renderable : this.renderables) {
         renderable.render(p_281549_, p_281550_, p_282878_, p_282465_);
         count++;
         if (showDebugOverlay) {
            p_281549_.drawString(
               this.font,
               Component.literal("[%02d] %s".formatted(count, renderable).replaceAll("net.minecraft.client.gui.", "Minecraft/"))
                  .withStyle(renderable instanceof Button ? (((Button)renderable).active ? ChatFormatting.WHITE : ChatFormatting.GRAY) : ChatFormatting.WHITE)
                  .withStyle(
                     renderable instanceof Button ? (((Button)renderable).visible ? ChatFormatting.WHITE : ChatFormatting.UNDERLINE) : ChatFormatting.WHITE
                  ),
               10,
               20 + count * 10,
               this.isFocused() && this.getFocused().equals(renderable) ? -16711936 : -1
            );
         }
      }

      if (!showDebugOverlay) {
         p_281549_.drawString(this.font, "Minecraft Terrain Plus Pack V1.1", 10, 10, -1);
         p_281549_.drawString(this.font, "By FLT_MIRU(bilibili.com), No relations to Mojang studio.", 10, 20, -1);
      }

      boolean shouldShowToolBar = p_282878_ > this.height - 20
         && !(this instanceof ChatScreen)
         && toolBarExpanded
         && ModMain.getOptions().getExpd_EnableToolBarValue();

      for (Button btn : this.toolbarBuilder.toolBarButtonList) {
         btn.active = shouldShowToolBar;
         btn.visible = shouldShowToolBar;
      }

      if (this.toolbarBuilder.toolBarExpandControlButton != null) {
         this.toolbarBuilder.toolBarExpandControlButton.active = p_282878_ > this.height - 20;
         this.toolbarBuilder.toolBarExpandControlButton.visible = p_282878_ > this.height - 20;
      }
   }

   @Override
   public boolean keyPressed(KeyEvent p_423266_) {
      boolean isKeyActionDisabledScreen = this.screenChecker.isTypeableScreen() || !ModMain.getOptions().getExpd_enableOperateMenuValue();
      if (p_423266_.key() == 84 && !isKeyActionDisabledScreen) {
         this.minecraft.setScreen(new TitleScreen());
         return true;
      }

      if (p_423266_.key() == 81 && !isKeyActionDisabledScreen) {
         System.exit(0);
         return true;
      }

      if (p_423266_.isEscape() && this.shouldCloseOnEsc()) {
         this.onClose();
         return true;
      }

      if (super.keyPressed(p_423266_)) {
         return true;
      }

      FocusNavigationEvent focusnavigationevent = (FocusNavigationEvent)(switch (p_423266_.key()) {
         case 258 -> this.createTabEvent(!p_423266_.hasShiftDown());
         default -> null;
         case 262 -> this.createArrowEvent(ScreenDirection.RIGHT);
         case 263 -> this.createArrowEvent(ScreenDirection.LEFT);
         case 264 -> this.createArrowEvent(ScreenDirection.DOWN);
         case 265 -> this.createArrowEvent(ScreenDirection.UP);
      });
      if (focusnavigationevent != null) {
         ComponentPath componentpath = super.nextFocusPath(focusnavigationevent);
         if (componentpath == null && focusnavigationevent instanceof FocusNavigationEvent.TabNavigation) {
            this.clearFocus();
            componentpath = super.nextFocusPath(focusnavigationevent);
         }

         if (componentpath != null) {
            this.changeFocus(componentpath);
         }
      }

      return false;
   }

   private FocusNavigationEvent.TabNavigation createTabEvent(boolean p_431575_) {
      return new FocusNavigationEvent.TabNavigation(p_431575_);
   }

   private FocusNavigationEvent.ArrowNavigation createArrowEvent(ScreenDirection p_265049_) {
      return new FocusNavigationEvent.ArrowNavigation(p_265049_);
   }

   protected void setInitialFocus() {
      if (this.minecraft.getLastInputType().isKeyboard()) {
         FocusNavigationEvent.TabNavigation focusnavigationevent$tabnavigation = new FocusNavigationEvent.TabNavigation(true);
         ComponentPath componentpath = super.nextFocusPath(focusnavigationevent$tabnavigation);
         if (componentpath != null) {
            this.changeFocus(componentpath);
         }
      }
   }

   protected void setInitialFocus(GuiEventListener p_265756_) {
      ComponentPath componentpath = ComponentPath.path(this, p_265756_.nextFocusPath(new FocusNavigationEvent.InitialFocus()));
      if (componentpath != null) {
         this.changeFocus(componentpath);
      }
   }

   public void clearFocus() {
      ComponentPath componentpath = this.getCurrentFocusPath();
      if (componentpath != null) {
         componentpath.applyFocus(false);
      }
   }

   @VisibleForTesting
   protected void changeFocus(ComponentPath p_265308_) {
      this.clearFocus();
      p_265308_.applyFocus(true);
   }

   public boolean shouldCloseOnEsc() {
      return true;
   }

   public void onClose() {
      this.minecraft.setScreen(null);
   }

   public <T extends GuiEventListener & Renderable & NarratableEntry> T addRenderableWidget(T p_169406_) {
      this.renderables.add(p_169406_);
      return this.addWidget(p_169406_);
   }

   protected <T extends Renderable> T addRenderableOnly(T p_254514_) {
      this.renderables.add(p_254514_);
      return p_254514_;
   }

   protected <T extends GuiEventListener & NarratableEntry> T addWidget(T p_96625_) {
      this.children.add(p_96625_);
      this.narratables.add(p_96625_);
      return p_96625_;
   }

   public void removeWidget(GuiEventListener p_169412_) {
      if (p_169412_ instanceof Renderable) {
         this.renderables.remove((Renderable)p_169412_);
      }

      if (p_169412_ instanceof NarratableEntry) {
         this.narratables.remove((NarratableEntry)p_169412_);
      }

      if (this.getFocused() == p_169412_) {
         this.clearFocus();
      }

      this.children.remove(p_169412_);
   }

   protected void clearWidgets() {
      this.renderables.clear();
      this.children.clear();
      this.narratables.clear();
   }

   public static List<Component> getTooltipFromItem(Minecraft p_281881_, ItemStack p_282833_) {
      return p_282833_.getTooltipLines(
         Item.TooltipContext.of(p_281881_.level),
         p_281881_.player,
         p_281881_.options.advancedItemTooltips ? TooltipFlag.Default.ADVANCED : TooltipFlag.Default.NORMAL
      );
   }

   protected void insertText(String p_96587_, boolean p_96588_) {
   }

   protected static void defaultHandleGameClickEvent(ClickEvent p_408337_, Minecraft p_407842_, @Nullable Screen p_407429_) {
      LocalPlayer localplayer = Objects.requireNonNull(p_407842_.player, "Player not available");
      switch (p_408337_) {
         case ClickEvent.RunCommand(String s):
            clickCommandAction(localplayer, s, p_407429_);
            break;
         case ClickEvent.ShowDialog clickevent$showdialog:
            localplayer.connection.showDialog(clickevent$showdialog.dialog(), p_407429_);
            break;
         case ClickEvent.Custom clickevent$custom:
            localplayer.connection.send(new ServerboundCustomClickActionPacket(clickevent$custom.id(), clickevent$custom.payload()));
            if (p_407842_.screen != p_407429_) {
               p_407842_.setScreen(p_407429_);
            }
            break;
         default:
            defaultHandleClickEvent(p_408337_, p_407842_, p_407429_);
      }
   }

   protected static void defaultHandleClickEvent(ClickEvent p_407893_, Minecraft p_410530_, @Nullable Screen p_407004_) {
      boolean flag = switch (p_407893_) {
         case ClickEvent.OpenUrl(URI uri) -> {
            clickUrlAction(p_410530_, p_407004_, uri);
            yield false;
         }
         case ClickEvent.OpenFile clickevent$openfile -> {
            Util.getPlatform().openFile(clickevent$openfile.file());
            yield true;
         }
         case ClickEvent.SuggestCommand(String s2) -> {
            String s1 = s2;
            if (p_407004_ != null) {
               p_407004_.insertText(s1, true);
            }

            yield true;
         }
         case ClickEvent.CopyToClipboard(String s) -> {
            p_410530_.keyboardHandler.setClipboard(s);
            yield true;
         }
         default -> {
            LOGGER.error("Don't know how to handle {}", p_407893_);
            yield true;
         }
      };
      if (flag && p_410530_.screen != p_407004_) {
         p_410530_.setScreen(p_407004_);
      }
   }

   protected static boolean clickUrlAction(Minecraft p_406192_, @Nullable Screen p_408543_, URI p_408598_) {
      if (!p_406192_.options.chatLinks().get()) {
         return false;
      }

      if (p_406192_.options.chatLinksPrompt().get()) {
         p_406192_.setScreen(new ConfirmLinkScreen(p_448029_ -> {
            if (p_448029_) {
               Util.getPlatform().openUri(p_408598_);
            }

            p_406192_.setScreen(p_408543_);
         }, p_408598_.toString(), false));
      } else {
         Util.getPlatform().openUri(p_408598_);
      }

      return true;
   }

   protected static void clickCommandAction(LocalPlayer p_409065_, String p_408291_, @Nullable Screen p_409951_) {
      p_409065_.connection.sendUnattendedCommand(Commands.trimOptionalPrefix(p_408291_), p_409951_);
   }

   public final void init(int p_96608_, int p_96609_) {
      this.width = p_96608_;
      this.height = p_96609_;
      if (!this.initialized) {
         this.init();
         this.setInitialFocus();
      } else {
         this.repositionElements();
      }

      this.initialized = true;
      this.triggerImmediateNarration(false);
      if (this.minecraft.getLastInputType().isKeyboard()) {
         this.setNarrationSuppressTime(Long.MAX_VALUE);
      } else {
         this.suppressNarration(NARRATE_SUPPRESS_AFTER_INIT_TIME);
      }

      this.toolbarBuilder.buildToolbar();
   }

   protected void rebuildWidgets() {
      this.clearWidgets();
      this.clearFocus();
      this.init();
      this.setInitialFocus();
   }

   protected void fadeWidgets(float p_409453_) {
      for (GuiEventListener guieventlistener : this.children()) {
         if (guieventlistener instanceof AbstractWidget abstractwidget) {
            abstractwidget.setAlpha(p_409453_);
         }
      }
   }

   @Override
   public List<? extends GuiEventListener> children() {
      return this.children;
   }

   protected void init() {
   }

   public void tick() {
   }

   public void removed() {
   }

   public void added() {
   }

   public void renderBackground(GuiGraphics p_283688_, int p_299421_, int p_298679_, float p_297268_) {
      if (this.isInGameUi()) {
         this.renderTransparentBackground(p_283688_);
      } else {
         if (this.minecraft.level == null) {
            this.renderPanorama(p_283688_, p_297268_);
         }

         this.renderBlurredBackground(p_283688_);
         this.renderMenuBackground(p_283688_);
      }

      this.minecraft.gui.renderDeferredSubtitles();
   }

   protected void renderBlurredBackground(GuiGraphics p_407253_) {
      float f = this.minecraft.options.getMenuBackgroundBlurriness();
      if (f >= 1.0F) {
         p_407253_.blurBeforeThisStratum();
      }
   }

   protected void renderPanorama(GuiGraphics p_332550_, float p_335227_) {
      this.minecraft.gameRenderer.getPanorama().render(p_332550_, this.width, this.height, this.panoramaShouldSpin());
   }

   protected void renderMenuBackground(GuiGraphics p_332667_) {
      this.renderMenuBackground(p_332667_, 0, 0, this.width, this.height);
   }

   protected void renderMenuBackground(GuiGraphics p_334761_, int p_328355_, int p_328091_, int p_332954_, int p_331811_) {
      renderMenuBackgroundTexture(
         p_334761_, this.minecraft.level == null ? MENU_BACKGROUND : INWORLD_MENU_BACKGROUND, p_328355_, p_328091_, 0.0F, 0.0F, p_332954_, p_331811_
      );
   }

   public static void renderMenuBackgroundTexture(
      GuiGraphics p_331670_, Identifier p_458732_, int p_332491_, int p_335034_, float p_330279_, float p_334888_, int p_331386_, int p_330145_
   ) {
      int i = 32;
      p_331670_.blit(RenderPipelines.GUI_TEXTURED, p_458732_, p_332491_, p_335034_, p_330279_, p_334888_, p_331386_, p_330145_, 32, 32);
   }

   public void renderTransparentBackground(GuiGraphics p_300203_) {
      p_300203_.fillGradient(0, 0, this.width, this.height, -1072689136, -804253680);
   }

   public boolean isPauseScreen() {
      return true;
   }

   public boolean isInGameUi() {
      return false;
   }

   protected boolean panoramaShouldSpin() {
      return true;
   }

   public boolean isAllowedInPortal() {
      return this.isPauseScreen();
   }

   protected void repositionElements() {
      this.rebuildWidgets();
   }

   public void resize(int p_96576_, int p_96577_) {
      this.width = p_96576_;
      this.height = p_96577_;
      this.repositionElements();
      this.toolbarBuilder.clearToolBar();
   }

   public void fillCrashDetails(CrashReport p_363781_) {
      CrashReportCategory crashreportcategory = p_363781_.addCategory("Affected screen", 1);
      crashreportcategory.setDetail("Screen name", () -> this.getClass().getCanonicalName());
   }

   protected boolean isValidCharacterForName(String p_96584_, int p_96586_, int p_422770_) {
      int i = p_96584_.indexOf(58);
      int j = p_96584_.indexOf(47);
      if (p_96586_ == 58) {
         return (j == -1 || p_422770_ <= j) && i == -1;
      } else {
         return p_96586_ == 47
            ? p_422770_ > i
            : p_96586_ == 95 || p_96586_ == 45 || p_96586_ >= 97 && p_96586_ <= 122 || p_96586_ >= 48 && p_96586_ <= 57 || p_96586_ == 46;
      }
   }

   @Override
   public boolean isMouseOver(double p_96595_, double p_96596_) {
      return true;
   }

   public void onFilesDrop(List<Path> p_96591_) {
   }

   private void scheduleNarration(long p_169381_, boolean p_169382_) {
      this.nextNarrationTime = Util.getMillis() + p_169381_;
      if (p_169382_) {
         this.narrationSuppressTime = Long.MIN_VALUE;
      }
   }

   private void suppressNarration(long p_169379_) {
      this.setNarrationSuppressTime(Util.getMillis() + p_169379_);
   }

   private void setNarrationSuppressTime(long p_427798_) {
      this.narrationSuppressTime = p_427798_;
   }

   public void afterMouseMove() {
      this.scheduleNarration(750L, false);
   }

   public void afterMouseAction() {
      this.scheduleNarration(200L, true);
   }

   public void afterKeyboardAction() {
      this.scheduleNarration(200L, true);
   }

   private boolean shouldRunNarration() {
      return SharedConstants.DEBUG_UI_NARRATION || this.minecraft.getNarrator().isActive();
   }

   public void handleDelayedNarration() {
      if (this.shouldRunNarration()) {
         long i = Util.getMillis();
         if (i > this.nextNarrationTime && i > this.narrationSuppressTime) {
            this.runNarration(true);
            this.nextNarrationTime = Long.MAX_VALUE;
         }
      }
   }

   public void triggerImmediateNarration(boolean p_169408_) {
      if (this.shouldRunNarration()) {
         this.runNarration(p_169408_);
      }
   }

   private void runNarration(boolean p_169410_) {
      this.narrationState.update(this::updateNarrationState);
      String s = this.narrationState.collectNarrationText(!p_169410_);
      if (!s.isEmpty()) {
         this.minecraft.getNarrator().saySystemNow(s);
      }
   }

   protected boolean shouldNarrateNavigation() {
      return true;
   }

   protected void updateNarrationState(NarrationElementOutput p_169396_) {
      p_169396_.add(NarratedElementType.TITLE, this.getNarrationMessage());
      if (this.shouldNarrateNavigation()) {
         p_169396_.add(NarratedElementType.USAGE, USAGE_NARRATION);
      }

      this.updateNarratedWidget(p_169396_);
   }

   protected void updateNarratedWidget(NarrationElementOutput p_169403_) {
      List<? extends NarratableEntry> list = this.narratables
         .stream()
         .flatMap(p_374575_ -> p_374575_.getNarratables().stream())
         .filter(NarratableEntry::isActive)
         .sorted(Comparator.comparingInt(TabOrderedElement::getTabOrderGroup))
         .toList();
      Screen.NarratableSearchResult screen$narratablesearchresult = findNarratableWidget(list, this.lastNarratable);
      if (screen$narratablesearchresult != null) {
         if (screen$narratablesearchresult.priority.isTerminal()) {
            this.lastNarratable = screen$narratablesearchresult.entry;
         }

         if (list.size() > 1) {
            p_169403_.add(
               NarratedElementType.POSITION, Component.translatable("narrator.position.screen", screen$narratablesearchresult.index + 1, list.size())
            );
            if (screen$narratablesearchresult.priority == NarratableEntry.NarrationPriority.FOCUSED) {
               p_169403_.add(NarratedElementType.USAGE, this.getUsageNarration());
            }
         }

         screen$narratablesearchresult.entry.updateNarration(p_169403_.nest());
      }
   }

   protected Component getUsageNarration() {
      return Component.translatable("narration.component_list.usage");
   }

   public static Screen.@Nullable NarratableSearchResult findNarratableWidget(List<? extends NarratableEntry> p_169401_, @Nullable NarratableEntry p_169402_) {
      Screen.NarratableSearchResult screen$narratablesearchresult = null;
      Screen.NarratableSearchResult screen$narratablesearchresult1 = null;
      int i = 0;

      for (int j = p_169401_.size(); i < j; i++) {
         NarratableEntry narratableentry = p_169401_.get(i);
         NarratableEntry.NarrationPriority narratableentry$narrationpriority = narratableentry.narrationPriority();
         if (narratableentry$narrationpriority.isTerminal()) {
            if (narratableentry != p_169402_) {
               return new Screen.NarratableSearchResult(narratableentry, i, narratableentry$narrationpriority);
            }

            screen$narratablesearchresult1 = new Screen.NarratableSearchResult(narratableentry, i, narratableentry$narrationpriority);
         } else if (narratableentry$narrationpriority.compareTo(
               screen$narratablesearchresult != null ? screen$narratablesearchresult.priority : NarratableEntry.NarrationPriority.NONE
            )
            > 0) {
            screen$narratablesearchresult = new Screen.NarratableSearchResult(narratableentry, i, narratableentry$narrationpriority);
         }
      }

      return screen$narratablesearchresult != null ? screen$narratablesearchresult : screen$narratablesearchresult1;
   }

   public void updateNarratorStatus(boolean p_345154_) {
      if (p_345154_) {
         this.scheduleNarration(NARRATE_DELAY_NARRATOR_ENABLED, false);
      }

      if (this.narratorButton != null) {
         this.narratorButton.setValue(this.minecraft.options.narrator().get());
      }
   }

   public Font getFont() {
      return this.font;
   }

   public boolean showsActiveEffects() {
      return false;
   }

   public boolean canInterruptWithAnotherScreen() {
      return this.shouldCloseOnEsc();
   }

   @Override
   public ScreenRectangle getRectangle() {
      return new ScreenRectangle(0, 0, this.width, this.height);
   }

   public @Nullable Music getBackgroundMusic() {
      return null;
   }

   @OnlyIn(Dist.CLIENT)
   public record NarratableSearchResult(NarratableEntry entry, int index, NarratableEntry.NarrationPriority priority) {
   }
}
