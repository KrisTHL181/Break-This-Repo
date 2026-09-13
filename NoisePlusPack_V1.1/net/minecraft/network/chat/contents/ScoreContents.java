package net.minecraft.network.chat.contents;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.selector.SelectorPattern;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.numbers.StyledFormat;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ReadOnlyScoreInfo;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.Scoreboard;
import org.jspecify.annotations.Nullable;

public record ScoreContents(Either<SelectorPattern, String> name, String objective) implements ComponentContents {
   public static final MapCodec<ScoreContents> INNER_CODEC = RecordCodecBuilder.mapCodec(
      p_358479_ -> p_358479_.group(
            Codec.either(SelectorPattern.CODEC, Codec.STRING).fieldOf("name").forGetter(ScoreContents::name),
            Codec.STRING.fieldOf("objective").forGetter(ScoreContents::objective)
         )
         .apply(p_358479_, ScoreContents::new)
   );
   public static final MapCodec<ScoreContents> MAP_CODEC = INNER_CODEC.fieldOf("score");

   @Override
   public MapCodec<ScoreContents> codec() {
      return MAP_CODEC;
   }

   private ScoreHolder findTargetName(CommandSourceStack p_237442_) throws CommandSyntaxException {
      Optional<SelectorPattern> optional = this.name.left();
      if (optional.isPresent()) {
         List<? extends Entity> list = optional.get().resolved().findEntities(p_237442_);
         if (!list.isEmpty()) {
            if (list.size() != 1) {
               throw EntityArgument.ERROR_NOT_SINGLE_ENTITY.create();
            } else {
               return list.getFirst();
            }
         } else {
            return ScoreHolder.forNameOnly(optional.get().pattern());
         }
      } else {
         return ScoreHolder.forNameOnly((String)this.name.right().orElseThrow());
      }
   }

   private MutableComponent getScore(ScoreHolder p_312678_, CommandSourceStack p_237451_) {
      MinecraftServer minecraftserver = p_237451_.getServer();
      if (minecraftserver != null) {
         Scoreboard scoreboard = minecraftserver.getScoreboard();
         Objective objective = scoreboard.getObjective(this.objective);
         if (objective != null) {
            ReadOnlyScoreInfo readonlyscoreinfo = scoreboard.getPlayerScoreInfo(p_312678_, objective);
            if (readonlyscoreinfo != null) {
               return readonlyscoreinfo.formatValue(objective.numberFormatOrDefault(StyledFormat.NO_STYLE));
            }
         }
      }

      return Component.empty();
   }

   @Override
   public MutableComponent resolve(@Nullable CommandSourceStack p_237444_, @Nullable Entity p_237445_, int p_237446_) throws CommandSyntaxException {
      if (p_237444_ == null) {
         return Component.empty();
      }

      ScoreHolder scoreholder = this.findTargetName(p_237444_);
      ScoreHolder scoreholder1 = p_237445_ != null && scoreholder.equals(ScoreHolder.WILDCARD) ? p_237445_ : scoreholder;
      return this.getScore(scoreholder1, p_237444_);
   }

   @Override
   public String toString() {
      return "score{name='" + this.name + "', objective='" + this.objective + "'}";
   }
}
