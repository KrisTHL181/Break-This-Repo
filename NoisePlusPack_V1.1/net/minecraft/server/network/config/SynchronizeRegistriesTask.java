package net.minecraft.server.network.config;

import com.mojang.serialization.DynamicOps;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.core.RegistrySynchronization;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundUpdateTagsPacket;
import net.minecraft.network.protocol.configuration.ClientboundRegistryDataPacket;
import net.minecraft.network.protocol.configuration.ClientboundSelectKnownPacks;
import net.minecraft.server.RegistryLayer;
import net.minecraft.server.network.ConfigurationTask;
import net.minecraft.server.packs.repository.KnownPack;
import net.minecraft.tags.TagNetworkSerialization;

public class SynchronizeRegistriesTask implements ConfigurationTask {
   public static final ConfigurationTask.Type TYPE = new ConfigurationTask.Type("synchronize_registries");
   private final List<KnownPack> requestedPacks;
   private final LayeredRegistryAccess<RegistryLayer> registries;

   public SynchronizeRegistriesTask(List<KnownPack> p_331975_, LayeredRegistryAccess<RegistryLayer> p_334926_) {
      this.requestedPacks = p_331975_;
      this.registries = p_334926_;
   }

   @Override
   public void start(Consumer<Packet<?>> p_333641_) {
      p_333641_.accept(new ClientboundSelectKnownPacks(this.requestedPacks));
   }

   private void sendRegistries(Consumer<Packet<?>> p_333495_, Set<KnownPack> p_335321_) {
      DynamicOps<Tag> dynamicops = this.registries.compositeAccess().createSerializationContext(NbtOps.INSTANCE);
      RegistrySynchronization.packRegistries(
         dynamicops,
         this.registries.getAccessFrom(RegistryLayer.WORLDGEN),
         p_335321_,
         (p_334638_, p_328189_) -> p_333495_.accept(new ClientboundRegistryDataPacket(p_334638_, p_328189_))
      );
      p_333495_.accept(new ClientboundUpdateTagsPacket(TagNetworkSerialization.serializeTagsToNetwork(this.registries)));
   }

   public void handleResponse(List<KnownPack> p_332734_, Consumer<Packet<?>> p_331332_) {
      if (p_332734_.equals(this.requestedPacks)) {
         this.sendRegistries(p_331332_, Set.copyOf(this.requestedPacks));
      } else {
         this.sendRegistries(p_331332_, Set.of());
      }
   }

   @Override
   public ConfigurationTask.Type type() {
      return TYPE;
   }
}
