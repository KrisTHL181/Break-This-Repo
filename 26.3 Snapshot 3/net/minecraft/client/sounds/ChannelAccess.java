package net.minecraft.client.sounds;

import com.google.common.collect.Sets;
import com.mojang.blaze3d.audio.Channel;
import com.mojang.blaze3d.audio.Library;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;

public class ChannelAccess {
   private final Set<ChannelAccess.ChannelHandle> channels = Sets.newIdentityHashSet();
   private final Library library;
   private final Executor executor;

   public ChannelAccess(final Library library, final Executor executor) {
      this.library = library;
      this.executor = executor;
   }

   public CompletableFuture<ChannelAccess.ChannelHandle> createHandle(final Library.Pool pool) {
      CompletableFuture<ChannelAccess.ChannelHandle> result = new CompletableFuture<>();
      this.executor.execute(() -> {
         Channel channel = this.library.acquireChannel(pool);
         if (channel != null) {
            ChannelAccess.ChannelHandle handle = new ChannelAccess.ChannelHandle(channel);
            this.channels.add(handle);
            result.complete(handle);
         } else {
            result.complete(null);
         }
      });
      return result;
   }

   public void executeOnChannels(final Consumer<Stream<Channel>> action) {
      this.executor.execute(() -> action.accept(this.channels.stream().map(channelHandle -> channelHandle.channel).filter(Objects::nonNull)));
   }

   public void scheduleTick() {
      this.executor.execute(() -> {
         Iterator<ChannelAccess.ChannelHandle> it = this.channels.iterator();

         while (it.hasNext()) {
            ChannelAccess.ChannelHandle handle = it.next();
            handle.channel.updateStream();
            if (handle.channel.stopped()) {
               handle.release();
               it.remove();
            }
         }
      });
   }

   public void clear() {
      this.channels.forEach(ChannelAccess.ChannelHandle::release);
      this.channels.clear();
   }

   public class ChannelHandle {
      private @Nullable Channel channel;
      private boolean stopped;

      public boolean isStopped() {
         return this.stopped;
      }

      public ChannelHandle(final Channel channel) {
         this.channel = channel;
      }

      public void execute(final Consumer<Channel> action) {
         ChannelAccess.this.executor.execute(() -> {
            if (this.channel != null) {
               action.accept(this.channel);
            }
         });
      }

      public void release() {
         this.stopped = true;
         ChannelAccess.this.library.releaseChannel(this.channel);
         this.channel = null;
      }
   }
}
