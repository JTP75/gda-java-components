package programmingtheiot.data;

import java.util.List;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;

public abstract class AnthropicContentBlock {
    public static final class Text extends AnthropicContentBlock {
        public String type = "text";
        public String text;
        public Text(String text) { 
            this.text = text; 
        }
    }
    public static final class ToolUse extends AnthropicContentBlock {
        public String type = "tool_use";
        public String id;
        public String name;
        public JsonObject input;
        public ToolUse(String id, String name, JsonObject input) { 
            this.id = id; this.name = name; this.input = input;
        }
    }
    public static final class ToolResult extends AnthropicContentBlock {
        public String type = "tool_result";
        @SerializedName("tool_use_id")
        public String toolUseId;
        public List<ToolResultContentBlock> content;
        public ToolResult(String toolUseId, List<ToolResultContentBlock> content) { 
            this.toolUseId = toolUseId; this.content = content;
        }
    }
}
