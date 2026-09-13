package net.minecraft.server;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.ImmutableMap.Builder;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.functions.CommandFunction;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.tags.TagLoader;
import org.slf4j.Logger;

public class ServerFunctionLibrary implements PreparableReloadListener {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final ResourceKey<Registry<CommandFunction<CommandSourceStack>>> TYPE_KEY = ResourceKey.createRegistryKey(
      Identifier.withDefaultNamespace("function")
   );
   private static final FileToIdConverter LISTER = new FileToIdConverter(Registries.elementsDirPath(TYPE_KEY), ".mcfunction");
   private volatile Map<Identifier, CommandFunction<CommandSourceStack>> functions = ImmutableMap.of();
   private final TagLoader<CommandFunction<CommandSourceStack>> tagsLoader = new TagLoader<>(
      (p_448852_, p_448853_) -> this.getFunction(p_448852_), Registries.tagsDirPath(TYPE_KEY)
   );
   private volatile Map<Identifier, List<CommandFunction<CommandSourceStack>>> tags = Map.of();
   private final PermissionSet functionCompilationPermissions;
   private final CommandDispatcher<CommandSourceStack> dispatcher;

   public Optional<CommandFunction<CommandSourceStack>> getFunction(Identifier p_453284_) {
      return Optional.ofNullable(this.functions.get(p_453284_));
   }

   public Map<Identifier, CommandFunction<CommandSourceStack>> getFunctions() {
      return this.functions;
   }

   public List<CommandFunction<CommandSourceStack>> getTag(Identifier p_456469_) {
      return this.tags.getOrDefault(p_456469_, List.of());
   }

   public Iterable<Identifier> getAvailableTags() {
      return this.tags.keySet();
   }

   public ServerFunctionLibrary(PermissionSet p_460917_, CommandDispatcher<CommandSourceStack> p_136054_) {
      this.functionCompilationPermissions = p_460917_;
      this.dispatcher = p_136054_;
   }

   @Override
   public CompletableFuture<Void> reload(
      PreparableReloadListener.SharedState p_427662_, Executor p_136061_, PreparableReloadListener.PreparationBarrier p_136057_, Executor p_136062_
   ) {
      ResourceManager resourcemanager = p_427662_.resourceManager();
      CompletableFuture<Map<Identifier, List<TagLoader.EntryWithSource>>> completablefuture = CompletableFuture.supplyAsync(
         () -> this.tagsLoader.load(resourcemanager), p_136061_
      );
      CompletableFuture<Map<Identifier, CompletableFuture<CommandFunction<CommandSourceStack>>>> completablefuture1 = CompletableFuture.<Map<Identifier, Resource>>supplyAsync(
            () -> LISTER.listMatchingResources(resourcemanager), p_136061_
         )
         .thenCompose(p_448848_ -> {
            Map<Identifier, CompletableFuture<CommandFunction<CommandSourceStack>>> map = Maps.newHashMap();
            CommandSourceStack commandsourcestack = Commands.createCompilationContext(this.functionCompilationPermissions);

            for (Entry<Identifier, Resource> entry : p_448848_.entrySet()) {
               Identifier identifier = entry.getKey();
               Identifier identifier1 = LISTER.fileToId(identifier);
               map.put(identifier1, CompletableFuture.supplyAsync(() -> {
                  List<String> list = readLines(entry.getValue());
                  return CommandFunction.fromLines(identifier1, this.dispatcher, commandsourcestack, list);
               }, p_136061_));
            }

            CompletableFuture<?>[] completablefuture2 = map.values().toArray(new CompletableFuture[0]);
            return CompletableFuture.allOf(completablefuture2).handle((p_179949_, p_179950_) -> map);
         });
      return completablefuture.thenCombine(completablefuture1, Pair::of)
         .thenCompose(p_136057_::wait)
         .thenAcceptAsync(
            p_179944_ -> {
               Map<Identifier, CompletableFuture<CommandFunction<CommandSourceStack>>> map = (Map<Identifier, CompletableFuture<CommandFunction<CommandSourceStack>>>)p_179944_.getSecond();
               Builder<Identifier, CommandFunction<CommandSourceStack>> builder = ImmutableMap.builder();
               map.forEach((p_455551_, p_179942_) -> p_179942_.handle((p_311296_, p_179955_) -> {
                  if (p_179955_ != null) {
                     LOGGER.error("Failed to load function {}", p_455551_, p_179955_);
                  } else {
                     builder.put(p_455551_, p_311296_);
                  }

                  return null;
               }).join());
               this.functions = builder.build();
               this.tags = this.tagsLoader.build((Map<Identifier, List<TagLoader.EntryWithSource>>)p_179944_.getFirst());
            },
            p_136062_
         );
   }

   private static List<String> readLines(Resource p_214317_) {
      try (BufferedReader bufferedreader = p_214317_.openAsReader()) {
         return bufferedreader.lines().toList();
      } catch (IOException ioexception) {
         throw new CompletionException(ioexception);
      }
   }
}
