package net.minecraft.server.jsonrpc.security;

import com.mojang.logging.LogUtils;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import org.slf4j.Logger;

public class JsonRpcSslContextProvider {
   private static final String PASSWORD_ENV_VARIABLE_KEY = "MINECRAFT_MANAGEMENT_TLS_KEYSTORE_PASSWORD";
   private static final String PASSWORD_SYSTEM_PROPERTY_KEY = "management.tls.keystore.password";
   private static final Logger log = LogUtils.getLogger();

   public static SslContext createFrom(String p_426761_, String p_425537_) throws Exception {
      if (p_426761_.isEmpty()) {
         throw new IllegalArgumentException("TLS is enabled but keystore is not configured");
      } else {
         File file1 = new File(p_426761_);
         if (file1.exists() && file1.isFile()) {
            String s = getKeystorePassword(p_425537_);
            return loadKeystoreFromPath(file1, s);
         } else {
            throw new IllegalArgumentException("Supplied keystore is not a file or does not exist: '" + p_426761_ + "'");
         }
      }
   }

   private static String getKeystorePassword(String p_428382_) {
      String s = System.getenv().get("MINECRAFT_MANAGEMENT_TLS_KEYSTORE_PASSWORD");
      if (s != null) {
         return s;
      }

      String s1 = System.getProperty("management.tls.keystore.password", null);
      return s1 != null ? s1 : p_428382_;
   }

   private static SslContext loadKeystoreFromPath(File p_430790_, String p_428301_) throws Exception {
      KeyStore keystore = KeyStore.getInstance("PKCS12");

      try (InputStream inputstream = new FileInputStream(p_430790_)) {
         keystore.load(inputstream, p_428301_.toCharArray());
      }

      KeyManagerFactory keymanagerfactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
      keymanagerfactory.init(keystore, p_428301_.toCharArray());
      TrustManagerFactory trustmanagerfactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
      trustmanagerfactory.init(keystore);
      return SslContextBuilder.forServer(keymanagerfactory).trustManager(trustmanagerfactory).build();
   }

   public static void printInstructions() {
      log.info("To use TLS for the management server, please follow these steps:");
      log.info("1. Set the server property 'management-server-tls-enabled' to 'true' to enable TLS");
      log.info("2. Create a keystore file of type PKCS12 containing your server certificate and private key");
      log.info("3. Set the server property 'management-server-tls-keystore' to the path of your keystore file");
      log.info(
         "4. Set the keystore password via the environment variable 'MINECRAFT_MANAGEMENT_TLS_KEYSTORE_PASSWORD', or system property 'management.tls.keystore.password', or server property 'management-server-tls-keystore-password'"
      );
      log.info("5. Restart the server to apply the changes.");
   }
}
