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

package programmingtheiot.gda.connection;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.ObjectInputFilter.Config;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.SSLSocketFactory;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import programmingtheiot.common.ConfigConst;
import programmingtheiot.common.ConfigUtil;
import programmingtheiot.common.IDataMessageListener;
import programmingtheiot.common.ResourceNameEnum;
import programmingtheiot.common.SimpleCertManagementUtil;
import programmingtheiot.data.ActuatorData;
import programmingtheiot.data.DataUtil;
import programmingtheiot.data.SensorData;
import programmingtheiot.data.SystemPerformanceData;

/**
 * Shell representation of class for student implementation.
 * 
 */
public class MqttClientConnector implements IPubSubClient, MqttCallbackExtended
{
	// static
	
	private static final Logger _Logger =
		Logger.getLogger(MqttClientConnector.class.getName());
	
	// params

	private boolean useAsyncClient = false;

	// private MqttClient mqttClient = null;
	private MqttAsyncClient mqttClient = null;

	private MqttConnectOptions connOptions = null;
	private MemoryPersistence persistence = null;
	private IDataMessageListener dataMsgListener = null;

	private String clientID = null;
	private String brokerAddr = null;
	private String host = ConfigConst.DEFAULT_HOST;
	private String protocol = ConfigConst.DEFAULT_MQTT_PROTOCOL;
	private int port = ConfigConst.DEFAULT_MQTT_PORT;
	private int brokerKeepAlive = ConfigConst.DEFAULT_KEEP_ALIVE;

	private String caFileName = null;
	private boolean enableEncryption = false;
	private boolean useCleanSession = false;
	private boolean enableAutoReconnect = true;
	
	// constructors
	
	/**
	 * Default.
	 * 
	 */
	public MqttClientConnector()
	{
		super();
		initClientParameters(ConfigConst.MQTT_GATEWAY_SERVICE);
	}

	public MqttClientConnector(boolean useAsync)
	{
		super();
		initClientParameters(ConfigConst.MQTT_GATEWAY_SERVICE);
	}
	
	
	// public methods
	
	@Override
	public boolean connectClient()
	{
		try {
			if (this.mqttClient == null) {
				// this.mqttClient = new MqttClient(this.brokerAddr, this.clientID, this.persistence);
				this.mqttClient = new MqttAsyncClient(this.brokerAddr, this.clientID, this.persistence);
				this.mqttClient.setCallback(this);
			}

			if (!this.mqttClient.isConnected()) {
				_Logger.info("MQTT client connecting to broker: " + this.brokerAddr);
				this.mqttClient.connect(this.connOptions);
				return true;
			} else {
				_Logger.warning("MQTT client already connected to broker");
			}
		} catch (MqttException e) {
			_Logger.severe("Failed to connect to MQTT broker. " + e);
		}

		return false;
	}

	@Override
	public boolean disconnectClient()
	{
		try {
			if (this.mqttClient != null && this.mqttClient.isConnected()) {
				_Logger.info("MQTT client disconnecting to broker: " + this.brokerAddr);
				this.mqttClient.disconnect();
				return true;
			} else {
				_Logger.warning("MQTT client not connected to broker");
			}

		} catch (Exception e) {
			_Logger.severe("Failed to disconnect from MQTT broker: " + e);
		}
		return false;
	}

	public boolean isConnected()
	{
		return this.mqttClient!=null && this.mqttClient.isConnected();
	}
	
	@Override
	public boolean publishMessage(ResourceNameEnum topic, String msg, int qos)
	{
		// validations
		if (topic == null) {
			// _Logger.warning("Resource is null. Unable to publish message: " + this.brokerAddr);
			return false;
		}
		if (msg == null || msg.length() == 0) {
			// _Logger.warning("Message is null or empty. Unable to publish message: " + this.brokerAddr);
			return false;
		}
		if (qos<0 || qos>2) {
			qos = ConfigConst.DEFAULT_QOS;
		}

		// publish
		try {
			MqttMessage mqttMsg = new MqttMessage(msg.getBytes());
			mqttMsg.setQos(qos);
			this.mqttClient.publish(topic.getResourceName(), mqttMsg);
			return true;
		} catch  (Exception e) {
			// _Logger.severe("Failed to publish message to topic '" + topic.getResourceName() + "': " + e);
		}

		return false;
	}

	@Override
	public boolean subscribeToTopic(ResourceNameEnum topic, int qos)
	{
		// validations
		if (topic == null) {
			_Logger.warning("Resource is null. Unable to subscribe: " + this.brokerAddr);
			return false;
		}
		if (qos<0 || qos>2) {
			qos = ConfigConst.DEFAULT_QOS;
		}

		// subscribe
		try {
			this.mqttClient.subscribe(topic.getResourceName(), qos);
			_Logger.info("Successfully subscribed to topic: " + topic.getResourceName());
			return true;
		} catch (Exception e) {
			_Logger.severe("Failed to subscribe to topic '" + topic + "': " + e);
		}

		return false;
	}

	@Override
	public boolean unsubscribeFromTopic(ResourceNameEnum topic)
	{
		// validation
		if (topic == null) {
			_Logger.warning("Resource is null. Unable to subscribe: " + this.brokerAddr);
			return false;
		}
		
		// unsubscribe
		try {
			this.mqttClient.unsubscribe(topic.getResourceName());
			_Logger.info("Successfully unsubscribed from topic: " + topic.getResourceName());
			return true;
		} catch (Exception e) {
			_Logger.severe("Failed to unsubscribe from topic '" + topic.getResourceName() + "':" + e);
		}

		return false;
	}

	@Override
	public boolean setConnectionListener(IConnectionListener listener)
	{
		return false;
	}
	
	@Override
	public boolean setDataMessageListener(IDataMessageListener listener)
	{
		if (listener != null) {
			this.dataMsgListener = listener;
			return true;
		}
		return false;
	}
	
	// callbacks
	
	@Override
	public void connectComplete(boolean reconnect, String serverURI)
	{
		_Logger.info("MQTT connection successful: (is reconnect = " + reconnect + "). Broker: " + serverURI);

		int qos = 1;
		
		this.subscribeToTopic(ResourceNameEnum.CDA_ACTUATOR_RESPONSE_RESOURCE, qos);
		this.subscribeToTopic(ResourceNameEnum.CDA_SENSOR_MSG_RESOURCE, qos);
		this.subscribeToTopic(ResourceNameEnum.CDA_SYSTEM_PERF_MSG_RESOURCE, qos);
	}

	@Override
	public void connectionLost(Throwable t)
	{
		_Logger.warning("Lost connection to MQTT broker: " + this.brokerAddr + ", " + t);
	}
	
	@Override
	public void deliveryComplete(IMqttDeliveryToken token)
	{
		// _Logger.info("Delivered MQTT message with ID: " + token.getMessageId());
	}
	
	@Override
	public void messageArrived(String topic, MqttMessage msg) throws Exception
	{
		_Logger.info("MQTT message arrived on topic: '" + topic + "'");

		// try actuator response data (yes this is not great practice but idk java)
		try {
			ActuatorData ad = DataUtil.getInstance()
				.jsonToActuatorData(new String(msg.getPayload()));

			_Logger.info("Received actuator response");
			if (this.dataMsgListener!=null) {
				this.dataMsgListener.handleActuatorCommandResponse(
					ResourceNameEnum.CDA_ACTUATOR_RESPONSE_RESOURCE, ad
				);
			}

			return;
		} catch (Exception e) {
			// ignore
		}

		// try sensor data
		try {
			SensorData sd = DataUtil.getInstance()
				.jsonToSensorData(new String(msg.getPayload()));

			_Logger.info("Received sensor data");
			if (this.dataMsgListener!=null) {
				this.dataMsgListener.handleSensorMessage(
					ResourceNameEnum.CDA_ACTUATOR_RESPONSE_RESOURCE, sd
				);
			}

			return;
		} catch (Exception e) {
			// ignore
		}

		// try sysperf data
		try {
			SystemPerformanceData spd = DataUtil.getInstance()
				.jsonToSystemPerformanceData(new String(msg.getPayload()));

			_Logger.info("Received system performance data");
			if (this.dataMsgListener!=null) {
				this.dataMsgListener.handleSystemPerformanceMessage(
					ResourceNameEnum.CDA_ACTUATOR_RESPONSE_RESOURCE, spd
				);
			}

			return;
		} catch (Exception e) {
			// ignore
		}

		_Logger.severe("Invalid message payload");
		throw new Exception("Invalid message payload");
	}

	// private methods
	
	/**
	 * Called by the constructor to set the MQTT client parameters to be used for the connection.
	 * 
	 * @param configSectionName The name of the configuration section to use for
	 * the MQTT client configuration parameters.
	 */
	private void initClientParameters(String configSectionName)
	{
		ConfigUtil cu = ConfigUtil.getInstance();

		// NOTE this is using sync client

		this.host = cu.getProperty(configSectionName, ConfigConst.HOST_KEY, ConfigConst.DEFAULT_HOST);
		this.port = cu.getInteger(configSectionName, ConfigConst.PORT_KEY, ConfigConst.DEFAULT_MQTT_SECURE_PORT);
		this.brokerKeepAlive = cu.getInteger(configSectionName, ConfigConst.KEEP_ALIVE_KEY, ConfigConst.DEFAULT_KEEP_ALIVE);
		this.enableEncryption = cu.getBoolean(configSectionName, ConfigConst.ENABLE_CRYPT_KEY);
		this.caFileName = cu.getProperty(configSectionName, ConfigConst.CERT_FILE_KEY, null);
		// this.useAsyncClient = cu.getProperty(ConfigConst.MQTT_GATEWAY_SERVICE, ConfigConst.USE_ASYNC_CLIENT_KEY);
		this.clientID = cu.getProperty(ConfigConst.GATEWAY_DEVICE, ConfigConst.DEVICE_LOCATION_ID_KEY, MqttClient.generateClientId());
		
		this.persistence = new MemoryPersistence();
		this.connOptions = new MqttConnectOptions();

		this.connOptions.setKeepAliveInterval(this.brokerKeepAlive);
		this.connOptions.setCleanSession(this.useCleanSession);
		this.connOptions.setAutomaticReconnect(this.enableAutoReconnect);
		
		try {
			if (this.enableEncryption) { initSecureConnectionParameters(configSectionName); }
			if (cu.hasProperty(configSectionName, ConfigConst.CRED_FILE_KEY)) {
				initCredentialConnectionParameters(configSectionName);
			}
		} catch (Exception e) {
			_Logger.log(Level.SEVERE, "Init secure connection failed:", e);
			this.enableEncryption = false;
		}

		this.brokerAddr  = this.protocol + "://" + this.host + ":" + this.port;

		_Logger.info("Client initialized: " + this.brokerAddr);
	}
	
	/**
	 * Called by {@link #initClientParameters(String)} to load credentials.
	 * 
	 * @param configSectionName The name of the configuration section to use for
	 * the MQTT client configuration parameters.
	 */
	private void initCredentialConnectionParameters(String configSectionName)
	{
		ConfigUtil cu = ConfigUtil.getInstance();
		try {
			_Logger.info("Checking credential file...");
			Properties props = cu.getCredentials(configSectionName);
			if (props!=null) {
				this.connOptions.setUserName(cu.getProperty(ConfigConst.USER_NAME_TOKEN_KEY, ""));
				this.connOptions.setPassword(cu.getProperty(ConfigConst.USER_AUTH_TOKEN_KEY, "").toCharArray());
				_Logger.info("Credentials set");
			} else {
				_Logger.warning("No credentials set");
			}
		} catch (Exception e) {
			_Logger.log(Level.SEVERE, "No credential file.");
			throw e;
		}
	}
	
	/**
	 * Called by {@link #initClientParameters(String)} to enable encryption.
	 * 
	 * @param configSectionName The name of the configuration section to use for
	 * the MQTT client configuration parameters.
	 * @throws Exception if file not found or path is null
	 */
	private void initSecureConnectionParameters(String configSectionName) throws Exception
	{
		ConfigUtil cu = ConfigUtil.getInstance();

		try {
			_Logger.info("Configuring TLS...");
			if (this.caFileName!=null) {
				File file = new File(this.caFileName);
				if (file.exists()) {
					_Logger.info("Found cred file at " + this.caFileName);
				} else {
					_Logger.info("Cred file does not exist");
					throw new FileNotFoundException(this.caFileName + "not found");
				}
			} else {
				_Logger.severe("No cred file provided");
				throw new Exception("No cred file provided");
			}

			SSLSocketFactory sslFactory = SimpleCertManagementUtil
				.getInstance()
				.loadCertificate(this.caFileName);
			this.connOptions.setSocketFactory(sslFactory);

			this.port = cu.getInteger(
				configSectionName,
				ConfigConst.SECURE_PORT_KEY,
				ConfigConst.DEFAULT_MQTT_SECURE_PORT
			);

			this.protocol = ConfigConst.DEFAULT_MQTT_SECURE_PROTOCOL;
		} catch (Exception e) {
			_Logger.log(Level.SEVERE, "Secure MQTT Connection failed: ", e);
			throw e;
		}
	}
}
