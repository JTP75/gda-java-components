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

import programmingtheiot.common.DefaultDataMessageListener;
import programmingtheiot.common.IDataMessageListener;
import programmingtheiot.common.ResourceNameEnum;
import programmingtheiot.data.DataUtil;
import programmingtheiot.data.SensorData;
import programmingtheiot.data.SystemPerformanceData;
import programmingtheiot.gda.connection.*;

/**
 * This test case class contains very basic integration tests for
 * CoapClientToServerConnector. It should not be considered complete,
 * but serve as a starting point for the student implementing
 * additional functionality within their Programming the IoT
 * environment.
 *
 */
public class CoapClientHandlersTest
{
	// static
	
	public static final int DEFAULT_TIMEOUT = 5;
	public static final boolean USE_DEFAULT_RESOURCES = true;
	
	private static final Logger _Logger =
		Logger.getLogger(CoapClientToServerConnectorTest.class.getName());
	
	private static CoapServerGateway coapServer = null;
	
	// member var's
	
	private CoapClientConnector coapClient = null;
	private IDataMessageListener dataMsgListener = null;
	
	
	// test setup methods
	
	/**
	 * @throws java.lang.Exception
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception
	{
		coapServer = new CoapServerGateway(new DefaultDataMessageListener());
		
		assertTrue(coapServer.startServer());
	}
	
	/**
	 * @throws java.lang.Exception
	 */
	@AfterClass
	public static void tearDownAfterClass() throws Exception
	{
		assertTrue(coapServer.stopServer());
	}
	
	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception
	{
		this.coapClient = new CoapClientConnector();
		this.dataMsgListener = new DefaultDataMessageListener();
		
		this.coapClient.setDataMessageListener(this.dataMsgListener);
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
	 * java -jar cf-client/target/cf-client-3.10.0.jar --method=PUT coap://localhost:5683/PIOT/ConstrainedDevice/SystemPerfMsg --payload "{"cpuUtilization": 1.0, "diskUtilization": 2.0, "memUtilization": 3.0 }"
	 */
	@Test
    public void testSystemPerformancePutMessage()
    {
        SystemPerformanceData spData = new SystemPerformanceData();
        
        String jsonData = DataUtil.getInstance().systemPerformanceDataToJson(spData);
        
        this.coapClient.sendPutRequest(
            ResourceNameEnum.CDA_SYSTEM_PERF_MSG_RESOURCE, null, USE_DEFAULT_RESOURCES, jsonData, DEFAULT_TIMEOUT);
    }
	
	/**
	 * java -jar cf-client/target/cf-client-3.10.0.jar --method=PUT coap://localhost:5683/PIOT/ConstrainedDevice/SensorfMsg --payload "{"cpuUtilization": 1.0, "diskUtilization": 2.0, "memUtilization": 3.0 }"
	 */
	@Test
    public void testTelemetryPutMessage()
    {
        SensorData sData = new SensorData();
        
        String jsonData = DataUtil.getInstance().sensorDataToJson(sData);
        
        this.coapClient.sendPutRequest(
            ResourceNameEnum.CDA_SENSOR_MSG_RESOURCE, null, USE_DEFAULT_RESOURCES, jsonData, DEFAULT_TIMEOUT);
    }
	
	/**
	 * java -jar cf-client/target/cf-client-3.10.0.jar --method=GET coap://localhost:5683/PIOT/ConstrainedDevice/ActuatorCmd
	 */
	@Test
    public void testActuatorGetMessage()
    {        
        this.coapClient.sendGetRequest(
            ResourceNameEnum.CDA_ACTUATOR_CMD_RESOURCE, null, USE_DEFAULT_RESOURCES, DEFAULT_TIMEOUT);
    }
	
}
