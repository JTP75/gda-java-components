/**
 * 
 * This class is part of the Programming the Internet of Things
 * project, and is available via the MIT License, which can be
 * found in the LICENSE file at the top level of this repository.
 * 
 * Copyright (c) 2020 - 2025 by Andrew D. King
 */ 

package programmingtheiot.integration.connection;

import static org.junit.Assert.*;

import java.util.logging.Logger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import programmingtheiot.common.ConfigConst;
import programmingtheiot.common.ConfigUtil;
import programmingtheiot.common.IDataMessageListener;
import programmingtheiot.common.ResourceNameEnum;
import programmingtheiot.data.*;
import programmingtheiot.gda.connection.*;

/**
 * This test case class contains very basic integration tests for
 * MqttClientControlPacketTest. It should not be considered complete,
 * but serve as a starting point for the student implementing
 * additional functionality within their Programming the IoT
 * environment.
 *
 */
public class MqttClientControlPacketTest
{
	// static
	
	private static final Logger _Logger =
		Logger.getLogger(MqttClientControlPacketTest.class.getName());
	
	
	// member var's
	
	private MqttClientConnector mqttClient = null;

	/**
	 * sleep try/catch wrapper
	 */
	private void sleep(long millis) {
		try {
			Thread.sleep(millis);
		} catch (Exception e) {
			// ignore
		}
	}
	
	
	// test setup methods
	
	@Before
	public void setUp() throws Exception
	{
		this.mqttClient = new MqttClientConnector();
	}
	
	@After
	public void tearDown() throws Exception
	{
	}
	
	// test methods
	
	@Test
	public void testConnectAndDisconnect()
	{
		assertTrue(this.mqttClient.connectClient());
		assertFalse(this.mqttClient.connectClient());
		
		sleep(2000);
		assertTrue(this.mqttClient.isConnected());

		assertTrue(this.mqttClient.disconnectClient());
		assertFalse(this.mqttClient.disconnectClient());
		
		sleep(2000);
		assertFalse(this.mqttClient.isConnected());
	}
	
	@Test
	public void testServerPing()
	{
		this.mqttClient.connectClient();
		sleep(80000);
		this.mqttClient.disconnectClient();
		sleep(2000);
	}
	
	@Test
	public void testPubSubQos1()
	{
		int qos = 1;
		this.mqttClient.connectClient();
		sleep(2000);
		
		this.mqttClient.subscribeToTopic(ResourceNameEnum.CDA_SENSOR_MSG_RESOURCE, qos);
		sleep(2000);

		this.mqttClient.publishMessage(ResourceNameEnum.CDA_SENSOR_MSG_RESOURCE, "ground control to major tom", qos);
		sleep(2000);

		this.mqttClient.unsubscribeFromTopic(ResourceNameEnum.CDA_SENSOR_MSG_RESOURCE);
		sleep(2000);

		this.mqttClient.disconnectClient();
		sleep(2000);
	}
	
	@Test
	public void testPubSubQos2()
	{
		int qos = 2;
		this.mqttClient.connectClient();
		sleep(2000);
		
		this.mqttClient.subscribeToTopic(ResourceNameEnum.CDA_SENSOR_MSG_RESOURCE, qos);
		sleep(2000);

		this.mqttClient.publishMessage(ResourceNameEnum.CDA_SENSOR_MSG_RESOURCE, "major tom to ground control", qos);
		sleep(2000);

		this.mqttClient.unsubscribeFromTopic(ResourceNameEnum.CDA_SENSOR_MSG_RESOURCE);
		sleep(2000);

		this.mqttClient.disconnectClient();
		sleep(2000);
	}
	
}
