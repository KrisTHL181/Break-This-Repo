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
      public ProblemReporter forChild(final ProblemReporter.PathElement path) {
         return this;
      }

      @Override
      public void report(final ProblemReporter.Problem problem) {
      }
   };

   ProblemReporter forChild(ProblemReporter.PathElement path);

   void report(ProblemReporter.Problem problem);

   class Collector implements ProblemReporter {
      public static final ProblemReporter.PathElement EMPTY_ROOT = () -> "";
      private final ProblemReporter.@Nullable Collector parent;
      private final ProblemReporter.PathElement element;
      private final Set<ProblemReporter.Collector.Entry> problems;

      public Collector() {
         this(EMPTY_ROOT);
      }

      public Collector(final ProblemReporter.PathElement root) {
         this.parent = null;
         this.problems = new LinkedHashSet<>();
         this.element = root;
      }

      private Collector(final ProblemReporter.Collector parent, final ProblemReporter.PathElement path) {
         this.problems = parent.problems;
         this.parent = parent;
         this.element = path;
      }

      @Override
      public ProblemReporter forChild(final ProblemReporter.PathElement path) {
         return new ProblemReporter.Collector(this, path);
      }

      @Override
      public void report(final ProblemReporter.Problem problem) {
         this.problems.add(new ProblemReporter.Collector.Entry(this, problem));
      }

      public boolean isEmpty() {
         return this.problems.isEmpty();
      }

      public void forEach(final BiConsumer<String, ProblemReporter.Problem> output) {
         List<ProblemReporter.PathElement> pathElements = new ArrayList<>();
         StringBuilder pathString = new StringBuilder();

         for (ProblemReporter.Collector.Entry entry : this.problems) {
            for (ProblemReporter.Collector current = entry.source; current != null; current = current.parent) {
               pathElements.add(current.element);
            }

            for (int i = pathElements.size() - 1; i >= 0; i--) {
               pathString.append(pathElements.get(i).get());
            }

            output.accept(pathString.toString(), entry.problem());
            pathString.setLength(0);
            pathElements.clear();
         }
      }

      public String getReport() {
         Multimap<String, ProblemReporter.Problem> groupedProblems = HashMultimap.create();
         this.forEach(groupedProblems::put);
         return groupedProblems.asMap()
            .entrySet()
            .stream()
            .map(
               entry -> " at "
                  + (String)entry.getKey()
                  + ": "
                  + ((Collection)entry.getValue()).stream().map(ProblemReporter.Problem::description).collect(Collectors.joining("; "))
            )
            .collect(Collectors.joining("\n"));
      }

      public String getTreeReport() {
         List<ProblemReporter.PathElement> pathElements = new ArrayList<>();
         ProblemReporter.Collector.ProblemTreeNode root = new ProblemReporter.Collector.ProblemTreeNode(this.element);

         for (ProblemReporter.Collector.Entry entry : this.problems) {
            for (ProblemReporter.Collector current = entry.source; current != this; current = current.parent) {
               pathElements.add(current.element);
            }

            ProblemReporter.Collector.ProblemTreeNode node = root;

            for (int i = pathElements.size() - 1; i >= 0; i--) {
               node = node.child(pathElements.get(i));
            }

            pathElements.clear();
            node.problems.add(entry.problem);
         }

         return String.join("\n", root.getLines());
      }

      private record Entry(ProblemReporter.Collector source, ProblemReporter.Problem problem) {
      }

      private record ProblemTreeNode(
         ProblemReporter.PathElement element,
         List<ProblemReporter.Problem> problems,
         Map<ProblemReporter.PathElement, ProblemReporter.Collector.ProblemTreeNode> children
      ) {
         public ProblemTreeNode(final ProblemReporter.PathElement pathElement) {
            this(pathElement, new ArrayList<>(), new LinkedHashMap<>());
         }

         public ProblemReporter.Collector.ProblemTreeNode child(final ProblemReporter.PathElement id) {
            return this.children.computeIfAbsent(id, ProblemReporter.Collector.ProblemTreeNode::new);
         }

         public List<String> getLines() {
            int problemCount = this.problems.size();
            int childrenCount = this.children.size();
            if (problemCount == 0 && childrenCount == 0) {
               return List.of();
            }

            if (problemCount == 0 && childrenCount == 1) {
               List<String> lines = new ArrayList<>();
               this.children.forEach((element, child) -> lines.addAll(child.getLines()));
               lines.set(0, this.element.get() + lines.get(0));
               return lines;
            }

            if (problemCount == 1 && childrenCount == 0) {
               return List.of(this.element.get() + ": " + this.problems.getFirst().description());
            }

            List<String> lines = new ArrayList<>();
            this.children.forEach((element, child) -> lines.addAll(child.getLines()));
            lines.replaceAll(s -> "  " + s);

            for (ProblemReporter.Problem problem : this.problems) {
               lines.add("  " + problem.description());
            }

            lines.addFirst(this.element.get() + ":");
            return lines;
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

   record MapEntryPathElement(String name, String key) implements ProblemReporter.PathElement {
      @Override
      public String get() {
         return "." + this.name + "[" + this.key + "]";
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

      public ScopedCollector(final Logger logger) {
         this.logger = logger;
      }

      public ScopedCollector(final ProblemReporter.PathElement root, final Logger logger) {
         super(root);
         this.logger = logger;
      }

      @Override
      public void close() {
         if (!this.isEmpty()) {
            this.logger.warn("[{}] Serialization errors:\n{}", this.logger.getName(), this.getTreeReport());
         }
      }
   }
}
