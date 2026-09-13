package net.minecraft.client.gui.components.toasts;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.color.ColorLerper;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.DyeColor;
import org.jspecify.annotations.Nullable;

public class NowPlayingToast implements Toast {
   private static final Identifier NOW_PLAYING_BACKGROUND_SPRITE = Identifier.withDefaultNamespace("toast/now_playing");
   private static final Identifier MUSIC_NOTES_SPRITE = Identifier.parse("icon/music_notes");
   private static final int PADDING = 7;
   private static final int MUSIC_NOTES_SIZE = 16;
   private static final int HEIGHT = 30;
   private static final int MUSIC_NOTES_SPACE = 30;
   private static final int VISIBILITY_DURATION = 5000;
   private static final int TEXT_COLOR = DyeColor.LIGHT_GRAY.getTextColor();
   private static final long MUSIC_COLOR_CHANGE_FREQUENCY_MS = 25L;
   private static int musicNoteColorTick;
   private static long lastMusicNoteColorChange;
   private static int musicNoteColor = -1;
   private boolean updateToast;
   private double notificationDisplayTimeMultiplier;
   private final Minecraft minecraft;
   private Toast.Visibility wantedVisibility = Toast.Visibility.HIDE;

   public NowPlayingToast() {
      this.minecraft = Minecraft.getInstance();
   }

   public static void extractToast(final GuiGraphicsExtractor graphics, final Font font) {
      String currentSong = getCurrentSongName();
      if (currentSong != null) {
         graphics.blitSprite(RenderPipelines.GUI_TEXTURED, NOW_PLAYING_BACKGROUND_SPRITE, 0, 0, getWidth(currentSong, font), 30);
         int notesOffset = 7;
         graphics.blitSprite(RenderPipelines.GUI_TEXTURED, MUSIC_NOTES_SPRITE, 7, 7, 16, 16, musicNoteColor);
         graphics.text(font, getNowPlayingString(currentSong), 30, 15 - 9 / 2, TEXT_COLOR);
      }
   }

   private static @Nullable String getCurrentSongName() {
      return Minecraft.getInstance().getMusicManager().getCurrentMusicTranslationKey();
   }

   public static void tickMusicNotes() {
      if (getCurrentSongName() != null) {
         long now = System.currentTimeMillis();
         if (now > lastMusicNoteColorChange + 25L) {
            musicNoteColorTick++;
            lastMusicNoteColorChange = now;
            musicNoteColor = ColorLerper.getLerpedColor(ColorLerper.Type.MUSIC_NOTE, musicNoteColorTick);
         }
      }
   }

   private static Component getNowPlayingString(final @Nullable String currentSongKey) {
      return currentSongKey == null ? Component.empty() : Component.translatable(currentSongKey.replace("/", "."));
   }

   public void showToast(final Options options) {
      this.updateToast = true;
      this.notificationDisplayTimeMultiplier = options.notificationDisplayTime().get();
      this.setWantedVisibility(Toast.Visibility.SHOW);
   }

   @Override
   public void update(final ToastManager manager, final long fullyVisibleForMs) {
      if (this.updateToast) {
         this.wantedVisibility = fullyVisibleForMs < 5000.0 * this.notificationDisplayTimeMultiplier ? Toast.Visibility.SHOW : Toast.Visibility.HIDE;
         tickMusicNotes();
      }
   }

   @Override
   public void extractRenderState(final GuiGraphicsExtractor graphics, final Font font, final long fullyVisibleForMs) {
      extractToast(graphics, font);
   }

   @Override
   public void onFinishedRendering() {
      this.updateToast = false;
   }

   @Override
   public int width() {
      return getWidth(getCurrentSongName(), this.minecraft.font);
   }

   private static int getWidth(final @Nullable String currentSong, final Font font) {
      return 30 + font.width(getNowPlayingString(currentSong)) + 7;
   }

   @Override
   public int height() {
      return 30;
   }

   @Override
   public float xPos(final int screenWidth, final float visiblePortion) {
      return this.width() * visiblePortion - this.width();
   }

   @Override
   public float yPos(final int firstSlotIndex) {
      return 0.0F;
   }

   @Override
   public Toast.Visibility getWantedVisibility() {
      return this.wantedVisibility;
   }

   public void setWantedVisibility(final Toast.Visibility visibility) {
      this.wantedVisibility = visibility;
   }
}
