package net.minecraft.world.level.storage.loot.providers.nbt;

import com.mojang.serialization.MapCodec;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootContextUser;
import org.jspecify.annotations.Nullable;

public interface NbtProvider extends LootContextUser {
   @Nullable Tag get(LootContext context);

   MapCodec<? extends NbtProvider> codec();
}
