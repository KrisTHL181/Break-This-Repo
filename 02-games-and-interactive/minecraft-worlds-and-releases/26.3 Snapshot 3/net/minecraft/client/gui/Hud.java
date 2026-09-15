package net.minecraft.client.gui;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Ordering;
import com.mojang.blaze3d.platform.Window;
import com.mojang.datafixers.util.Pair;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.Optionull;
import net.minecraft.client.AttackIndicatorStatus;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.BossHealthOverlay;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.gui.components.SubtitleOverlay;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.client.gui.components.spectator.SpectatorGui;
import net.minecraft.client.gui.contextualbar.ContextualBar;
import net.minecraft.client.gui.contextualbar.ExperienceBar;
import net.minecraft.client.gui.contextualbar.JumpableVehicleBar;
import net.minecraft.client.gui.contextualbar.LocatorBar;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.Lightmap;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.WaypointStyleManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.numbers.NumberFormat;
import net.minecraft.network.chat.numbers.StyledFormat;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringUtil;
import net.minecraft.util.Util;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PlayerRideableJumping;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.AttackRange;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.TeamColor;
import org.jspecify.annotations.Nullable;

public class Hud {
   private static final Identifier CROSSHAIR_SPRITE = Identifier.withDefaultNamespace("hud/crosshair");
   private static final Identifier CROSSHAIR_ATTACK_INDICATOR_FULL_SPRITE = Identifier.withDefaultNamespace("hud/crosshair_attack_indicator_full");
   private static final Identifier CROSSHAIR_ATTACK_INDICATOR_BACKGROUND_SPRITE = Identifier.withDefaultNamespace("hud/crosshair_attack_indicator_background");
   private static final Identifier CROSSHAIR_ATTACK_INDICATOR_PROGRESS_SPRITE = Identifier.withDefaultNamespace("hud/crosshair_attack_indicator_progress");
   private static final Identifier EFFECT_BACKGROUND_AMBIENT_SPRITE = Identifier.withDefaultNamespace("hud/effect_background_ambient");
   private static final Identifier EFFECT_BACKGROUND_SPRITE = Identifier.withDefaultNamespace("hud/effect_background");
   private static final Identifier HOTBAR_SPRITE = Identifier.withDefaultNamespace("hud/hotbar");
   private static final Identifier HOTBAR_SELECTION_SPRITE = Identifier.withDefaultNamespace("hud/hotbar_selection");
   private static final Identifier HOTBAR_OFFHAND_LEFT_SPRITE = Identifier.withDefaultNamespace("hud/hotbar_offhand_left");
   private static final Identifier HOTBAR_OFFHAND_RIGHT_SPRITE = Identifier.withDefaultNamespace("hud/hotbar_offhand_right");
   private static final Identifier HOTBAR_ATTACK_INDICATOR_BACKGROUND_SPRITE = Identifier.withDefaultNamespace("hud/hotbar_attack_indicator_background");
   private static final Identifier HOTBAR_ATTACK_INDICATOR_PROGRESS_SPRITE = Identifier.withDefaultNamespace("hud/hotbar_attack_indicator_progress");
   private static final Identifier ARMOR_EMPTY_SPRITE = Identifier.withDefaultNamespace("hud/armor_empty");
   private static final Identifier ARMOR_HALF_SPRITE = Identifier.withDefaultNamespace("hud/armor_half");
   private static final Identifier ARMOR_FULL_SPRITE = Identifier.withDefaultNamespace("hud/armor_full");
   private static final Identifier FOOD_EMPTY_HUNGER_SPRITE = Identifier.withDefaultNamespace("hud/food_empty_hunger");
   private static final Identifier FOOD_HALF_HUNGER_SPRITE = Identifier.withDefaultNamespace("hud/food_half_hunger");
   private static final Identifier FOOD_FULL_HUNGER_SPRITE = Identifier.withDefaultNamespace("hud/food_full_hunger");
   private static final Identifier FOOD_EMPTY_SPRITE = Identifier.withDefaultNamespace("hud/food_empty");
   private static final Identifier FOOD_HALF_SPRITE = Identifier.withDefaultNamespace("hud/food_half");
   private static final Identifier FOOD_FULL_SPRITE = Identifier.withDefaultNamespace("hud/food_full");
   private static final Identifier AIR_SPRITE = Identifier.withDefaultNamespace("hud/air");
   private static final Identifier AIR_POPPING_SPRITE = Identifier.withDefaultNamespace("hud/air_bursting");
   private static final Identifier AIR_EMPTY_SPRITE = Identifier.withDefaultNamespace("hud/air_empty");
   private static final Identifier HEART_VEHICLE_CONTAINER_SPRITE = Identifier.withDefaultNamespace("hud/heart/vehicle_container");
   private static final Identifier HEART_VEHICLE_FULL_SPRITE = Identifier.withDefaultNamespace("hud/heart/vehicle_full");
   private static final Identifier HEART_VEHICLE_HALF_SPRITE = Identifier.withDefaultNamespace("hud/heart/vehicle_half");
   private static final Identifier VIGNETTE_LOCATION = Identifier.withDefaultNamespace("textures/misc/vignette.png");
   public static final Identifier NAUSEA_LOCATION = Identifier.withDefaultNamespace("textures/misc/nausea.png");
   private static final Identifier SPYGLASS_SCOPE_LOCATION = Identifier.withDefaultNamespace("textures/misc/spyglass_scope.png");
   private static final Identifier POWDER_SNOW_OUTLINE_LOCATION = Identifier.withDefaultNamespace("textures/misc/powder_snow_outline.png");
   private static final Comparator<PlayerScoreEntry> SCORE_DISPLAY_ORDER = Comparator.comparing(PlayerScoreEntry::value)
      .reversed()
      .thenComparing(PlayerScoreEntry::owner, String.CASE_INSENSITIVE_ORDER);
   private static final Component DEMO_EXPIRED_TEXT = Component.translatable("demo.demoExpired");
   private static final Component SAVING_TEXT = Component.translatable("menu.savingLevel");
   private static final float MIN_CROSSHAIR_ATTACK_SPEED = 5.0F;
   private static final int EXPERIENCE_BAR_DISPLAY_TICKS = 100;
   private static final int NUM_HEARTS_PER_ROW = 10;
   private static final int LINE_HEIGHT = 10;
   private static final String SPACER = ": ";
   private static final float PORTAL_OVERLAY_ALPHA_MIN = 0.2F;
   private static final int HEART_SIZE = 9;
   private static final int HEART_SEPARATION = 8;
   private static final int NUM_AIR_BUBBLES = 10;
   private static final int AIR_BUBBLE_SIZE = 9;
   private static final int AIR_BUBBLE_SEPERATION = 8;
   private static final int AIR_BUBBLE_POPPING_DURATION = 2;
   private static final int EMPTY_AIR_BUBBLE_DELAY_DURATION = 1;
   private static final float AIR_BUBBLE_POP_SOUND_VOLUME_BASE = 0.5F;
   private static final float AIR_BUBBLE_POP_SOUND_VOLUME_INCREMENT = 0.1F;
   private static final float AIR_BUBBLE_POP_SOUND_PITCH_BASE = 1.0F;
   private static final float AIR_BUBBLE_POP_SOUND_PITCH_INCREMENT = 0.1F;
   private static final int NUM_AIR_BUBBLE_POPPED_BEFORE_SOUND_VOLUME_INCREASE = 3;
   private static final int NUM_AIR_BUBBLE_POPPED_BEFORE_SOUND_PITCH_INCREASE = 5;
   private static final float AUTOSAVE_FADE_SPEED_FACTOR = 0.2F;
   private static final int SAVING_INDICATOR_WIDTH_PADDING_RIGHT = 5;
   private static final int SAVING_INDICATOR_HEIGHT_PADDING_BOTTOM = 5;
   private final RandomSource random = RandomSource.create();
   private final Minecraft minecraft;
   private final ChatComponent chat;
   private final WaypointStyleManager waypointStyles = new WaypointStyleManager();
   private boolean isHidden;
   private int tickCount;
   private @Nullable Component overlayMessageString;
   private int overlayMessageTime;
   private boolean animateOverlayMessageColor;
   public float vignetteBrightness = 1.0F;
   private int toolHighlightTimer;
   private ItemStack lastToolHighlight = ItemStack.EMPTY;
   private final DebugScreenOverlay debugOverlay;
   private final SubtitleOverlay subtitleOverlay;
   private final SpectatorGui spectatorGui;
   private final PlayerTabOverlay tabList;
   private final BossHealthOverlay bossOverlay;
   private int titleTime;
   private @Nullable Component title;
   private @Nullable Component subtitle;
   private int titleFadeInTime;
   private int titleStayTime;
   private int titleFadeOutTime;
   private int lastHealth;
   private int displayHealth;
   private long lastHealthTime;
   private long healthBlinkTime;
   private int lastBubblePopSoundPlayed;
   private @Nullable Runnable deferredSubtitles;
   private float autosaveIndicatorValue;
   private float lastAutosaveIndicatorValue;
   private Pair<Hud.ContextualInfo, ContextualBar> contextualInfoBar = Pair.of(Hud.ContextualInfo.EMPTY, ContextualBar.EMPTY);
   private final Map<Hud.ContextualInfo, Supplier<ContextualBar>> contextualInfoBars;
   private float scopeScale;

   public Hud(final Minecraft minecraft) {
      this.minecraft = minecraft;
      this.debugOverlay = new DebugScreenOverlay(minecraft);
      this.spectatorGui = new SpectatorGui(minecraft);
      this.chat = new ChatComponent(minecraft);
      this.tabList = new PlayerTabOverlay(minecraft, this);
      this.bossOverlay = new BossHealthOverlay(minecraft);
      this.subtitleOverlay = new SubtitleOverlay(minecraft);
      this.contextualInfoBars = ImmutableMap.of(
         Hud.ContextualInfo.EMPTY,
         (Supplier<ContextualBar>)() -> ContextualBar.EMPTY,
         Hud.ContextualInfo.EXPERIENCE,
         (Supplier<ContextualBar>)() -> new ExperienceBar(minecraft),
         Hud.ContextualInfo.LOCATOR,
         (Supplier<ContextualBar>)() -> new LocatorBar(minecraft),
         Hud.ContextualInfo.JUMPABLE_VEHICLE,
         (Supplier<ContextualBar>)() -> new JumpableVehicleBar(minecraft)
      );
      this.resetTitleTimes();
   }

   public void registerReloadListeners(final ReloadableResourceManager resourceManager) {
      resourceManager.registerReloadListener(this.waypointStyles);
   }

   public void toggle() {
      this.isHidden = !this.isHidden;
   }

   public boolean isHidden() {
      return this.isHidden;
   }

   public void resetTitleTimes() {
      this.titleFadeInTime = 10;
      this.titleStayTime = 70;
      this.titleFadeOutTime = 20;
   }

   public void extractRenderState(final GuiGraphicsExtractor graphics, final DeltaTracker deltaTracker) {
      this.minecraft.gameRenderer.gameRenderState().guiRenderState.isHudHidden = this.isHidden;
      if (!(this.minecraft.gui.screen() instanceof LevelLoadingScreen)) {
         if (!this.isHidden) {
            this.extractCameraOverlays(graphics, deltaTracker);
            this.extractCrosshair(graphics, deltaTracker);
            graphics.nextStratum();
            this.extractHotbarAndDecorations(graphics, deltaTracker);
            this.extractEffects(graphics, deltaTracker);
            this.extractBossOverlay(graphics, deltaTracker);
         }

         this.extractSleepOverlay(graphics, deltaTracker);
         if (!this.isHidden) {
            this.extractDemoOverlay(graphics, deltaTracker);
            this.extractScoreboardSidebar(graphics, deltaTracker);
            this.extractOverlayMessage(graphics, deltaTracker);
            this.extractTitle(graphics, deltaTracker);
            this.extractChat(graphics, deltaTracker);
            this.extractTabList(graphics, deltaTracker);
            this.extractSubtitleOverlay(graphics, this.minecraft.gui.screen() == null || this.minecraft.gui.screen().isInGameUi());
         } else if (this.minecraft.gui.screen() != null && this.minecraft.gui.screen().isInGameUi()) {
            this.extractSubtitleOverlay(graphics, true);
         }
      }
   }

   private void extractBossOverlay(final GuiGraphicsExtractor graphics, final DeltaTracker deltaTracker) {
      this.bossOverlay.extractRenderState(graphics);
   }

   public void extractDebugOverlay(final GuiGraphicsExtractor graphics) {
      this.debugOverlay.extractRenderState(graphics);
   }

   private void extractSubtitleOverlay(final GuiGraphicsExtractor graphics, final boolean deferRendering) {
      if (deferRendering) {
         this.deferredSubtitles = () -> this.subtitleOverlay.extractRenderState(graphics);
      } else {
         this.deferredSubtitles = null;
         this.subtitleOverlay.extractRenderState(graphics);
      }
   }

   public void extractDeferredSubtitles() {
      if (this.deferredSubtitles != null) {
         this.deferredSubtitles.run();
         this.deferredSubtitles = null;
      }
   }

   private void extractCameraOverlays(final GuiGraphicsExtractor graphics, final DeltaTracker deltaTracker) {
      if (this.minecraft.options.vignette().get()) {
         this.extractVignette(graphics, this.minecraft.getCameraEntity());
      }

      LocalPlayer player = this.minecraft.player;
      float gameTimeDeltaTicks = deltaTracker.getGameTimeDeltaTicks();
      this.scopeScale = Mth.lerp(0.5F * gameTimeDeltaTicks, this.scopeScale, 1.125F);
      if (this.minecraft.options.getCameraType().isFirstPerson()) {
         if (player.isScoping()) {
            this.extractSpyglassOverlay(graphics, this.scopeScale);
         } else {
            this.scopeScale = 0.5F;

            for (EquipmentSlot slot : EquipmentSlot.values()) {
               ItemStack item = player.getItemBySlot(slot);
               Equippable equippable = item.get(DataComponents.EQUIPPABLE);
               if (equippable != null && equippable.slot() == slot && equippable.cameraOverlay().isPresent()) {
                  this.extractTextureOverlay(graphics, equippable.cameraOverlay().get().withPath(p -> "textures/" + p + ".png"), 1.0F);
               }
            }
         }
      }

      if (player.getTicksFrozen() > 0) {
         this.extractTextureOverlay(graphics, POWDER_SNOW_OUTLINE_LOCATION, player.getPercentFrozen());
      }

      float partialTicks = deltaTracker.getGameTimeDeltaPartialTick(false);
      float portalIntensity = Mth.lerp(partialTicks, player.oPortalEffectIntensity, player.portalEffectIntensity);
      float nauseaIntensity = player.getEffectBlendFactor(MobEffects.NAUSEA, partialTicks);
      if (portalIntensity > 0.0F) {
         this.extractPortalOverlay(graphics, portalIntensity);
      } else if (nauseaIntensity > 0.0F) {
         float screenEffectScale = this.minecraft.options.screenEffectScale().get().floatValue();
         if (screenEffectScale < 1.0F) {
            float overlayStrength = nauseaIntensity * (1.0F - screenEffectScale);
            this.extractConfusionOverlay(graphics, overlayStrength);
         }
      }
   }

   private void extractSleepOverlay(final GuiGraphicsExtractor graphics, final DeltaTracker deltaTracker) {
      if (this.minecraft.player.getSleepTimer() > 0) {
         Profiler.get().push("sleep");
         graphics.nextStratum();
         float sleepTimer = this.minecraft.player.getSleepTimer();
         float amount = sleepTimer / 100.0F;
         if (amount > 1.0F) {
            amount = 1.0F - (sleepTimer - 100.0F) / 10.0F;
         }

         int color = (int)(220.0F * amount) << 24 | 1052704;
         graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), color);
         Profiler.get().pop();
      }
   }

   private void extractOverlayMessage(final GuiGraphicsExtractor graphics, final DeltaTracker deltaTracker) {
      Font font = this.getFont();
      if (this.overlayMessageString != null && this.overlayMessageTime > 0) {
         Profiler.get().push("overlayMessage");
         float t = this.overlayMessageTime - deltaTracker.getGameTimeDeltaPartialTick(false);
         int alpha = (int)(t * 255.0F / 20.0F);
         if (alpha > 255) {
            alpha = 255;
         }

         if (alpha > 0) {
            graphics.nextStratum();
            graphics.pose().pushMatrix();
            graphics.pose().translate(graphics.guiWidth() / 2, graphics.guiHeight() - 68);
            int color;
            if (this.animateOverlayMessageColor) {
               color = Mth.hsvToArgb(t / 50.0F, 0.7F, 0.6F, alpha);
            } else {
               color = ARGB.white(alpha);
            }

            int width = font.width(this.overlayMessageString);
            graphics.textWithBackdrop(font, this.overlayMessageString, -width / 2, -4, width, color);
            graphics.pose().popMatrix();
         }

         Profiler.get().pop();
      }
   }

   private void extractTitle(final GuiGraphicsExtractor graphics, final DeltaTracker deltaTracker) {
      if (this.title != null && this.titleTime > 0) {
         Font font = this.getFont();
         Profiler.get().push("titleAndSubtitle");
         float t = this.titleTime - deltaTracker.getGameTimeDeltaPartialTick(false);
         int alpha = 255;
         if (this.titleTime > this.titleFadeOutTime + this.titleStayTime) {
            float time = this.titleFadeInTime + this.titleStayTime + this.titleFadeOutTime - t;
            alpha = (int)(time * 255.0F / this.titleFadeInTime);
         }

         if (this.titleTime <= this.titleFadeOutTime) {
            alpha = (int)(t * 255.0F / this.titleFadeOutTime);
         }

         alpha = Mth.clamp(alpha, 0, 255);
         if (alpha > 0) {
            graphics.nextStratum();
            graphics.pose().pushMatrix();
            graphics.pose().translate(graphics.guiWidth() / 2, graphics.guiHeight() / 2);
            graphics.pose().pushMatrix();
            graphics.pose().scale(4.0F, 4.0F);
            int titleWidth = font.width(this.title);
            int textColor = ARGB.white(alpha);
            graphics.textWithBackdrop(font, this.title, -titleWidth / 2, -10, titleWidth, textColor);
            graphics.pose().popMatrix();
            if (this.subtitle != null) {
               graphics.pose().pushMatrix();
               graphics.pose().scale(2.0F, 2.0F);
               int subtitleWidth = font.width(this.subtitle);
               graphics.textWithBackdrop(font, this.subtitle, -subtitleWidth / 2, 5, subtitleWidth, textColor);
               graphics.pose().popMatrix();
            }

            graphics.pose().popMatrix();
         }

         Profiler.get().pop();
      }
   }

   private void extractChat(final GuiGraphicsExtractor graphics, final DeltaTracker deltaTracker) {
      if (this.minecraft.player != null && !this.chat.isChatFocused()) {
         Window window = this.minecraft.getWindow();
         int mouseX = Mth.floor(this.minecraft.mouseHandler.getScaledXPos(window));
         int mouseY = Mth.floor(this.minecraft.mouseHandler.getScaledYPos(window));
         graphics.nextStratum();
         this.chat.extractRenderState(graphics, this.getFont(), this.tickCount, mouseX, mouseY, ChatComponent.DisplayMode.BACKGROUND, false);
      }
   }

   private void extractScoreboardSidebar(final GuiGraphicsExtractor graphics, final DeltaTracker deltaTracker) {
      Scoreboard scoreboard = this.minecraft.level.getScoreboard();
      Objective teamObjective = null;
      PlayerTeam playerTeam = scoreboard.getPlayersTeam(this.minecraft.player.getScoreboardName());
      if (playerTeam != null) {
         Optional<TeamColor> teamColor = playerTeam.getColor();
         if (teamColor.isPresent()) {
            teamObjective = scoreboard.getDisplayObjective(teamColor.get().displaySlot());
         }
      }

      Objective displayObjective = teamObjective != null ? teamObjective : scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
      if (displayObjective != null) {
         graphics.nextStratum();
         this.displayScoreboardSidebar(graphics, displayObjective);
      }
   }

   private void extractTabList(final GuiGraphicsExtractor graphics, final DeltaTracker deltaTracker) {
      Scoreboard scoreboard = this.minecraft.level.getScoreboard();
      Objective displayObjective = scoreboard.getDisplayObjective(DisplaySlot.LIST);
      if (!this.minecraft.options.keyPlayerList.isDown()
         || this.minecraft.isLocalServer() && this.minecraft.player.connection.getListedOnlinePlayers().size() <= 1 && displayObjective == null) {
         this.tabList.setVisible(false);
      } else {
         this.tabList.setVisible(true);
         graphics.nextStratum();
         this.tabList.extractRenderState(graphics, graphics.guiWidth(), scoreboard, displayObjective);
      }
   }

   private void extractCrosshair(final GuiGraphicsExtractor graphics, final DeltaTracker deltaTracker) {
      Options options = this.minecraft.options;
      if (options.getCameraType().isFirstPerson()) {
         if (this.minecraft.gameMode.getPlayerMode() != GameType.SPECTATOR || this.canRenderCrosshairForSpectator(this.minecraft.hitResult)) {
            if (!this.minecraft.debugEntries.isCurrentlyEnabled(DebugScreenEntries.THREE_DIMENSIONAL_CROSSHAIR)) {
               graphics.nextStratum();
               int size = 15;
               graphics.blitSprite(RenderPipelines.CROSSHAIR, CROSSHAIR_SPRITE, (graphics.guiWidth() - 15) / 2, (graphics.guiHeight() - 15) / 2, 15, 15);
               if (this.minecraft.options.attackIndicator().get() == AttackIndicatorStatus.CROSSHAIR) {
                  float attackStrengthScale = this.minecraft.player.getAttackStrengthScale(0.0F);
                  boolean renderMaxAttackIndicator = false;
                  if (this.minecraft.crosshairPickEntity != null && this.minecraft.crosshairPickEntity instanceof LivingEntity && attackStrengthScale >= 1.0F) {
                     renderMaxAttackIndicator = this.minecraft.player.getCurrentItemAttackStrengthDelay() > 5.0F;
                     renderMaxAttackIndicator &= this.minecraft.crosshairPickEntity.isAlive();
                     AttackRange attackRange = this.minecraft.player.getActiveItem().get(DataComponents.ATTACK_RANGE);
                     renderMaxAttackIndicator &= attackRange == null || attackRange.isInRange(this.minecraft.player, this.minecraft.hitResult.getLocation());
                  }

                  int y = graphics.guiHeight() / 2 - 7 + 16;
                  int x = graphics.guiWidth() / 2 - 8;
                  if (renderMaxAttackIndicator) {
                     graphics.blitSprite(RenderPipelines.CROSSHAIR, CROSSHAIR_ATTACK_INDICATOR_FULL_SPRITE, x, y, 16, 16);
                  } else if (attackStrengthScale < 1.0F) {
                     int progress = (int)(attackStrengthScale * 17.0F);
                     graphics.blitSprite(RenderPipelines.CROSSHAIR, CROSSHAIR_ATTACK_INDICATOR_BACKGROUND_SPRITE, x, y, 16, 4);
                     graphics.blitSprite(RenderPipelines.CROSSHAIR, CROSSHAIR_ATTACK_INDICATOR_PROGRESS_SPRITE, 16, 4, 0, 0, x, y, progress, 4);
                  }
               }
            }
         }
      }
   }

   private boolean canRenderCrosshairForSpectator(final @Nullable HitResult hitResult) {
      if (hitResult == null) {
         return false;
      } else if (hitResult.getType() == HitResult.Type.ENTITY) {
         return ((EntityHitResult)hitResult).getEntity() instanceof MenuProvider;
      } else if (hitResult.getType() == HitResult.Type.BLOCK) {
         BlockPos pos = ((BlockHitResult)hitResult).getBlockPos();
         Level level = this.minecraft.level;
         return level.getBlockState(pos).getMenuProvider(level, pos) != null;
      } else {
         return false;
      }
   }

   private void extractEffects(final GuiGraphicsExtractor graphics, final DeltaTracker deltaTracker) {
      Collection<MobEffectInstance> activeEffects = this.minecraft.player.getActiveEffects();
      if (!activeEffects.isEmpty() && (this.minecraft.gui.screen() == null || !this.minecraft.gui.screen().showsActiveEffects())) {
         int beneficialCount = 0;
         int harmfulCount = 0;

         for (MobEffectInstance instance : Ordering.natural().reverse().sortedCopy(activeEffects)) {
            Holder<MobEffect> effect = instance.getEffect();
            if (instance.showIcon()) {
               int x = graphics.guiWidth();
               int y = 1;
               if (this.minecraft.isDemo()) {
                  y += 15;
               }

               if (effect.value().isBeneficial()) {
                  beneficialCount++;
                  x -= 25 * beneficialCount;
               } else {
                  harmfulCount++;
                  x -= 25 * harmfulCount;
                  y += 26;
               }

               float alpha = 1.0F;
               if (instance.isAmbient()) {
                  graphics.blitSprite(RenderPipelines.GUI_TEXTURED, EFFECT_BACKGROUND_AMBIENT_SPRITE, x, y, 24, 24);
               } else {
                  graphics.blitSprite(RenderPipelines.GUI_TEXTURED, EFFECT_BACKGROUND_SPRITE, x, y, 24, 24);
                  if (instance.endsWithin(200)) {
                     int remainingDuration = instance.getDuration();
                     int usedSeconds = 10 - remainingDuration / 20;
                     alpha = Mth.clamp(remainingDuration / 10.0F / 5.0F * 0.5F, 0.0F, 0.5F)
                        + Mth.cos(remainingDuration * (float) Math.PI / 5.0F) * Mth.clamp(usedSeconds / 10.0F * 0.25F, 0.0F, 0.25F);
                     alpha = Mth.clamp(alpha, 0.0F, 1.0F);
                  }
               }

               graphics.blitSprite(RenderPipelines.GUI_TEXTURED, getMobEffectSprite(effect), x + 3, y + 3, 18, 18, ARGB.white(alpha));
            }
         }
      }
   }

   public static Identifier getMobEffectSprite(final Holder<MobEffect> effect) {
      return effect.unwrapKey().map(ResourceKey::identifier).map(id -> id.withPrefix("mob_effect/")).orElseGet(MissingTextureAtlasSprite::getLocation);
   }

   private void extractHotbarAndDecorations(final GuiGraphicsExtractor graphics, final DeltaTracker deltaTracker) {
      if (this.minecraft.gameMode.getPlayerMode() == GameType.SPECTATOR) {
         this.spectatorGui.extractHotbar(graphics);
      } else {
         this.extractItemHotbar(graphics, deltaTracker);
      }

      if (this.minecraft.gameMode.canHurtPlayer()) {
         this.extractPlayerHealth(graphics);
      }

      this.extractVehicleHealth(graphics);
      Hud.ContextualInfo nextContextualInfo = this.nextContextualInfoState();
      if (nextContextualInfo != this.contextualInfoBar.getFirst()) {
         this.contextualInfoBar = Pair.of(nextContextualInfo, this.contextualInfoBars.get(nextContextualInfo).get());
      }

      ((ContextualBar)this.contextualInfoBar.getSecond()).extractBackground(graphics, deltaTracker);
      if (this.minecraft.gameMode.hasExperience() && this.minecraft.player.experienceLevel > 0) {
         ContextualBar.extractExperienceLevel(graphics, this.minecraft.font, this.minecraft.player.experienceLevel);
      }

      ((ContextualBar)this.contextualInfoBar.getSecond()).extractRenderState(graphics, deltaTracker);
      if (this.minecraft.gameMode.getPlayerMode() != GameType.SPECTATOR) {
         this.extractSelectedItemName(graphics);
      } else if (this.minecraft.player.isSpectator()) {
         this.spectatorGui.extractAction(graphics);
      }
   }

   private void extractItemHotbar(final GuiGraphicsExtractor graphics, final DeltaTracker deltaTracker) {
      Player player = this.getCameraPlayer();
      if (player != null) {
         ItemStack offhand = player.getOffhandItem();
         HumanoidArm offhandArm = player.getMainArm().getOpposite();
         int screenCenter = graphics.guiWidth() / 2;
         int hotbarWidth = 182;
         int halfHotbar = 91;
         graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR_SPRITE, screenCenter - 91, graphics.guiHeight() - 22, 182, 22);
         graphics.blitSprite(
            RenderPipelines.GUI_TEXTURED,
            HOTBAR_SELECTION_SPRITE,
            screenCenter - 91 - 1 + player.getInventory().getSelectedSlot() * 20,
            graphics.guiHeight() - 22 - 1,
            24,
            23
         );
         if (!offhand.isEmpty()) {
            if (offhandArm == HumanoidArm.LEFT) {
               graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR_OFFHAND_LEFT_SPRITE, screenCenter - 91 - 29, graphics.guiHeight() - 23, 29, 24);
            } else {
               graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR_OFFHAND_RIGHT_SPRITE, screenCenter + 91, graphics.guiHeight() - 23, 29, 24);
            }
         }

         int seed = 1;

         for (int i = 0; i < 9; i++) {
            int x = screenCenter - 90 + i * 20 + 2;
            int y = graphics.guiHeight() - 16 - 3;
            this.extractSlot(graphics, x, y, deltaTracker, player, player.getInventory().getItem(i), seed++);
         }

         if (!offhand.isEmpty()) {
            int y = graphics.guiHeight() - 16 - 3;
            if (offhandArm == HumanoidArm.LEFT) {
               this.extractSlot(graphics, screenCenter - 91 - 26, y, deltaTracker, player, offhand, seed++);
            } else {
               this.extractSlot(graphics, screenCenter + 91 + 10, y, deltaTracker, player, offhand, seed++);
            }
         }

         if (this.minecraft.options.attackIndicator().get() == AttackIndicatorStatus.HOTBAR) {
            float attackStrengthScale = this.minecraft.player.getAttackStrengthScale(0.0F);
            if (attackStrengthScale < 1.0F) {
               int y = graphics.guiHeight() - 20;
               int x = screenCenter + 91 + 6;
               if (offhandArm == HumanoidArm.RIGHT) {
                  x = screenCenter - 91 - 22;
               }

               int progress = (int)(attackStrengthScale * 19.0F);
               graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR_ATTACK_INDICATOR_BACKGROUND_SPRITE, x, y, 18, 18);
               graphics.blitSprite(
                  RenderPipelines.GUI_TEXTURED, HOTBAR_ATTACK_INDICATOR_PROGRESS_SPRITE, 18, 18, 0, 18 - progress, x, y + 18 - progress, 18, progress
               );
            }
         }
      }
   }

   private void extractSelectedItemName(final GuiGraphicsExtractor graphics) {
      if (this.toolHighlightTimer > 0 && !this.lastToolHighlight.isEmpty()) {
         MutableComponent str = Component.empty().append(this.lastToolHighlight.getHoverName()).withStyle(this.lastToolHighlight.getRarity().color());
         if (this.lastToolHighlight.has(DataComponents.CUSTOM_NAME)) {
            str.withStyle(ChatFormatting.ITALIC);
         }

         int strWidth = this.getFont().width(str);
         int x = (graphics.guiWidth() - strWidth) / 2;
         int y = graphics.guiHeight() - 59;
         if (!this.minecraft.gameMode.canHurtPlayer()) {
            y += 14;
         }

         int alpha = (int)(this.toolHighlightTimer * 256.0F / 10.0F);
         if (alpha > 255) {
            alpha = 255;
         }

         if (alpha > 0) {
            graphics.textWithBackdrop(this.getFont(), str, x, y, strWidth, ARGB.white(alpha));
         }
      }
   }

   private void extractDemoOverlay(final GuiGraphicsExtractor graphics, final DeltaTracker deltaTracker) {
      if (this.minecraft.isDemo()) {
         Profiler.get().push("demo");
         graphics.nextStratum();
         Component msg;
         if (this.minecraft.level.getGameTime() >= 120500L) {
            msg = DEMO_EXPIRED_TEXT;
         } else {
            msg = Component.translatable(
               "demo.remainingTime",
               StringUtil.formatTickDuration((int)(120500L - this.minecraft.level.getGameTime()), this.minecraft.level.tickRateManager().tickrate())
            );
         }

         int width = this.getFont().width(msg);
         int textX = graphics.guiWidth() - width - 10;
         int textY = 5;
         graphics.textWithBackdrop(this.getFont(), msg, textX, 5, width, -1);
         Profiler.get().pop();
      }
   }

   private void displayScoreboardSidebar(final GuiGraphicsExtractor graphics, final Objective objective) {
      Scoreboard scoreboard = objective.getScoreboard();
      NumberFormat objectiveScoreFormat = objective.numberFormatOrDefault(StyledFormat.SIDEBAR_DEFAULT);

      record DisplayEntry(Component name, Component score, int scoreWidth) {
      }

      DisplayEntry[] entriesToDisplay = scoreboard.listPlayerScores(objective)
         .stream()
         .filter(input -> !input.isHidden())
         .sorted(SCORE_DISPLAY_ORDER)
         .limit(15L)
         .map(score -> {
            PlayerTeam team = scoreboard.getPlayersTeam(score.owner());
            Component ownerName = score.ownerName();
            Component name = PlayerTeam.formatNameForTeam(team, ownerName);
            Component scoreString = score.formatValue(objectiveScoreFormat);
            int scoreWidth = this.getFont().width(scoreString);
            return new DisplayEntry(name, scoreString, scoreWidth);
         })
         .toArray(DisplayEntry[]::new);
      Component objectiveDisplayName = objective.getDisplayName();
      int objectiveDisplayNameWidth = this.getFont().width(objectiveDisplayName);
      int biggestWidth = objectiveDisplayNameWidth;
      int spacerWidth = this.getFont().width(": ");

      for (DisplayEntry entry : entriesToDisplay) {
         biggestWidth = Math.max(biggestWidth, this.getFont().width(entry.name) + (entry.scoreWidth > 0 ? spacerWidth + entry.scoreWidth : 0));
      }

      int width = biggestWidth;
      int entriesCount = entriesToDisplay.length;
      int height = entriesCount * 9;
      int bottom = graphics.guiHeight() / 2 + height / 3;
      int rightPadding = 3;
      int left = graphics.guiWidth() - width - 3;
      int right = graphics.guiWidth() - 3 + 2;
      int backgroundColor = this.minecraft.options.getBackgroundColor(0.3F);
      int headerBackgroundColor = this.minecraft.options.getBackgroundColor(0.4F);
      int headerY = bottom - entriesCount * 9;
      graphics.fill(left - 2, headerY - 9 - 1, right, headerY - 1, headerBackgroundColor);
      graphics.fill(left - 2, headerY - 1, right, bottom, backgroundColor);
      graphics.text(this.getFont(), objectiveDisplayName, left + width / 2 - objectiveDisplayNameWidth / 2, headerY - 9, -1, false);

      for (int i = 0; i < entriesCount; i++) {
         DisplayEntry e = entriesToDisplay[i];
         int y = bottom - (entriesCount - i) * 9;
         graphics.text(this.getFont(), e.name, left, y, -1, false);
         graphics.text(this.getFont(), e.score, right - e.scoreWidth, y, -1, false);
      }
   }

   private @Nullable Player getCameraPlayer() {
      return this.minecraft.getCameraEntity() instanceof Player player ? player : null;
   }

   private @Nullable LivingEntity getPlayerVehicleWithHealth() {
      Player player = this.getCameraPlayer();
      if (player != null) {
         Entity vehicle = player.getVehicle();
         if (vehicle == null) {
            return null;
         }

         if (vehicle instanceof LivingEntity livingEntity) {
            return livingEntity;
         }
      }

      return null;
   }

   private int getVehicleMaxHearts(final @Nullable LivingEntity vehicle) {
      if (vehicle != null && vehicle.showVehicleHealth()) {
         float maxVehicleHealth = vehicle.getMaxHealth();
         int hearts = (int)(maxVehicleHealth + 0.5F) / 2;
         if (hearts > 30) {
            hearts = 30;
         }

         return hearts;
      } else {
         return 0;
      }
   }

   private int getVisibleVehicleHeartRows(final int hearts) {
      return (int)Math.ceil(hearts / 10.0);
   }

   private void extractPlayerHealth(final GuiGraphicsExtractor graphics) {
      Player player = this.getCameraPlayer();
      if (player != null) {
         int currentHealth = Mth.ceil(player.getHealth());
         boolean blink = this.healthBlinkTime > this.tickCount && (this.healthBlinkTime - this.tickCount) / 3L % 2L == 1L;
         long timeMillis = Util.getMillis();
         if (currentHealth < this.lastHealth && player.invulnerableTime > 0) {
            this.lastHealthTime = timeMillis;
            this.healthBlinkTime = this.tickCount + 20;
         } else if (currentHealth > this.lastHealth && player.invulnerableTime > 0) {
            this.lastHealthTime = timeMillis;
            this.healthBlinkTime = this.tickCount + 10;
         }

         if (timeMillis - this.lastHealthTime > 1000L) {
            this.displayHealth = currentHealth;
            this.lastHealthTime = timeMillis;
         }

         this.lastHealth = currentHealth;
         int oldHealth = this.displayHealth;
         this.random.setSeed(this.tickCount * 312871);
         int xLeft = graphics.guiWidth() / 2 - 91;
         int xRight = graphics.guiWidth() / 2 + 91;
         int yLineBase = graphics.guiHeight() - 39;
         float maxHealth = Math.max((float)player.getAttributeValue(Attributes.MAX_HEALTH), Math.max(oldHealth, currentHealth));
         int totalAbsorption = Mth.ceil(player.getAbsorptionAmount());
         int numHealthRows = Mth.ceil((maxHealth + totalAbsorption) / 2.0F / 10.0F);
         int healthRowHeight = Math.max(10 - (numHealthRows - 2), 3);
         int yLineAir = yLineBase - 10;
         int heartOffsetIndex = -1;
         if (player.hasEffect(MobEffects.REGENERATION)) {
            heartOffsetIndex = this.tickCount % Mth.ceil(maxHealth + 5.0F);
         }

         Profiler.get().push("armor");
         extractArmor(graphics, player, yLineBase, numHealthRows, healthRowHeight, xLeft);
         Profiler.get().popPush("health");
         this.extractHearts(graphics, player, xLeft, yLineBase, healthRowHeight, heartOffsetIndex, maxHealth, currentHealth, oldHealth, totalAbsorption, blink);
         LivingEntity vehicleWithHearts = this.getPlayerVehicleWithHealth();
         int vehicleHearts = this.getVehicleMaxHearts(vehicleWithHearts);
         if (vehicleHearts == 0) {
            Profiler.get().popPush("food");
            this.extractFood(graphics, player, yLineBase, xRight);
            yLineAir -= 10;
         }

         Profiler.get().popPush("air");
         this.extractAirBubbles(graphics, player, vehicleHearts, yLineAir, xRight);
         Profiler.get().pop();
      }
   }

   private static void extractArmor(
      final GuiGraphicsExtractor graphics, final Player player, final int yLineBase, final int numHealthRows, final int healthRowHeight, final int xLeft
   ) {
      int armor = player.getArmorValue();
      if (armor > 0) {
         int yLineArmor = yLineBase - (numHealthRows - 1) * healthRowHeight - 10;

         for (int i = 0; i < 10; i++) {
            int xo = xLeft + i * 8;
            if (i * 2 + 1 < armor) {
               graphics.blitSprite(RenderPipelines.GUI_TEXTURED, ARMOR_FULL_SPRITE, xo, yLineArmor, 9, 9);
            }

            if (i * 2 + 1 == armor) {
               graphics.blitSprite(RenderPipelines.GUI_TEXTURED, ARMOR_HALF_SPRITE, xo, yLineArmor, 9, 9);
            }

            if (i * 2 + 1 > armor) {
               graphics.blitSprite(RenderPipelines.GUI_TEXTURED, ARMOR_EMPTY_SPRITE, xo, yLineArmor, 9, 9);
            }
         }
      }
   }

   private void extractHearts(
      final GuiGraphicsExtractor graphics,
      final Player player,
      final int xLeft,
      final int yLineBase,
      final int healthRowHeight,
      final int heartOffsetIndex,
      final float maxHealth,
      final int currentHealth,
      final int oldHealth,
      final int absorption,
      final boolean blink
   ) {
      Hud.HeartType type = Hud.HeartType.forPlayer(player);
      boolean isHardcore = player.level().getLevelData().isHardcore();
      int healthContainerCount = Mth.ceil(maxHealth / 2.0);
      int absorptionContainerCount = Mth.ceil(absorption / 2.0);
      int maxHealthHalvesCount = healthContainerCount * 2;

      for (int containerIndex = healthContainerCount + absorptionContainerCount - 1; containerIndex >= 0; containerIndex--) {
         int row = containerIndex / 10;
         int column = containerIndex % 10;
         int xo = xLeft + column * 8;
         int yo = yLineBase - row * healthRowHeight;
         if (currentHealth + absorption <= 4) {
            yo += this.random.nextInt(2);
         }

         if (containerIndex < healthContainerCount && containerIndex == heartOffsetIndex) {
            yo -= 2;
         }

         this.extractHeart(graphics, Hud.HeartType.CONTAINER, xo, yo, isHardcore, blink, false);
         int halves = containerIndex * 2;
         boolean isAbsorptionHeart = containerIndex >= healthContainerCount;
         if (isAbsorptionHeart) {
            int absorptionHalves = halves - maxHealthHalvesCount;
            if (absorptionHalves < absorption) {
               boolean halfHeart = absorptionHalves + 1 == absorption;
               this.extractHeart(graphics, type == Hud.HeartType.WITHERED ? type : Hud.HeartType.ABSORBING, xo, yo, isHardcore, false, halfHeart);
            }
         }

         if (blink && halves < oldHealth) {
            boolean halfHeart = halves + 1 == oldHealth;
            this.extractHeart(graphics, type, xo, yo, isHardcore, true, halfHeart);
         }

         if (halves < currentHealth) {
            boolean halfHeart = halves + 1 == currentHealth;
            this.extractHeart(graphics, type, xo, yo, isHardcore, false, halfHeart);
         }
      }
   }

   private void extractHeart(
      final GuiGraphicsExtractor graphics,
      final Hud.HeartType type,
      final int xo,
      final int yo,
      final boolean isHardcore,
      final boolean blinks,
      final boolean half
   ) {
      graphics.blitSprite(RenderPipelines.GUI_TEXTURED, type.getSprite(isHardcore, half, blinks), xo, yo, 9, 9);
   }

   private void extractAirBubbles(final GuiGraphicsExtractor graphics, final Player player, final int vehicleHearts, int yLineAir, final int xRight) {
      int maxAirSupplyTicks = player.getMaxAirSupply();
      int currentAirSupplyTicks = Math.clamp(player.getAirSupply(), 0, maxAirSupplyTicks);
      boolean isUnderWater = player.isEyeInFluid(FluidTags.WATER);
      if (isUnderWater || currentAirSupplyTicks < maxAirSupplyTicks) {
         yLineAir = this.getAirBubbleYLine(vehicleHearts, yLineAir);
         int fullAirBubbles = getCurrentAirSupplyBubble(currentAirSupplyTicks, maxAirSupplyTicks, -2);
         int poppingAirBubblePosition = getCurrentAirSupplyBubble(currentAirSupplyTicks, maxAirSupplyTicks, 0);
         int emptyAirBubbles = 10
            - getCurrentAirSupplyBubble(currentAirSupplyTicks, maxAirSupplyTicks, getEmptyBubbleDelayDuration(currentAirSupplyTicks, isUnderWater));
         boolean isPoppingBubble = fullAirBubbles != poppingAirBubblePosition;
         if (!isUnderWater) {
            this.lastBubblePopSoundPlayed = 0;
         }

         for (int airBubble = 1; airBubble <= 10; airBubble++) {
            int airBubbleXPos = xRight - (airBubble - 1) * 8 - 9;
            if (airBubble <= fullAirBubbles) {
               graphics.blitSprite(RenderPipelines.GUI_TEXTURED, AIR_SPRITE, airBubbleXPos, yLineAir, 9, 9);
            } else if (isPoppingBubble && airBubble == poppingAirBubblePosition && isUnderWater) {
               graphics.blitSprite(RenderPipelines.GUI_TEXTURED, AIR_POPPING_SPRITE, airBubbleXPos, yLineAir, 9, 9);
               this.playAirBubblePoppedSound(airBubble, player, emptyAirBubbles);
            } else if (airBubble > 10 - emptyAirBubbles) {
               int wobbleYOffset = emptyAirBubbles == 10 && this.tickCount % 2 == 0 ? this.random.nextInt(2) : 0;
               graphics.blitSprite(RenderPipelines.GUI_TEXTURED, AIR_EMPTY_SPRITE, airBubbleXPos, yLineAir + wobbleYOffset, 9, 9);
            }
         }
      }
   }

   private int getAirBubbleYLine(final int vehicleHearts, int yLineAir) {
      int rowOffset = this.getVisibleVehicleHeartRows(vehicleHearts) - 1;
      return yLineAir - rowOffset * 10;
   }

   private static int getCurrentAirSupplyBubble(final int currentAirSupplyTicks, final int maxAirSupplyTicks, final int tickOffset) {
      return Mth.ceil((float)((currentAirSupplyTicks + tickOffset) * 10) / maxAirSupplyTicks);
   }

   private static int getEmptyBubbleDelayDuration(final int currentAirSupplyTicks, final boolean isUnderWater) {
      return currentAirSupplyTicks != 0 && isUnderWater ? 1 : 0;
   }

   private void playAirBubblePoppedSound(final int bubble, final Player player, final int emptyAirBubbles) {
      if (this.lastBubblePopSoundPlayed != bubble) {
         float soundVolume = 0.5F + 0.1F * Math.max(0, emptyAirBubbles - 3 + 1);
         float soundPitch = 1.0F + 0.1F * Math.max(0, emptyAirBubbles - 5 + 1);
         player.playSound(SoundEvents.BUBBLE_POP, soundVolume, soundPitch);
         this.lastBubblePopSoundPlayed = bubble;
      }
   }

   private void extractFood(final GuiGraphicsExtractor graphics, final Player player, final int yLineBase, final int xRight) {
      FoodData foodData = player.getFoodData();
      int food = foodData.getFoodLevel();

      for (int i = 0; i < 10; i++) {
         int yo = yLineBase;
         Identifier empty;
         Identifier half;
         Identifier full;
         if (player.hasEffect(MobEffects.HUNGER)) {
            empty = FOOD_EMPTY_HUNGER_SPRITE;
            half = FOOD_HALF_HUNGER_SPRITE;
            full = FOOD_FULL_HUNGER_SPRITE;
         } else {
            empty = FOOD_EMPTY_SPRITE;
            half = FOOD_HALF_SPRITE;
            full = FOOD_FULL_SPRITE;
         }

         if (player.getFoodData().getSaturationLevel() <= 0.0F && this.tickCount % (food * 3 + 1) == 0) {
            yo += this.random.nextInt(3) - 1;
         }

         int xo = xRight - i * 8 - 9;
         graphics.blitSprite(RenderPipelines.GUI_TEXTURED, empty, xo, yo, 9, 9);
         if (i * 2 + 1 < food) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, full, xo, yo, 9, 9);
         }

         if (i * 2 + 1 == food) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, half, xo, yo, 9, 9);
         }
      }
   }

   private void extractVehicleHealth(final GuiGraphicsExtractor graphics) {
      LivingEntity vehicleWithHearts = this.getPlayerVehicleWithHealth();
      if (vehicleWithHearts != null) {
         int hearts = this.getVehicleMaxHearts(vehicleWithHearts);
         if (hearts != 0) {
            int currentHealth = (int)Math.ceil(vehicleWithHearts.getHealth());
            Profiler.get().popPush("mountHealth");
            int yLine1 = graphics.guiHeight() - 39;
            int xRight = graphics.guiWidth() / 2 + 91;
            int yo = yLine1;

            for (int baseHealth = 0; hearts > 0; baseHealth += 20) {
               int rowHearts = Math.min(hearts, 10);
               hearts -= rowHearts;

               for (int i = 0; i < rowHearts; i++) {
                  int xo = xRight - i * 8 - 9;
                  graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HEART_VEHICLE_CONTAINER_SPRITE, xo, yo, 9, 9);
                  if (i * 2 + 1 + baseHealth < currentHealth) {
                     graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HEART_VEHICLE_FULL_SPRITE, xo, yo, 9, 9);
                  }

                  if (i * 2 + 1 + baseHealth == currentHealth) {
                     graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HEART_VEHICLE_HALF_SPRITE, xo, yo, 9, 9);
                  }
               }

               yo -= 10;
            }
         }
      }
   }

   private void extractTextureOverlay(final GuiGraphicsExtractor graphics, final Identifier texture, final float alpha) {
      int color = ARGB.white(alpha);
      graphics.blit(
         RenderPipelines.GUI_TEXTURED, texture, 0, 0, 0.0F, 0.0F, graphics.guiWidth(), graphics.guiHeight(), graphics.guiWidth(), graphics.guiHeight(), color
      );
   }

   private void extractSpyglassOverlay(final GuiGraphicsExtractor graphics, final float scale) {
      float srcWidth = Math.min(graphics.guiWidth(), graphics.guiHeight());
      float srcHeight = srcWidth;
      float ratio = Math.min(graphics.guiWidth() / srcWidth, graphics.guiHeight() / srcHeight) * scale;
      int width = Mth.floor(srcWidth * ratio);
      int height = Mth.floor(srcHeight * ratio);
      int left = (graphics.guiWidth() - width) / 2;
      int top = (graphics.guiHeight() - height) / 2;
      int right = left + width;
      int bottom = top + height;
      graphics.blit(RenderPipelines.GUI_TEXTURED, SPYGLASS_SCOPE_LOCATION, left, top, 0.0F, 0.0F, width, height, width, height);
      graphics.fill(RenderPipelines.GUI, 0, bottom, graphics.guiWidth(), graphics.guiHeight(), -16777216);
      graphics.fill(RenderPipelines.GUI, 0, 0, graphics.guiWidth(), top, -16777216);
      graphics.fill(RenderPipelines.GUI, 0, top, left, bottom, -16777216);
      graphics.fill(RenderPipelines.GUI, right, top, graphics.guiWidth(), bottom, -16777216);
   }

   private void updateVignetteBrightness(final Entity camera) {
      BlockPos blockPos = BlockPos.containing(camera.getX(), camera.getEyeY(), camera.getZ());
      float levelBrightness = Lightmap.getBrightness(camera.level().dimensionType(), camera.level().getMaxLocalRawBrightness(blockPos));
      float brightness = Mth.clamp(1.0F - levelBrightness, 0.0F, 1.0F);
      this.vignetteBrightness = this.vignetteBrightness + (brightness - this.vignetteBrightness) * 0.01F;
   }

   private void extractVignette(final GuiGraphicsExtractor graphics, final @Nullable Entity camera) {
      WorldBorder worldBorder = this.minecraft.level.getWorldBorder();
      float borderWarningStrength = 0.0F;
      if (camera != null) {
         float distToBorder = (float)worldBorder.getDistanceToBorder(camera);
         double movingBlocksThreshold = Math.min(
            worldBorder.getLerpSpeed() * worldBorder.getWarningTime(), Math.abs(worldBorder.getLerpTarget() - worldBorder.getSize())
         );
         double warningDistance = Math.max(worldBorder.getWarningBlocks(), movingBlocksThreshold);
         if (distToBorder < warningDistance) {
            borderWarningStrength = 1.0F - (float)(distToBorder / warningDistance);
         }
      }

      float brightness = Mth.clamp(this.vignetteBrightness, 0.0F, 1.0F);
      int color;
      if (borderWarningStrength > 0.0F) {
         borderWarningStrength = Mth.clamp(borderWarningStrength, 0.0F, 1.0F);
         float red = brightness * (1.0F - borderWarningStrength);
         float greenBlue = brightness + (1.0F - brightness) * borderWarningStrength;
         color = ARGB.colorFromFloat(1.0F, red, greenBlue, greenBlue);
      } else {
         color = ARGB.colorFromFloat(1.0F, brightness, brightness, brightness);
      }

      graphics.blit(
         RenderPipelines.VIGNETTE,
         VIGNETTE_LOCATION,
         0,
         0,
         0.0F,
         0.0F,
         graphics.guiWidth(),
         graphics.guiHeight(),
         graphics.guiWidth(),
         graphics.guiHeight(),
         color
      );
   }

   private void extractPortalOverlay(final GuiGraphicsExtractor graphics, float alpha) {
      if (alpha < 1.0F) {
         alpha *= alpha;
         alpha *= alpha;
         alpha = alpha * 0.8F + 0.2F;
      }

      int color = ARGB.white(alpha);
      TextureAtlasSprite slot = this.minecraft.getModelManager().getBlockStateModelSet().getParticleMaterial(Blocks.NETHER_PORTAL.defaultBlockState()).sprite();
      graphics.blitSprite(RenderPipelines.GUI_TEXTURED, slot, 0, 0, graphics.guiWidth(), graphics.guiHeight(), color);
   }

   private void extractConfusionOverlay(final GuiGraphicsExtractor graphics, final float strength) {
      int screenWidth = graphics.guiWidth();
      int screenHeight = graphics.guiHeight();
      graphics.pose().pushMatrix();
      float size = Mth.lerp(strength, 2.0F, 1.0F);
      graphics.pose().translate(screenWidth / 2.0F, screenHeight / 2.0F);
      graphics.pose().scale(size, size);
      graphics.pose().translate(-screenWidth / 2.0F, -screenHeight / 2.0F);
      float red = 0.2F * strength;
      float green = 0.4F * strength;
      float blue = 0.2F * strength;
      graphics.blit(
         RenderPipelines.GUI_NAUSEA_OVERLAY,
         NAUSEA_LOCATION,
         0,
         0,
         0.0F,
         0.0F,
         screenWidth,
         screenHeight,
         screenWidth,
         screenHeight,
         ARGB.colorFromFloat(1.0F, red, green, blue)
      );
      graphics.pose().popMatrix();
   }

   private void extractSlot(
      final GuiGraphicsExtractor graphics,
      final int x,
      final int y,
      final DeltaTracker deltaTracker,
      final Player player,
      final ItemStack itemStack,
      final int seed
   ) {
      if (!itemStack.isEmpty()) {
         float pop = itemStack.getPopTime() - deltaTracker.getGameTimeDeltaPartialTick(false);
         if (pop > 0.0F) {
            float squeeze = 1.0F + pop / 5.0F;
            graphics.pose().pushMatrix();
            graphics.pose().translate(x + 8, y + 12);
            graphics.pose().scale(1.0F / squeeze, (squeeze + 1.0F) / 2.0F);
            graphics.pose().translate(-(x + 8), -(y + 12));
         }

         graphics.item(player, itemStack, x, y, seed);
         if (pop > 0.0F) {
            graphics.pose().popMatrix();
         }

         graphics.itemDecorations(this.minecraft.font, itemStack, x, y);
      }
   }

   public void tick(final boolean pause) {
      this.tickAutosaveIndicator();
      if (!pause) {
         this.tick();
      }
   }

   private void tick() {
      if (this.overlayMessageTime > 0) {
         this.overlayMessageTime--;
      }

      if (this.titleTime > 0) {
         this.titleTime--;
         if (this.titleTime <= 0) {
            this.title = null;
            this.subtitle = null;
         }
      }

      this.tickCount++;
      Entity camera = this.minecraft.getCameraEntity();
      if (camera != null) {
         this.updateVignetteBrightness(camera);
      }

      if (this.minecraft.player != null) {
         ItemStack selected = this.minecraft.player.getInventory().getSelectedItem();
         if (selected.isEmpty()) {
            this.toolHighlightTimer = 0;
         } else if (this.lastToolHighlight.isEmpty()
            || !selected.is(this.lastToolHighlight.getItem())
            || !selected.getHoverName().equals(this.lastToolHighlight.getHoverName())) {
            this.toolHighlightTimer = (int)(40.0 * this.minecraft.options.notificationDisplayTime().get());
         } else if (this.toolHighlightTimer > 0) {
            this.toolHighlightTimer--;
         }

         this.lastToolHighlight = selected;
      }

      this.chat.tick();
   }

   private void tickAutosaveIndicator() {
      MinecraftServer server = this.minecraft.getSingleplayerServer();
      boolean isAutosaving = server != null && server.isCurrentlySaving();
      this.lastAutosaveIndicatorValue = this.autosaveIndicatorValue;
      this.autosaveIndicatorValue = Mth.lerp(0.2F, this.autosaveIndicatorValue, isAutosaving ? 1.0F : 0.0F);
   }

   public void setNowPlaying(final Component string) {
      Component message = Component.translatable("record.nowPlaying", string);
      this.setOverlayMessage(message, true);
      this.minecraft.getNarrator().saySystemNow(message);
   }

   public void setOverlayMessage(final Component string, final boolean animate) {
      this.overlayMessageString = string;
      this.overlayMessageTime = 60;
      this.animateOverlayMessageColor = animate;
   }

   public void setTimes(final int fadeInTime, final int stayTime, final int fadeOutTime) {
      if (fadeInTime >= 0) {
         this.titleFadeInTime = fadeInTime;
      }

      if (stayTime >= 0) {
         this.titleStayTime = stayTime;
      }

      if (fadeOutTime >= 0) {
         this.titleFadeOutTime = fadeOutTime;
      }

      if (this.titleTime > 0) {
         this.titleTime = this.titleFadeInTime + this.titleStayTime + this.titleFadeOutTime;
      }
   }

   public void setSubtitle(final Component subtitle) {
      this.subtitle = subtitle;
   }

   public void setTitle(final Component title) {
      this.title = title;
      this.titleTime = this.titleFadeInTime + this.titleStayTime + this.titleFadeOutTime;
   }

   public void clearTitles() {
      this.title = null;
      this.subtitle = null;
      this.titleTime = 0;
   }

   public ChatComponent getChat() {
      return this.chat;
   }

   public WaypointStyleManager getWaypointStyles() {
      return this.waypointStyles;
   }

   public int getGuiTicks() {
      return this.tickCount;
   }

   public Font getFont() {
      return this.minecraft.font;
   }

   public SpectatorGui getSpectatorGui() {
      return this.spectatorGui;
   }

   public PlayerTabOverlay getTabList() {
      return this.tabList;
   }

   public void onDisconnected() {
      this.tabList.reset();
      this.bossOverlay.reset();
      this.minecraft.gui.toastManager().clear();
      this.debugOverlay.reset();
      this.chat.clearMessages(true);
      this.clearTitles();
      this.resetTitleTimes();
   }

   public BossHealthOverlay getBossOverlay() {
      return this.bossOverlay;
   }

   public DebugScreenOverlay getDebugOverlay() {
      return this.debugOverlay;
   }

   public void clearCache() {
      this.debugOverlay.clearChunkCache();
   }

   public void extractSavingIndicator(final GuiGraphicsExtractor graphics, final DeltaTracker deltaTracker) {
      if (this.minecraft.options.showAutosaveIndicator().get() && (this.autosaveIndicatorValue > 0.0F || this.lastAutosaveIndicatorValue > 0.0F)) {
         int alpha = Mth.floor(
            255.0F * Mth.clamp(Mth.lerp(deltaTracker.getRealtimeDeltaTicks(), this.lastAutosaveIndicatorValue, this.autosaveIndicatorValue), 0.0F, 1.0F)
         );
         if (alpha > 0) {
            Font font = this.getFont();
            int width = font.width(SAVING_TEXT);
            int color = ARGB.color(alpha, -1);
            int textX = graphics.guiWidth() - width - 5;
            int textY = graphics.guiHeight() - 9 - 5;
            graphics.nextStratum();
            graphics.textWithBackdrop(font, SAVING_TEXT, textX, textY, width, color);
         }
      }
   }

   private boolean willPrioritizeExperienceInfo() {
      return this.minecraft.player.experienceDisplayStartTick + 100 > this.minecraft.player.tickCount;
   }

   private boolean willPrioritizeJumpInfo() {
      return this.minecraft.player.getJumpRidingScale() > 0.0F
         || Optionull.mapOrDefault(this.minecraft.player.jumpableVehicle(), PlayerRideableJumping::getJumpCooldown, 0) > 0;
   }

   private Hud.ContextualInfo nextContextualInfoState() {
      boolean canShowLocatorInfo = this.minecraft.player.connection.getWaypointManager().hasWaypoints();
      boolean canShowVehicleJumpInfo = this.minecraft.player.jumpableVehicle() != null;
      boolean canShowExperienceInfo = this.minecraft.gameMode.hasExperience();
      if (canShowLocatorInfo) {
         if (canShowVehicleJumpInfo && this.willPrioritizeJumpInfo()) {
            return Hud.ContextualInfo.JUMPABLE_VEHICLE;
         } else {
            return canShowExperienceInfo && this.willPrioritizeExperienceInfo() ? Hud.ContextualInfo.EXPERIENCE : Hud.ContextualInfo.LOCATOR;
         }
      } else if (canShowVehicleJumpInfo) {
         return Hud.ContextualInfo.JUMPABLE_VEHICLE;
      } else {
         return canShowExperienceInfo ? Hud.ContextualInfo.EXPERIENCE : Hud.ContextualInfo.EMPTY;
      }
   }

   private enum ContextualInfo {
      EMPTY,
      EXPERIENCE,
      LOCATOR,
      JUMPABLE_VEHICLE;
   }

   private enum HeartType {
      CONTAINER(
         Identifier.withDefaultNamespace("hud/heart/container"),
         Identifier.withDefaultNamespace("hud/heart/container_blinking"),
         Identifier.withDefaultNamespace("hud/heart/container"),
         Identifier.withDefaultNamespace("hud/heart/container_blinking"),
         Identifier.withDefaultNamespace("hud/heart/container_hardcore"),
         Identifier.withDefaultNamespace("hud/heart/container_hardcore_blinking"),
         Identifier.withDefaultNamespace("hud/heart/container_hardcore"),
         Identifier.withDefaultNamespace("hud/heart/container_hardcore_blinking")
      ),
      NORMAL(
         Identifier.withDefaultNamespace("hud/heart/full"),
         Identifier.withDefaultNamespace("hud/heart/full_blinking"),
         Identifier.withDefaultNamespace("hud/heart/half"),
         Identifier.withDefaultNamespace("hud/heart/half_blinking"),
         Identifier.withDefaultNamespace("hud/heart/hardcore_full"),
         Identifier.withDefaultNamespace("hud/heart/hardcore_full_blinking"),
         Identifier.withDefaultNamespace("hud/heart/hardcore_half"),
         Identifier.withDefaultNamespace("hud/heart/hardcore_half_blinking")
      ),
      POISIONED(
         Identifier.withDefaultNamespace("hud/heart/poisoned_full"),
         Identifier.withDefaultNamespace("hud/heart/poisoned_full_blinking"),
         Identifier.withDefaultNamespace("hud/heart/poisoned_half"),
         Identifier.withDefaultNamespace("hud/heart/poisoned_half_blinking"),
         Identifier.withDefaultNamespace("hud/heart/poisoned_hardcore_full"),
         Identifier.withDefaultNamespace("hud/heart/poisoned_hardcore_full_blinking"),
         Identifier.withDefaultNamespace("hud/heart/poisoned_hardcore_half"),
         Identifier.withDefaultNamespace("hud/heart/poisoned_hardcore_half_blinking")
      ),
      WITHERED(
         Identifier.withDefaultNamespace("hud/heart/withered_full"),
         Identifier.withDefaultNamespace("hud/heart/withered_full_blinking"),
         Identifier.withDefaultNamespace("hud/heart/withered_half"),
         Identifier.withDefaultNamespace("hud/heart/withered_half_blinking"),
         Identifier.withDefaultNamespace("hud/heart/withered_hardcore_full"),
         Identifier.withDefaultNamespace("hud/heart/withered_hardcore_full_blinking"),
         Identifier.withDefaultNamespace("hud/heart/withered_hardcore_half"),
         Identifier.withDefaultNamespace("hud/heart/withered_hardcore_half_blinking")
      ),
      ABSORBING(
         Identifier.withDefaultNamespace("hud/heart/absorbing_full"),
         Identifier.withDefaultNamespace("hud/heart/absorbing_full_blinking"),
         Identifier.withDefaultNamespace("hud/heart/absorbing_half"),
         Identifier.withDefaultNamespace("hud/heart/absorbing_half_blinking"),
         Identifier.withDefaultNamespace("hud/heart/absorbing_hardcore_full"),
         Identifier.withDefaultNamespace("hud/heart/absorbing_hardcore_full_blinking"),
         Identifier.withDefaultNamespace("hud/heart/absorbing_hardcore_half"),
         Identifier.withDefaultNamespace("hud/heart/absorbing_hardcore_half_blinking")
      ),
      FROZEN(
         Identifier.withDefaultNamespace("hud/heart/frozen_full"),
         Identifier.withDefaultNamespace("hud/heart/frozen_full_blinking"),
         Identifier.withDefaultNamespace("hud/heart/frozen_half"),
         Identifier.withDefaultNamespace("hud/heart/frozen_half_blinking"),
         Identifier.withDefaultNamespace("hud/heart/frozen_hardcore_full"),
         Identifier.withDefaultNamespace("hud/heart/frozen_hardcore_full_blinking"),
         Identifier.withDefaultNamespace("hud/heart/frozen_hardcore_half"),
         Identifier.withDefaultNamespace("hud/heart/frozen_hardcore_half_blinking")
      );

      private final Identifier full;
      private final Identifier fullBlinking;
      private final Identifier half;
      private final Identifier halfBlinking;
      private final Identifier hardcoreFull;
      private final Identifier hardcoreFullBlinking;
      private final Identifier hardcoreHalf;
      private final Identifier hardcoreHalfBlinking;

      HeartType(
         final Identifier full,
         final Identifier fullBlinking,
         final Identifier half,
         final Identifier halfBlinking,
         final Identifier hardcoreFull,
         final Identifier hardcoreFullBlinking,
         final Identifier hardcoreHalf,
         final Identifier hardcoreHalfBlinking
      ) {
         this.full = full;
         this.fullBlinking = fullBlinking;
         this.half = half;
         this.halfBlinking = halfBlinking;
         this.hardcoreFull = hardcoreFull;
         this.hardcoreFullBlinking = hardcoreFullBlinking;
         this.hardcoreHalf = hardcoreHalf;
         this.hardcoreHalfBlinking = hardcoreHalfBlinking;
      }

      public Identifier getSprite(final boolean isHardcore, final boolean isHalf, final boolean isBlink) {
         if (!isHardcore) {
            if (isHalf) {
               return isBlink ? this.halfBlinking : this.half;
            } else {
               return isBlink ? this.fullBlinking : this.full;
            }
         } else if (isHalf) {
            return isBlink ? this.hardcoreHalfBlinking : this.hardcoreHalf;
         } else {
            return isBlink ? this.hardcoreFullBlinking : this.hardcoreFull;
         }
      }

      private static Hud.HeartType forPlayer(final Player player) {
         Hud.HeartType type;
         if (player.hasEffect(MobEffects.POISON)) {
            type = POISIONED;
         } else if (player.hasEffect(MobEffects.WITHER)) {
            type = WITHERED;
         } else if (player.isFullyFrozen()) {
            type = FROZEN;
         } else {
            type = NORMAL;
         }

         return type;
      }
   }
}
