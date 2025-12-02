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

import java.util.logging.Level;
import java.util.logging.Logger;

import programmingtheiot.common.ConfigConst;
import programmingtheiot.common.ConfigUtil;
import programmingtheiot.common.IDataMessageListener;
import programmingtheiot.common.ResourceNameEnum;
import programmingtheiot.data.DataUtil;
import programmingtheiot.data.SensorData;
import programmingtheiot.data.SystemPerformanceData;

/**
 * Shell representation of class for student implementation.
 *
 */
@SuppressWarnings("unused")
public class CloudClientConnector implements ICloudClient
{
	// static
	
	private static final Logger _Logger =
		Logger.getLogger(CloudClientConnector.class.getName());
	
	// private var's
	private String prefix = "";
	private MqttClientConnector mqtt = null;
	private IDataMessageListener listener = null;
	private int qos = 1;
	
	// constructors
	
	/**
	 * Default.
	 * 
	 */
	public CloudClientConnector()
	{
		this.prefix = ConfigUtil.getInstance().getProperty(
			ConfigConst.CLOUD_GATEWAY_SERVICE, 
			ConfigConst.BASE_TOPIC_KEY, 
			"/"
		);
		
		if (!this.prefix.endsWith("/")) { this.prefix += "/"; }
	}
	
	// public methods
	
	@Override
	public boolean connectClient()
	{
		if (this.mqtt == null) {
			this.mqtt = new MqttClientConnector(ConfigConst.CLOUD_GATEWAY_SERVICE);
			if (this.listener != null) {
				this.mqtt.setDataMessageListener(this.listener);
			}
		}

		return mqtt.connectClient();
	}

	@Override
	public boolean disconnectClient()
	{
		if (this.mqtt == null) { return false; }

		return mqtt.disconnectClient();
	}

	@Override
	public boolean setDataMessageListener(IDataMessageListener listener)
	{
		if (listener != null) {
			this.listener = listener;
			return true;
		}
		return false;
	}

	@Override
	public boolean sendEdgeDataToCloud(ResourceNameEnum resource, SensorData data)
	{
		if (resource!=null && data!=null) {
			String payload = DataUtil.getInstance().sensorDataToTVJson(data);
			return publishMessageToCloud(resource, data.getName(), payload);
		}
		return false;
	}

	@Override
	public boolean sendEdgeDataToCloud(ResourceNameEnum resource, SystemPerformanceData data)
	{
		if (resource!=null && data!=null) {
			// handle by converting sys perf data to sensor datas

			SensorData cpuData = new SensorData();
			cpuData.updateData(data);
			cpuData.setName(ConfigConst.CPU_UTIL_NAME);
			cpuData.setValue(data.getCpuUtilization());
			
			boolean cpuDataSuccess = sendEdgeDataToCloud(resource, cpuData);
			
			if (! cpuDataSuccess) {
				_Logger.warning("Failed to send CPU utilization data to cloud service.");
			}
			
			SensorData memData = new SensorData();
			memData.updateData(data);
			memData.setName(ConfigConst.MEM_UTIL_NAME);
			memData.setValue(data.getMemoryUtilization());
			
			boolean memDataSuccess = sendEdgeDataToCloud(resource, memData);
			
			if (! memDataSuccess) {
				_Logger.warning("Failed to send memory utilization data to cloud service.");
			}

			return cpuDataSuccess && memDataSuccess;
		}
		return false;
	}

	@Override
	public boolean subscribeToCloudEvents(ResourceNameEnum resource)
	{
		String topic = createTopicName(resource);
		if (this.mqtt != null && this.mqtt.isConnected()) {
			// TODO this is a generic handler

			this.mqtt.subscribeToTopic(topic, qos, null);
			return true;
		} else {
			_Logger.warning("MQTT not connected. Ignoring sub topic: " + topic);
			return false;
		}
	}

	@Override
	public boolean unsubscribeFromCloudEvents(ResourceNameEnum resource)
	{
		String topic = createTopicName(resource);
		if (this.mqtt != null && this.mqtt.isConnected()) {
			this.mqtt.unsubscribeFromTopic(topic);
			return true;
		} else {
			_Logger.warning("MQTT not connected. Ignoring unsub topic: " + topic);
			return false;
		}
	}
	
	
	// private methods
	
	private String createTopicName(ResourceNameEnum resource)
	{
		return createTopicName(resource.getDeviceName(), resource.getResourceType());
	}

	private String createTopicName(String deviceName, String typeName)
	{
		return prefix + deviceName + "/" + typeName;
	}

	private boolean publishMessageToCloud(ResourceNameEnum resource, String itemName, String payload)
	{
		String topic = createTopicName(resource) + "-" + itemName;

		return publishMessageToCloud(topic, payload);
	}

	private boolean publishMessageToCloud(String topic, String payload)
	{
		try {
			if (!this.mqtt.isConnected()) {
				_Logger.warning("MQTT client is not connected to broker");
				return false;
			}

			_Logger.info("Publishing payload to CSP topic: " + topic);

			// TODO this is where throttling will go (if necessary)

			return this.mqtt.publishMessage(topic, payload.getBytes(), this.qos);
		} catch (Exception e) {
			_Logger.warning("Failed to publish to CSP topic: " + topic);
			return false;
		}
	}
}
