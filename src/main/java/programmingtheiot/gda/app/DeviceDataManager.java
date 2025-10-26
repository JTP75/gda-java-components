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

import java.util.logging.Level;
import java.util.logging.Logger;

import programmingtheiot.common.ConfigConst;
import programmingtheiot.common.ConfigUtil;
import programmingtheiot.common.IActuatorDataListener;
import programmingtheiot.common.IDataMessageListener;
import programmingtheiot.common.ResourceNameEnum;

import programmingtheiot.data.ActuatorData;
import programmingtheiot.data.DataUtil;
import programmingtheiot.data.SensorData;
import programmingtheiot.data.SystemPerformanceData;

import programmingtheiot.gda.connection.CloudClientConnector;
import programmingtheiot.gda.connection.CoapServerGateway;
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
	private IPubSubClient cloudClient = null;
	private IPersistenceClient persistenceClient = null;
	private IRequestResponseClient smtpClient = null;
	private CoapServerGateway coapServer = null;
	private SystemPerformanceManager systemPerfMgr = null;
	
	// constructors
	
	public DeviceDataManager()
	{
		super();
	
		ConfigUtil configUtil = ConfigUtil.getInstance();
		
		this.enableMqttClient = configUtil.getBoolean(
			ConfigConst.GATEWAY_DEVICE, 
			ConfigConst.ENABLE_MQTT_CLIENT_KEY
		);
		
		this.enableCoapServer = configUtil.getBoolean(
			ConfigConst.GATEWAY_DEVICE, 
			ConfigConst.ENABLE_COAP_SERVER_KEY
		);
		
		this.enableCloudClient = configUtil.getBoolean(
			ConfigConst.GATEWAY_DEVICE, 
			ConfigConst.ENABLE_CLOUD_CLIENT_KEY
		);
		
		this.enablePersistenceClient = configUtil.getBoolean(
			ConfigConst.GATEWAY_DEVICE, 
			ConfigConst.ENABLE_PERSISTENCE_CLIENT_KEY
		);

		this.enableSystemPerf = configUtil.getBoolean(
			ConfigConst.GATEWAY_DEVICE, 
			ConfigConst.ENABLE_SYSTEM_PERF_KEY
		);

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
			if (this.persistenceClient != null) {
				this.persistenceClient.storeData(resourceName.getResourceName(), 0, data);
			}
			return true;
		}
		return false;
	}

	@Override
	public boolean handleSystemPerformanceMessage(ResourceNameEnum resourceName, SystemPerformanceData data)
	{
		if (data != null) {
			_Logger.info("Handling System Performance Message: " + data.getName());
			if (data.hasError()) { _Logger.warning("Error in System Performance Data"); }
			return true;
		}
		return false;
	}
	
	public void setActuatorDataListener(String name, IActuatorDataListener listener)
	{
	}
	
	public void startManager()
	{
		_Logger.info("Starting DeviceDataManager...");
		if (this.systemPerfMgr != null) { this.systemPerfMgr.startManager(); }
		if (this.persistenceClient != null) { this.persistenceClient.connectClient(); }
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

	// add methods later
	
}
