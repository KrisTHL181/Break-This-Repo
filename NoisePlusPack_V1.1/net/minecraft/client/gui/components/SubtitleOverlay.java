package net.minecraft.client.gui.components;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.audio.ListenerTransform;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEventListener;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class SubtitleOverlay implements SoundEventListener {
   private static final long DISPLAY_TIME = 3000L;
   private final Minecraft minecraft;
   private final List<SubtitleOverlay.Subtitle> subtitles = Lists.newArrayList();
   private boolean isListening;
   private final List<SubtitleOverlay.Subtitle> audibleSubtitles = new ArrayList<>();

   public SubtitleOverlay(Minecraft p_94641_) {
      this.minecraft = p_94641_;
   }

   public void render(GuiGraphics p_282562_) {
      SoundManager soundmanager = this.minecraft.getSoundManager();
      if (!this.isListening && this.minecraft.options.showSubtitles().get()) {
         soundmanager.addListener(this);
         this.isListening = true;
      } else if (this.isListening && !this.minecraft.options.showSubtitles().get()) {
         soundmanager.removeListener(this);
         this.isListening = false;
      }

      if (this.isListening) {
         ListenerTransform listenertransform = soundmanager.getListenerTransform();
         Vec3 vec3 = listenertransform.position();
         Vec3 vec31 = listenertransform.forward();
         Vec3 vec32 = listenertransform.right();
         this.audibleSubtitles.clear();

         for (SubtitleOverlay.Subtitle subtitleoverlay$subtitle : this.subtitles) {
            if (subtitleoverlay$subtitle.isAudibleFrom(vec3)) {
               this.audibleSubtitles.add(subtitleoverlay$subtitle);
            }
         }

         if (!this.audibleSubtitles.isEmpty()) {
            int l1 = 0;
            int i2 = 0;
            double d0 = this.minecraft.options.notificationDisplayTime().get();
            Iterator<SubtitleOverlay.Subtitle> iterator = this.audibleSubtitles.iterator();

            while (iterator.hasNext()) {
               SubtitleOverlay.Subtitle subtitleoverlay$subtitle1 = iterator.next();
               subtitleoverlay$subtitle1.purgeOldInstances(3000.0 * d0);
               if (!subtitleoverlay$subtitle1.isStillActive()) {
                  iterator.remove();
               } else {
                  i2 = Math.max(i2, this.minecraft.font.width(subtitleoverlay$subtitle1.getText()));
               }
            }

            i2 += this.minecraft.font.width("<") + this.minecraft.font.width(" ") + this.minecraft.font.width(">") + this.minecraft.font.width(" ");
            if (!this.audibleSubtitles.isEmpty()) {
               p_282562_.nextStratum();
            }

            for (SubtitleOverlay.Subtitle subtitleoverlay$subtitle2 : this.audibleSubtitles) {
               int i = 255;
               Component component = subtitleoverlay$subtitle2.getText();
               SubtitleOverlay.SoundPlayedAt subtitleoverlay$soundplayedat = subtitleoverlay$subtitle2.getClosest(vec3);
               if (subtitleoverlay$soundplayedat != null) {
                  Vec3 vec33 = subtitleoverlay$soundplayedat.location.subtract(vec3).normalize();
                  double d1 = vec32.dot(vec33);
                  double d2 = vec31.dot(vec33);
                  boolean flag = d2 > 0.5;
                  int j = i2 / 2;
                  int k = 9;
                  int l = k / 2;
                  float f = 1.0F;
                  int i1 = this.minecraft.font.width(component);
                  int j1 = Mth.floor(Mth.clampedLerp((float)(Util.getMillis() - subtitleoverlay$soundplayedat.time) / (float)(3000.0 * d0), 255.0F, 75.0F));
                  p_282562_.pose().pushMatrix();
                  p_282562_.pose().translate(p_282562_.guiWidth() - j * 1.0F - 2.0F, p_282562_.guiHeight() - 35 - l1 * (k + 1) * 1.0F);
                  p_282562_.pose().scale(1.0F, 1.0F);
                  p_282562_.fill(-j - 1, -l - 1, j + 1, l + 1, this.minecraft.options.getBackgroundColor(0.8F));
                  int k1 = ARGB.color(255, j1, j1, j1);
                  if (!flag) {
                     if (d1 > 0.0) {
                        p_282562_.drawString(this.minecraft.font, ">", j - this.minecraft.font.width(">"), -l, k1);
                     } else if (d1 < 0.0) {
                        p_282562_.drawString(this.minecraft.font, "<", -j, -l, k1);
                     }
                  }

                  p_282562_.drawString(this.minecraft.font, component, -i1 / 2, -l, k1);
                  p_282562_.pose().popMatrix();
                  l1++;
               }
            }
         }
      }
   }

   @Override
   public void onPlaySound(SoundInstance p_94645_, WeighedSoundEvents p_94646_, float p_311530_) {
      if (p_94646_.getSubtitle() != null) {
         Component component = p_94646_.getSubtitle();
         if (!this.subtitles.isEmpty()) {
            for (SubtitleOverlay.Subtitle subtitleoverlay$subtitle : this.subtitles) {
               if (subtitleoverlay$subtitle.getText().equals(component)) {
                  subtitleoverlay$subtitle.refresh(new Vec3(p_94645_.getX(), p_94645_.getY(), p_94645_.getZ()));
                  return;
               }
            }
         }

         this.subtitles.add(new SubtitleOverlay.Subtitle(component, p_311530_, new Vec3(p_94645_.getX(), p_94645_.getY(), p_94645_.getZ())));
      }
   }

   @OnlyIn(Dist.CLIENT)
   record SoundPlayedAt(Vec3 location, long time) {
   }

   @OnlyIn(Dist.CLIENT)
   static class Subtitle {
      private final Component text;
      private final float range;
      private final List<SubtitleOverlay.SoundPlayedAt> playedAt = new ArrayList<>();

      public Subtitle(Component p_169072_, float p_312799_, Vec3 p_169073_) {
         this.text = p_169072_;
         this.range = p_312799_;
         this.playedAt.add(new SubtitleOverlay.SoundPlayedAt(p_169073_, Util.getMillis()));
      }

      public Component getText() {
         return this.text;
      }

      public SubtitleOverlay.@Nullable SoundPlayedAt getClosest(Vec3 p_344371_) {
         if (this.playedAt.isEmpty()) {
            return null;
         } else {
            return this.playedAt.size() == 1
               ? this.playedAt.getFirst()
               : this.playedAt.stream().min(Comparator.comparingDouble(p_344048_ -> p_344048_.location().distanceTo(p_344371_))).orElse(null);
         }
      }

      public void refresh(Vec3 p_94657_) {
         this.playedAt.removeIf(p_342347_ -> p_94657_.equals(p_342347_.location()));
         this.playedAt.add(new SubtitleOverlay.SoundPlayedAt(p_94657_, Util.getMillis()));
      }

      public boolean isAudibleFrom(Vec3 p_313169_) {
         if (Float.isInfinite(this.range)) {
            return true;
         }

         if (this.playedAt.isEmpty()) {
            return false;
         }

         SubtitleOverlay.SoundPlayedAt subtitleoverlay$soundplayedat = this.getClosest(p_313169_);
         return subtitleoverlay$soundplayedat == null ? false : p_313169_.closerThan(subtitleoverlay$soundplayedat.location, this.range);
      }

      public void purgeOldInstances(double p_345350_) {
         long i = Util.getMillis();
         this.playedAt.removeIf(p_342900_ -> i - p_342900_.time() > p_345350_);
      }

      public boolean isStillActive() {
         return !this.playedAt.isEmpty();
      }
   }
}
