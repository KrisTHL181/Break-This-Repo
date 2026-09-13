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
   private static final String ID_PREFIX = "command_storage_";
   private final Map<String, CommandStorage.Container> namespaces = new HashMap<>();
   private final DimensionDataStorage storage;

   public CommandStorage(DimensionDataStorage p_78035_) {
      this.storage = p_78035_;
   }

   public CompoundTag get(Identifier p_460728_) {
      CommandStorage.Container commandstorage$container = this.getContainer(p_460728_.getNamespace());
      return commandstorage$container != null ? commandstorage$container.get(p_460728_.getPath()) : new CompoundTag();
   }

   private CommandStorage.@Nullable Container getContainer(String p_393886_) {
      CommandStorage.Container commandstorage$container = this.namespaces.get(p_393886_);
      if (commandstorage$container != null) {
         return commandstorage$container;
      }

      CommandStorage.Container commandstorage$container1 = this.storage.get(CommandStorage.Container.type(p_393886_));
      if (commandstorage$container1 != null) {
         this.namespaces.put(p_393886_, commandstorage$container1);
      }

      return commandstorage$container1;
   }

   private CommandStorage.Container getOrCreateContainer(String p_393897_) {
      CommandStorage.Container commandstorage$container = this.namespaces.get(p_393897_);
      if (commandstorage$container != null) {
         return commandstorage$container;
      }

      CommandStorage.Container commandstorage$container1 = this.storage.computeIfAbsent(CommandStorage.Container.type(p_393897_));
      this.namespaces.put(p_393897_, commandstorage$container1);
      return commandstorage$container1;
   }

   public void set(Identifier p_453940_, CompoundTag p_78048_) {
      this.getOrCreateContainer(p_453940_.getNamespace()).put(p_453940_.getPath(), p_78048_);
   }

   public Stream<Identifier> keys() {
      return this.namespaces.entrySet().stream().flatMap(p_164841_ -> p_164841_.getValue().getKeys(p_164841_.getKey()));
   }

   static String createId(String p_78038_) {
      return "command_storage_" + p_78038_;
   }

   static class Container extends SavedData {
      public static final Codec<CommandStorage.Container> CODEC = RecordCodecBuilder.create(
         p_391107_ -> p_391107_.group(
               Codec.unboundedMap(ExtraCodecs.RESOURCE_PATH_CODEC, CompoundTag.CODEC).fieldOf("contents").forGetter(p_391108_ -> p_391108_.storage)
            )
            .apply(p_391107_, CommandStorage.Container::new)
      );
      private final Map<String, CompoundTag> storage;

      private Container(Map<String, CompoundTag> p_397341_) {
         this.storage = new HashMap<>(p_397341_);
      }

      private Container() {
         this(new HashMap<>());
      }

      public static SavedDataType<CommandStorage.Container> type(String p_393598_) {
         return new SavedDataType<>(CommandStorage.createId(p_393598_), CommandStorage.Container::new, CODEC, DataFixTypes.SAVED_DATA_COMMAND_STORAGE);
      }

      public CompoundTag get(String p_78059_) {
         CompoundTag compoundtag = this.storage.get(p_78059_);
         return compoundtag != null ? compoundtag : new CompoundTag();
      }

      public void put(String p_78064_, CompoundTag p_78065_) {
         if (p_78065_.isEmpty()) {
            this.storage.remove(p_78064_);
         } else {
            this.storage.put(p_78064_, p_78065_);
         }

         this.setDirty();
      }

      public Stream<Identifier> getKeys(String p_78073_) {
         return this.storage.keySet().stream().map(p_450061_ -> Identifier.fromNamespaceAndPath(p_78073_, p_450061_));
      }
   }
}
