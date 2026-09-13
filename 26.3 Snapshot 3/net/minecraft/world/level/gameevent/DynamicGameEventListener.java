package net.minecraft.world.level.gameevent;

import java.util.function.Consumer;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.jspecify.annotations.Nullable;

public class DynamicGameEventListener<T extends GameEventListener> {
   private final T listener;
   private @Nullable SectionPos lastSection;

   public DynamicGameEventListener(final T listener) {
      this.listener = listener;
   }

   public void add(final ServerLevel level) {
      this.move(level);
   }

   public T getListener() {
      return this.listener;
   }

   public void remove(final ServerLevel level) {
      ifChunkExists(level, this.lastSection, dispatcher -> dispatcher.unregister(this.listener));
   }

   public void move(final ServerLevel level) {
      this.listener.getListenerSource().getPosition(level).map(SectionPos::of).ifPresent(currentSection -> {
         if (this.lastSection == null || !this.lastSection.equals(currentSection)) {
            ifChunkExists(level, this.lastSection, dispatcher -> dispatcher.unregister(this.listener));
            this.lastSection = currentSection;
            ifChunkExists(level, this.lastSection, dispatcher -> dispatcher.register(this.listener));
         }
      });
   }

   private static void ifChunkExists(final LevelReader level, final @Nullable SectionPos sectionPos, final Consumer<GameEventListenerRegistry> action) {
      if (sectionPos != null) {
         ChunkAccess chunk = level.getChunk(sectionPos.x(), sectionPos.z(), ChunkStatus.FULL, false);
         if (chunk != null) {
            action.accept(chunk.getListenerRegistry(sectionPos.y()));
         }
      }
   }
}
