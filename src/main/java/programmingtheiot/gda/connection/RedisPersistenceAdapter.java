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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import programmingtheiot.common.ConfigConst;
import programmingtheiot.common.ConfigUtil;
import programmingtheiot.data.ActuatorData;
import programmingtheiot.data.DataUtil;
import programmingtheiot.data.SensorData;
import programmingtheiot.data.SystemPerformanceData;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisConnectionException;

/**
 * Shell representation of class for student implementation.
 * 
 */
public class RedisPersistenceAdapter implements IPersistenceClient
{
	// static
	
	private static final Logger _Logger =
		Logger.getLogger(RedisPersistenceAdapter.class.getName());
	
	// private var's
	private String host;
	private int port;
	private Jedis client = null;
	private boolean connected = false;
	
	// constructors
	
	/**
	 * Default.
	 * 
	 */
	public RedisPersistenceAdapter()
	{
		super();

		this.host = ConfigUtil.getInstance().getProperty(
			ConfigConst.DATA_GATEWAY_SERVICE, 
			ConfigConst.HOST_KEY
		);

		this.port = ConfigUtil.getInstance().getInteger(
			ConfigConst.DATA_GATEWAY_SERVICE, 
			ConfigConst.PORT_KEY
		);

		// create redis cli instance
		this.client = new Jedis(this.host, this.port);
		
		initConfig();
	}
	
	
	// public methods
	
	// public methods

	public boolean isConnected() {
		return this.connected;
	}
	
	/**
	 *
	 */
	@Override
	public boolean connectClient()
	{
		if (connected) {
			_Logger.warning("Redis client is already connected.");
			return true;
		}

		try {
			this.client.connect();
			if (this.client.ping().equals("PONG")) {
				_Logger.info("Redis client connected to " + this.host + ":" + this.port);
				this.connected = true;
				return true;
			} else {
				_Logger.log(Level.SEVERE, "Could not connect to Redis server at " + this.host + ":" + this.port);
				this.connected = false;
				return false;
			}
		} catch (JedisConnectionException e) {
			_Logger.log(Level.SEVERE, "Could not connect to Redis server at " + this.host + ":" + this.port, e);
			this.connected = false;
			return false;
		}
	}

	/**
	 *
	 */
	@Override
	public boolean disconnectClient()
	{
		if (!connected)	{
			_Logger.warning("Redis client is already disconnected.");
			return true;
		}

		try {
			this.client.disconnect();
			this.connected = false;
			_Logger.info("Redis client disconnected from " + this.host + ":" + this.port);
			return true;
		} catch (JedisConnectionException e) {
			_Logger.log(Level.SEVERE, "Could not disconnect from Redis server at " + this.host + ":" + this.port, e);
			this.connected = true;
			return false;
		}
	}

	/**
	 * retrieves all actuator data for the given topic between the given dates (inclusive)
	 * 
	 * @note skipping date filtering for now
	 */
	@Override
	public ActuatorData[] getActuatorData(String topic, Date startDate, Date endDate)
	{
		if (!connected) {
			_Logger.warning("Redis client is not connected.");
			return null;
		}

		if (topic == null || topic.isEmpty()) {
			_Logger.warning("Topic is null or empty.");
			return null;
		}
		
		List<ActuatorData> dataList = new ArrayList<>();

		String rsltJson = this.client.get(topic);
		ActuatorData ad = DataUtil.getInstance().jsonToActuatorData(rsltJson);
		if (ad != null) {
			dataList.add(ad);
		}

		return dataList.toArray(new ActuatorData[0]);
	}

	/**
	 * retrieves all sensor data for the given topic between the given dates (inclusive)
	 */
	@Override
	public SensorData[] getSensorData(String topic, Date startDate, Date endDate)
	{
		if (!connected) {
			_Logger.warning("Redis client is not connected.");
			return null;
		}

		if (topic == null || topic.isEmpty()) {
			_Logger.warning("Topic is null or empty.");
			return null;
		}

		List<SensorData> dataList = new ArrayList<>();

		String rsltJson = this.client.get(topic);
		SensorData sd = DataUtil.getInstance().jsonToSensorData(rsltJson);
		if (sd != null) {
			dataList.add(sd);
		}

		return dataList.toArray(new SensorData[0]);
	}

	/**
	 *
	 */
	@Override
	public void registerDataStorageListener(Class cType, IPersistenceListener listener, String... topics)
	{
	}

	/**
	 *
	 */
	@Override
	public boolean storeData(String topic, int qos, ActuatorData... data)
	{
		if (!connected) {
			_Logger.warning("Redis client is not connected.");
			return false;
		}

		if (topic == null || topic.isEmpty()) {
			_Logger.warning("Topic is null or empty.");
			return false;
		}

		if (data == null || data.length == 0) {
			_Logger.warning("No ActuatorData provided to store.");
			return false;
		}

		try {
			for (ActuatorData ad : data) {
				if (ad != null) {
					String key = topic + ":" + ad.getName() + ":" + System.currentTimeMillis();
					String jsonData = DataUtil.getInstance().actuatorDataToJson(ad);
					this.client.set(key, jsonData);
				}
			}
			return true;
		} catch (JedisConnectionException e) {
			_Logger.log(Level.SEVERE, "Error storing actuator data to Redis server.", e);
			return false;
		}
	}

	/**
	 *
	 */
	@Override
	public boolean storeData(String topic, int qos, SensorData... data)
	{
		if (!connected) {
			_Logger.warning("Redis client is not connected.");
			return false;
		}

		if (topic == null || topic.isEmpty()) {
			_Logger.warning("Topic is null or empty.");
			return false;
		}

		if (data == null || data.length == 0) {
			_Logger.warning("No SensorData provided to store.");
			return false;
		}

		try {
			for (SensorData sd : data) {
				if (sd != null) {
					String key = topic + ":" + sd.getName() + ":" + System.currentTimeMillis();
					String jsonData = DataUtil.getInstance().sensorDataToJson(sd);
					this.client.set(key, jsonData);
				}
			}
			return true;
		} catch (JedisConnectionException e) {
			_Logger.log(Level.SEVERE, "Error storing sensor data to Redis server.", e);
			return false;
		}
	}

	/**
	 *
	 */
	@Override
	public boolean storeData(String topic, int qos, SystemPerformanceData... data)
	{
		if (!connected) {
			_Logger.warning("Redis client is not connected.");
			return false;
		}

		if (topic == null || topic.isEmpty()) {
			_Logger.warning("Topic is null or empty.");
			return false;
		}

		if (data == null || data.length == 0) {
			_Logger.warning("No SystemPerformanceData provided to store.");
			return false;
		}

		try {
			for (SystemPerformanceData spd : data) {
				if (spd != null) {
					String key = topic + ":" + spd.getName() + ":" + System.currentTimeMillis();
					String jsonData = DataUtil.getInstance().systemPerformanceDataToJson(spd);
					this.client.set(key, jsonData);
				}
			}
			return true;
		} catch (JedisConnectionException e) {
			_Logger.log(Level.SEVERE, "Error storing system performance data to Redis server.", e);
			return false;
		}
	}
	
	
	// private methods
	
	/**
	 * 
	 */
	private void initConfig()
	{
	}

}
