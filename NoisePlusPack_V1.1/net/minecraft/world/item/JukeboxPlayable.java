package net.minecraft.world.item;

import com.mojang.serialization.Codec;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.Style;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.component.TooltipProvider;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.JukeboxBlock;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

public record JukeboxPlayable(EitherHolder<JukeboxSong> song) implements TooltipProvider {
   public static final Codec<JukeboxPlayable> CODEC = EitherHolder.codec(Registries.JUKEBOX_SONG, JukeboxSong.CODEC)
      .xmap(JukeboxPlayable::new, JukeboxPlayable::song);
   public static final StreamCodec<RegistryFriendlyByteBuf, JukeboxPlayable> STREAM_CODEC = StreamCodec.composite(
      EitherHolder.streamCodec(Registries.JUKEBOX_SONG, JukeboxSong.STREAM_CODEC), JukeboxPlayable::song, JukeboxPlayable::new
   );

   @Override
   public void addToTooltip(Item.TooltipContext p_343529_, Consumer<Component> p_344027_, TooltipFlag p_344530_, DataComponentGetter p_392270_) {
      HolderLookup.Provider holderlookup$provider = p_343529_.registries();
      if (holderlookup$provider != null) {
         this.song.unwrap(holderlookup$provider).ifPresent(p_449804_ -> {
            Component component = ComponentUtils.mergeStyles(p_449804_.value().description(), Style.EMPTY.withColor(ChatFormatting.GRAY));
            p_344027_.accept(component);
         });
      }
   }

   public static InteractionResult tryInsertIntoJukebox(Level p_342790_, BlockPos p_344904_, ItemStack p_345065_, Player p_342036_) {
      JukeboxPlayable jukeboxplayable = p_345065_.get(DataComponents.JUKEBOX_PLAYABLE);
      if (jukeboxplayable == null) {
         return InteractionResult.TRY_WITH_EMPTY_HAND;
      }

      BlockState blockstate = p_342790_.getBlockState(p_344904_);
      if (blockstate.is(Blocks.JUKEBOX) && !blockstate.getValue(JukeboxBlock.HAS_RECORD)) {
         if (!p_342790_.isClientSide()) {
            ItemStack itemstack = p_345065_.consumeAndReturn(1, p_342036_);
            if (p_342790_.getBlockEntity(p_344904_) instanceof JukeboxBlockEntity jukeboxblockentity) {
               jukeboxblockentity.setTheItem(itemstack);
               p_342790_.gameEvent(GameEvent.BLOCK_CHANGE, p_344904_, GameEvent.Context.of(p_342036_, blockstate));
            }

            p_342036_.awardStat(Stats.PLAY_RECORD);
         }

         return InteractionResult.SUCCESS;
      } else {
         return InteractionResult.TRY_WITH_EMPTY_HAND;
      }
   }
}
