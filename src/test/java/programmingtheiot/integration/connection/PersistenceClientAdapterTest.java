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
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import programmingtheiot.data.ActuatorData;
import programmingtheiot.data.DataUtil;
import programmingtheiot.data.SensorData;
import programmingtheiot.data.SystemPerformanceData;
import programmingtheiot.gda.connection.RedisPersistenceAdapter;
import redis.clients.jedis.Jedis;

/**
 * This test case class contains very basic integration tests for
 * RedisPersistenceAdapter. It should not be considered complete,
 * but serve as a starting point for the student implementing
 * additional functionality within their Programming the IoT
 * environment.
 *
 */
public class PersistenceClientAdapterTest
{
	// static
	
	private static final Logger _Logger =
		Logger.getLogger(PersistenceClientAdapterTest.class.getName());
	
	
	// member var's
	
	private RedisPersistenceAdapter rpa = null;
	
	
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
		this.rpa = new RedisPersistenceAdapter();
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
	 * Test method for {@link programmingtheiot.gda.connection.RedisPersistenceAdapter#connectClient()}.
	 */
	@Test
	public void testConnectClient()
	{
		assertTrue(this.rpa.connectClient());
		assertTrue(this.rpa.isConnected());
	}
	
	/**
	 * Test method for {@link programmingtheiot.gda.connection.RedisPersistenceAdapter#disconnectClient()}.
	 */
	@Test
	public void testDisconnectClient()
	{
		this.rpa.connectClient();
		assertTrue(this.rpa.disconnectClient());
		assertFalse(this.rpa.isConnected());
	}
	
	/**
	 * Test method for {@link programmingtheiot.gda.connection.RedisPersistenceAdapter#getActuatorData(java.lang.String, java.util.Date, java.util.Date)}.
	 */
	@Test
	public void testGetActuatorData()
	{
		// add data to redis db
		ActuatorData ad = new ActuatorData();
		ad.setName("testActuatorData");
		ad.setCommand(1);
		ad.setValue(123.45f);
		Jedis client = new Jedis("localhost", 6379);
		String ads = DataUtil.getInstance().actuatorDataToJson(ad);
		try {
			client.connect();
			assertTrue(client.ping().equals("PONG"));
			assertTrue(client.set("actuatortopic", ads).equals("OK"));
		} catch (Exception e) {
			fail("Could not connect to Redis server at localhost:6379");
		}

		// now retrieve it via rpa
		this.rpa.connectClient();
		ActuatorData[] adArr = this.rpa.getActuatorData("actuatortopic", null, null);
		assertNotNull(adArr);
		assertEquals(1, adArr.length);
		assertEquals("testActuatorData", adArr[0].getName());
		assertEquals(1, adArr[0].getCommand());
		assertEquals(123.45f, adArr[0].getValue(), 0.001f);
		this.rpa.disconnectClient();
		client.close();
	}
	
	/**
	 * Test method for {@link programmingtheiot.gda.connection.RedisPersistenceAdapter#getSensorData(java.lang.String, java.util.Date, java.util.Date)}.
	 */
	@Test
	public void testGetSensorData()
	{
		// add data to redis db
		SensorData sd = new SensorData();
		sd.setName("testSensorData");
		sd.setValue(123.45f);
		Jedis client = new Jedis("localhost", 6379);
		String ads = DataUtil.getInstance().sensorDataToJson(sd);
		try {
			client.connect();
			assertTrue(client.ping().equals("PONG"));
			assertTrue(client.set("sensortopic", ads).equals("OK"));
		} catch (Exception e) {
			fail("Could not connect to Redis server at localhost:6379");
		}

		// now retrieve it via rpa
		this.rpa.connectClient();
		SensorData[] sdArr = this.rpa.getSensorData("sensortopic", null, null);
		assertNotNull(sdArr);
		assertEquals(1, sdArr.length);
		assertEquals("testSensorData", sdArr[0].getName());
		assertEquals(123.45f, sdArr[0].getValue(), 0.001f);
		this.rpa.disconnectClient();
		client.close();
	}
	
	/**
	 * Test method for {@link programmingtheiot.gda.connection.RedisPersistenceAdapter#storeData(java.lang.String, int, programmingtheiot.data.ActuatorData[])}.
	 */
	@Test
	public void testStoreDataStringIntActuatorDataArray()
	{
		this.rpa.connectClient();
		
		ActuatorData ad = new ActuatorData();
		ad.setName("testActuatorData");
		ad.setCommand(1);
		ad.setValue(123.45f);
		
		assertTrue(this.rpa.storeData("actuatortopic", 0, ad));
		
		this.rpa.disconnectClient();
	}
	
	/**
	 * Test method for {@link programmingtheiot.gda.connection.RedisPersistenceAdapter#storeData(java.lang.String, int, programmingtheiot.data.SensorData[])}.
	 */
	@Test
	public void testStoreDataStringIntSensorDataArray()
	{
		this.rpa.connectClient();
		
		SensorData sd = new SensorData();
		sd.setName("testSensorData");
		sd.setValue(123.45f);
		
		assertTrue(this.rpa.storeData("sensortopic", 0, sd));
		
		this.rpa.disconnectClient();
	}
	
	/**
	 * Test method for {@link programmingtheiot.gda.connection.RedisPersistenceAdapter#storeData(java.lang.String, int, programmingtheiot.data.SystemPerformanceData[])}.
	 */
	@Test
	public void testStoreDataStringIntSystemPerformanceDataArray()
	{
		this.rpa.connectClient();
		
		SystemPerformanceData spd = new SystemPerformanceData();
		spd.setCpuUtilization(12.34f);
		spd.setMemoryUtilization(34.56f);
		
		assertTrue(this.rpa.storeData("systemperformancetopic", 0, spd));
		
		this.rpa.disconnectClient();
	}
	
}
