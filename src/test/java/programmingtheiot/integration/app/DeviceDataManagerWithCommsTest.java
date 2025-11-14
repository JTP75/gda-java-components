/**
 * This class is part of the Programming the Internet of Things
 * project, and is available via the MIT License, which can be
 * found in the LICENSE file at the top level of this repository.
 * 
 * Copyright (c) 2020 - 2025 by Andrew D. King
 */ 

package programmingtheiot.integration.app;

import java.util.logging.Logger;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import programmingtheiot.common.ResourceNameEnum;
import programmingtheiot.data.DataUtil;
import programmingtheiot.data.SensorData;
import programmingtheiot.gda.app.DeviceDataManager;
import programmingtheiot.gda.connection.IPubSubClient;
import programmingtheiot.gda.connection.MqttClientConnector;

/**
 * This test case class contains very basic integration tests for
 * DeviceDataManager. It should not be considered complete,
 * but serve as a starting point for the student implementing
 * additional functionality within their Programming the IoT
 * environment.
 *
 */
public class DeviceDataManagerWithCommsTest
{
	// static
	
	private static final Logger _Logger =
		Logger.getLogger(DeviceDataManagerWithCommsTest.class.getName());
	

	// member var's
	
	
	// test setup methods
	
	/**
	 * @throws java.lang.Exception
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception
	{
	}
	
	/**
	 * @throws java.lang.Exception
	 */
	@AfterClass
	public static void tearDownAfterClass() throws Exception
	{
	}
	
	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception
	{
	}
	
	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception
	{
	}
	
	
	// test methods
	
	/**
	 * Test method for running the DeviceDataManager.
	 */
	@Test
	public void testStartAndStopManager()
	{
		DeviceDataManager devDataMgr = new DeviceDataManager();
		devDataMgr.startManager();
		try { Thread.sleep(1000L); } catch (InterruptedException e) {}
		
		try {
			Thread.sleep(120000L);
		} catch (InterruptedException e) {
			// ignore
		}
		
		devDataMgr.stopManager();
		try { Thread.sleep(1000L); } catch (InterruptedException e) {}
	}
	
	/**
	 * Test method for running the DeviceDataManager with MQTT.
	 * 
	 * @note this test will not work if mqttClient and devDataMgr.mqttClient
	 * 		 have the same clientID
	 */
	@Test
	public void testStartAndStopManagerWithMqtt()
	{
		DeviceDataManager devDataMgr = new DeviceDataManager();
		devDataMgr.startManager();
		try { Thread.sleep(1000L); } catch (InterruptedException e) {}
		
		IPubSubClient mqttClient = new MqttClientConnector();
		mqttClient.connectClient();
		try { Thread.sleep(1000L); } catch (InterruptedException e) {}
		
		SensorData sd = new SensorData();
		sd.setName("Some Sensor");
		sd.setLocationID("constraineddevice001");
		
		String sdJson = DataUtil.getInstance().sensorDataToJson(sd);
		
		// DeviceDataManager should already be subscribed to the following resource
		mqttClient.publishMessage(ResourceNameEnum.CDA_SENSOR_MSG_RESOURCE, sdJson, 1);
		
		try {
			Thread.sleep(10000L);
		} catch (InterruptedException e) {
			// ignore
		}
		
		mqttClient.disconnectClient();
		try { Thread.sleep(1000L); } catch (InterruptedException e) {}

		devDataMgr.stopManager();
		try { Thread.sleep(1000L); } catch (InterruptedException e) {}
	}
	
	/*
	 * NOTE: The following methods can be removed, or you may choose to implement them
	 * within your own DeviceDataManager test class.
	 */
	
	/**
	def testStartAndStopManagerNoComms(self):

	def testStartAndStopManagerWithMqtt(self):

	def testStartAndStopManagerWithCoap(self):

	def testStartAndStopManagerWithMqttAndCoap(self):
	 * 
	 */
	
}
