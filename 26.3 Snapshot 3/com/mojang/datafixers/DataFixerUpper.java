package com.mojang.datafixers;

import com.google.common.collect.Lists;
import com.mojang.datafixers.functions.PointFreeRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import it.unimi.dsi.fastutil.ints.Int2ObjectSortedMap;
import it.unimi.dsi.fastutil.ints.IntSortedSet;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataFixerUpper implements DataFixer {
   public static boolean ERRORS_ARE_FATAL = false;
   private static final Logger LOGGER = LoggerFactory.getLogger(DataFixerUpper.class);
   protected static final PointFreeRule OPTIMIZATION_RULE = DataFixUtils.make(
      () -> PointFreeRule.everywhere(
         PointFreeRule.seq(
            PointFreeRule.CataFuseSame.INSTANCE,
            PointFreeRule.CataFuseDifferent.INSTANCE,
            PointFreeRule.CompRewrite.together(PointFreeRule.LensComp.INSTANCE, PointFreeRule.SortProj.INSTANCE, PointFreeRule.SortInj.INSTANCE)
         ),
         PointFreeRule.AppNest.INSTANCE
      )
   );
   private final Int2ObjectSortedMap<Schema> schemas;
   private final List<DataFix> globalList;
   private final IntSortedSet fixerVersions;
   private final Long2ObjectMap<TypeRewriteRule> rules = Long2ObjectMaps.synchronize(new Long2ObjectOpenHashMap());

   protected DataFixerUpper(Int2ObjectSortedMap<Schema> schemas, List<DataFix> globalList, IntSortedSet fixerVersions) {
      this.schemas = schemas;
      this.globalList = globalList;
      this.fixerVersions = fixerVersions;
   }

   @Override
   public <T> Dynamic<T> update(DSL.TypeReference type, Dynamic<T> input, int version, int newVersion) {
      if (version < newVersion) {
         Type<?> dataType = this.getType(type, version);
         DataResult<T> read = dataType.readAndWrite(
            input.getOps(), this.getType(type, newVersion), this.getRule(version, newVersion), OPTIMIZATION_RULE, input.getValue()
         );
         T result = read.resultOrPartial(LOGGER::error).orElse(input.getValue());
         return new Dynamic<>(input.getOps(), result);
      } else {
         return input;
      }
   }

   @Override
   public Schema getSchema(int key) {
      return (Schema)this.schemas.get(getLowestSchemaSameVersion(this.schemas, key));
   }

   protected Type<?> getType(DSL.TypeReference type, int version) {
      return this.getSchema(DataFixUtils.makeKey(version)).getTypeRaw(type);
   }

   protected static int getLowestSchemaSameVersion(Int2ObjectSortedMap<Schema> schemas, int versionKey) {
      return versionKey < schemas.firstIntKey() ? schemas.firstIntKey() : schemas.subMap(0, versionKey + 1).lastIntKey();
   }

   private int getLowestFixSameVersion(int versionKey) {
      return versionKey < this.fixerVersions.firstInt() ? this.fixerVersions.firstInt() - 1 : this.fixerVersions.subSet(0, versionKey + 1).lastInt();
   }

   protected TypeRewriteRule getRule(int version, int newVersion) {
      if (version >= newVersion) {
         return TypeRewriteRule.nop();
      }

      long key = (long)version << 32 | newVersion;
      return (TypeRewriteRule)this.rules.computeIfAbsent(key, k -> {
         int expandedVersion = this.getLowestFixSameVersion(DataFixUtils.makeKey(version));
         List<TypeRewriteRule> rules = Lists.newArrayList();

         for (DataFix fix : this.globalList) {
            int expandedFixVersion = fix.getVersionKey();
            int fixVersion = DataFixUtils.getVersion(expandedFixVersion);
            if (expandedFixVersion > expandedVersion && fixVersion <= newVersion) {
               TypeRewriteRule fixRule = fix.getRule();
               if (fixRule != TypeRewriteRule.nop()) {
                  rules.add(fixRule);
               }
            }
         }

         return TypeRewriteRule.seq(rules);
      });
   }

   protected IntSortedSet fixerVersions() {
      return this.fixerVersions;
   }
}
