package net.minecraft.server.commands.data;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.logging.LogUtils;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Function;
import net.minecraft.advancements.criterion.NbtPredicate;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.NbtPathArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.TagValueInput;
import org.slf4j.Logger;

public class EntityDataAccessor implements DataAccessor {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final SimpleCommandExceptionType ERROR_NO_PLAYERS = new SimpleCommandExceptionType(Component.translatable("commands.data.entity.invalid"));
   public static final Function<String, DataCommands.DataProvider> PROVIDER = p_139517_ -> new DataCommands.DataProvider() {
      @Override
      public DataAccessor access(CommandContext<CommandSourceStack> p_139530_) throws CommandSyntaxException {
         return new EntityDataAccessor(EntityArgument.getEntity(p_139530_, p_139517_));
      }

      @Override
      public ArgumentBuilder<CommandSourceStack, ?> wrap(
         ArgumentBuilder<CommandSourceStack, ?> p_139527_, Function<ArgumentBuilder<CommandSourceStack, ?>, ArgumentBuilder<CommandSourceStack, ?>> p_139528_
      ) {
         return p_139527_.then(Commands.literal("entity").then(p_139528_.apply(Commands.argument(p_139517_, EntityArgument.entity()))));
      }
   };
   private final Entity entity;

   public EntityDataAccessor(Entity p_139510_) {
      this.entity = p_139510_;
   }

   @Override
   public void setData(CompoundTag p_139519_) throws CommandSyntaxException {
      if (this.entity instanceof Player) {
         throw ERROR_NO_PLAYERS.create();
      }

      UUID uuid = this.entity.getUUID();

      try (ProblemReporter.ScopedCollector problemreporter$scopedcollector = new ProblemReporter.ScopedCollector(this.entity.problemPath(), LOGGER)) {
         this.entity.load(TagValueInput.create(problemreporter$scopedcollector, this.entity.registryAccess(), p_139519_));
         this.entity.setUUID(uuid);
      }
   }

   @Override
   public CompoundTag getData() {
      return NbtPredicate.getEntityTagToCompare(this.entity);
   }

   @Override
   public Component getModifiedSuccess() {
      return Component.translatable("commands.data.entity.modified", this.entity.getDisplayName());
   }

   @Override
   public Component getPrintSuccess(Tag p_139521_) {
      return Component.translatable("commands.data.entity.query", this.entity.getDisplayName(), NbtUtils.toPrettyComponent(p_139521_));
   }

   @Override
   public Component getPrintSuccess(NbtPathArgument.NbtPath p_139513_, double p_139514_, int p_139515_) {
      return Component.translatable(
         "commands.data.entity.get", p_139513_.asString(), this.entity.getDisplayName(), String.format(Locale.ROOT, "%.2f", p_139514_), p_139515_
      );
   }
}
