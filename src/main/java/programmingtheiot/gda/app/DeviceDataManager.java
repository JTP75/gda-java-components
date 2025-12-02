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
import java.util.logging.Level;
import java.util.logging.Logger;

import kotlin.NotImplementedError;
import programmingtheiot.common.ConfigConst;
import programmingtheiot.common.ConfigUtil;
import programmingtheiot.common.IActuatorDataListener;
import programmingtheiot.common.IDataMessageListener;
import programmingtheiot.common.ResourceNameEnum;

import programmingtheiot.data.ActuatorData;
import programmingtheiot.data.BaseIotData;
import programmingtheiot.data.DataUtil;
import programmingtheiot.data.SensorData;
import programmingtheiot.data.SystemPerformanceData;

import programmingtheiot.gda.connection.CloudClientConnector;
import programmingtheiot.gda.connection.CoapServerGateway;
import programmingtheiot.gda.connection.ICloudClient;
import programmingtheiot.gda.connection.IPersistenceClient;
import programmingtheiot.gda.connection.IPubSubClient;
import programmingtheiot.gda.connection.IRequestResponseClient;
import programmingtheiot.gda.connection.MqttClientConnector;
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
	private boolean enableSystemPerf = false;
	
	private IActuatorDataListener actuatorDataListener = null;
	private IPubSubClient mqttClient = null;
	private ICloudClient cloudClient = null;
	private IPersistenceClient persistenceClient = null;
	private IRequestResponseClient smtpClient = null;
	private CoapServerGateway coapServer = null;
	private SystemPerformanceManager systemPerfMgr = null;

	private ActuatorData   latestHumidifierActuatorData = null;
	private ActuatorData   latestHumidifierActuatorResponse = null;
	private SensorData     latestHumiditySensorData = null;
	private OffsetDateTime latestHumiditySensorTimeStamp = null;

	private boolean handleHumidityChangeOnDevice = false;
	private int     lastKnownHumidifierCommand   = ConfigConst.OFF_COMMAND;
	
	private long    humidityMaxTimePastThreshold = 300; // seconds
	private float   nominalHumiditySetting   = 40.0f;
	private float   triggerHumidifierFloor   = 30.0f;
	private float   triggerHumidifierCeiling = 50.0f;
	
	// constructors
	
	public DeviceDataManager()
	{
		super();
	
		ConfigUtil configUtil = ConfigUtil.getInstance();
		
		this.enableMqttClient = configUtil.getBoolean(ConfigConst.GATEWAY_DEVICE, ConfigConst.ENABLE_MQTT_CLIENT_KEY);
		this.enableCoapServer = configUtil.getBoolean(ConfigConst.GATEWAY_DEVICE, ConfigConst.ENABLE_COAP_SERVER_KEY);
		this.enableCloudClient = configUtil.getBoolean(ConfigConst.GATEWAY_DEVICE, ConfigConst.ENABLE_CLOUD_CLIENT_KEY);		
		this.enablePersistenceClient = configUtil.getBoolean(ConfigConst.GATEWAY_DEVICE, ConfigConst.ENABLE_PERSISTENCE_CLIENT_KEY);
		this.enableSystemPerf = configUtil.getBoolean(ConfigConst.GATEWAY_DEVICE, ConfigConst.ENABLE_SYSTEM_PERF_KEY);

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

			this.sendActuatorCommandtoCda(resourceName, data);
			return true;
		}
		return false;
	}

	@Override
	public boolean handleIncomingMessage(ResourceNameEnum resourceName, String msg)
	{
		if (msg != null) {
			_Logger.info("Handling Generic Message: " + msg);
			return true;
		}
		return false;
	}

	@Override
	public boolean handleSensorMessage(ResourceNameEnum resourceName, SensorData data)
	{
		if (data != null) {
			_Logger.info("Handling Sensor Message: " + data.getName());
			if (data.hasError()) { _Logger.warning("Error in Sensor Data"); }
			
			String jsonData = DataUtil.getInstance().sensorDataToJson(data);
			_Logger.fine("Serialize sensor data: " + jsonData);

			int qos = ConfigConst.DEFAULT_QOS;
			if (this.persistenceClient != null) {
				this.persistenceClient.storeData(resourceName.getResourceName(), 0, data);
			}

			handleIncomingMessage(resourceName, data);

			if (this.cloudClient == null) {
				_Logger.warning("CSP is null");
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
				_Logger.warning("CSP is null");
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
		if (data.getTypeID()==ConfigConst.HUMIDITY_SENSOR_TYPE) {
			handleHumiditySensorAnalysis(resource, data);
		}
	}

	// private void handleIncomingMessage(ResourceNameEnum resource, SystemPerformanceData data) { ...

	private void handleUpstreamTransmission(ResourceNameEnum resource, String jsonData, int qos) {
		throw new NotImplementedError("dont use this");

		// _Logger.info("Sending JSON data to CSP: " + resource);

		// if (this.cloudClient == null) {
		// 	_Logger.severe("CSP is null");
		// 	return;
		// }

		// if (!this.cloudClient.sendEdgeDataToCloud(resource, jsonData)) {
		// 	_Logger.severe("Failed to send data to CSP");
		// }
	}

	private void handleHumiditySensorAnalysis(ResourceNameEnum resource, SensorData data) {
		_Logger.info("Analyzing humidity data from CDA: " + data.getLocationID() + ". Value: " + data.getValue());
	
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
}
