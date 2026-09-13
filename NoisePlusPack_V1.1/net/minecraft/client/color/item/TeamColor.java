package net.minecraft.client.color.item;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.ARGB;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.scores.Team;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public record TeamColor(int defaultColor) implements ItemTintSource {
   public static final MapCodec<TeamColor> MAP_CODEC = RecordCodecBuilder.mapCodec(
      p_377470_ -> p_377470_.group(ExtraCodecs.RGB_COLOR_CODEC.fieldOf("default").forGetter(TeamColor::defaultColor)).apply(p_377470_, TeamColor::new)
   );

   @Override
   public int calculate(ItemStack p_376346_, @Nullable ClientLevel p_376497_, @Nullable LivingEntity p_377938_) {
      if (p_377938_ != null) {
         Team team = p_377938_.getTeam();
         if (team != null) {
            ChatFormatting chatformatting = team.getColor();
            if (chatformatting.getColor() != null) {
               return ARGB.opaque(chatformatting.getColor());
            }
         }
      }

      return ARGB.opaque(this.defaultColor);
   }

   @Override
   public MapCodec<TeamColor> type() {
      return MAP_CODEC;
   }
}
