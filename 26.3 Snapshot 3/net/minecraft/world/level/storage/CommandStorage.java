package net.minecraft.world.level.storage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jspecify.annotations.Nullable;

public class CommandStorage {
   private static final String COMMAND_STORAGE = "command_storage";
   private final Map<String, CommandStorage.Container> namespaces = new HashMap<>();
   private final SavedDataStorage savedDataStorage;

   public CommandStorage(final SavedDataStorage savedDataStorage) {
      this.savedDataStorage = savedDataStorage;
   }

   public CompoundTag get(final Identifier id) {
      CommandStorage.Container container = this.getContainer(id.getNamespace());
      return container != null ? container.get(id.getPath()) : new CompoundTag();
   }

   private CommandStorage.@Nullable Container getContainer(final String namespace) {
      CommandStorage.Container container = this.namespaces.get(namespace);
      if (container != null) {
         return container;
      }

      CommandStorage.Container newContainer = this.savedDataStorage.get(CommandStorage.Container.type(namespace));
      if (newContainer != null) {
         this.namespaces.put(namespace, newContainer);
      }

      return newContainer;
   }

   private CommandStorage.Container getOrCreateContainer(final String namespace) {
      CommandStorage.Container container = this.namespaces.get(namespace);
      if (container != null) {
         return container;
      }

      CommandStorage.Container newContainer = this.savedDataStorage.computeIfAbsent(CommandStorage.Container.type(namespace));
      this.namespaces.put(namespace, newContainer);
      return newContainer;
   }

   public void set(final Identifier id, final CompoundTag contents) {
      this.getOrCreateContainer(id.getNamespace()).put(id.getPath(), contents);
   }

   public Stream<Identifier> keys() {
      return this.namespaces.entrySet().stream().flatMap(e -> e.getValue().getKeys(e.getKey()));
   }

   private static class Container extends SavedData {
      public static final Codec<CommandStorage.Container> CODEC = RecordCodecBuilder.create(
         i -> i.group(Codec.unboundedMap(ExtraCodecs.RESOURCE_PATH_CODEC, CompoundTag.CODEC).fieldOf("contents").forGetter(container -> container.storage))
            .apply(i, CommandStorage.Container::new)
      );
      private final Map<String, CompoundTag> storage;

      private Container(final Map<String, CompoundTag> storage) {
         this.storage = new HashMap<>(storage);
      }

      private Container() {
         this(new HashMap<>());
      }

      public static SavedDataType<CommandStorage.Container> type(final String namespace) {
         return new SavedDataType<>(
            Identifier.fromNamespaceAndPath(namespace, "command_storage"), CommandStorage.Container::new, CODEC, DataFixTypes.SAVED_DATA_COMMAND_STORAGE
         );
      }

      public CompoundTag get(final String id) {
         CompoundTag result = this.storage.get(id);
         return result != null ? result : new CompoundTag();
      }

      public void put(final String id, final CompoundTag contents) {
         if (contents.isEmpty()) {
            this.storage.remove(id);
         } else {
            this.storage.put(id, contents);
         }

         this.setDirty();
      }

      public Stream<Identifier> getKeys(final String namespace) {
         return this.storage.keySet().stream().map(p -> Identifier.fromNamespaceAndPath(namespace, p));
      }
   }
}
