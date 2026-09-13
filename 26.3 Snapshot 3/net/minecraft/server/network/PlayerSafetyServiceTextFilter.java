package net.minecraft.server.network;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.microsoft.aad.msal4j.ClientCredentialFactory;
import com.microsoft.aad.msal4j.ClientCredentialParameters;
import com.microsoft.aad.msal4j.ConfidentialClientApplication;
import com.microsoft.aad.msal4j.IAuthenticationResult;
import com.microsoft.aad.msal4j.IClientCertificate;
import com.microsoft.aad.msal4j.ConfidentialClientApplication.Builder;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import net.minecraft.util.GsonHelper;
import org.jspecify.annotations.Nullable;

public class PlayerSafetyServiceTextFilter extends ServerTextFilter {
   private final ConfidentialClientApplication client;
   private final ClientCredentialParameters clientParameters;
   private final Set<String> fullyFilteredEvents;
   private final int connectionReadTimeoutMs;

   private PlayerSafetyServiceTextFilter(
      final URL chatEndpoint,
      final ServerTextFilter.MessageEncoder chatEncoder,
      final ServerTextFilter.IgnoreStrategy chatIgnoreStrategy,
      final ExecutorService workerPool,
      final ConfidentialClientApplication client,
      final ClientCredentialParameters clientParameters,
      final Set<String> fullyFilteredEvents,
      final int connectionReadTimeoutMs
   ) {
      super(chatEndpoint, chatEncoder, chatIgnoreStrategy, workerPool);
      this.client = client;
      this.clientParameters = clientParameters;
      this.fullyFilteredEvents = fullyFilteredEvents;
      this.connectionReadTimeoutMs = connectionReadTimeoutMs;
   }

   public static @Nullable ServerTextFilter createTextFilterFromConfig(final String textFilteringConfig) {
      JsonObject parsedConfig = GsonHelper.parse(textFilteringConfig);
      URI host = URI.create(GsonHelper.getAsString(parsedConfig, "apiServer"));
      String apiPath = GsonHelper.getAsString(parsedConfig, "apiPath");
      String scope = GsonHelper.getAsString(parsedConfig, "scope");
      String serverId = GsonHelper.getAsString(parsedConfig, "serverId", "");
      String applicationId = GsonHelper.getAsString(parsedConfig, "applicationId");
      String tenantId = GsonHelper.getAsString(parsedConfig, "tenantId");
      String roomId = GsonHelper.getAsString(parsedConfig, "roomId", "Java:Chat");
      String certificatePath = GsonHelper.getAsString(parsedConfig, "certificatePath");
      String certificatePassword = GsonHelper.getAsString(parsedConfig, "certificatePassword", "");
      int hashesToDrop = GsonHelper.getAsInt(parsedConfig, "hashesToDrop", -1);
      int maxConcurrentRequests = GsonHelper.getAsInt(parsedConfig, "maxConcurrentRequests", 7);
      JsonArray fullyFilteredEvents = GsonHelper.getAsJsonArray(parsedConfig, "fullyFilteredEvents");
      Set<String> fullyFilteredEventsSet = new HashSet<>();
      fullyFilteredEvents.forEach(elements -> fullyFilteredEventsSet.add(GsonHelper.convertToString(elements, "filteredEvent")));
      int connectionReadTimeoutMs = GsonHelper.getAsInt(parsedConfig, "connectionReadTimeoutMs", 2000);

      URL chatEndpoint;
      try {
         chatEndpoint = host.resolve(apiPath).toURL();
      } catch (MalformedURLException e) {
         throw new RuntimeException(e);
      }

      ServerTextFilter.MessageEncoder chatEncoder = (sender, message) -> {
         JsonObject object = new JsonObject();
         object.addProperty("userId", sender.id().toString());
         object.addProperty("userDisplayName", sender.name());
         object.addProperty("server", serverId);
         object.addProperty("room", roomId);
         object.addProperty("area", "JavaChatRealms");
         object.addProperty("data", message);
         object.addProperty("language", "*");
         return object;
      };
      ServerTextFilter.IgnoreStrategy ignoreStrategy = ServerTextFilter.IgnoreStrategy.select(hashesToDrop);
      ExecutorService workerPool = createWorkerPool(maxConcurrentRequests);

      IClientCertificate certificate;
      try (InputStream inputStream = Files.newInputStream(Path.of(certificatePath))) {
         certificate = ClientCredentialFactory.createFromCertificate(inputStream, certificatePassword);
      } catch (Exception e) {
         LOGGER.warn("Failed to open certificate file");
         return null;
      }

      ConfidentialClientApplication client;
      try {
         client = ((Builder)((Builder)ConfidentialClientApplication.builder(applicationId, certificate).sendX5c(true).executorService(workerPool))
               .authority(String.format(Locale.ROOT, "https://login.microsoftonline.com/%s/", tenantId)))
            .build();
      } catch (Exception e) {
         LOGGER.warn("Failed to create confidential client application");
         return null;
      }

      ClientCredentialParameters parameters = ClientCredentialParameters.builder(Set.of(scope)).build();
      return new PlayerSafetyServiceTextFilter(
         chatEndpoint, chatEncoder, ignoreStrategy, workerPool, client, parameters, fullyFilteredEventsSet, connectionReadTimeoutMs
      );
   }

   private IAuthenticationResult aquireIAuthenticationResult() {
      return (IAuthenticationResult)this.client.acquireToken(this.clientParameters).join();
   }

   @Override
   protected void setAuthorizationProperty(final HttpURLConnection connection) {
      IAuthenticationResult authenticationResult = this.aquireIAuthenticationResult();
      connection.setRequestProperty("Authorization", "Bearer " + authenticationResult.accessToken());
   }

   @Override
   protected FilteredText filterText(final String message, final ServerTextFilter.IgnoreStrategy ignoreStrategy, final JsonObject response) {
      JsonObject result = GsonHelper.getAsJsonObject(response, "result", null);
      if (result == null) {
         return FilteredText.fullyFiltered(message);
      }

      boolean filtered = GsonHelper.getAsBoolean(result, "filtered", true);
      if (!filtered) {
         return FilteredText.passThrough(message);
      }

      for (JsonElement element : GsonHelper.getAsJsonArray(result, "events", new JsonArray())) {
         JsonObject object = element.getAsJsonObject();
         String event = GsonHelper.getAsString(object, "id", "");
         if (this.fullyFilteredEvents.contains(event)) {
            return FilteredText.fullyFiltered(message);
         }
      }

      JsonArray redactedTextIndices = GsonHelper.getAsJsonArray(result, "redactedTextIndex", new JsonArray());
      return new FilteredText(message, this.parseMask(message, redactedTextIndices, ignoreStrategy));
   }

   @Override
   protected int connectionReadTimeout() {
      return this.connectionReadTimeoutMs;
   }
}
