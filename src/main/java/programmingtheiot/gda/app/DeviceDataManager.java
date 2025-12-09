/**
 * This class is part of the Programming the Internet of Things
 * project, and is available via the MIT License, which can be
 * found in the LICENSE file at the top level of this repository.
 * 
 * You may find it more helpful to your design to adjust the
 * functionality, constants and interfaces (if there are any)
 * provided within in order to meet the needs of your specific
 * Programming the Internet of Things project.
 */

package programmingtheiot.gda.app;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import programmingtheiot.common.ConfigConst;
import programmingtheiot.common.ConfigUtil;
import programmingtheiot.common.IActuatorDataListener;
import programmingtheiot.common.IDataMessageListener;
import programmingtheiot.common.ResourceNameEnum;

import programmingtheiot.data.ActuatorData;
import programmingtheiot.data.AnthropicContentBlock;
import programmingtheiot.data.AnthropicContentBlockTypeAdapter;
import programmingtheiot.data.AnthropicMessage;
import programmingtheiot.data.AnthropicRole;
import programmingtheiot.data.BaseIotData;
import programmingtheiot.data.DataUtil;
import programmingtheiot.data.LLMHttpResponse;
import programmingtheiot.data.LLMHttpResponseDeserializer;
import programmingtheiot.data.SensorData;
import programmingtheiot.data.SystemPerformanceData;
import programmingtheiot.data.ToolResultContentBlock;
import programmingtheiot.data.AnthropicContentBlock.Text;
import programmingtheiot.gda.connection.CloudClientConnector;
import programmingtheiot.gda.connection.CoapServerGateway;
import programmingtheiot.gda.connection.ICloudClient;
import programmingtheiot.gda.connection.IPersistenceClient;
import programmingtheiot.gda.connection.IPubSubClient;
import programmingtheiot.gda.connection.IRequestResponseClient;
import programmingtheiot.gda.connection.MqttClientConnector;
import programmingtheiot.gda.connection.PuetceClientConnector;
import programmingtheiot.gda.connection.RedisPersistenceAdapter;
import programmingtheiot.gda.connection.SmtpClientConnector;
import programmingtheiot.gda.system.SystemPerformanceManager;
import redis.clients.jedis.JedisPubSub;

/**
 * Shell representation of class for student implementation.
 *
 */
@SuppressWarnings("unused")
public class DeviceDataManager extends JedisPubSub implements IDataMessageListener
{
	// static
	
	private static final Logger _Logger =
		Logger.getLogger(DeviceDataManager.class.getName());
	
	// private var's	
	private boolean enableMqttClient = true;
	private boolean enableCoapServer = false;
	private boolean enableCloudClient = false;
	private boolean enableSmtpClient = false;
	private boolean enablePersistenceClient = false;
	private boolean enablePuetceClient = false;

	private boolean enableSystemPerf = false;
	
	private Gson gson = null;
	
	private IActuatorDataListener actuatorDataListener = null;
	private IPubSubClient mqttClient = null;
	private ICloudClient cloudClient = null;
	private IPersistenceClient persistenceClient = null;
	private IRequestResponseClient smtpClient = null;
	private CoapServerGateway coapServer = null;
	private PuetceClientConnector puetceClient = null;

	private SystemPerformanceManager systemPerfMgr = null;

	private ActuatorData   latestHumidifierActuatorData = null;
	private SensorData     latestHumiditySensorData = null;
	private OffsetDateTime latestHumiditySensorTimeStamp = null;

	
	private boolean handleHumidityChangeOnDevice = false;
	private boolean useVerboseToolExecutions = false;

	private long    humidityMaxTimePastThreshold = 300; // seconds
	private float   nominalHumiditySetting   = 40.0f;
	private float   triggerHumidifierFloor   = 30.0f;
	private float   triggerHumidifierCeiling = 50.0f;
	
	// private state vars

	private ActuatorData latestHumidifierActuatorResponse = null;
	private String lastLocationID = "";
	private String lastMessageLocationID = "";
	private String lastToolID = "";
	private int lastKnownHumidifierCommand = ConfigConst.OFF_COMMAND;
	private String lastKnownSpeechResult = "";
	private List<AnthropicMessage> conversation = null;
	
	// constructors
	
	public DeviceDataManager()
	{
		super();
		
		this.gson = new GsonBuilder()
			.registerTypeAdapter(AnthropicContentBlock.class, new AnthropicContentBlockTypeAdapter())
			.registerTypeAdapter(LLMHttpResponse.class, new LLMHttpResponseDeserializer())
			.create();
	
		ConfigUtil configUtil = ConfigUtil.getInstance();
		
		this.enableMqttClient = configUtil.getBoolean(ConfigConst.GATEWAY_DEVICE, ConfigConst.ENABLE_MQTT_CLIENT_KEY);
		this.enableCoapServer = configUtil.getBoolean(ConfigConst.GATEWAY_DEVICE, ConfigConst.ENABLE_COAP_SERVER_KEY);
		this.enableCloudClient = configUtil.getBoolean(ConfigConst.GATEWAY_DEVICE, ConfigConst.ENABLE_CLOUD_CLIENT_KEY);		
		this.enablePersistenceClient = configUtil.getBoolean(ConfigConst.GATEWAY_DEVICE, ConfigConst.ENABLE_PERSISTENCE_CLIENT_KEY);
		this.enablePuetceClient = configUtil.getBoolean(ConfigConst.GATEWAY_DEVICE, ConfigConst.ENABLE_PUETCE_CLIENT_KEY);

		this.enableSystemPerf = configUtil.getBoolean(ConfigConst.GATEWAY_DEVICE, ConfigConst.ENABLE_SYSTEM_PERF_KEY);

		this.useVerboseToolExecutions = configUtil.getBoolean(ConfigConst.PUETCE_GATEWAY_SERVICE, ConfigConst.VERBOSE_TOOL_EXECUTIONS_KEY);

		this.humidityMaxTimePastThreshold = configUtil.getInteger(ConfigConst.GATEWAY_DEVICE, ConfigConst.HUMID_MAX_TIME_PAST_THRESH_KEY, 300);
		this.nominalHumiditySetting = configUtil.getFloat(ConfigConst.GATEWAY_DEVICE, ConfigConst.NOMINAL_HUMID_KEY, 40.0f);
		this.triggerHumidifierFloor = configUtil.getFloat(ConfigConst.GATEWAY_DEVICE, ConfigConst.TRIGGER_HUMID_FLOOR_KEY, 30.0f);
		this.triggerHumidifierCeiling = configUtil.getFloat(ConfigConst.GATEWAY_DEVICE, ConfigConst.TRIGGER_HUMID_CEIL_KEY, 50.0f);

		// TODO validate the humidity/humidifier values

		if (this.enableSystemPerf) {
			this.systemPerfMgr = new SystemPerformanceManager();
			this.systemPerfMgr.setDataMessageListener(this);
			_Logger.info("System performance management enabled");
		}

		if (this.enablePersistenceClient) {
			this.persistenceClient = new RedisPersistenceAdapter();
			_Logger.info("Persistence client enabled");
		}

		if (this.enableMqttClient) {
			this.mqttClient = new MqttClientConnector();
			_Logger.info("MQTT client enabled");
			this.mqttClient.setDataMessageListener(this);
		}

		if (this.enableCoapServer) {
			this.coapServer = new CoapServerGateway();
			_Logger.info("CoAP server enabled");
			this.coapServer.setDataMessageListener(this);
		}

		if (this.enableCloudClient) {
			this.cloudClient = new CloudClientConnector();
			_Logger.info("Cloud client enabled");
			this.cloudClient.setDataMessageListener(this);
		}

		if (this.enablePuetceClient) {
			this.puetceClient = new PuetceClientConnector();
			_Logger.info("PUETCE LLM client enabled");
			this.puetceClient.setDataMessageListener(this);
			
			this.conversation = new ArrayList<>();
		}

		// init state
		
		initConnections();
	}
	
	public DeviceDataManager(
		boolean enableMqttClient,
		boolean enableCoapClient,
		boolean enableCloudClient,
		boolean enableSmtpClient,
		boolean enablePersistenceClient)
	{
		super();

		initConnections();
	}
	
	// public methods
	
	@Override
	public boolean handleActuatorCommandResponse(ResourceNameEnum resourceName, ActuatorData data)
	{
		if (data != null) {
			_Logger.info("Handling Actuator Command Response: " + data.getName());
			if (data.hasError()) { _Logger.warning("Error in Actuator Data"); }
			if (this.persistenceClient != null) {
				this.persistenceClient.storeData(resourceName.getResourceName(), 0, data);
			}
			return true;
		} 
		return false;
	}

	@Override
	public boolean handleActuatorCommandRequest(ResourceNameEnum resourceName, ActuatorData data)
	{
		if (data != null) {
			_Logger.log(
				Level.INFO,
				"Actuator request received: {0}. Message: {1}",
				new Object[] {
					resourceName.getResourceName(), 
					Integer.valueOf(
						data.getCommand()
					)
				}
			);

			if (data.hasError()) { 
				_Logger.warning("Error flag in Actuator Data"); 
			}

			int qos = ConfigConst.DEFAULT_QOS;
			
			// TODO optionally preprocess actuator data
			data.setLocationID(this.lastLocationID);

			this.sendActuatorCommandtoCda(resourceName, data);
			return true;
		}
		return false;
	}

	@Override
	public boolean handleIncomingMessage(ResourceNameEnum resourceName, String msg)
	{
		if (msg != null) {
			_Logger.info("Handling Generic Message: " + resourceName.getResourceName());
			msg = msg.replace("null", "\"\"");
			LLMHttpResponse response = new LLMHttpResponse(null,null);
			try {
            	response = gson.fromJson(msg, LLMHttpResponse.class);
			} catch (Exception e) {
				_Logger.severe("Failed to deserialize into http response: " + e);
				throw e;
			}
			_Logger.info("Deserialized message" + resourceName.getResourceName());

            switch (resourceName) {
            case GDA_MESSAGE_PUETCE_RESOURCE: 
				AnthropicMessage message = gson.fromJson(response.data, AnthropicMessage.class);
				handleMessagesResponse(resourceName, message);
                break;
			case GDA_EXECUTE_TOOL_PUETCE_RESOURCE:
				JsonObject result = gson.fromJson(response.data, JsonObject.class);
				ArrayList<ToolResultContentBlock> blocks = new ArrayList<>();
				for (var block : result.get("value").getAsJsonArray()) {
					try {
						ToolResultContentBlock b = gson.fromJson(block, ToolResultContentBlock.Text.class);
						blocks.add(b);
					} catch (Exception e) {
						_Logger.warning("this only supports text blocks");
					}
				}
				handleExecuteToolResponse(resourceName, blocks);
				break;
			case CDA_ACTUATOR_CMD_RESOURCE:
				return handleCloudActuatorCommand(resourceName, msg);
            default: 
                break;
            }

			return true;
		}
		return false;
	}

	@Override
	public boolean handleSensorMessage(ResourceNameEnum resourceName, SensorData data)
	{
		if (data != null) {
			_Logger.info("Handling Sensor Message: " + data.getName());
			this.lastLocationID = data.getLocationID();

			if (data.hasError()) { _Logger.warning("Error in Sensor Data"); }
			
			String jsonData = DataUtil.getInstance().sensorDataToJson(data);
			_Logger.fine("Serialize sensor data: " + jsonData);

			int qos = ConfigConst.DEFAULT_QOS;
			if (this.persistenceClient != null) {
				this.persistenceClient.storeData(resourceName.getResourceName(), 0, data);
			}

			handleIncomingMessage(resourceName, data);

			if (this.cloudClient == null) {
				return false;
			}

			if (!this.cloudClient.sendEdgeDataToCloud(resourceName, data)) {
				_Logger.severe("Failed to send data to CSP");
				return false;
			}
			// handleUpstreamTransmission(resourceName, jsonData, qos);

			return true;
		}
		return false;
	}

	@Override
	public boolean handleSystemPerformanceMessage(ResourceNameEnum resourceName, SystemPerformanceData data)
	{
		if (data != null) {
			_Logger.info("Handling System Performance Message: " + data.getName());

			if (data.hasError()) { 
				_Logger.warning("Error flag in System Performance Data"); 
			}

			if (this.cloudClient == null) {
				return false;
			}

			if (!this.cloudClient.sendEdgeDataToCloud(resourceName, data)) {
				_Logger.severe("Failed to send data to CSP");
				return false;
			}
			return true;
		}
		return false;
	}
	
	public void setActuatorDataListener(String name, IActuatorDataListener listener)
	{
		if (listener != null) {
			this.actuatorDataListener = listener;
		}
	}
	
	public void startManager()
	{
		_Logger.info("Starting DeviceDataManager...");

		if (this.cloudClient != null) {
			if (this.cloudClient.connectClient()) {
				_Logger.info("Cloud client connected");
			} else {
				_Logger.severe("Cloud client failed to connect");
			}
		}
		if (this.persistenceClient != null) {
			this.persistenceClient.connectClient();
		}
		if (this.mqttClient != null) {
			if (this.mqttClient.connectClient()) {
				_Logger.info("Successfully connected to MQTT broker.");
				int qos = ConfigUtil.getInstance().getInteger(
					ConfigConst.GATEWAY_DEVICE,
					ConfigConst.DEFAULT_QOS_KEY,
					ConfigConst.DEFAULT_QOS
				);
				
				// TODO these will be relocated later
				this.mqttClient.subscribeToTopic(ResourceNameEnum.GDA_MGMT_STATUS_MSG_RESOURCE, qos);
				this.mqttClient.subscribeToTopic(ResourceNameEnum.CDA_ACTUATOR_RESPONSE_RESOURCE, qos);
				this.mqttClient.subscribeToTopic(ResourceNameEnum.CDA_SENSOR_MSG_RESOURCE, qos);
				this.mqttClient.subscribeToTopic(ResourceNameEnum.CDA_SYSTEM_PERF_MSG_RESOURCE, qos);
			} else {
				_Logger.severe("Failed to connect to MQTT broker.");
			}
		}
		if (this.coapServer != null) {
			if (this.coapServer.startServer()) {
				_Logger.info("CoAP server started.");
			} else {
				_Logger.severe("Failed to start CoAP server. Check log file for details.");
			}
		}
		if (this.systemPerfMgr != null) { this.systemPerfMgr.startManager(); }

		_Logger.info("DeviceDataManager started");
	}
	
	public void stopManager()
	{
		_Logger.info("Stopping DeviceDataManager...");

		if (this.systemPerfMgr != null) { this.systemPerfMgr.stopManager(); }
		if (this.persistenceClient != null) { this.persistenceClient.disconnectClient(); }
		if (this.mqttClient != null) {
			this.mqttClient.unsubscribeFromTopic(ResourceNameEnum.GDA_MGMT_STATUS_MSG_RESOURCE);
			this.mqttClient.unsubscribeFromTopic(ResourceNameEnum.CDA_ACTUATOR_RESPONSE_RESOURCE);
			this.mqttClient.unsubscribeFromTopic(ResourceNameEnum.CDA_SENSOR_MSG_RESOURCE);
			this.mqttClient.unsubscribeFromTopic(ResourceNameEnum.CDA_SYSTEM_PERF_MSG_RESOURCE);
			
			if (this.mqttClient.disconnectClient()) {
				_Logger.info("Successfully disconnected to MQTT broker.");
			} else {
				_Logger.severe("Failed to disconnect to MQTT broker.");
			}
		}
		if (this.coapServer != null) {
			if (this.coapServer.stopServer()) {
				_Logger.info("CoAP server stopped.");
			} else {
				_Logger.severe("Failed to stop CoAP server. Check log file for details.");
			}
		}
		if (this.cloudClient != null) {
			if (this.cloudClient.disconnectClient()) {
				_Logger.info("Cloud client disconnected");
			} else {
				_Logger.severe("Cloud client failed to disconnect");
			}
		}

		_Logger.info("DeviceDataManager stopped");
	}

	
	// private methods
	
	/**
	 * Initializes the enabled connections. This will NOT start them, but only create the
	 * instances that will be used in the {@link #startManager() and #stopManager()) methods.
	 * 
	 */
	private void initConnections()
	{
	}

	private void handleIncomingMessage(ResourceNameEnum resource, SensorData data) {
		switch (data.getTypeID()) {
		case ConfigConst.HUMIDITY_SENSOR_TYPE:
			handleHumiditySensorAnalysis(resource, data);
			break;
		case ConfigConst.SPEECH_SENSOR_TYPE:
			handleSpeechSensorAnalysis(resource, data);
			break;
		default:
			_Logger.fine("Sensor type '" + data.getName() + "' is not handled on this device");
			break;
		}
	}

	// private void handleIncomingMessage(ResourceNameEnum resource, SystemPerformanceData data) { ...

	private void handleUpstreamTransmission(ResourceNameEnum resource, String jsonData, int qos) {
		// throw new NotImplementedError("dont use this");

		// _Logger.info("Sending JSON data to CSP: " + resource);

		// if (this.cloudClient == null) {
		// 	_Logger.severe("CSP is null");
		// 	return;
		// }

		// if (!this.cloudClient.sendEdgeDataToCloud(resource, jsonData)) {
		// 	_Logger.severe("Failed to send data to CSP");
		// }
	}

	@SuppressWarnings("unused")
	private void handleHumiditySensorAnalysis(ResourceNameEnum resource, SensorData data) {
		_Logger.info("Analyzing humidity data from CDA: " + data.getLocationID() + ". Value: " + data.getValue());

		// TODO were bypassing this because it is breaking something...
		if (true) {return;}

		boolean isLow  = data.getValue() < this.triggerHumidifierFloor;
		boolean isHigh = data.getValue() > this.triggerHumidifierCeiling;

		// TODO replace all of ts
		// 		e.g. a PID controller might be better
		
		if (isLow || isHigh) {

			_Logger.info("Humidity data from CDA exceeds nominal range.");
			
			if (this.latestHumiditySensorData == null) {

				// set properties then exit
				// wait for a second sample
				this.latestHumiditySensorData = data;
				this.latestHumiditySensorTimeStamp = getDateTimeFromData(data);
				
				_Logger.info(
					"Starting humidity nominal exception timer. Waiting for seconds: " +
					this.humidityMaxTimePastThreshold
				);
				
				return;

			} else {

				OffsetDateTime curHumiditySensorTimeStamp = getDateTimeFromData(data);
				long diffSeconds = ChronoUnit.SECONDS.between(
					this.latestHumiditySensorTimeStamp, 
					curHumiditySensorTimeStamp
				);
				
				_Logger.info("Checking Humidity value exception time delta: " + diffSeconds);
				
				if (diffSeconds >= this.humidityMaxTimePastThreshold) {
					
					ActuatorData ad = new ActuatorData();
					ad.setName(ConfigConst.HUMIDIFIER_ACTUATOR_NAME);
					ad.setLocationID(data.getLocationID());
					ad.setTypeID(ConfigConst.HUMIDIFIER_ACTUATOR_TYPE);
					ad.setValue(this.nominalHumiditySetting);
					
					if (isLow) {
						ad.setCommand(ConfigConst.ON_COMMAND);
					} else if (isHigh) {
						ad.setCommand(ConfigConst.OFF_COMMAND);
					}
					
					_Logger.info(
						"Humidity exceptional value reached. Sending actuation event to CDA: " +
						ad
					);
					
					this.lastKnownHumidifierCommand = ad.getCommand();
					sendActuatorCommandtoCda(ResourceNameEnum.CDA_ACTUATOR_CMD_RESOURCE, ad);
					
					// set ActuatorData and reset SensorData (and timestamp)
					this.latestHumidifierActuatorData = ad;
					this.latestHumiditySensorData = null;
					this.latestHumiditySensorTimeStamp = null;

				}
			}

		} else if (this.lastKnownHumidifierCommand == ConfigConst.ON_COMMAND) {

			if (this.latestHumidifierActuatorData != null) {

				if (this.latestHumidifierActuatorData.getValue() >= this.nominalHumiditySetting) {

					this.latestHumidifierActuatorData.setCommand(ConfigConst.OFF_COMMAND);
					
					_Logger.info(
						"Humidity nominal value reached. Sending OFF actuation event to CDA: " +
						this.latestHumidifierActuatorData);
					
					sendActuatorCommandtoCda(
						ResourceNameEnum.CDA_ACTUATOR_CMD_RESOURCE, this.latestHumidifierActuatorData);
					
					// reset ActuatorData and SensorData (and timestamp)
					this.lastKnownHumidifierCommand = this.latestHumidifierActuatorData.getCommand();
					this.latestHumidifierActuatorData = null;
					this.latestHumiditySensorData = null;
					this.latestHumiditySensorTimeStamp = null;

				} else {

					_Logger.info("Humidifier is still on. Not yet at nominal levels (OK).");

				}

			} else {

				// shouldn't happen, unless some other logic
				// nullifies the class-scoped ActuatorData instance
				_Logger.warning("ERROR: ActuatorData for humidifier is null (shouldn't be). Can't send command.");

			}
		}
	}

	private void sendActuatorCommandtoCda(ResourceNameEnum resource, ActuatorData data)
	{ // pasta
		// NOTE: This is how an ActuatorData command will get passed to the CDA
		// when the GDA is providing the CoAP server and hosting the appropriate
		// ActuatorData resource. It will typically be used when the OBSERVE
		// client (the CDA, assuming the GDA is the server and CDA is the client)
		// has sent an OBSERVE GET request to the ActuatorData resource.
		if (this.actuatorDataListener != null) {
			this.actuatorDataListener.onActuatorDataUpdate(data);
		}
		
		// NOTE: This is how an ActuatorData command will get passed to the CDA
		// when using MQTT to communicate between the GDA and CDA
		if (this.enableMqttClient && this.mqttClient != null) {
			String jsonData = DataUtil.getInstance().actuatorDataToJson(data);
			_Logger.info("sending to cda (inside fcn now)");
			
			if (this.mqttClient.publishMessage(resource, jsonData, ConfigConst.DEFAULT_QOS)) {
				_Logger.info(
					"Published ActuatorData command from GDA to CDA: " + data.getCommand());
			} else {
				_Logger.warning(
					"Failed to publish ActuatorData command from GDA to CDA: " + data.getCommand());
			}
		}
	}
	
	private OffsetDateTime getDateTimeFromData(BaseIotData data)
	{ // pasta
		OffsetDateTime odt = null;
		
		try {
			odt = OffsetDateTime.parse(data.getTimeStamp());
		} catch (Exception e) {
			_Logger.warning(
				"Failed to extract ISO 8601 timestamp from IoT data. Using local current time.");
			
			// TODO: this won't be accurate, but should be reasonably close, as the CDA will
			// most likely have recently sent the data to the GDA
			odt = OffsetDateTime.now();
		}
		
		return odt;
	}

	private boolean handleCloudActuatorCommand(ResourceNameEnum resource, String msg) {
		ActuatorData data = DataUtil.getInstance().jsonToActuatorData(msg);
		String json = DataUtil.getInstance().actuatorDataToJson(data);

		int qos = ConfigUtil.getInstance().getInteger(
			ConfigConst.MQTT_GATEWAY_SERVICE, 
			ConfigConst.DEFAULT_QOS_KEY, 
			ConfigConst.DEFAULT_QOS
		);
		if (this.mqttClient != null) {
			_Logger.fine("Publishing data to broker");
			return this.mqttClient.publishMessage(resource, json, qos);
		} else {
			_Logger.warning("No mqtt client to publish to");
			return true;
		}
	}

	private void handleMessagesResponse(ResourceNameEnum resource, AnthropicMessage message) {
		_Logger.info("Handling response from anthropic: " + resource.getResourceName() 
			+ " (" + message.content.size() + " content block(s))");

		// entry point for anthropic response logic
		// 	- tool uses need to be handled here

		// validation (this should always be an ASSISTANT message)

		if (message.role != AnthropicRole.ASSISTANT) {
			_Logger.severe("Invalid response role: expected 'assitant', found '" + message.role.toString() + "'");
			return;
		}
		this.conversation.add(message); // TODO hide tool use and tool results

		boolean containsToolUse = message.content.stream().anyMatch(block -> block instanceof AnthropicContentBlock.ToolUse);

		message.content.forEach(block -> {
			if (block instanceof AnthropicContentBlock.Text) {
				_Logger.info("Handling text response");
				AnthropicContentBlock.Text textBlock = (AnthropicContentBlock.Text)block;

				if (!containsToolUse || useVerboseToolExecutions) {
					// say the response
					ActuatorData ad = new ActuatorData();
					ad.setName(ConfigConst.TTS_ACTUATOR_NAME);
					ad.setTypeID(ConfigConst.TTS_ACTUATOR_TYPE);
					ad.setCommand(ConfigConst.ON_COMMAND);
					ad.setLocationID(this.lastMessageLocationID);
					ad.setStateData(textBlock.text);

					sendActuatorCommandtoCda(ResourceNameEnum.CDA_ACTUATOR_CMD_RESOURCE, ad);
				}
			} else if (block instanceof AnthropicContentBlock.ToolUse) {
				_Logger.info("Handling tool_use response");
				AnthropicContentBlock.ToolUse toolUseBlock = (AnthropicContentBlock.ToolUse)block;

				if (useVerboseToolExecutions) {
					// say executing tool
					ActuatorData ad = new ActuatorData();
					ad.setName(ConfigConst.TTS_ACTUATOR_NAME);
					ad.setTypeID(ConfigConst.TTS_ACTUATOR_TYPE);
					ad.setCommand(ConfigConst.ON_COMMAND);
					ad.setLocationID(this.lastMessageLocationID);
					ad.setStateData("Executing " + toolUseBlock.name);

					sendActuatorCommandtoCda(ResourceNameEnum.CDA_ACTUATOR_CMD_RESOURCE, ad);
				}

				// execute tool
				this.lastToolID = toolUseBlock.id;
				puetceClient.executeTool(toolUseBlock.name, toolUseBlock.input);
			} else {
				_Logger.severe("Unexpected block type: " + block.getClass().getSimpleName());
			}
		});
	}

	private void handleExecuteToolResponse(ResourceNameEnum resource, List<ToolResultContentBlock> result) {
		_Logger.info("Handling response from tool execution: " + resource.getResourceName());

		ArrayList<ToolResultContentBlock> tcontent = new ArrayList<>();
		for (var tblock : result) {
			tcontent.add(tblock);
		}

		AnthropicContentBlock.ToolResult block = new AnthropicContentBlock.ToolResult(this.lastToolID, tcontent, false);
		ArrayList<AnthropicContentBlock> content = new ArrayList<>();
		content.add(block);

		AnthropicMessage toolResultMessage = new AnthropicMessage(AnthropicRole.USER, content);
		conversation.add(toolResultMessage);

		puetceClient.sendMessage(
			conversation,
			"You are generating a spoken response. Your " + //
			"response should as concise as possible. Responses should " + //
			"be plain text (no markdown). Your responses should be short " + //
			"and conversational: 100 words or less. Your response should " + //
			"contain no markdown syntax.",
			true,
			0.5f
		);

		// TODO
	}

	private void handleSpeechSensorAnalysis(ResourceNameEnum resource, SensorData data) {
		_Logger.info("Analyzing speech data from " + data.getLocationID() + ". State data: " + data.getStateData());
		// TODO this is a workaround
		this.lastMessageLocationID = data.getLocationID();

		// this is the core of ALL OUTBOUND ANTHROPIC MESSAGE LOGIC
		// this might need to be broken into a separate module and/or handled elsewhere...
		try {
			JsonObject value = gson.fromJson(data.getStateData(), JsonObject.class);

			// repeat logic is janky... event-driven arch would be much safer
			if (
				value.get("result").getAsString().length() > 0 &&
				!value.get("result").getAsString().equals(this.lastKnownSpeechResult)
			) {
				// if speech detected is not a repeat and not empty
				if (!value.get("isComplete").getAsBoolean()) {
					_Logger.fine("Using incomplete vosk result");
				} else {
					_Logger.fine("Using complete vosk result");
				}

				if (enablePuetceClient) {
					if (value.get("result").getAsString().toLowerCase().trim().equals("reset")) {
						_Logger.info("Reset conversation...");

						this.conversation.clear();

						ActuatorData ad = new ActuatorData();
						ad.setName(ConfigConst.TTS_ACTUATOR_NAME);
						ad.setTypeID(ConfigConst.TTS_ACTUATOR_TYPE);
						ad.setCommand(ConfigConst.ON_COMMAND);
						ad.setLocationID(this.lastMessageLocationID);
						ad.setStateData("Conversation reset");

						_Logger.info("Sending actuator cmd to cda");
						sendActuatorCommandtoCda(ResourceNameEnum.CDA_ACTUATOR_CMD_RESOURCE, ad);
					} else {
						_Logger.info("Sending message to anthropic...");

						// build new message
						AnthropicContentBlock block = new AnthropicContentBlock.Text(value.get("result").getAsString());
						ArrayList<AnthropicContentBlock> content = new ArrayList<>();
						content.add(block);
						AnthropicMessage newMessage = new AnthropicMessage(AnthropicRole.USER, content);

						// append new message to conversation
						this.conversation.add(newMessage);

						boolean success = this.puetceClient.sendMessage(
							conversation,
							"You are generating a spoken response. Your " + //
							"response should as concise as possible. Responses should " + //
							"be plain text (no markdown). Your responses should be short " + //
							"and conversational: 100 words or less. Your response should " + //
							"contain no markdown syntax.",
							true,
							0.5f
						);

						if (!success) {
							_Logger.severe("send message failed");
						}
					}
				} else {
					_Logger.warning("LLM client not enabled");
				}
			}

			this.lastKnownSpeechResult = value.get("result").getAsString();
		} catch (JsonSyntaxException e) {
			_Logger.warning("Failed to parse json speech data: " + e);
		}
	}
}
