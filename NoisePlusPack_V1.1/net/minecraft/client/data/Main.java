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
import net.minecraft.obfuscate.DontObfuscate;
import net.minecraft.server.Bootstrap;
import net.minecraft.util.Util;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class Main {
   @DontObfuscate
   @SuppressForbidden(reason = "System.out needed before bootstrap")
   public static void main(String[] p_375526_) throws IOException {
      SharedConstants.tryDetectVersion();
      OptionParser optionparser = new OptionParser();
      OptionSpec<Void> optionspec = optionparser.accepts("help", "Show the help menu").forHelp();
      OptionSpec<Void> optionspec1 = optionparser.accepts("client", "Include client generators");
      OptionSpec<Void> optionspec2 = optionparser.accepts("all", "Include all generators");
      OptionSpec<String> optionspec3 = optionparser.accepts("output", "Output folder").withRequiredArg().defaultsTo("generated", new String[0]);
      OptionSet optionset = optionparser.parse(p_375526_);
      if (!optionset.has(optionspec) && optionset.hasOptions()) {
         Path path = Paths.get((String)optionspec3.value(optionset));
         boolean flag = optionset.has(optionspec2);
         boolean flag1 = flag || optionset.has(optionspec1);
         Bootstrap.bootStrap();
         ClientBootstrap.bootstrap();
         DataGenerator datagenerator = new DataGenerator(path, SharedConstants.getCurrentVersion(), true);
         addClientProviders(datagenerator, flag1);
         datagenerator.run();
         Util.shutdownExecutors();
      } else {
         optionparser.printHelpOn(System.out);
      }
   }

   public static void addClientProviders(DataGenerator p_377181_, boolean p_375717_) {
      DataGenerator.PackGenerator datagenerator$packgenerator = p_377181_.getVanillaPack(p_375717_);
      datagenerator$packgenerator.addProvider(ModelProvider::new);
      datagenerator$packgenerator.addProvider(EquipmentAssetProvider::new);
      datagenerator$packgenerator.addProvider(WaypointStyleProvider::new);
      datagenerator$packgenerator.addProvider(AtlasProvider::new);
   }
}
