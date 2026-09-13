package net.minecraft.server.commands;

import com.google.common.base.Stopwatch;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import java.time.Duration;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceOrTagArgument;
import net.minecraft.commands.arguments.ResourceOrTagKeyArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.structure.Structure;
import org.slf4j.Logger;

public class LocateCommand {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final DynamicCommandExceptionType ERROR_STRUCTURE_NOT_FOUND = new DynamicCommandExceptionType(
      p_308765_ -> Component.translatableEscape("commands.locate.structure.not_found", p_308765_)
   );
   private static final DynamicCommandExceptionType ERROR_STRUCTURE_INVALID = new DynamicCommandExceptionType(
      p_308764_ -> Component.translatableEscape("commands.locate.structure.invalid", p_308764_)
   );
   private static final DynamicCommandExceptionType ERROR_BIOME_NOT_FOUND = new DynamicCommandExceptionType(
      p_308763_ -> Component.translatableEscape("commands.locate.biome.not_found", p_308763_)
   );
   private static final DynamicCommandExceptionType ERROR_POI_NOT_FOUND = new DynamicCommandExceptionType(
      p_308766_ -> Component.translatableEscape("commands.locate.poi.not_found", p_308766_)
   );
   private static final int MAX_STRUCTURE_SEARCH_RADIUS = 100;
   private static final int MAX_BIOME_SEARCH_RADIUS = 6400;
   private static final int BIOME_SAMPLE_RESOLUTION_HORIZONTAL = 32;
   private static final int BIOME_SAMPLE_RESOLUTION_VERTICAL = 64;
   private static final int POI_SEARCH_RADIUS = 256;

   public static void register(CommandDispatcher<CommandSourceStack> p_249870_, CommandBuildContext p_248936_) {
      p_249870_.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("locate")
                     .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)))
                  .then(
                     Commands.literal("structure")
                        .then(
                           Commands.argument("structure", ResourceOrTagKeyArgument.resourceOrTagKey(Registries.STRUCTURE))
                              .executes(
                                 p_258233_ -> locateStructure(
                                    (CommandSourceStack)p_258233_.getSource(),
                                    ResourceOrTagKeyArgument.getResourceOrTagKey(p_258233_, "structure", Registries.STRUCTURE, ERROR_STRUCTURE_INVALID)
                                 )
                              )
                        )
                  ))
               .then(
                  Commands.literal("biome")
                     .then(
                        Commands.argument("biome", ResourceOrTagArgument.resourceOrTag(p_248936_, Registries.BIOME))
                           .executes(
                              p_258232_ -> locateBiome(
                                 (CommandSourceStack)p_258232_.getSource(), ResourceOrTagArgument.getResourceOrTag(p_258232_, "biome", Registries.BIOME)
                              )
                           )
                     )
               ))
            .then(
               Commands.literal("poi")
                  .then(
                     Commands.argument("poi", ResourceOrTagArgument.resourceOrTag(p_248936_, Registries.POINT_OF_INTEREST_TYPE))
                        .executes(
                           p_258234_ -> locatePoi(
                              (CommandSourceStack)p_258234_.getSource(),
                              ResourceOrTagArgument.getResourceOrTag(p_258234_, "poi", Registries.POINT_OF_INTEREST_TYPE)
                           )
                        )
                  )
            )
      );
   }

   private static Optional<? extends HolderSet.ListBacked<Structure>> getHolders(
      ResourceOrTagKeyArgument.Result<Structure> p_251212_, Registry<Structure> p_249691_
   ) {
      return (Optional<? extends HolderSet.ListBacked<Structure>>)p_251212_.unwrap()
         .map(p_358601_ -> p_249691_.get(p_358601_).map(p_214491_ -> HolderSet.direct(p_214491_)), p_249691_::get);
   }

   private static int locateStructure(CommandSourceStack p_214472_, ResourceOrTagKeyArgument.Result<Structure> p_249893_) throws CommandSyntaxException {
      Registry<Structure> registry = p_214472_.getLevel().registryAccess().lookupOrThrow(Registries.STRUCTURE);
      HolderSet<Structure> holderset = (HolderSet<Structure>)getHolders(p_249893_, registry)
         .orElseThrow(() -> ERROR_STRUCTURE_INVALID.create(p_249893_.asPrintable()));
      BlockPos blockpos = BlockPos.containing(p_214472_.getPosition());
      ServerLevel serverlevel = p_214472_.getLevel();
      Stopwatch stopwatch = Stopwatch.createStarted(Util.TICKER);
      Pair<BlockPos, Holder<Structure>> pair = serverlevel.getChunkSource()
         .getGenerator()
         .findNearestMapStructure(serverlevel, holderset, blockpos, 100, false);
      stopwatch.stop();
      if (pair == null) {
         throw ERROR_STRUCTURE_NOT_FOUND.create(p_249893_.asPrintable());
      } else {
         return showLocateResult(p_214472_, p_249893_, blockpos, pair, "commands.locate.structure.success", false, stopwatch.elapsed());
      }
   }

   private static int locateBiome(CommandSourceStack p_252062_, ResourceOrTagArgument.Result<Biome> p_249756_) throws CommandSyntaxException {
      BlockPos blockpos = BlockPos.containing(p_252062_.getPosition());
      Stopwatch stopwatch = Stopwatch.createStarted(Util.TICKER);
      Pair<BlockPos, Holder<Biome>> pair = p_252062_.getLevel().findClosestBiome3d(p_249756_, blockpos, 6400, 32, 64);
      stopwatch.stop();
      if (pair == null) {
         throw ERROR_BIOME_NOT_FOUND.create(p_249756_.asPrintable());
      } else {
         return showLocateResult(p_252062_, p_249756_, blockpos, pair, "commands.locate.biome.success", true, stopwatch.elapsed());
      }
   }

   private static int locatePoi(CommandSourceStack p_252013_, ResourceOrTagArgument.Result<PoiType> p_249480_) throws CommandSyntaxException {
      BlockPos blockpos = BlockPos.containing(p_252013_.getPosition());
      ServerLevel serverlevel = p_252013_.getLevel();
      Stopwatch stopwatch = Stopwatch.createStarted(Util.TICKER);
      Optional<Pair<Holder<PoiType>, BlockPos>> optional = serverlevel.getPoiManager().findClosestWithType(p_249480_, blockpos, 256, PoiManager.Occupancy.ANY);
      stopwatch.stop();
      if (optional.isEmpty()) {
         throw ERROR_POI_NOT_FOUND.create(p_249480_.asPrintable());
      } else {
         return showLocateResult(p_252013_, p_249480_, blockpos, optional.get().swap(), "commands.locate.poi.success", false, stopwatch.elapsed());
      }
   }

   public static int showLocateResult(
      CommandSourceStack p_263098_,
      ResourceOrTagArgument.Result<?> p_262956_,
      BlockPos p_262917_,
      Pair<BlockPos, ? extends Holder<?>> p_263074_,
      String p_262937_,
      boolean p_263051_,
      Duration p_263028_
   ) {
      String s = (String)p_262956_.unwrap()
         .map(p_248147_ -> p_262956_.asPrintable(), p_326290_ -> p_262956_.asPrintable() + " (" + ((Holder)p_263074_.getSecond()).getRegisteredName() + ")");
      return showLocateResult(p_263098_, p_262917_, p_263074_, p_262937_, p_263051_, s, p_263028_);
   }

   public static int showLocateResult(
      CommandSourceStack p_263019_,
      ResourceOrTagKeyArgument.Result<?> p_263031_,
      BlockPos p_262989_,
      Pair<BlockPos, ? extends Holder<?>> p_262959_,
      String p_263045_,
      boolean p_262934_,
      Duration p_262960_
   ) {
      String s = (String)p_263031_.unwrap()
         .map(
            p_448965_ -> p_448965_.identifier().toString(),
            p_448964_ -> "#" + p_448964_.location() + " (" + ((Holder)p_262959_.getSecond()).getRegisteredName() + ")"
         );
      return showLocateResult(p_263019_, p_262989_, p_262959_, p_263045_, p_262934_, s, p_262960_);
   }

   private static int showLocateResult(
      CommandSourceStack p_262983_,
      BlockPos p_263016_,
      Pair<BlockPos, ? extends Holder<?>> p_262941_,
      String p_263083_,
      boolean p_263010_,
      String p_263048_,
      Duration p_263040_
   ) {
      BlockPos blockpos = (BlockPos)p_262941_.getFirst();
      int i = p_263010_
         ? Mth.floor(Mth.sqrt((float)p_263016_.distSqr(blockpos)))
         : Mth.floor(dist(p_263016_.getX(), p_263016_.getZ(), blockpos.getX(), blockpos.getZ()));
      String s = p_263010_ ? String.valueOf(blockpos.getY()) : "~";
      Component component = ComponentUtils.wrapInSquareBrackets(Component.translatable("chat.coordinates", blockpos.getX(), s, blockpos.getZ()))
         .withStyle(
            p_390062_ -> p_390062_.withColor(ChatFormatting.GREEN)
               .withClickEvent(new ClickEvent.SuggestCommand("/tp @s " + blockpos.getX() + " " + s + " " + blockpos.getZ()))
               .withHoverEvent(new HoverEvent.ShowText(Component.translatable("chat.coordinates.tooltip")))
         );
      p_262983_.sendSuccess(() -> Component.translatable(p_263083_, p_263048_, component, i), false);
      LOGGER.info("Locating element {} took {} ms", p_263048_, p_263040_.toMillis());
      return i;
   }

   private static float dist(int p_137854_, int p_137855_, int p_137856_, int p_137857_) {
      int i = p_137856_ - p_137854_;
      int j = p_137857_ - p_137855_;
      return Mth.sqrt(i * i + j * j);
   }
}
