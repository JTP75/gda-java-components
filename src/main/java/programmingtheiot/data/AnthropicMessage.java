package programmingtheiot.data;

import java.util.List;

public class AnthropicMessage {
    public AnthropicRole role;
    public List<AnthropicContentBlock> content;
    public AnthropicMessage(AnthropicRole role, List<AnthropicContentBlock> content) {
        this.role = role; this.content = content;
    }
}