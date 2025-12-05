package programmingtheiot.data;

import com.google.gson.annotations.SerializedName;

public enum AnthropicRole {
    @SerializedName("user") USER, 
    @SerializedName("assistant") ASSISTANT;
}
