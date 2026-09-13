package net.minecraft.client.data;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import net.minecraft.SharedConstants;
import net.minecraft.SuppressForbidden;
import net.minecraft.client.ClientBootstrap;
import net.minecraft.client.data.models.EquipmentAssetProvider;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.WaypointStyleProvider;
import net.minecraft.data.DataGenerator;
import net.minecraft.server.Bootstrap;
import net.minecraft.util.Util;

public class Main {
   @SuppressForbidden(reason = "System.out needed before bootstrap")
   public static void main(final String[] args) throws IOException {
      SharedConstants.tryDetectVersion();
      OptionParser parser = new OptionParser();
      OptionSpec<Void> helpOption = parser.accepts("help", "Show the help menu").forHelp();
      OptionSpec<Void> clientOption = parser.accepts("client", "Include client generators");
      OptionSpec<Void> allOption = parser.accepts("all", "Include all generators");
      OptionSpec<String> outputOption = parser.accepts("output", "Output folder").withRequiredArg().defaultsTo("generated", new String[0]);
      OptionSet optionSet = parser.parse(args);
      if (!optionSet.has(helpOption) && optionSet.hasOptions()) {
         Path output = Paths.get((String)outputOption.value(optionSet));
         boolean allOptions = optionSet.has(allOption);
         boolean client = allOptions || optionSet.has(clientOption);
         Bootstrap.bootStrap();
         ClientBootstrap.bootstrap();
         DataGenerator generator = new DataGenerator.Cached(output, SharedConstants.getCurrentVersion(), true);
         addClientProviders(generator, client);
         generator.run();
         Util.shutdownExecutors();
      } else {
         parser.printHelpOn(System.out);
      }
   }

   public static void addClientProviders(final DataGenerator generator, final boolean client) {
      DataGenerator.PackGenerator clientVanillaPack = generator.getVanillaPack(client);
      clientVanillaPack.addProvider(ModelProvider::new);
      clientVanillaPack.addProvider(EquipmentAssetProvider::new);
      clientVanillaPack.addProvider(WaypointStyleProvider::new);
      clientVanillaPack.addProvider(AtlasProvider::new);
   }
}
