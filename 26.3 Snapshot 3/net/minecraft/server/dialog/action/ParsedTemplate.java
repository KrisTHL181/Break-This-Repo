package net.minecraft.server.dialog.action;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.List;
import java.util.Map;
import net.minecraft.commands.functions.StringTemplate;

public class ParsedTemplate {
   public static final Codec<ParsedTemplate> CODEC = Codec.STRING.comapFlatMap(ParsedTemplate::parse, t -> t.raw);
   public static final Codec<String> VARIABLE_CODEC = Codec.STRING
      .validate(s -> StringTemplate.isValidVariableName(s) ? DataResult.success(s) : DataResult.error(() -> s + " is not a valid input name"));
   private final String raw;
   private final StringTemplate parsed;

   private ParsedTemplate(final String raw, final StringTemplate parsed) {
      this.raw = raw;
      this.parsed = parsed;
   }

   private static DataResult<ParsedTemplate> parse(final String value) {
      StringTemplate template;
      try {
         template = StringTemplate.fromString(value);
      } catch (Exception e) {
         return DataResult.error(() -> "Failed to parse template " + value + ": " + e.getMessage());
      }

      return DataResult.success(new ParsedTemplate(value, template));
   }

   public String instantiate(final Map<String, String> arguments) {
      List<String> values = this.parsed.variables().stream().map(k -> arguments.getOrDefault(k, "")).toList();
      return this.parsed.substitute(values);
   }
}
