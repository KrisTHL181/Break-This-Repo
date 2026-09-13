package com.mojang.datafixers;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import it.unimi.dsi.fastutil.ints.Int2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectSortedMap;
import it.unimi.dsi.fastutil.ints.IntAVLTreeSet;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntSortedSet;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataFixerBuilder {
   private static final Logger LOGGER = LoggerFactory.getLogger(DataFixerBuilder.class);
   private final int dataVersion;
   private final Int2ObjectSortedMap<Schema> schemas = new Int2ObjectAVLTreeMap();
   private final List<DataFix> globalList = new ArrayList<>();
   private final IntSortedSet fixerVersions = new IntAVLTreeSet();

   public DataFixerBuilder(int dataVersion) {
      this.dataVersion = dataVersion;
   }

   public Schema addSchema(int version, BiFunction<Integer, Schema, Schema> factory) {
      return this.addSchema(version, 0, factory);
   }

   public Schema addSchema(int version, int subVersion, BiFunction<Integer, Schema, Schema> factory) {
      int key = DataFixUtils.makeKey(version, subVersion);
      Schema parent = this.schemas.isEmpty() ? null : (Schema)this.schemas.get(DataFixerUpper.getLowestSchemaSameVersion(this.schemas, key - 1));
      Schema schema = factory.apply(DataFixUtils.makeKey(version, subVersion), parent);
      this.addSchema(schema);
      return schema;
   }

   public void addSchema(Schema schema) {
      this.schemas.put(schema.getVersionKey(), schema);
   }

   public void addFixer(DataFix fix) {
      int version = DataFixUtils.getVersion(fix.getVersionKey());
      if (version > this.dataVersion) {
         LOGGER.warn("Ignored fix registered for version: {} as the DataVersion of the game is: {}", version, this.dataVersion);
      } else {
         this.globalList.add(fix);
         this.fixerVersions.add(fix.getVersionKey());
      }
   }

   public DataFixerBuilder.Result build() {
      DataFixerUpper fixer = new DataFixerUpper(new Int2ObjectAVLTreeMap(this.schemas), new ArrayList<>(this.globalList), new IntAVLTreeSet(this.fixerVersions));
      return new DataFixerBuilder.Result(fixer);
   }

   public class Result {
      private final DataFixerUpper fixerUpper;

      public Result(DataFixerUpper fixerUpper) {
         this.fixerUpper = fixerUpper;
      }

      public DataFixer fixer() {
         return this.fixerUpper;
      }

      public CompletableFuture<?> optimize(Set<DSL.TypeReference> requiredTypes, Executor executor) {
         Instant started = Instant.now();
         List<CompletableFuture<?>> doneFutures = new ArrayList<>();
         List<CompletableFuture<?>> failFutures = new ArrayList<>();
         Set<String> requiredTypeNames = requiredTypes.stream().map(DSL.TypeReference::typeName).collect(Collectors.toSet());
         IntIterator iterator = this.fixerUpper.fixerVersions().iterator();

         while (iterator.hasNext()) {
            int versionKey = iterator.nextInt();
            Schema schema = (Schema)DataFixerBuilder.this.schemas.get(versionKey);

            for (String typeName : schema.types()) {
               if (requiredTypeNames.contains(typeName)) {
                  CompletableFuture<Void> doneFuture = CompletableFuture.runAsync(() -> {
                     Type<?> dataType = schema.getType(() -> typeName);
                     TypeRewriteRule rule = this.fixerUpper.getRule(DataFixUtils.getVersion(versionKey), DataFixerBuilder.this.dataVersion);
                     dataType.rewrite(rule, DataFixerUpper.OPTIMIZATION_RULE);
                  }, executor);
                  doneFutures.add(doneFuture);
                  CompletableFuture<?> failFuture = new CompletableFuture();
                  doneFuture.exceptionally(e -> {
                     failFuture.completeExceptionally(e);
                     return null;
                  });
                  failFutures.add(failFuture);
               }
            }
         }

         CompletableFuture<?> doneFuture = CompletableFuture.allOf(doneFutures.toArray(CompletableFuture[]::new))
            .thenAccept(
               ignored -> DataFixerBuilder.LOGGER
                  .info("{} Datafixer optimizations took {} milliseconds", doneFutures.size(), Duration.between(started, Instant.now()).toMillis())
            );
         CompletableFuture<?> failFuture = CompletableFuture.anyOf(failFutures.toArray(CompletableFuture[]::new));
         return CompletableFuture.anyOf(doneFuture, failFuture);
      }
   }
}
