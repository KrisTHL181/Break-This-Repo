package net.minecraft.client.resources.model;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import java.io.Reader;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelDispatcher;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.StrictJsonParser;
import net.minecraft.util.Util;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import org.slf4j.Logger;

public class BlockStateModelLoader {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final FileToIdConverter BLOCKSTATE_LISTER = FileToIdConverter.json("blockstates");

   public static CompletableFuture<BlockStateModelLoader.LoadedModels> loadBlockStates(final ResourceManager manager, final Executor executor) {
      Function<Identifier, StateDefinition<Block, BlockState>> definitionToBlockState = BlockStateDefinitions.definitionLocationToBlockStateMapper();
      return CompletableFuture.<Map<Identifier, List<Resource>>>supplyAsync(() -> BLOCKSTATE_LISTER.listMatchingResourceStacks(manager), executor)
         .thenCompose(
            resources -> {
               List<CompletableFuture<BlockStateModelLoader.LoadedModels>> result = new ArrayList<>(resources.size());

               for (Entry<Identifier, List<Resource>> resourceStack : resources.entrySet()) {
                  result.add(
                     CompletableFuture.supplyAsync(
                        () -> {
                           Identifier stateDefinitionId = BLOCKSTATE_LISTER.fileToId(resourceStack.getKey());
                           StateDefinition<Block, BlockState> stateDefinition = definitionToBlockState.apply(stateDefinitionId);
                           if (stateDefinition == null) {
                              LOGGER.debug("Discovered unknown block state definition {}, ignoring", stateDefinitionId);
                              return null;
                           }

                           List<Resource> stack = resourceStack.getValue();
                           List<BlockStateModelLoader.LoadedBlockStateModelDispatcher> loadedStack = new ArrayList<>(stack.size());

                           for (Resource resource : stack) {
                              try (Reader reader = resource.openAsReader()) {
                                 JsonElement element = StrictJsonParser.parse(reader);
                                 BlockStateModelDispatcher definition = (BlockStateModelDispatcher)BlockStateModelDispatcher.CODEC
                                    .parse(JsonOps.INSTANCE, element)
                                    .getOrThrow(JsonParseException::new);
                                 loadedStack.add(new BlockStateModelLoader.LoadedBlockStateModelDispatcher(resource.sourcePackId(), definition));
                              } catch (Exception e) {
                                 LOGGER.error(
                                    "Failed to load blockstate definition {} from pack {}", new Object[]{stateDefinitionId, resource.sourcePackId(), e}
                                 );
                              }
                           }

                           try {
                              return loadBlockStateDefinitionStack(stateDefinitionId, stateDefinition, loadedStack);
                           } catch (Exception e) {
                              LOGGER.error("Failed to load blockstate definition {}", stateDefinitionId, e);
                              return null;
                           }
                        },
                        executor
                     )
                  );
               }

               return Util.sequence(result).thenApply(partialMaps -> {
                  Map<BlockState, BlockStateModel.UnbakedRoot> fullMap = new IdentityHashMap<>();

                  for (BlockStateModelLoader.LoadedModels partialMap : partialMaps) {
                     if (partialMap != null) {
                        fullMap.putAll(partialMap.models());
                     }
                  }

                  return new BlockStateModelLoader.LoadedModels(fullMap);
               });
            }
         );
   }

   private static BlockStateModelLoader.LoadedModels loadBlockStateDefinitionStack(
      final Identifier stateDefinitionId,
      final StateDefinition<Block, BlockState> stateDefinition,
      final List<BlockStateModelLoader.LoadedBlockStateModelDispatcher> definitionStack
   ) {
      Map<BlockState, BlockStateModel.UnbakedRoot> result = new IdentityHashMap<>();

      for (BlockStateModelLoader.LoadedBlockStateModelDispatcher definition : definitionStack) {
         result.putAll(definition.contents.instantiate(stateDefinition, () -> stateDefinitionId + "/" + definition.source));
      }

      return new BlockStateModelLoader.LoadedModels(result);
   }

   private record LoadedBlockStateModelDispatcher(String source, BlockStateModelDispatcher contents) {
   }

   public record LoadedModels(Map<BlockState, BlockStateModel.UnbakedRoot> models) {
   }
}
