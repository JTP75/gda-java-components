package programmingtheiot.data;

import com.google.gson.JsonObject;

public class ExecuteToolRequest {
    public String name;
    public JsonObject input;
    public ExecuteToolRequest(String name, JsonObject input) {
        this.name = name; this.input = input;
    }
}
