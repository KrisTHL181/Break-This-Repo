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
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.ResolutionContext;
import net.minecraft.network.chat.numbers.StyledFormat;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.CompilableString;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ReadOnlyScoreInfo;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.Scoreboard;

public record ScoreContents(Either<CompilableString<EntitySelector>, String> name, String objective) implements ComponentContents {
   public static final MapCodec<ScoreContents> INNER_CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(
            Codec.either(EntitySelector.COMPILABLE_CODEC, Codec.STRING).fieldOf("name").forGetter(ScoreContents::name),
            Codec.STRING.fieldOf("objective").forGetter(ScoreContents::objective)
         )
         .apply(i, ScoreContents::new)
   );
   public static final MapCodec<ScoreContents> MAP_CODEC = INNER_CODEC.fieldOf("score");

   @Override
   public MapCodec<ScoreContents> codec() {
      return MAP_CODEC;
   }

   private ScoreHolder findTargetName(final CommandSourceStack source) throws CommandSyntaxException {
      Optional<CompilableString<EntitySelector>> selector = this.name.left();
      if (selector.isPresent()) {
         List<? extends Entity> entities = selector.get().compiled().findEntities(source);
         if (!entities.isEmpty()) {
            if (entities.size() != 1) {
               throw EntityArgument.ERROR_NOT_SINGLE_ENTITY.create();
            } else {
               return entities.getFirst();
            }
         } else {
            return ScoreHolder.forNameOnly(selector.get().source());
         }
      } else {
         return ScoreHolder.forNameOnly((String)this.name.right().orElseThrow());
      }
   }

   private MutableComponent getScore(final ScoreHolder name, final CommandSourceStack source) {
      MinecraftServer server = source.getServer();
      if (server != null) {
         Scoreboard scoreboard = server.getScoreboard();
         Objective objective = scoreboard.getObjective(this.objective);
         if (objective != null) {
            ReadOnlyScoreInfo scoreInfo = scoreboard.getPlayerScoreInfo(name, objective);
            if (scoreInfo != null) {
               return scoreInfo.formatValue(objective.numberFormatOrDefault(StyledFormat.NO_STYLE));
            }
         }
      }

      return Component.empty();
   }

   @Override
   public MutableComponent resolve(final ResolutionContext context, final int recursionDepth) throws CommandSyntaxException {
      CommandSourceStack source = context.source();
      if (source == null) {
         return Component.empty();
      }

      ScoreHolder scoreHolder = this.findTargetName(source);
      Entity entity = context.defaultScoreboardEntity();
      ScoreHolder scoreName = entity != null && scoreHolder.equals(ScoreHolder.WILDCARD) ? entity : scoreHolder;
      return this.getScore(scoreName, source);
   }

   @Override
   public String toString() {
      return "score{name='" + this.name + "', objective='" + this.objective + "'}";
   }
}
