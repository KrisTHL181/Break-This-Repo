package net.minecraft.world.item.crafting;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public record RecipeSerializer<T extends Recipe<?>>(MapCodec<T> codec, @Deprecated StreamCodec<RegistryFriendlyByteBuf, T> streamCodec) {
}
