package net.minecraft.stats;

import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import net.minecraft.core.Registry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public class StatType<T> implements Iterable<Stat<T>> {
   private final Registry<T> registry;
   private final Map<T, Stat<T>> map = new IdentityHashMap<>();
   private final Component displayName;
   private final StreamCodec<RegistryFriendlyByteBuf, Stat<T>> streamCodec;

   public StatType(Registry<T> p_12892_, Component p_300913_) {
      this.registry = p_12892_;
      this.displayName = p_300913_;
      this.streamCodec = ByteBufCodecs.registry(p_12892_.key()).map(this::get, Stat::getValue);
   }

   public StreamCodec<RegistryFriendlyByteBuf, Stat<T>> streamCodec() {
      return this.streamCodec;
   }

   public boolean contains(T p_12898_) {
      return this.map.containsKey(p_12898_);
   }

   public Stat<T> get(T p_12900_, StatFormatter p_12901_) {
      return this.map.computeIfAbsent(p_12900_, p_12896_ -> new Stat<>(this, (T)p_12896_, p_12901_));
   }

   public Registry<T> getRegistry() {
      return this.registry;
   }

   @Override
   public Iterator<Stat<T>> iterator() {
      return this.map.values().iterator();
   }

   public Stat<T> get(T p_12903_) {
      return this.get(p_12903_, StatFormatter.DEFAULT);
   }

   public Component getDisplayName() {
      return this.displayName;
   }
}
