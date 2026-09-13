package net.minecraft.util;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import net.minecraft.resources.ResourceKey;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public interface ProblemReporter {
   ProblemReporter DISCARDING = new ProblemReporter() {
      @Override
      public ProblemReporter forChild(ProblemReporter.PathElement p_408120_) {
         return this;
      }

      @Override
      public void report(ProblemReporter.Problem p_407886_) {
      }
   };

   ProblemReporter forChild(ProblemReporter.PathElement var1);

   void report(ProblemReporter.Problem var1);

   class Collector implements ProblemReporter {
      public static final ProblemReporter.PathElement EMPTY_ROOT = () -> "";
      private final ProblemReporter.@Nullable Collector parent;
      private final ProblemReporter.PathElement element;
      private final Set<ProblemReporter.Collector.Entry> problems;

      public Collector() {
         this(EMPTY_ROOT);
      }

      public Collector(ProblemReporter.PathElement p_408664_) {
         this.parent = null;
         this.problems = new LinkedHashSet<>();
         this.element = p_408664_;
      }

      private Collector(ProblemReporter.Collector p_407313_, ProblemReporter.PathElement p_410029_) {
         this.problems = p_407313_.problems;
         this.parent = p_407313_;
         this.element = p_410029_;
      }

      @Override
      public ProblemReporter forChild(ProblemReporter.PathElement p_409390_) {
         return new ProblemReporter.Collector(this, p_409390_);
      }

      @Override
      public void report(ProblemReporter.Problem p_408383_) {
         this.problems.add(new ProblemReporter.Collector.Entry(this, p_408383_));
      }

      public boolean isEmpty() {
         return this.problems.isEmpty();
      }

      public void forEach(BiConsumer<String, ProblemReporter.Problem> p_409758_) {
         List<ProblemReporter.PathElement> list = new ArrayList<>();
         StringBuilder stringbuilder = new StringBuilder();

         for (ProblemReporter.Collector.Entry problemreporter$collector$entry : this.problems) {
            for (ProblemReporter.Collector problemreporter$collector = problemreporter$collector$entry.source;
               problemreporter$collector != null;
               problemreporter$collector = problemreporter$collector.parent
            ) {
               list.add(problemreporter$collector.element);
            }

            for (int i = list.size() - 1; i >= 0; i--) {
               stringbuilder.append(list.get(i).get());
            }

            p_409758_.accept(stringbuilder.toString(), problemreporter$collector$entry.problem());
            stringbuilder.setLength(0);
            list.clear();
         }
      }

      public String getReport() {
         Multimap<String, ProblemReporter.Problem> multimap = HashMultimap.create();
         this.forEach(multimap::put);
         return multimap.asMap()
            .entrySet()
            .stream()
            .map(
               p_405243_ -> " at "
                  + (String)p_405243_.getKey()
                  + ": "
                  + ((Collection)p_405243_.getValue()).stream().map(ProblemReporter.Problem::description).collect(Collectors.joining("; "))
            )
            .collect(Collectors.joining("\n"));
      }

      public String getTreeReport() {
         List<ProblemReporter.PathElement> list = new ArrayList<>();
         ProblemReporter.Collector.ProblemTreeNode problemreporter$collector$problemtreenode = new ProblemReporter.Collector.ProblemTreeNode(this.element);

         for (ProblemReporter.Collector.Entry problemreporter$collector$entry : this.problems) {
            for (ProblemReporter.Collector problemreporter$collector = problemreporter$collector$entry.source;
               problemreporter$collector != this;
               problemreporter$collector = problemreporter$collector.parent
            ) {
               list.add(problemreporter$collector.element);
            }

            ProblemReporter.Collector.ProblemTreeNode problemreporter$collector$problemtreenode1 = problemreporter$collector$problemtreenode;

            for (int i = list.size() - 1; i >= 0; i--) {
               problemreporter$collector$problemtreenode1 = problemreporter$collector$problemtreenode1.child(list.get(i));
            }

            list.clear();
            problemreporter$collector$problemtreenode1.problems.add(problemreporter$collector$entry.problem);
         }

         return String.join("\n", problemreporter$collector$problemtreenode.getLines());
      }

      record Entry(ProblemReporter.Collector source, ProblemReporter.Problem problem) {
      }

      record ProblemTreeNode(
         ProblemReporter.PathElement element,
         List<ProblemReporter.Problem> problems,
         Map<ProblemReporter.PathElement, ProblemReporter.Collector.ProblemTreeNode> children
      ) {
         public ProblemTreeNode(ProblemReporter.PathElement p_407934_) {
            this(p_407934_, new ArrayList<>(), new LinkedHashMap<>());
         }

         public ProblemReporter.Collector.ProblemTreeNode child(ProblemReporter.PathElement p_406158_) {
            return this.children.computeIfAbsent(p_406158_, ProblemReporter.Collector.ProblemTreeNode::new);
         }

         public List<String> getLines() {
            int i = this.problems.size();
            int j = this.children.size();
            if (i == 0 && j == 0) {
               return List.of();
            }

            if (i == 0 && j == 1) {
               List<String> list1 = new ArrayList<>();
               this.children.forEach((p_410609_, p_408302_) -> list1.addAll(p_408302_.getLines()));
               list1.set(0, this.element.get() + list1.get(0));
               return list1;
            }

            if (i == 1 && j == 0) {
               return List.of(this.element.get() + ": " + this.problems.getFirst().description());
            }

            List<String> list = new ArrayList<>();
            this.children.forEach((p_406053_, p_406339_) -> list.addAll(p_406339_.getLines()));
            list.replaceAll(p_406314_ -> "  " + p_406314_);

            for (ProblemReporter.Problem problemreporter$problem : this.problems) {
               list.add("  " + problemreporter$problem.description());
            }

            list.addFirst(this.element.get() + ":");
            return list;
         }
      }
   }

   record ElementReferencePathElement(ResourceKey<?> id) implements ProblemReporter.PathElement {
      @Override
      public String get() {
         return "->{" + this.id.identifier() + "@" + this.id.registry() + "}";
      }
   }

   record FieldPathElement(String name) implements ProblemReporter.PathElement {
      @Override
      public String get() {
         return "." + this.name;
      }
   }

   record IndexedFieldPathElement(String name, int index) implements ProblemReporter.PathElement {
      @Override
      public String get() {
         return "." + this.name + "[" + this.index + "]";
      }
   }

   record IndexedPathElement(int index) implements ProblemReporter.PathElement {
      @Override
      public String get() {
         return "[" + this.index + "]";
      }
   }

   @FunctionalInterface
   interface PathElement {
      String get();
   }

   interface Problem {
      String description();
   }

   record RootElementPathElement(ResourceKey<?> id) implements ProblemReporter.PathElement {
      @Override
      public String get() {
         return "{" + this.id.identifier() + "@" + this.id.registry() + "}";
      }
   }

   record RootFieldPathElement(String name) implements ProblemReporter.PathElement {
      @Override
      public String get() {
         return this.name;
      }
   }

   class ScopedCollector extends ProblemReporter.Collector implements AutoCloseable {
      private final Logger logger;

      public ScopedCollector(Logger p_407581_) {
         this.logger = p_407581_;
      }

      public ScopedCollector(ProblemReporter.PathElement p_409679_, Logger p_407894_) {
         super(p_409679_);
         this.logger = p_407894_;
      }

      @Override
      public void close() {
         if (!this.isEmpty()) {
            this.logger.warn("[{}] Serialization errors:\n{}", this.logger.getName(), this.getTreeReport());
         }
      }
   }
}
