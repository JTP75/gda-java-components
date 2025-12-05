package programmingtheiot.data;

public abstract class ToolResultContentBlock {
    public static final class Text extends ToolResultContentBlock {
        public String type = "text";
        public String text;
        public Text(String text) { 
            this.text = text; 
        }
    }
}
