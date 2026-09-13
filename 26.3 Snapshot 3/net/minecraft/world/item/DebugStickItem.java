package net.minecraft.world.item;

import java.util.Collection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Util;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.component.DebugStickState;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;
import org.jspecify.annotations.Nullable;

public class DebugStickItem extends Item {
   public DebugStickItem(final Item.Properties properties) {
      super(properties);
   }

   @Override
   public boolean canDestroyBlock(final ItemStack itemStack, final BlockState state, final Level level, final BlockPos pos, final LivingEntity user) {
      if (user instanceof ServerPlayer player) {
         this.handleInteraction(player, state, level, pos, false, itemStack);
      }

      return false;
   }

   @Override
   public InteractionResult useOn(final UseOnContext context) {
      Player player = context.getPlayer();
      Level level = context.getLevel();
      if (player instanceof ServerPlayer serverPlayer) {
         BlockPos pos = context.getClickedPos();
         if (!this.handleInteraction(serverPlayer, level.getBlockState(pos), level, pos, true, context.getItemInHand())) {
            return InteractionResult.FAIL;
         }
      }

      return InteractionResult.SUCCESS;
   }

   private boolean handleInteraction(
      final ServerPlayer player, final BlockState state, final LevelAccessor level, final BlockPos pos, final boolean cycle, final ItemStack itemStackInHand
   ) {
      if (!player.canUseGameMasterBlocks()) {
         return false;
      }

      Holder<Block> block = state.typeHolder();
      StateDefinition<Block, BlockState> definition = block.value().getStateDefinition();
      Collection<Property<?>> properties = definition.getProperties();
      if (properties.isEmpty()) {
         message(player, Component.translatable(this.descriptionId + ".empty", block.getRegisteredName()));
         return false;
      }

      DebugStickState debugStickState = itemStackInHand.get(DataComponents.DEBUG_STICK_STATE);
      if (debugStickState == null) {
         return false;
      }

      Property<?> property = debugStickState.properties().get(block);
      if (cycle) {
         if (property == null) {
            property = properties.iterator().next();
         }

         BlockState newState = cycleState(state, property, player.isSecondaryUseActive());
         level.setBlock(pos, newState, 18);
         message(player, Component.translatable(this.descriptionId + ".update", property.getName(), getNameHelper(newState, property)));
      } else {
         property = getRelative(properties, property, player.isSecondaryUseActive());
         itemStackInHand.set(DataComponents.DEBUG_STICK_STATE, debugStickState.withProperty(block, property));
         message(player, Component.translatable(this.descriptionId + ".select", property.getName(), getNameHelper(state, property)));
      }

      return true;
   }

   private static <T extends Comparable<T>> BlockState cycleState(final BlockState state, final Property<T> property, final boolean backward) {
      return state.setValue(property, getRelative(property.getPossibleValues(), state.getValue(property), backward));
   }

   private static <T> T getRelative(final Iterable<T> collection, final @Nullable T current, final boolean backward) {
      return backward ? Util.findPreviousInIterable(collection, current) : Util.findNextInIterable(collection, current);
   }

   private static void message(final ServerPlayer player, final Component message) {
      player.sendOverlayMessage(message);
   }

   private static <T extends Comparable<T>> String getNameHelper(final BlockState state, final Property<T> property) {
      return property.getName(state.getValue(property));
   }
}
