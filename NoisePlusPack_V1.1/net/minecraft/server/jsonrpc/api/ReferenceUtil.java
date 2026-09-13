package net.minecraft.server.jsonrpc.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.net.URI;
import java.net.URISyntaxException;

public class ReferenceUtil {
   public static final Codec<URI> REFERENCE_CODEC = Codec.STRING.comapFlatMap(p_423824_ -> {
      try {
         return DataResult.success(new URI(p_423824_));
      } catch (URISyntaxException urisyntaxexception) {
         return DataResult.error(urisyntaxexception::getMessage);
      }
   }, URI::toString);

   public static URI createLocalReference(String p_428071_) {
      return URI.create("#/components/schemas/" + p_428071_);
   }
}
