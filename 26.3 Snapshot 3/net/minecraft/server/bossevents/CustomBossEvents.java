package net.minecraft.server.bossevents;

import com.google.common.collect.Maps;
import com.mojang.serialization.Codec;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jspecify.annotations.Nullable;

public class CustomBossEvents extends SavedData {
   private static final Codec<Map<Identifier, CustomBossEvent.Packed>> EVENTS_CODEC = Codec.unboundedMap(Identifier.CODEC, CustomBossEvent.Packed.CODEC);
   private static final Codec<CustomBossEvents> CODEC = EVENTS_CODEC.xmap(events -> {
      CustomBossEvents r = new CustomBossEvents();
      events.forEach((id, packed) -> r.events.put(id, CustomBossEvent.load(UUID.randomUUID(), id, packed, r::setDirty)));
      return r;
   }, c -> Util.mapValues(c.events, CustomBossEvent::pack));
   public static final SavedDataType<CustomBossEvents> TYPE = new SavedDataType<>(
      Identifier.withDefaultNamespace("custom_boss_events"), CustomBossEvents::new, CODEC, DataFixTypes.SAVED_DATA_CUSTOM_BOSS_EVENTS
   );
   private final Map<Identifier, CustomBossEvent> events = Maps.newHashMap();

   public @Nullable CustomBossEvent get(final Identifier id) {
      return this.events.get(id);
   }

   public CustomBossEvent create(final RandomSource random, final Identifier id, final Component name) {
      CustomBossEvent result = new CustomBossEvent(Mth.createInsecureUUID(random), id, name, this::setDirty);
      this.events.put(id, result);
      this.setDirty();
      return result;
   }

   public void remove(final CustomBossEvent event) {
      if (this.events.remove(event.customId()) != null) {
         this.setDirty();
      }
   }

   public Collection<Identifier> getIds() {
      return this.events.keySet();
   }

   public Collection<CustomBossEvent> getEvents() {
      return this.events.values();
   }

   public void onPlayerConnect(final ServerPlayer player) {
      for (CustomBossEvent event : this.events.values()) {
         event.onPlayerConnect(player);
      }
   }

   public void onPlayerDisconnect(final ServerPlayer player) {
      for (CustomBossEvent event : this.events.values()) {
         event.onPlayerDisconnect(player);
      }
   }
}
