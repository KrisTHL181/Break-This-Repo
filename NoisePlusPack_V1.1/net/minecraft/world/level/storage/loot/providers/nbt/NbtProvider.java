package net.minecraft.world.level.storage.loot.providers.nbt;

import java.util.Set;
import net.minecraft.nbt.Tag;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.level.storage.loot.LootContext;
import org.jspecify.annotations.Nullable;

public interface NbtProvider {
   @Nullable Tag get(LootContext var1);

   Set<ContextKey<?>> getReferencedContextParams();

   LootNbtProviderType getType();
}
