package net.minecraft.server.commands.data;

import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.logging.LogUtils;
import java.util.Locale;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.NbtPathArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.commands.ArgProvider;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import org.slf4j.Logger;

public class BlockDataAccessor implements DataAccessor {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final SimpleCommandExceptionType ERROR_NOT_A_BLOCK_ENTITY = new SimpleCommandExceptionType(
      Component.translatable("commands.data.block.invalid")
   );
   public static final ArgProvider.Factory<DataAccessor> PROVIDER = arg -> ArgProvider.create(
      "block", () -> Commands.argument(arg, BlockPosArgument.blockPos()), context -> {
         BlockPos pos = BlockPosArgument.getLoadedBlockPos(context, arg);
         BlockEntity entity = ((CommandSourceStack)context.getSource()).getLevel().getBlockEntity(pos);
         if (entity == null) {
            throw ERROR_NOT_A_BLOCK_ENTITY.create();
         } else {
            return new BlockDataAccessor(entity, pos);
         }
      }
   );
   private final BlockEntity entity;
   private final BlockPos pos;

   public BlockDataAccessor(final BlockEntity entity, final BlockPos pos) {
      this.entity = entity;
      this.pos = pos;
   }

   @Override
   public void setData(final CompoundTag tag) {
      BlockState state = this.entity.getLevel().getBlockState(this.pos);

      try (ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(this.entity.problemPath(), LOGGER)) {
         this.entity.loadWithComponents(TagValueInput.create(reporter, this.entity.getLevel().registryAccess(), tag));
         this.entity.setChanged();
         this.entity.getLevel().sendBlockUpdated(this.pos, state, state, 3);
      }
   }

   @Override
   public CompoundTag getData() {
      return this.entity.saveWithFullMetadata(this.entity.getLevel().registryAccess());
   }

   @Override
   public Component getModifiedSuccess() {
      return Component.translatable("commands.data.block.modified", this.pos.getX(), this.pos.getY(), this.pos.getZ());
   }

   @Override
   public Component getPrintSuccess(final Tag data) {
      return Component.translatable("commands.data.block.query", this.pos.getX(), this.pos.getY(), this.pos.getZ(), NbtUtils.toPrettyComponent(data));
   }

   @Override
   public Component getPrintSuccess(final NbtPathArgument.NbtPath path, final double scale, final int value) {
      return Component.translatable(
         "commands.data.block.get", path.asString(), this.pos.getX(), this.pos.getY(), this.pos.getZ(), String.format(Locale.ROOT, "%.2f", scale), value
      );
   }
}
