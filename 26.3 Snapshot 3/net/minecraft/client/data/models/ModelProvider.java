package net.minecraft.client.data.models;

import com.google.common.collect.Maps;
import com.google.gson.JsonElement;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.client.data.models.blockstates.BlockModelDefinitionGenerator;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.client.data.models.model.ModelInstance;
import net.minecraft.client.data.models.model.ModelLocationUtils;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelDispatcher;
import net.minecraft.client.renderer.item.ClientItem;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class ModelProvider implements DataProvider {
   private final PackOutput.PathProvider blockStatePathProvider;
   private final PackOutput.PathProvider itemInfoPathProvider;
   private final PackOutput.PathProvider modelPathProvider;

   public ModelProvider(final PackOutput output) {
      this.blockStatePathProvider = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "blockstates");
      this.itemInfoPathProvider = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "items");
      this.modelPathProvider = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "models");
   }

   @Override
   public CompletableFuture<?> run(final CachedOutput cache) {
      ModelProvider.ItemInfoCollector itemModels = new ModelProvider.ItemInfoCollector();
      ModelProvider.BlockStateGeneratorCollector blockStateGenerators = new ModelProvider.BlockStateGeneratorCollector();
      ModelProvider.SimpleModelCollector simpleModels = new ModelProvider.SimpleModelCollector();
      new BlockModelGenerators(blockStateGenerators, itemModels, simpleModels).run();
      new ItemModelGenerators(itemModels, simpleModels).run();
      blockStateGenerators.validate();
      itemModels.finalizeAndValidate();
      return CompletableFuture.allOf(
         blockStateGenerators.save(cache, this.blockStatePathProvider),
         simpleModels.save(cache, this.modelPathProvider),
         itemModels.save(cache, this.itemInfoPathProvider)
      );
   }

   @Override
   public final String getName() {
      return "Model Definitions";
   }

   private static class BlockStateGeneratorCollector implements Consumer<BlockModelDefinitionGenerator> {
      private final Map<Block, BlockModelDefinitionGenerator> generators = new HashMap<>();

      public void accept(final BlockModelDefinitionGenerator generator) {
         Block block = generator.block();
         BlockModelDefinitionGenerator prev = this.generators.put(block, generator);
         if (prev != null) {
            throw new IllegalStateException("Duplicate blockstate definition for " + block);
         }
      }

      public void validate() {
         List<Identifier> missingDefinitions = BuiltInRegistries.BLOCK
            .listElements()
            .filter(e -> !this.generators.containsKey(e.value()))
            .map(e -> e.key().identifier())
            .toList();
         if (!missingDefinitions.isEmpty()) {
            throw new IllegalStateException("Missing blockstate definitions for: " + missingDefinitions);
         }
      }

      public CompletableFuture<?> save(final CachedOutput cache, final PackOutput.PathProvider pathProvider) {
         Map<Block, BlockStateModelDispatcher> definitions = Maps.transformValues(this.generators, BlockModelDefinitionGenerator::create);
         Function<Block, Path> pathGetter = block -> pathProvider.json(block.builtInRegistryHolder().key().identifier());
         return DataProvider.saveAll(cache, BlockStateModelDispatcher.CODEC, pathGetter, definitions);
      }
   }

   private static class ItemInfoCollector implements ItemModelOutput {
      private final Map<Item, ClientItem> itemInfos = new HashMap<>();
      private final Map<Item, Item> copies = new HashMap<>();

      @Override
      public void accept(final Item item, final ItemModel.Unbaked model, final ClientItem.Properties properties) {
         this.register(item, new ClientItem(model, properties));
      }

      private void register(final Item item, final ClientItem itemInfo) {
         ClientItem prev = this.itemInfos.put(item, itemInfo);
         if (prev != null) {
            throw new IllegalStateException("Duplicate item model definition for " + item);
         }
      }

      @Override
      public void copy(final Item donor, final Item acceptor) {
         this.copies.put(acceptor, donor);
      }

      public void finalizeAndValidate() {
         BuiltInRegistries.ITEM.forEach(item -> {
            if (!this.copies.containsKey(item)) {
               if (item instanceof BlockItem blockItem && !this.itemInfos.containsKey(blockItem)) {
                  Identifier targetModel = ModelLocationUtils.getModelLocation(blockItem.getBlock());
                  this.accept(blockItem, ItemModelUtils.plainModel(targetModel));
               }
            }
         });
         this.copies.forEach((acceptor, donor) -> {
            ClientItem donorInfo = this.itemInfos.get(donor);
            if (donorInfo == null) {
               throw new IllegalStateException("Missing donor: " + donor + " -> " + acceptor);
            }

            this.register(acceptor, donorInfo);
         });
         List<Identifier> missingDefinitions = BuiltInRegistries.ITEM
            .listElements()
            .filter(e -> !this.itemInfos.containsKey(e.value()))
            .map(e -> e.key().identifier())
            .toList();
         if (!missingDefinitions.isEmpty()) {
            throw new IllegalStateException("Missing item model definitions for: " + missingDefinitions);
         }
      }

      public CompletableFuture<?> save(final CachedOutput cache, final PackOutput.PathProvider pathProvider) {
         return DataProvider.saveAll(cache, ClientItem.CODEC, item -> pathProvider.json(item.builtInRegistryHolder().key().identifier()), this.itemInfos);
      }
   }

   private static class SimpleModelCollector implements BiConsumer<Identifier, ModelInstance> {
      private final Map<Identifier, ModelInstance> models = new HashMap<>();

      public void accept(final Identifier id, final ModelInstance contents) {
         Supplier<JsonElement> prev = this.models.put(id, contents);
         if (prev != null) {
            throw new IllegalStateException("Duplicate model definition for " + id);
         }
      }

      public CompletableFuture<?> save(final CachedOutput cache, final PackOutput.PathProvider pathProvider) {
         return DataProvider.saveAll(cache, Supplier::get, pathProvider::json, this.models);
      }
   }
}
