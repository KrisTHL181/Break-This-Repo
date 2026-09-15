package net.minecraft.world.item.slot;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootContextUser;

public interface SlotSource extends LootContextUser {
   MapCodec<? extends SlotSource> codec();

   SlotCollection provide(LootContext var1);
}
