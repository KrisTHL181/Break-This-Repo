package net.minecraft.world.entity.variant;

import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class VariantUtils {
   public static final String TAG_VARIANT = "variant";

   public static <T> Holder<T> getDefaultOrAny(final RegistryAccess registryAccess, final ResourceKey<T> id) {
      Registry<T> registry = registryAccess.lookupOrThrow(id.registryKey());
      return registry.get(id).or(registry::getAny).orElseThrow();
   }

   public static <T> Holder<T> getAny(final RegistryAccess registryAccess, final ResourceKey<? extends Registry<T>> registryId) {
      return registryAccess.lookupOrThrow(registryId).getAny().orElseThrow();
   }

   public static <T> void writeVariant(final ValueOutput output, final Holder<T> holder) {
      holder.unwrapKey().ifPresent(k -> output.store("variant", Identifier.CODEC, k.identifier()));
   }

   public static <T> Optional<Holder<T>> readVariant(final ValueInput input, final ResourceKey<? extends Registry<T>> registryId) {
      return input.<Identifier>read("variant", Identifier.CODEC).map(id -> ResourceKey.create(registryId, id)).flatMap(input.lookup()::get);
   }

   public static <T extends PriorityProvider<SpawnContext, ?>> Optional<Holder.Reference<T>> selectVariantToSpawn(
      final SpawnContext context, final ResourceKey<Registry<T>> variantRegistry
   ) {
      ServerLevelAccessor level = context.level();
      Stream<Holder.Reference<T>> entries = level.registryAccess().lookupOrThrow(variantRegistry).listElements();
      return PriorityProvider.pick(entries, Holder::value, level.getRandom(), context);
   }
}
