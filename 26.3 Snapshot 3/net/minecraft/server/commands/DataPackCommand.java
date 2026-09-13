package net.minecraft.server.commands;

import com.google.common.collect.Lists;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.DataResult.Error;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import net.minecraft.SharedConstants;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ComponentArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.util.FileUtil;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;

public class DataPackCommand {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final DynamicCommandExceptionType ERROR_UNKNOWN_PACK = new DynamicCommandExceptionType(
      id -> Component.translatableEscape("commands.datapack.unknown", id)
   );
   private static final DynamicCommandExceptionType ERROR_PACK_ALREADY_ENABLED = new DynamicCommandExceptionType(
      id -> Component.translatableEscape("commands.datapack.enable.failed", id)
   );
   private static final DynamicCommandExceptionType ERROR_PACK_ALREADY_DISABLED = new DynamicCommandExceptionType(
      id -> Component.translatableEscape("commands.datapack.disable.failed", id)
   );
   private static final DynamicCommandExceptionType ERROR_CANNOT_DISABLE_FEATURE = new DynamicCommandExceptionType(
      id -> Component.translatableEscape("commands.datapack.disable.failed.feature", id)
   );
   private static final Dynamic2CommandExceptionType ERROR_PACK_FEATURES_NOT_ENABLED = new Dynamic2CommandExceptionType(
      (id, flags) -> Component.translatableEscape("commands.datapack.enable.failed.no_flags", id, flags)
   );
   private static final DynamicCommandExceptionType ERROR_PACK_INVALID_NAME = new DynamicCommandExceptionType(
      id -> Component.translatableEscape("commands.datapack.create.invalid_name", id)
   );
   private static final DynamicCommandExceptionType ERROR_PACK_INVALID_FULL_NAME = new DynamicCommandExceptionType(
      id -> Component.translatableEscape("commands.datapack.create.invalid_full_name", id)
   );
   private static final DynamicCommandExceptionType ERROR_PACK_ALREADY_EXISTS = new DynamicCommandExceptionType(
      id -> Component.translatableEscape("commands.datapack.create.already_exists", id)
   );
   private static final Dynamic2CommandExceptionType ERROR_PACK_METADATA_ENCODE_FAILURE = new Dynamic2CommandExceptionType(
      (id, error) -> Component.translatableEscape("commands.datapack.create.metadata_encode_failure", id, error)
   );
   private static final DynamicCommandExceptionType ERROR_PACK_IO_FAILURE = new DynamicCommandExceptionType(
      id -> Component.translatableEscape("commands.datapack.create.io_failure", id)
   );
   private static final SuggestionProvider<CommandSourceStack> SELECTED_PACKS = (c, p) -> SharedSuggestionProvider.suggest(
      ((CommandSourceStack)c.getSource()).getServer().getPackRepository().getSelectedIds().stream().map(StringArgumentType::escapeIfRequired), p
   );
   private static final SuggestionProvider<CommandSourceStack> UNSELECTED_PACKS = (c, p) -> {
      PackRepository packRepository = ((CommandSourceStack)c.getSource()).getServer().getPackRepository();
      Collection<String> selectedIds = packRepository.getSelectedIds();
      FeatureFlagSet enabledFeatures = ((CommandSourceStack)c.getSource()).enabledFeatures();
      return SharedSuggestionProvider.suggest(
         packRepository.getAvailablePacks()
            .stream()
            .filter(pack -> pack.getRequestedFeatures().isSubsetOf(enabledFeatures))
            .map(Pack::getId)
            .filter(id -> !selectedIds.contains(id))
            .map(StringArgumentType::escapeIfRequired),
         p
      );
   };

   public static void register(final CommandDispatcher<CommandSourceStack> dispatcher, final CommandBuildContext context) {
      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal(
                           "datapack"
                        )
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)))
                     .then(
                        Commands.literal("enable")
                           .then(
                              ((RequiredArgumentBuilder)((RequiredArgumentBuilder)((RequiredArgumentBuilder)((RequiredArgumentBuilder)Commands.argument(
                                                "name", StringArgumentType.string()
                                             )
                                             .suggests(UNSELECTED_PACKS)
                                             .executes(
                                                c -> enablePack(
                                                   (CommandSourceStack)c.getSource(),
                                                   getPack(c, "name", true),
                                                   (l, p) -> p.getDefaultPosition().insert(l, p, Pack::selectionConfig, false)
                                                )
                                             ))
                                          .then(
                                             Commands.literal("after")
                                                .then(
                                                   Commands.argument("existing", StringArgumentType.string())
                                                      .suggests(SELECTED_PACKS)
                                                      .executes(
                                                         c -> enablePack(
                                                            (CommandSourceStack)c.getSource(),
                                                            getPack(c, "name", true),
                                                            (l, p) -> l.add(l.indexOf(getPack(c, "existing", false)) + 1, p)
                                                         )
                                                      )
                                                )
                                          ))
                                       .then(
                                          Commands.literal("before")
                                             .then(
                                                Commands.argument("existing", StringArgumentType.string())
                                                   .suggests(SELECTED_PACKS)
                                                   .executes(
                                                      c -> enablePack(
                                                         (CommandSourceStack)c.getSource(),
                                                         getPack(c, "name", true),
                                                         (l, p) -> l.add(l.indexOf(getPack(c, "existing", false)), p)
                                                      )
                                                   )
                                             )
                                       ))
                                    .then(
                                       Commands.literal("last")
                                          .executes(c -> enablePack((CommandSourceStack)c.getSource(), getPack(c, "name", true), List::add))
                                    ))
                                 .then(
                                    Commands.literal("first")
                                       .executes(c -> enablePack((CommandSourceStack)c.getSource(), getPack(c, "name", true), (l, p) -> l.add(0, p)))
                                 )
                           )
                     ))
                  .then(
                     Commands.literal("disable")
                        .then(
                           Commands.argument("name", StringArgumentType.string())
                              .suggests(SELECTED_PACKS)
                              .executes(c -> disablePack((CommandSourceStack)c.getSource(), getPack(c, "name", false)))
                        )
                  ))
               .then(
                  ((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("list").executes(c -> listPacks((CommandSourceStack)c.getSource())))
                        .then(Commands.literal("available").executes(c -> listAvailablePacks((CommandSourceStack)c.getSource()))))
                     .then(Commands.literal("enabled").executes(c -> listEnabledPacks((CommandSourceStack)c.getSource())))
               ))
            .then(
               ((LiteralArgumentBuilder)Commands.literal("create").requires(Commands.hasPermission(Commands.LEVEL_OWNERS)))
                  .then(
                     Commands.argument("id", StringArgumentType.string())
                        .then(
                           Commands.argument("description", ComponentArgument.textComponent(context))
                              .executes(
                                 c -> createPack(
                                    (CommandSourceStack)c.getSource(),
                                    StringArgumentType.getString(c, "id"),
                                    ComponentArgument.getResolvedComponent(c, "description")
                                 )
                              )
                        )
                  )
            )
      );
   }

   private static int createPack(final CommandSourceStack source, final String id, final Component description) throws CommandSyntaxException {
      Path datapackDir = source.getServer().getWorldPath(LevelResource.DATAPACK_DIR);
      if (!FileUtil.isValidPathSegment(id)) {
         throw ERROR_PACK_INVALID_NAME.create(id);
      }

      if (!FileUtil.isPathPartPortable(id)) {
         throw ERROR_PACK_INVALID_FULL_NAME.create(id);
      }

      Path packDir = datapackDir.resolve(id);
      if (Files.exists(packDir)) {
         throw ERROR_PACK_ALREADY_EXISTS.create(id);
      }

      PackMetadataSection packMetadataSection = new PackMetadataSection(
         description, SharedConstants.getCurrentVersion().packVersion(PackType.SERVER_DATA).minorRange()
      );
      DataResult<JsonElement> encodedMeta = PackMetadataSection.SERVER_TYPE.codec().encodeStart(JsonOps.INSTANCE, packMetadataSection);
      Optional<Error<JsonElement>> error = encodedMeta.error();
      if (error.isPresent()) {
         throw ERROR_PACK_METADATA_ENCODE_FAILURE.create(id, error.get().message());
      }

      JsonObject topMcmeta = new JsonObject();
      topMcmeta.add(PackMetadataSection.SERVER_TYPE.name(), (JsonElement)encodedMeta.getOrThrow());

      try {
         Files.createDirectory(packDir);
         Files.createDirectory(packDir.resolve(PackType.SERVER_DATA.getDirectory()));

         try (BufferedWriter mcmetaFile = Files.newBufferedWriter(packDir.resolve("pack.mcmeta"), StandardCharsets.UTF_8)) {
            JsonWriter jsonWriter = new JsonWriter(mcmetaFile);

            try {
               jsonWriter.setSerializeNulls(false);
               jsonWriter.setIndent("  ");
               GsonHelper.writeValue(jsonWriter, topMcmeta, null);
            } catch (Throwable var15) {
               try {
                  jsonWriter.close();
               } catch (Throwable var14) {
                  var15.addSuppressed(var14);
               }

               throw var15;
            }

            jsonWriter.close();
         }
      } catch (IOException e) {
         LOGGER.warn("Failed to create pack at {}", datapackDir.toAbsolutePath(), e);
         throw ERROR_PACK_IO_FAILURE.create(id);
      }

      source.sendSuccess(() -> Component.translatable("commands.datapack.create.success", id), true);
      return 1;
   }

   private static int enablePack(final CommandSourceStack source, final Pack unopened, final DataPackCommand.Inserter inserter) throws CommandSyntaxException {
      PackRepository packRepository = source.getServer().getPackRepository();
      List<Pack> selected = Lists.newArrayList(packRepository.getSelectedPacks());
      inserter.apply(selected, unopened);
      source.sendSuccess(() -> Component.translatable("commands.datapack.modify.enable", unopened.getChatLink(true)), true);
      ReloadCommand.reloadPacks(selected.stream().map(Pack::getId).collect(Collectors.toList()), source);
      return selected.size();
   }

   private static int disablePack(final CommandSourceStack source, final Pack unopened) {
      PackRepository packRepository = source.getServer().getPackRepository();
      List<Pack> selected = Lists.newArrayList(packRepository.getSelectedPacks());
      selected.remove(unopened);
      source.sendSuccess(() -> Component.translatable("commands.datapack.modify.disable", unopened.getChatLink(true)), true);
      ReloadCommand.reloadPacks(selected.stream().map(Pack::getId).collect(Collectors.toList()), source);
      return selected.size();
   }

   private static int listPacks(final CommandSourceStack source) {
      return listEnabledPacks(source) + listAvailablePacks(source);
   }

   private static int listAvailablePacks(final CommandSourceStack source) {
      PackRepository repository = source.getServer().getPackRepository();
      repository.reload();
      Collection<Pack> selectedPacks = repository.getSelectedPacks();
      Collection<Pack> availablePacks = repository.getAvailablePacks();
      FeatureFlagSet enabledFeatures = source.enabledFeatures();
      List<Pack> unselectedPacks = availablePacks.stream()
         .filter(p -> !selectedPacks.contains(p) && p.getRequestedFeatures().isSubsetOf(enabledFeatures))
         .toList();
      if (unselectedPacks.isEmpty()) {
         source.sendSuccess(() -> Component.translatable("commands.datapack.list.available.none"), false);
      } else {
         source.sendSuccess(
            () -> Component.translatable(
               "commands.datapack.list.available.success", unselectedPacks.size(), ComponentUtils.formatList(unselectedPacks, p -> p.getChatLink(false))
            ),
            false
         );
      }

      return unselectedPacks.size();
   }

   private static int listEnabledPacks(final CommandSourceStack source) {
      PackRepository repository = source.getServer().getPackRepository();
      repository.reload();
      Collection<? extends Pack> selectedPacks = repository.getSelectedPacks();
      if (selectedPacks.isEmpty()) {
         source.sendSuccess(() -> Component.translatable("commands.datapack.list.enabled.none"), false);
      } else {
         source.sendSuccess(
            () -> Component.translatable(
               "commands.datapack.list.enabled.success", selectedPacks.size(), ComponentUtils.formatList(selectedPacks, p -> p.getChatLink(true))
            ),
            false
         );
      }

      return selectedPacks.size();
   }

   private static Pack getPack(final CommandContext<CommandSourceStack> context, final String name, final boolean enabling) throws CommandSyntaxException {
      String id = StringArgumentType.getString(context, name);
      PackRepository repository = ((CommandSourceStack)context.getSource()).getServer().getPackRepository();
      Pack pack = repository.getPack(id);
      if (pack == null) {
         throw ERROR_UNKNOWN_PACK.create(id);
      } else {
         boolean enabled = repository.getSelectedPacks().contains(pack);
         if (enabling && enabled) {
            throw ERROR_PACK_ALREADY_ENABLED.create(id);
         } else if (!enabling && !enabled) {
            throw ERROR_PACK_ALREADY_DISABLED.create(id);
         } else {
            FeatureFlagSet availableFeatures = ((CommandSourceStack)context.getSource()).enabledFeatures();
            FeatureFlagSet requestedFeatures = pack.getRequestedFeatures();
            if (!enabling && !requestedFeatures.isEmpty() && pack.getPackSource() == PackSource.FEATURE) {
               throw ERROR_CANNOT_DISABLE_FEATURE.create(id);
            } else if (!requestedFeatures.isSubsetOf(availableFeatures)) {
               throw ERROR_PACK_FEATURES_NOT_ENABLED.create(id, FeatureFlags.printMissingFlags(availableFeatures, requestedFeatures));
            } else {
               return pack;
            }
         }
      }
   }

   private interface Inserter {
      void apply(final List<Pack> selected, final Pack pack) throws CommandSyntaxException;
   }
}
