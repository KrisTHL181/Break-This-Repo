package net.minecraft.client.gui.components.toasts;

import com.google.common.collect.ImmutableList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class SystemToast implements Toast {
   private static final Identifier BACKGROUND_SPRITE = Identifier.withDefaultNamespace("toast/system");
   private static final int MAX_LINE_SIZE = 200;
   private static final int LINE_SPACING = 12;
   private static final int MARGIN = 10;
   private final SystemToast.SystemToastId id;
   private Component title;
   private List<FormattedCharSequence> messageLines;
   private long lastChanged;
   private boolean changed;
   private final int width;
   private boolean forceHide;
   private Toast.Visibility wantedVisibility = Toast.Visibility.HIDE;

   public SystemToast(SystemToast.SystemToastId p_94832_, Component p_94833_, @Nullable Component p_94834_) {
      this(
         p_94832_,
         p_94833_,
         nullToEmpty(p_94834_),
         Math.max(160, 30 + Math.max(Minecraft.getInstance().font.width(p_94833_), p_94834_ == null ? 0 : Minecraft.getInstance().font.width(p_94834_)))
      );
   }

   public static SystemToast multiline(Minecraft p_94848_, SystemToast.SystemToastId p_94849_, Component p_94850_, Component p_94851_) {
      Font font = p_94848_.font;
      List<FormattedCharSequence> list = font.split(p_94851_, 200);
      int i = Math.max(200, list.stream().mapToInt(font::width).max().orElse(200));
      return new SystemToast(p_94849_, p_94850_, list, i + 30);
   }

   private SystemToast(SystemToast.SystemToastId p_94827_, Component p_94828_, List<FormattedCharSequence> p_94829_, int p_94830_) {
      this.id = p_94827_;
      this.title = p_94828_;
      this.messageLines = p_94829_;
      this.width = p_94830_;
   }

   private static ImmutableList<FormattedCharSequence> nullToEmpty(@Nullable Component p_94861_) {
      return p_94861_ == null ? ImmutableList.of() : ImmutableList.of(p_94861_.getVisualOrderText());
   }

   @Override
   public int width() {
      return this.width;
   }

   @Override
   public int height() {
      return 20 + Math.max(this.messageLines.size(), 1) * 12;
   }

   public void forceHide() {
      this.forceHide = true;
   }

   @Override
   public Toast.Visibility getWantedVisibility() {
      return this.wantedVisibility;
   }

   @Override
   public void update(ToastManager p_361843_, long p_364076_) {
      if (this.changed) {
         this.lastChanged = p_364076_;
         this.changed = false;
      }

      double d0 = this.id.displayTime * p_361843_.getNotificationDisplayTimeMultiplier();
      long i = p_364076_ - this.lastChanged;
      this.wantedVisibility = !this.forceHide && i < d0 ? Toast.Visibility.SHOW : Toast.Visibility.HIDE;
   }

   @Override
   public void render(GuiGraphics p_281624_, Font p_368558_, long p_282762_) {
      p_281624_.blitSprite(RenderPipelines.GUI_TEXTURED, BACKGROUND_SPRITE, 0, 0, this.width(), this.height());
      if (this.messageLines.isEmpty()) {
         p_281624_.drawString(p_368558_, this.title, 18, 12, -256, false);
      } else {
         p_281624_.drawString(p_368558_, this.title, 18, 7, -256, false);

         for (int i = 0; i < this.messageLines.size(); i++) {
            p_281624_.drawString(p_368558_, this.messageLines.get(i), 18, 18 + i * 12, -1, false);
         }
      }
   }

   public void reset(Component p_94863_, @Nullable Component p_94864_) {
      this.title = p_94863_;
      this.messageLines = nullToEmpty(p_94864_);
      this.changed = true;
   }

   public SystemToast.SystemToastId getToken() {
      return this.id;
   }

   public static void add(ToastManager p_362779_, SystemToast.SystemToastId p_94857_, Component p_94858_, @Nullable Component p_94859_) {
      p_362779_.addToast(new SystemToast(p_94857_, p_94858_, p_94859_));
   }

   public static void addOrUpdate(ToastManager p_360727_, SystemToast.SystemToastId p_94871_, Component p_94872_, @Nullable Component p_94873_) {
      SystemToast systemtoast = p_360727_.getToast(SystemToast.class, p_94871_);
      if (systemtoast == null) {
         add(p_360727_, p_94871_, p_94872_, p_94873_);
      } else {
         systemtoast.reset(p_94872_, p_94873_);
      }
   }

   public static void forceHide(ToastManager p_366670_, SystemToast.SystemToastId p_311637_) {
      SystemToast systemtoast = p_366670_.getToast(SystemToast.class, p_311637_);
      if (systemtoast != null) {
         systemtoast.forceHide();
      }
   }

   public static void onWorldAccessFailure(Minecraft p_94853_, String p_94854_) {
      add(
         p_94853_.getToastManager(),
         SystemToast.SystemToastId.WORLD_ACCESS_FAILURE,
         Component.translatable("selectWorld.access_failure"),
         Component.literal(p_94854_)
      );
   }

   public static void onWorldDeleteFailure(Minecraft p_94867_, String p_94868_) {
      add(
         p_94867_.getToastManager(),
         SystemToast.SystemToastId.WORLD_ACCESS_FAILURE,
         Component.translatable("selectWorld.delete_failure"),
         Component.literal(p_94868_)
      );
   }

   public static void onPackCopyFailure(Minecraft p_94876_, String p_94877_) {
      add(p_94876_.getToastManager(), SystemToast.SystemToastId.PACK_COPY_FAILURE, Component.translatable("pack.copyFailure"), Component.literal(p_94877_));
   }

   public static void onFileDropFailure(Minecraft p_343671_, int p_343465_) {
      add(
         p_343671_.getToastManager(),
         SystemToast.SystemToastId.FILE_DROP_FAILURE,
         Component.translatable("gui.fileDropFailure.title"),
         Component.translatable("gui.fileDropFailure.detail", p_343465_)
      );
   }

   public static void onLowDiskSpace(Minecraft p_335579_) {
      addOrUpdate(
         p_335579_.getToastManager(),
         SystemToast.SystemToastId.LOW_DISK_SPACE,
         Component.translatable("chunk.toast.lowDiskSpace"),
         Component.translatable("chunk.toast.lowDiskSpace.description")
      );
   }

   public static void onChunkLoadFailure(Minecraft p_335709_, ChunkPos p_330201_) {
      addOrUpdate(
         p_335709_.getToastManager(),
         SystemToast.SystemToastId.CHUNK_LOAD_FAILURE,
         Component.translatable("chunk.toast.loadFailure", Component.translationArg(p_330201_)).withStyle(ChatFormatting.RED),
         Component.translatable("chunk.toast.checkLog")
      );
   }

   public static void onChunkSaveFailure(Minecraft p_328693_, ChunkPos p_333444_) {
      addOrUpdate(
         p_328693_.getToastManager(),
         SystemToast.SystemToastId.CHUNK_SAVE_FAILURE,
         Component.translatable("chunk.toast.saveFailure", Component.translationArg(p_333444_)).withStyle(ChatFormatting.RED),
         Component.translatable("chunk.toast.checkLog")
      );
   }

   @OnlyIn(Dist.CLIENT)
   public static class SystemToastId {
      public static final SystemToast.SystemToastId NARRATOR_TOGGLE = new SystemToast.SystemToastId();
      public static final SystemToast.SystemToastId WORLD_BACKUP = new SystemToast.SystemToastId();
      public static final SystemToast.SystemToastId PACK_LOAD_FAILURE = new SystemToast.SystemToastId();
      public static final SystemToast.SystemToastId WORLD_ACCESS_FAILURE = new SystemToast.SystemToastId();
      public static final SystemToast.SystemToastId PACK_COPY_FAILURE = new SystemToast.SystemToastId();
      public static final SystemToast.SystemToastId FILE_DROP_FAILURE = new SystemToast.SystemToastId();
      public static final SystemToast.SystemToastId PERIODIC_NOTIFICATION = new SystemToast.SystemToastId();
      public static final SystemToast.SystemToastId LOW_DISK_SPACE = new SystemToast.SystemToastId(10000L);
      public static final SystemToast.SystemToastId CHUNK_LOAD_FAILURE = new SystemToast.SystemToastId();
      public static final SystemToast.SystemToastId CHUNK_SAVE_FAILURE = new SystemToast.SystemToastId();
      public static final SystemToast.SystemToastId UNSECURE_SERVER_WARNING = new SystemToast.SystemToastId(10000L);
      final long displayTime;

      public SystemToastId(long p_311745_) {
         this.displayTime = p_311745_;
      }

      public SystemToastId() {
         this(5000L);
      }
   }
}
