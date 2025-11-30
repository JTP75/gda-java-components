package programmingtheiot.data;

import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

public class LLMHttpResponseDeserializer implements JsonDeserializer<LLMHttpResponse> {

    @Override
    public LLMHttpResponse deserialize(
        JsonElement json, 
        Type typeOfT, 
        JsonDeserializationContext context
    ) throws JsonParseException {
        JsonObject obj = json.getAsJsonObject();
        String status = obj.get("status").getAsString();

        JsonElement element = obj.get("data");
        JsonObject data;
        
        if (element.isJsonPrimitive()) {
            data = new JsonObject();
            data.addProperty("value", element.getAsString());
        } else {
            data = element.getAsJsonObject();
        }

        return new LLMHttpResponse(status, data);
    }
    
}
