package net.minecraft.network.chat.contents.data;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import java.util.stream.Stream;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.nbt.CompoundTag;

public interface DataSource {
   Stream<CompoundTag> getData(CommandSourceStack var1) throws CommandSyntaxException;

   MapCodec<? extends DataSource> codec();
}
