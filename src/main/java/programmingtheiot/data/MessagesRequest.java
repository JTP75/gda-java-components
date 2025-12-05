package programmingtheiot.data;

import java.util.List;

public class MessagesRequest {
    public List<AnthropicMessage> messages = null;
    public float randomness = 0.5f;
    public String sys_prompt = null;
    public boolean use_tools = false;
    public MessagesRequest(List<AnthropicMessage> messages) {
        this.messages = messages;
        this.sys_prompt = "This is the default system prompt. Your name is Ripley";
        this.use_tools = false;
        this.randomness = 0.5f;
    }
    public MessagesRequest(
        List<AnthropicMessage> messages,
        String systemPrompt,
        boolean useTools,
        float randomness
    ) {
        this.messages = messages;
        this.sys_prompt = systemPrompt;
        this.use_tools = useTools;
        this.randomness = randomness;
    }
}
