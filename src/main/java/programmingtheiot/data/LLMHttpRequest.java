package programmingtheiot.data;

import com.google.gson.JsonObject;

public class LLMHttpRequest {
    public String endpoint;
    public JsonObject data;
    public LLMHttpRequest(String endpoint, JsonObject data) {
        this.endpoint = endpoint; this.data = data;
    }
}
