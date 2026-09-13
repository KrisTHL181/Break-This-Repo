package net.minecraft.server.commands.data;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import java.util.Locale;
import java.util.function.Function;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.commands.arguments.NbtPathArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.storage.CommandStorage;

public class StorageDataAccessor implements DataAccessor {
   static final SuggestionProvider<CommandSourceStack> SUGGEST_STORAGE = (p_139547_, p_139548_) -> SharedSuggestionProvider.suggestResource(
      getGlobalTags(p_139547_).keys(), p_139548_
   );
   public static final Function<String, DataCommands.DataProvider> PROVIDER = p_139554_ -> new DataCommands.DataProvider() {
      @Override
      public DataAccessor access(CommandContext<CommandSourceStack> p_139570_) {
         return new StorageDataAccessor(StorageDataAccessor.getGlobalTags(p_139570_), IdentifierArgument.getId(p_139570_, p_139554_));
      }

      @Override
      public ArgumentBuilder<CommandSourceStack, ?> wrap(
         ArgumentBuilder<CommandSourceStack, ?> p_139567_, Function<ArgumentBuilder<CommandSourceStack, ?>, ArgumentBuilder<CommandSourceStack, ?>> p_139568_
      ) {
         return p_139567_.then(
            Commands.literal("storage")
               .then(p_139568_.apply(Commands.argument(p_139554_, IdentifierArgument.id()).suggests(StorageDataAccessor.SUGGEST_STORAGE)))
         );
      }
   };
   private final CommandStorage storage;
   private final Identifier id;

   static CommandStorage getGlobalTags(CommandContext<CommandSourceStack> p_139561_) {
      return ((CommandSourceStack)p_139561_.getSource()).getServer().getCommandStorage();
   }

   StorageDataAccessor(CommandStorage p_139537_, Identifier p_459932_) {
      this.storage = p_139537_;
      this.id = p_459932_;
   }

   @Override
   public void setData(CompoundTag p_139556_) {
      this.storage.set(this.id, p_139556_);
   }

   @Override
   public CompoundTag getData() {
      return this.storage.get(this.id);
   }

   @Override
   public Component getModifiedSuccess() {
      return Component.translatable("commands.data.storage.modified", Component.translationArg(this.id));
   }

   @Override
   public Component getPrintSuccess(Tag p_139558_) {
      return Component.translatable("commands.data.storage.query", Component.translationArg(this.id), NbtUtils.toPrettyComponent(p_139558_));
   }

   @Override
   public Component getPrintSuccess(NbtPathArgument.NbtPath p_139550_, double p_139551_, int p_139552_) {
      return Component.translatable(
         "commands.data.storage.get", p_139550_.asString(), Component.translationArg(this.id), String.format(Locale.ROOT, "%.2f", p_139551_), p_139552_
      );
   }
}
