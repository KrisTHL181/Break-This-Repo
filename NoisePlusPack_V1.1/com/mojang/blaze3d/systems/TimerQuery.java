package com.mojang.blaze3d.systems;

import java.util.OptionalLong;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class TimerQuery {
   private @Nullable CommandEncoder activeEncoder;
   private @Nullable GpuQuery activeGpuQuery;

   public static TimerQuery getInstance() {
      return TimerQuery.TimerQueryLazyLoader.INSTANCE;
   }

   public boolean isRecording() {
      return this.activeGpuQuery != null;
   }

   public void beginProfile() {
      RenderSystem.assertOnRenderThread();
      if (this.activeGpuQuery != null) {
         throw new IllegalStateException("Current profile not ended");
      }

      this.activeEncoder = RenderSystem.getDevice().createCommandEncoder();
      this.activeGpuQuery = this.activeEncoder.timerQueryBegin();
   }

   public TimerQuery.FrameProfile endProfile() {
      RenderSystem.assertOnRenderThread();
      if (this.activeGpuQuery != null && this.activeEncoder != null) {
         this.activeEncoder.timerQueryEnd(this.activeGpuQuery);
         TimerQuery.FrameProfile timerquery$frameprofile = new TimerQuery.FrameProfile(this.activeGpuQuery);
         this.activeGpuQuery = null;
         this.activeEncoder = null;
         return timerquery$frameprofile;
      } else {
         throw new IllegalStateException("endProfile called before beginProfile");
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class FrameProfile {
      private static final long NO_RESULT = 0L;
      private static final long CANCELLED_RESULT = -1L;
      private final GpuQuery gpuQuery;
      private long timerResult = 0L;

      FrameProfile(GpuQuery p_454961_) {
         this.gpuQuery = p_454961_;
      }

      public void cancel() {
         RenderSystem.assertOnRenderThread();
         if (this.timerResult == 0L) {
            this.timerResult = -1L;
            this.gpuQuery.close();
         }
      }

      public boolean isDone() {
         RenderSystem.assertOnRenderThread();
         if (this.timerResult != 0L) {
            return true;
         } else {
            OptionalLong optionallong = this.gpuQuery.getValue();
            if (optionallong.isPresent()) {
               this.timerResult = optionallong.getAsLong();
               this.gpuQuery.close();
               return true;
            } else {
               return false;
            }
         }
      }

      public long get() {
         RenderSystem.assertOnRenderThread();
         if (this.timerResult == 0L) {
            OptionalLong optionallong = this.gpuQuery.getValue();
            if (optionallong.isPresent()) {
               this.timerResult = optionallong.getAsLong();
               this.gpuQuery.close();
            }
         }

         return this.timerResult;
      }
   }

   @OnlyIn(Dist.CLIENT)
   static class TimerQueryLazyLoader {
      static final TimerQuery INSTANCE = instantiate();

      private TimerQueryLazyLoader() {
      }

      private static TimerQuery instantiate() {
         return new TimerQuery();
      }
   }
}
