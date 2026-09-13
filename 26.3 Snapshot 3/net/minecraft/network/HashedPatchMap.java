package net.minecraft.network;

import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.TypedDataComponent;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record HashedPatchMap(Map<DataComponentType<?>, Integer> addedComponents, Set<DataComponentType<?>> removedComponents) {
   public static final StreamCodec<RegistryFriendlyByteBuf, HashedPatchMap> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.map(HashMap::new, ByteBufCodecs.registry(Registries.DATA_COMPONENT_TYPE), ByteBufCodecs.INT, 256),
      HashedPatchMap::addedComponents,
      ByteBufCodecs.collection(HashSet::new, ByteBufCodecs.registry(Registries.DATA_COMPONENT_TYPE), 256),
      HashedPatchMap::removedComponents,
      HashedPatchMap::new
   );

   public static HashedPatchMap create(final DataComponentPatch patch, final HashedPatchMap.HashGenerator hasher) {
      DataComponentPatch.SplitResult split = patch.split();
      Map<DataComponentType<?>, Integer> setComponentHashes = new IdentityHashMap<>(split.added().size());
      split.added().forEach(e -> setComponentHashes.put(e.type(), hasher.apply((TypedDataComponent<?>)e)));
      return new HashedPatchMap(setComponentHashes, split.removed());
   }

   public boolean matches(final DataComponentPatch patch, final HashedPatchMap.HashGenerator hasher) {
      DataComponentPatch.SplitResult split = patch.split();
      if (!split.removed().equals(this.removedComponents)) {
         return false;
      }

      if (this.addedComponents.size() != split.added().size()) {
         return false;
      }

      for (TypedDataComponent<?> typedDataComponent : split.added()) {
         Integer expectedHash = this.addedComponents.get(typedDataComponent.type());
         if (expectedHash == null) {
            return false;
         }

         Integer actualHash = hasher.apply(typedDataComponent);
         if (!actualHash.equals(expectedHash)) {
            return false;
         }
      }

      return true;
   }

   @FunctionalInterface
   public interface HashGenerator extends Function<TypedDataComponent<?>, Integer> {
   }
}
