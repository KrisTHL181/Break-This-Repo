package net.minecraft.world.level.levelgen.structure.templatesystem.loader;

import com.mojang.datafixers.DataFixer;
import com.mojang.logging.LogUtils;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.stream.Stream.Builder;
import net.minecraft.core.HolderGetter;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.slf4j.Logger;

public class DirectoryTemplateSource extends TemplateSource {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final Path sourceDir;
   private final FileToIdConverter fileToIdConverter;
   private final boolean loadAsText;

   public DirectoryTemplateSource(
      final DataFixer fixerUpper,
      final HolderGetter<Block> blockLookup,
      final Path sourceDir,
      final PackType packType,
      final FileToIdConverter fileToIdConverter,
      final boolean loadAsText
   ) {
      this(fixerUpper, blockLookup, sourceDir.resolve(packType.getDirectory()), fileToIdConverter, loadAsText);
   }

   public DirectoryTemplateSource(
      final DataFixer fixerUpper,
      final HolderGetter<Block> blockLookup,
      final Path sourceDir,
      final FileToIdConverter fileToIdConverter,
      final boolean loadAsText
   ) {
      super(fixerUpper, blockLookup);
      this.sourceDir = sourceDir;
      this.fileToIdConverter = fileToIdConverter;
      this.loadAsText = loadAsText;
   }

   @Override
   public Optional<StructureTemplate> load(final Identifier id) {
      if (!Files.isDirectory(this.sourceDir)) {
         return Optional.empty();
      }

      IoSupplier<InputStream> resource = PathPackResources.getResource(this.sourceDir, this.fileToIdConverter.idToFile(id));
      return resource == null
         ? Optional.empty()
         : this.load(resource, this.loadAsText, e -> LOGGER.error("Couldn't load structure from {}:{}", new Object[]{this.sourceDir, id, e}));
   }

   @Override
   public Stream<Identifier> list() {
      if (!Files.isDirectory(this.sourceDir)) {
         return Stream.empty();
      }

      Builder<Identifier> resultBuilder = Stream.builder();

      for (String namespace : PathPackResources.getNamespaces(this.sourceDir)) {
         PathPackResources.listResources(this.sourceDir, namespace, this.fileToIdConverter.prefix(), (id, var3) -> {
            if (this.fileToIdConverter.extensionMatches(id)) {
               resultBuilder.accept(this.fileToIdConverter.fileToId(id));
            }
         });
      }

      return resultBuilder.build();
   }
}
