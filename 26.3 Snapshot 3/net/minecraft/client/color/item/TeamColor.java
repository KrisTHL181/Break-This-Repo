package net.minecraft.client.color.item;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.ARGB;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.scores.Team;
import org.jspecify.annotations.Nullable;

public record TeamColor(int defaultColor) implements ItemTintSource {
   public static final MapCodec<TeamColor> MAP_CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(ExtraCodecs.RGB_COLOR_CODEC.fieldOf("default").forGetter(TeamColor::defaultColor)).apply(i, TeamColor::new)
   );

   @Override
   public int calculate(final ItemStack itemStack, final @Nullable ClientLevel level, final @Nullable LivingEntity owner) {
      if (owner != null) {
         Team team = owner.getTeam();
         if (team != null) {
            Optional<net.minecraft.world.scores.TeamColor> color = team.getColor();
            if (color.isPresent()) {
               return ARGB.opaque(color.get().rgb());
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
