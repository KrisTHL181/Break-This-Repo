package net.minecraft.server.commands.data;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.arguments.NbtPathArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;

public interface DataAccessor {
   void setData(CompoundTag var1) throws CommandSyntaxException;

   CompoundTag getData() throws CommandSyntaxException;

   Component getModifiedSuccess();

   Component getPrintSuccess(Tag var1);

   Component getPrintSuccess(NbtPathArgument.NbtPath var1, double var2, int var4);
}
