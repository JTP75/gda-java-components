package programmingtheiot.data;

import java.io.IOException;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

public class AnthropicContentBlockTypeAdapter extends TypeAdapter<AnthropicContentBlock> {
    private Gson gson = new Gson();

    @Override
    public void write(JsonWriter out, AnthropicContentBlock value) throws IOException {
        if (value instanceof AnthropicContentBlock.Text) {
            gson.toJson(value, AnthropicContentBlock.Text.class, out);
        } else if (value instanceof AnthropicContentBlock.ToolUse) {
            gson.toJson(value, AnthropicContentBlock.ToolUse.class, out);
        } else if (value instanceof AnthropicContentBlock.ToolResult) {
            gson.toJson(value, AnthropicContentBlock.ToolResult.class, out);
        }
    }

    @Override
    public AnthropicContentBlock read(JsonReader in) throws IOException {
        JsonObject json = JsonParser.parseReader(in).getAsJsonObject();
        String type = json.get("type").getAsString();

        switch (type) {
            case "text":
                return gson.fromJson(json, AnthropicContentBlock.Text.class);
            case "tool_use":
                return gson.fromJson(json, AnthropicContentBlock.ToolUse.class);
            case "tool_result":
                return gson.fromJson(json, AnthropicContentBlock.ToolResult.class);
            default:
                throw new JsonSyntaxException("Unknown type: " + type);
        }
    }
    
}
