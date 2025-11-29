package programmingtheiot.gda.connection;

import programmingtheiot.common.IDataMessageListener;
import programmingtheiot.common.ResourceNameEnum;
import programmingtheiot.data.AnthropicMessage;
import programmingtheiot.data.LLMHttpRequest;
import programmingtheiot.data.MessagesRequest;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.logging.Logger;

import com.google.gson.Gson;

import programmingtheiot.common.ConfigConst;
import programmingtheiot.common.ConfigUtil;

public class PuetceClientConnector implements IRequestResponseClient
{
	// static
	
	private static final Logger _Logger =
		Logger.getLogger(PuetceClientConnector.class.getName());

    private String host = ConfigConst.DEFAULT_HOST;
	private int port = ConfigConst.DEFAULT_PUETCE_PORT;
    private String uriPath = null;

    private IDataMessageListener dataMessageListener = null;
    
    private HttpClient client = null;

    public PuetceClientConnector() {
        super();

        this.host = ConfigUtil.getInstance().getProperty(
            ConfigConst.PUETCE_GATEWAY_SERVICE, 
            ConfigConst.HOST_KEY
        );
        this.port = ConfigUtil.getInstance().getInteger(
            ConfigConst.PUETCE_GATEWAY_SERVICE, 
            ConfigConst.PORT_KEY
        );

        this.uriPath = "http://" + host + ":" + port + "/";

        this.client = HttpClient.newHttpClient();
    }

    // convenience

    public boolean sendMessage(
        List<AnthropicMessage> messages,
        String systemPrompt,
        boolean useTools,
        float randomness
    ) {
        Gson gson = new Gson();

        // collect arguments
        MessagesRequest msgReq = new MessagesRequest(
            messages, systemPrompt, useTools, randomness);

        // collect into custom http request for PUETCE backend
        LLMHttpRequest req = new LLMHttpRequest(
            "send_message", gson.toJsonTree(msgReq).getAsJsonObject());

        return sendPostRequest(
            ResourceNameEnum.GDA_MESSAGE_PUETCE_RESOURCE, null, false, gson.toJson(req), 5);
    }

    // super impls

    @Override
    public void clearEndpointPath() {
    }

    @Override
    public boolean sendDiscoveryRequest(int timeout) {
        return true;
    }

    @Override
    public boolean sendDeleteRequest(ResourceNameEnum resource, String name, boolean enableCON, int timeout) {
        return true;
    }

    @Override
    public boolean sendGetRequest(ResourceNameEnum resource, String name, boolean enableCON, int timeout) {
        String uri = uriPath + resource.getResourceName();
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(uri))
            .GET()
            .header("Content-Type", "application/json")
            .build();

        HttpResponse<String> response = null;
        try {
            response = this.client.send(request, HttpResponse.BodyHandlers.ofString());

            _Logger.info("Received: " + response);

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                _Logger.info("Received error (code " + response.statusCode() + ") response: " + response.body());
                return false;
            } else {
                _Logger.info("Received response: " + response.body());

                this._onGetResponse(response, resource);

                return true;
            }
        } catch (Exception e) {
            _Logger.warning("HTTP request failed: " + e);

            return false;
        }
    }

    @Override
    public boolean sendPostRequest(ResourceNameEnum resource, String name, boolean enableCON, String payload, int timeout) {
        String uri = uriPath + resource.getResourceName();
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(uri))
            .POST(HttpRequest.BodyPublishers.ofString(payload))
            .header("Content-Type", "application/json")
            .build();
            
        try {
            // HttpResponse<String> response = this.client.send(request, HttpResponse.BodyHandlers.ofString());
            // this._onPostResponse(response, resource);
            this.client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> _onPostResponse(response, resource))
                .exceptionally(ex -> {
                    _Logger.severe("HTTP request failed (async): " + ex);
                    return null;
                });
            return true;
        } catch (Exception e) {
            _Logger.warning("HTTP request failed: " + e);
            return false;
        }
    }

    @Override
    public boolean sendPutRequest(ResourceNameEnum resource, String name, boolean enableCON, String payload,
            int timeout) {
        return true;
    }

    @Override
    public boolean setDataMessageListener(IDataMessageListener listener) {
        if (this.dataMessageListener == null) {
            this.dataMessageListener = listener;
            return true;
        }
        return false;
    }

    @Override
    public void setEndpointPath(ResourceNameEnum resource) {
    }

    @Override
    public boolean startObserver(ResourceNameEnum resource, String name, int ttl) {
        return true;
    }

    @Override
    public boolean stopObserver(ResourceNameEnum resourceType, String name, int timeout) {
        return true;
    }

    // callbacks

    protected void _onGetResponse(HttpResponse<String> response, ResourceNameEnum resource) {
        
    }

    protected void _onPostResponse(HttpResponse<String> response, ResourceNameEnum resource) {
        
        if (response == null) {
            _Logger.warning("Received null response");
        } else if (response.statusCode() < 200 || response.statusCode() >= 300) {
            _Logger.warning("Received error (code " + response.statusCode() + ") response: " + response.body());
        } else {
            _Logger.info("Received response: " + response.body());
            dataMessageListener.handleIncomingMessage(resource, response.body());
        }
    }
}
