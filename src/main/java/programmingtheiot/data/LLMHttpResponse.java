package programmingtheiot.data;

import com.google.gson.JsonObject;

public class LLMHttpResponse {
    public String status;
    public JsonObject data;
    public LLMHttpResponse(String status, JsonObject data) {
        this.status = status; this.data = data;
    }
}
