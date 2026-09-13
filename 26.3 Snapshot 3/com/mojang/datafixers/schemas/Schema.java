package com.mojang.datafixers.schemas;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.types.families.RecursiveTypeFamily;
import com.mojang.datafixers.types.families.TypeFamily;
import com.mojang.datafixers.types.templates.RecursivePoint;
import com.mojang.datafixers.types.templates.TaggedChoice;
import com.mojang.datafixers.types.templates.TypeTemplate;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

public class Schema {
   private final Object2IntMap<String> recursiveTypes = new Object2IntOpenHashMap();
   private final Map<String, Supplier<TypeTemplate>> typeTemplates = Maps.newHashMap();
   private final Map<String, Type<?>> types;
   private final int versionKey;
   private final String name;
   private final Schema parent;

   public Schema(int versionKey, Schema parent) {
      this.versionKey = versionKey;
      int subVersion = DataFixUtils.getSubVersion(versionKey);
      this.name = "V" + DataFixUtils.getVersion(versionKey) + (subVersion == 0 ? "" : "." + subVersion);
      this.parent = parent;
      this.registerTypes(this, this.registerEntities(this), this.registerBlockEntities(this));
      this.types = this.buildTypes();
   }

   protected Map<String, Type<?>> buildTypes() {
      Map<String, Type<?>> types = Maps.newHashMap();
      List<TypeTemplate> templates = Lists.newArrayList();
      ObjectIterator choice = this.recursiveTypes.object2IntEntrySet().iterator();

      while (choice.hasNext()) {
         Entry<String> entry = (Entry<String>)choice.next();
         templates.add(DSL.check((String)entry.getKey(), entry.getIntValue(), this.getTemplate((String)entry.getKey())));
      }

      TypeTemplate choicex = templates.stream().reduce(DSL::or).get();
      TypeFamily family = new RecursiveTypeFamily(this.name, choicex);

      for (String name : this.typeTemplates.keySet()) {
         int recurseId = this.recursiveTypes.getOrDefault(name, -1);
         Type<?> type;
         if (recurseId != -1) {
            type = family.apply(recurseId);
         } else {
            type = this.getTemplate(name).apply(family).apply(-1);
         }

         types.put(name, type);
      }

      return types;
   }

   public Set<String> types() {
      return this.types.keySet();
   }

   public Type<?> getTypeRaw(DSL.TypeReference type) {
      String name = type.typeName();
      return this.types.computeIfAbsent(name, key -> {
         throw new IllegalArgumentException("Unknown type: " + name);
      });
   }

   public Type<?> getType(DSL.TypeReference type) {
      String name = type.typeName();
      Type<?> type1 = this.types.computeIfAbsent(name, key -> {
         throw new IllegalArgumentException("Unknown type: " + name);
      });
      return type1 instanceof RecursivePoint.RecursivePointType
         ? type1.findCheckedType(-1).orElseThrow(() -> new IllegalStateException("Could not find choice type in the recursive type"))
         : type1;
   }

   public TypeTemplate resolveTemplate(String name) {
      return this.typeTemplates.getOrDefault(name, () -> {
         throw new IllegalArgumentException("Unknown type: " + name);
      }).get();
   }

   public TypeTemplate id(String name) {
      int id = this.recursiveTypes.getOrDefault(name, -1);
      return id != -1 ? DSL.id(id) : this.getTemplate(name);
   }

   protected TypeTemplate getTemplate(String name) {
      return DSL.named(name, this.resolveTemplate(name));
   }

   public Type<?> getChoiceType(DSL.TypeReference type, String choiceName) {
      TaggedChoice.TaggedChoiceType<?> choiceType = this.findChoiceType(type);
      if (!choiceType.types().containsKey(choiceName)) {
         throw new IllegalArgumentException("Data fixer not registered for: " + choiceName + " in " + type.typeName());
      } else {
         return choiceType.types().get(choiceName);
      }
   }

   public TaggedChoice.TaggedChoiceType<?> findChoiceType(DSL.TypeReference type) {
      return this.getType(type).findChoiceType("id", -1).orElseThrow(() -> new IllegalArgumentException("Not a choice type"));
   }

   public void registerTypes(Schema schema, Map<String, Supplier<TypeTemplate>> entityTypes, Map<String, Supplier<TypeTemplate>> blockEntityTypes) {
      this.parent.registerTypes(schema, entityTypes, blockEntityTypes);
   }

   public Map<String, Supplier<TypeTemplate>> registerEntities(Schema schema) {
      return this.parent.registerEntities(schema);
   }

   public Map<String, Supplier<TypeTemplate>> registerBlockEntities(Schema schema) {
      return this.parent.registerBlockEntities(schema);
   }

   public void registerSimple(Map<String, Supplier<TypeTemplate>> map, String name) {
      this.register(map, name, DSL::remainder);
   }

   public void register(Map<String, Supplier<TypeTemplate>> map, String name, Function<String, TypeTemplate> template) {
      this.register(map, name, () -> template.apply(name));
   }

   public void register(Map<String, Supplier<TypeTemplate>> map, String name, Supplier<TypeTemplate> template) {
      map.put(name, template);
   }

   public void registerType(boolean recursive, DSL.TypeReference type, Supplier<TypeTemplate> template) {
      this.typeTemplates.put(type.typeName(), template);
      if (recursive && !this.recursiveTypes.containsKey(type.typeName())) {
         this.recursiveTypes.put(type.typeName(), this.recursiveTypes.size());
      }
   }

   public int getVersionKey() {
      return this.versionKey;
   }

   public Schema getParent() {
      return this.parent;
   }
}
