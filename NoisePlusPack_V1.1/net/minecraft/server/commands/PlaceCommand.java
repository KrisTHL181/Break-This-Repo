package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import java.util.Optional;
import net.minecraft.IdentifierException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.commands.arguments.ResourceKeyArgument;
import net.minecraft.commands.arguments.TemplateMirrorArgument;
import net.minecraft.commands.arguments.TemplateRotationArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.StructureBlockEntity;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockRotProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

public class PlaceCommand {
   private static final SimpleCommandExceptionType ERROR_FEATURE_FAILED = new SimpleCommandExceptionType(
      Component.translatable("commands.place.feature.failed")
   );
   private static final SimpleCommandExceptionType ERROR_JIGSAW_FAILED = new SimpleCommandExceptionType(Component.translatable("commands.place.jigsaw.failed"));
   private static final SimpleCommandExceptionType ERROR_STRUCTURE_FAILED = new SimpleCommandExceptionType(
      Component.translatable("commands.place.structure.failed")
   );
   private static final DynamicCommandExceptionType ERROR_TEMPLATE_INVALID = new DynamicCommandExceptionType(
      p_308787_ -> Component.translatableEscape("commands.place.template.invalid", p_308787_)
   );
   private static final SimpleCommandExceptionType ERROR_TEMPLATE_FAILED = new SimpleCommandExceptionType(
      Component.translatable("commands.place.template.failed")
   );
   private static final SuggestionProvider<CommandSourceStack> SUGGEST_TEMPLATES = (p_214552_, p_214553_) -> {
      StructureTemplateManager structuretemplatemanager = ((CommandSourceStack)p_214552_.getSource()).getLevel().getStructureManager();
      return SharedSuggestionProvider.suggestResource(structuretemplatemanager.listTemplates(), p_214553_);
   };

   public static void register(CommandDispatcher<CommandSourceStack> p_214548_) {
      p_214548_.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("place")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)))
                     .then(
                        Commands.literal("feature")
                           .then(
                              ((RequiredArgumentBuilder)Commands.argument("feature", ResourceKeyArgument.key(Registries.CONFIGURED_FEATURE))
                                    .executes(
                                       p_274824_ -> placeFeature(
                                          (CommandSourceStack)p_274824_.getSource(),
                                          ResourceKeyArgument.getConfiguredFeature(p_274824_, "feature"),
                                          BlockPos.containing(((CommandSourceStack)p_274824_.getSource()).getPosition())
                                       )
                                    ))
                                 .then(
                                    Commands.argument("pos", BlockPosArgument.blockPos())
                                       .executes(
                                          p_248163_ -> placeFeature(
                                             (CommandSourceStack)p_248163_.getSource(),
                                             ResourceKeyArgument.getConfiguredFeature(p_248163_, "feature"),
                                             BlockPosArgument.getLoadedBlockPos(p_248163_, "pos")
                                          )
                                       )
                                 )
                           )
                     ))
                  .then(
                     Commands.literal("jigsaw")
                        .then(
                           Commands.argument("pool", ResourceKeyArgument.key(Registries.TEMPLATE_POOL))
                              .then(
                                 Commands.argument("target", IdentifierArgument.id())
                                    .then(
                                       ((RequiredArgumentBuilder)Commands.argument("max_depth", IntegerArgumentType.integer(1, 20))
                                             .executes(
                                                p_448975_ -> placeJigsaw(
                                                   (CommandSourceStack)p_448975_.getSource(),
                                                   ResourceKeyArgument.getStructureTemplatePool(p_448975_, "pool"),
                                                   IdentifierArgument.getId(p_448975_, "target"),
                                                   IntegerArgumentType.getInteger(p_448975_, "max_depth"),
                                                   BlockPos.containing(((CommandSourceStack)p_448975_.getSource()).getPosition())
                                                )
                                             ))
                                          .then(
                                             Commands.argument("position", BlockPosArgument.blockPos())
                                                .executes(
                                                   p_448974_ -> placeJigsaw(
                                                      (CommandSourceStack)p_448974_.getSource(),
                                                      ResourceKeyArgument.getStructureTemplatePool(p_448974_, "pool"),
                                                      IdentifierArgument.getId(p_448974_, "target"),
                                                      IntegerArgumentType.getInteger(p_448974_, "max_depth"),
                                                      BlockPosArgument.getLoadedBlockPos(p_448974_, "position")
                                                   )
                                                )
                                          )
                                    )
                              )
                        )
                  ))
               .then(
                  Commands.literal("structure")
                     .then(
                        ((RequiredArgumentBuilder)Commands.argument("structure", ResourceKeyArgument.key(Registries.STRUCTURE))
                              .executes(
                                 p_274826_ -> placeStructure(
                                    (CommandSourceStack)p_274826_.getSource(),
                                    ResourceKeyArgument.getStructure(p_274826_, "structure"),
                                    BlockPos.containing(((CommandSourceStack)p_274826_.getSource()).getPosition())
                                 )
                              ))
                           .then(
                              Commands.argument("pos", BlockPosArgument.blockPos())
                                 .executes(
                                    p_248168_ -> placeStructure(
                                       (CommandSourceStack)p_248168_.getSource(),
                                       ResourceKeyArgument.getStructure(p_248168_, "structure"),
                                       BlockPosArgument.getLoadedBlockPos(p_248168_, "pos")
                                    )
                                 )
                           )
                     )
               ))
            .then(
               Commands.literal("template")
                  .then(
                     ((RequiredArgumentBuilder)Commands.argument("template", IdentifierArgument.id())
                           .suggests(SUGGEST_TEMPLATES)
                           .executes(
                              p_448987_ -> placeTemplate(
                                 (CommandSourceStack)p_448987_.getSource(),
                                 IdentifierArgument.getId(p_448987_, "template"),
                                 BlockPos.containing(((CommandSourceStack)p_448987_.getSource()).getPosition()),
                                 Rotation.NONE,
                                 Mirror.NONE,
                                 1.0F,
                                 0,
                                 false
                              )
                           ))
                        .then(
                           ((RequiredArgumentBuilder)Commands.argument("pos", BlockPosArgument.blockPos())
                                 .executes(
                                    p_448990_ -> placeTemplate(
                                       (CommandSourceStack)p_448990_.getSource(),
                                       IdentifierArgument.getId(p_448990_, "template"),
                                       BlockPosArgument.getLoadedBlockPos(p_448990_, "pos"),
                                       Rotation.NONE,
                                       Mirror.NONE,
                                       1.0F,
                                       0,
                                       false
                                    )
                                 ))
                              .then(
                                 ((RequiredArgumentBuilder)Commands.argument("rotation", TemplateRotationArgument.templateRotation())
                                       .executes(
                                          p_448978_ -> placeTemplate(
                                             (CommandSourceStack)p_448978_.getSource(),
                                             IdentifierArgument.getId(p_448978_, "template"),
                                             BlockPosArgument.getLoadedBlockPos(p_448978_, "pos"),
                                             TemplateRotationArgument.getRotation(p_448978_, "rotation"),
                                             Mirror.NONE,
                                             1.0F,
                                             0,
                                             false
                                          )
                                       ))
                                    .then(
                                       ((RequiredArgumentBuilder)Commands.argument("mirror", TemplateMirrorArgument.templateMirror())
                                             .executes(
                                                p_448980_ -> placeTemplate(
                                                   (CommandSourceStack)p_448980_.getSource(),
                                                   IdentifierArgument.getId(p_448980_, "template"),
                                                   BlockPosArgument.getLoadedBlockPos(p_448980_, "pos"),
                                                   TemplateRotationArgument.getRotation(p_448980_, "rotation"),
                                                   TemplateMirrorArgument.getMirror(p_448980_, "mirror"),
                                                   1.0F,
                                                   0,
                                                   false
                                                )
                                             ))
                                          .then(
                                             ((RequiredArgumentBuilder)Commands.argument("integrity", FloatArgumentType.floatArg(0.0F, 1.0F))
                                                   .executes(
                                                      p_448979_ -> placeTemplate(
                                                         (CommandSourceStack)p_448979_.getSource(),
                                                         IdentifierArgument.getId(p_448979_, "template"),
                                                         BlockPosArgument.getLoadedBlockPos(p_448979_, "pos"),
                                                         TemplateRotationArgument.getRotation(p_448979_, "rotation"),
                                                         TemplateMirrorArgument.getMirror(p_448979_, "mirror"),
                                                         FloatArgumentType.getFloat(p_448979_, "integrity"),
                                                         0,
                                                         false
                                                      )
                                                   ))
                                                .then(
                                                   ((RequiredArgumentBuilder)Commands.argument("seed", IntegerArgumentType.integer())
                                                         .executes(
                                                            p_448976_ -> placeTemplate(
                                                               (CommandSourceStack)p_448976_.getSource(),
                                                               IdentifierArgument.getId(p_448976_, "template"),
                                                               BlockPosArgument.getLoadedBlockPos(p_448976_, "pos"),
                                                               TemplateRotationArgument.getRotation(p_448976_, "rotation"),
                                                               TemplateMirrorArgument.getMirror(p_448976_, "mirror"),
                                                               FloatArgumentType.getFloat(p_448976_, "integrity"),
                                                               IntegerArgumentType.getInteger(p_448976_, "seed"),
                                                               false
                                                            )
                                                         ))
                                                      .then(
                                                         Commands.literal("strict")
                                                            .executes(
                                                               p_448977_ -> placeTemplate(
                                                                  (CommandSourceStack)p_448977_.getSource(),
                                                                  IdentifierArgument.getId(p_448977_, "template"),
                                                                  BlockPosArgument.getLoadedBlockPos(p_448977_, "pos"),
                                                                  TemplateRotationArgument.getRotation(p_448977_, "rotation"),
                                                                  TemplateMirrorArgument.getMirror(p_448977_, "mirror"),
                                                                  FloatArgumentType.getFloat(p_448977_, "integrity"),
                                                                  IntegerArgumentType.getInteger(p_448977_, "seed"),
                                                                  true
                                                               )
                                                            )
                                                      )
                                                )
                                          )
                                    )
                              )
                        )
                  )
            )
      );
   }

   public static int placeFeature(CommandSourceStack p_214576_, Holder.Reference<ConfiguredFeature<?, ?>> p_248822_, BlockPos p_214578_) throws CommandSyntaxException {
      ServerLevel serverlevel = p_214576_.getLevel();
      ConfiguredFeature<?, ?> configuredfeature = p_248822_.value();
      ChunkPos chunkpos = new ChunkPos(p_214578_);
      checkLoaded(serverlevel, new ChunkPos(chunkpos.x - 1, chunkpos.z - 1), new ChunkPos(chunkpos.x + 1, chunkpos.z + 1));
      if (!configuredfeature.place(serverlevel, serverlevel.getChunkSource().getGenerator(), serverlevel.getRandom(), p_214578_)) {
         throw ERROR_FEATURE_FAILED.create();
      }

      String s = p_248822_.key().identifier().toString();
      p_214576_.sendSuccess(() -> Component.translatable("commands.place.feature.success", s, p_214578_.getX(), p_214578_.getY(), p_214578_.getZ()), true);
      return 1;
   }

   public static int placeJigsaw(CommandSourceStack p_214570_, Holder<StructureTemplatePool> p_214571_, Identifier p_454618_, int p_214573_, BlockPos p_214574_) throws CommandSyntaxException {
      ServerLevel serverlevel = p_214570_.getLevel();
      ChunkPos chunkpos = new ChunkPos(p_214574_);
      checkLoaded(serverlevel, chunkpos, chunkpos);
      if (!JigsawPlacement.generateJigsaw(serverlevel, p_214571_, p_454618_, p_214573_, p_214574_, false)) {
         throw ERROR_JIGSAW_FAILED.create();
      }

      p_214570_.sendSuccess(() -> Component.translatable("commands.place.jigsaw.success", p_214574_.getX(), p_214574_.getY(), p_214574_.getZ()), true);
      return 1;
   }

   public static int placeStructure(CommandSourceStack p_214588_, Holder.Reference<Structure> p_251799_, BlockPos p_214590_) throws CommandSyntaxException {
      ServerLevel serverlevel = p_214588_.getLevel();
      Structure structure = p_251799_.value();
      ChunkGenerator chunkgenerator = serverlevel.getChunkSource().getGenerator();
      StructureStart structurestart = structure.generate(
         p_251799_,
         serverlevel.dimension(),
         p_214588_.registryAccess(),
         chunkgenerator,
         chunkgenerator.getBiomeSource(),
         serverlevel.getChunkSource().randomState(),
         serverlevel.getStructureManager(),
         serverlevel.getSeed(),
         new ChunkPos(p_214590_),
         0,
         serverlevel,
         p_214580_ -> true
      );
      if (!structurestart.isValid()) {
         throw ERROR_STRUCTURE_FAILED.create();
      }

      BoundingBox boundingbox = structurestart.getBoundingBox();
      ChunkPos chunkpos = new ChunkPos(SectionPos.blockToSectionCoord(boundingbox.minX()), SectionPos.blockToSectionCoord(boundingbox.minZ()));
      ChunkPos chunkpos1 = new ChunkPos(SectionPos.blockToSectionCoord(boundingbox.maxX()), SectionPos.blockToSectionCoord(boundingbox.maxZ()));
      checkLoaded(serverlevel, chunkpos, chunkpos1);
      ChunkPos.rangeClosed(chunkpos, chunkpos1)
         .forEach(
            p_448986_ -> structurestart.placeInChunk(
               serverlevel,
               serverlevel.structureManager(),
               chunkgenerator,
               serverlevel.getRandom(),
               new BoundingBox(
                  p_448986_.getMinBlockX(),
                  serverlevel.getMinY(),
                  p_448986_.getMinBlockZ(),
                  p_448986_.getMaxBlockX(),
                  serverlevel.getMaxY() + 1,
                  p_448986_.getMaxBlockZ()
               ),
               p_448986_
            )
         );
      String s = p_251799_.key().identifier().toString();
      p_214588_.sendSuccess(() -> Component.translatable("commands.place.structure.success", s, p_214590_.getX(), p_214590_.getY(), p_214590_.getZ()), true);
      return 1;
   }

   public static int placeTemplate(
      CommandSourceStack p_214562_,
      Identifier p_457055_,
      BlockPos p_214564_,
      Rotation p_214565_,
      Mirror p_214566_,
      float p_214567_,
      int p_214568_,
      boolean p_397563_
   ) throws CommandSyntaxException {
      ServerLevel serverlevel = p_214562_.getLevel();
      StructureTemplateManager structuretemplatemanager = serverlevel.getStructureManager();

      Optional<StructureTemplate> optional;
      try {
         optional = structuretemplatemanager.get(p_457055_);
      } catch (IdentifierException identifierexception) {
         throw ERROR_TEMPLATE_INVALID.create(p_457055_);
      }

      if (optional.isEmpty()) {
         throw ERROR_TEMPLATE_INVALID.create(p_457055_);
      }

      StructureTemplate structuretemplate = optional.get();
      checkLoaded(serverlevel, new ChunkPos(p_214564_), new ChunkPos(p_214564_.offset(structuretemplate.getSize())));
      StructurePlaceSettings structureplacesettings = new StructurePlaceSettings().setMirror(p_214566_).setRotation(p_214565_).setKnownShape(p_397563_);
      if (p_214567_ < 1.0F) {
         structureplacesettings.clearProcessors().addProcessor(new BlockRotProcessor(p_214567_)).setRandom(StructureBlockEntity.createRandom(p_214568_));
      }

      boolean flag = structuretemplate.placeInWorld(
         serverlevel, p_214564_, p_214564_, structureplacesettings, StructureBlockEntity.createRandom(p_214568_), 2 | (p_397563_ ? 816 : 0)
      );
      if (!flag) {
         throw ERROR_TEMPLATE_FAILED.create();
      }

      p_214562_.sendSuccess(
         () -> Component.translatable(
            "commands.place.template.success", Component.translationArg(p_457055_), p_214564_.getX(), p_214564_.getY(), p_214564_.getZ()
         ),
         true
      );
      return 1;
   }

   private static void checkLoaded(ServerLevel p_214544_, ChunkPos p_214545_, ChunkPos p_214546_) throws CommandSyntaxException {
      if (ChunkPos.rangeClosed(p_214545_, p_214546_).filter(p_448982_ -> !p_214544_.isLoaded(p_448982_.getWorldPosition())).findAny().isPresent()) {
         throw BlockPosArgument.ERROR_NOT_LOADED.create();
      }
   }
}
