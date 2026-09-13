package net.minecraft.world.item;

import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.decoration.GlowItemFrame;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.decoration.painting.Painting;
import net.minecraft.world.entity.decoration.painting.PaintingVariant;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;

public class HangingEntityItem extends Item {
   private static final Component TOOLTIP_RANDOM_VARIANT = Component.translatable("painting.random").withStyle(ChatFormatting.GRAY);
   private final EntityType<? extends HangingEntity> type;

   public HangingEntityItem(final EntityType<? extends HangingEntity> type, final Item.Properties properties) {
      super(properties);
      this.type = type;
   }

   @Override
   public InteractionResult useOn(final UseOnContext context) {
      BlockPos pos = context.getClickedPos();
      Direction clickedFace = context.getClickedFace();
      BlockPos blockPos = pos.relative(clickedFace);
      Player player = context.getPlayer();
      ItemStack itemInHand = context.getItemInHand();
      if (player != null && !this.mayPlace(player, clickedFace, itemInHand, blockPos)) {
         return InteractionResult.FAIL;
      }

      Level level = context.getLevel();
      HangingEntity entity;
      if (this.type == EntityTypes.PAINTING) {
         Optional<Painting> painting = Painting.create(level, blockPos, clickedFace);
         if (painting.isEmpty()) {
            return InteractionResult.CONSUME;
         }

         entity = painting.get();
      } else if (this.type == EntityTypes.ITEM_FRAME) {
         entity = new ItemFrame(level, blockPos, clickedFace);
      } else {
         if (this.type != EntityTypes.GLOW_ITEM_FRAME) {
            return InteractionResult.SUCCESS;
         }

         entity = new GlowItemFrame(level, blockPos, clickedFace);
      }

      EntityType.<HangingEntity>createDefaultStackConfig(level, itemInHand, player).apply(entity);
      if (entity.survives()) {
         if (!level.isClientSide()) {
            entity.playPlacementSound();
            level.gameEvent(player, GameEvent.ENTITY_PLACE, entity.position());
            level.addFreshEntity(entity);
         }

         itemInHand.shrink(1);
         return InteractionResult.SUCCESS;
      } else {
         return InteractionResult.CONSUME;
      }
   }

   protected boolean mayPlace(final Player player, final Direction direction, final ItemStack itemStack, final BlockPos blockPos) {
      return !direction.getAxis().isVertical() && player.mayUseItemAt(blockPos, direction, itemStack);
   }

   @Override
   public void appendHoverText(
      final ItemStack itemStack,
      final Item.TooltipContext context,
      final TooltipDisplay display,
      final Consumer<Component> builder,
      final TooltipFlag tooltipFlag
   ) {
      if (this.type == EntityTypes.PAINTING && display.shows(DataComponents.PAINTING_VARIANT)) {
         Holder<PaintingVariant> variant = itemStack.get(DataComponents.PAINTING_VARIANT);
         if (variant != null) {
            variant.value().title().ifPresent(builder);
            variant.value().author().ifPresent(builder);
            builder.accept(Component.translatable("painting.dimensions", variant.value().width(), variant.value().height()));
         } else if (tooltipFlag.isCreative()) {
            builder.accept(TOOLTIP_RANDOM_VARIANT);
         }
      }
   }
}
