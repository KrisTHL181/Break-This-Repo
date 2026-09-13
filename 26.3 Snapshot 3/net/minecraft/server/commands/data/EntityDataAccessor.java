package net.minecraft.server.commands.data;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.logging.LogUtils;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.advancements.predicates.NbtPredicate;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.NbtPathArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.commands.ArgProvider;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.TagValueInput;
import org.slf4j.Logger;

public class EntityDataAccessor implements DataAccessor {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final SimpleCommandExceptionType ERROR_NO_PLAYERS = new SimpleCommandExceptionType(Component.translatable("commands.data.entity.invalid"));
   public static final ArgProvider.Factory<DataAccessor> PROVIDER = arg -> ArgProvider.create(
      "entity", () -> Commands.argument(arg, EntityArgument.entity()), c -> new EntityDataAccessor(EntityArgument.getEntity(c, arg))
   );
   private final Entity entity;

   public EntityDataAccessor(final Entity entity) {
      this.entity = entity;
   }

   @Override
   public void setData(final CompoundTag tag) throws CommandSyntaxException {
      if (this.entity instanceof Player) {
         throw ERROR_NO_PLAYERS.create();
      }

      UUID uuid = this.entity.getUUID();

      try (ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(this.entity.problemPath(), LOGGER)) {
         this.entity.load(TagValueInput.create(reporter, this.entity.registryAccess(), tag));
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
   public Component getPrintSuccess(final Tag data) {
      return Component.translatable("commands.data.entity.query", this.entity.getDisplayName(), NbtUtils.toPrettyComponent(data));
   }

   @Override
   public Component getPrintSuccess(final NbtPathArgument.NbtPath path, final double scale, final int value) {
      return Component.translatable("commands.data.entity.get", path.asString(), this.entity.getDisplayName(), String.format(Locale.ROOT, "%.2f", scale), value);
   }
}
