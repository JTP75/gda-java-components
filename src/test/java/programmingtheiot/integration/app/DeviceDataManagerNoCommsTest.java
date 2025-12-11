/**
 * This class is part of the Programming the Internet of Things
 * project, and is available via the MIT License, which can be
 * found in the LICENSE file at the top level of this repository.
 * 
 * Copyright (c) 2020 - 2025 by Andrew D. King
 */ 

package programmingtheiot.integration.app;

import static org.junit.Assert.*;

import java.util.logging.Logger;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import programmingtheiot.common.ConfigConst;
import programmingtheiot.common.ResourceNameEnum;
import programmingtheiot.data.SensorData;
import programmingtheiot.gda.app.DeviceDataManager;

/**
 * This test case class contains very basic integration tests for
 * DeviceDataManager. It should not be considered complete,
 * but serve as a starting point for the student implementing
 * additional functionality within their Programming the IoT
 * environment.
 *
 */
public class DeviceDataManagerNoCommsTest
{
	// static
	
	private static final Logger _Logger =
		Logger.getLogger(DeviceDataManagerNoCommsTest.class.getName());
	

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
	// @Test
	public void testStartAndStopManagerNoComms()
	{
		DeviceDataManager devDataMgr = new DeviceDataManager();
		
		devDataMgr.startManager();
		
		try {
			Thread.sleep(60000L);
		} catch (InterruptedException e) {
			// ignore
		}
		
		devDataMgr.stopManager();
	}

	/**
	 * Test for repeat messages in speech data
	 */
	// @Test
	public void testSpeechSensorDataAnalysisRepeats()
	{
		DeviceDataManager devDataMgr = new DeviceDataManager();
		SensorData sd1;
		
		devDataMgr.startManager();
		try { Thread.sleep(2000L); } catch (InterruptedException e) {}
		
		_Logger.info("Handling incomplete result (1st)");
		sd1 = new SensorData();
		sd1.setTypeID(ConfigConst.SPEECH_SENSOR_TYPE);
		sd1.setStateData(makeStateData("this is a partial result", "this is a partial result", false));
		devDataMgr.handleSensorMessage(ResourceNameEnum.CDA_SENSOR_MSG_RESOURCE, sd1);
		try { Thread.sleep(2000L); } catch (InterruptedException e) {}
		
		_Logger.info("Handling incomplete result (2nd)");
		sd1 = new SensorData();
		sd1.setTypeID(ConfigConst.SPEECH_SENSOR_TYPE);
		sd1.setStateData(makeStateData("this is a partial result", "this is a partial result", false));
		devDataMgr.handleSensorMessage(ResourceNameEnum.CDA_SENSOR_MSG_RESOURCE, sd1);
		try { Thread.sleep(2000L); } catch (InterruptedException e) {}
		
		_Logger.info("Handling complete result (1st)");
		sd1 = new SensorData();
		sd1.setTypeID(ConfigConst.SPEECH_SENSOR_TYPE);
		sd1.setStateData(makeStateData("this is a partial result", "this is a final result", true));
		devDataMgr.handleSensorMessage(ResourceNameEnum.CDA_SENSOR_MSG_RESOURCE, sd1);
		try { Thread.sleep(2000L); } catch (InterruptedException e) {}
		
		_Logger.info("Handling complete result (2nd)");
		sd1 = new SensorData();
		sd1.setTypeID(ConfigConst.SPEECH_SENSOR_TYPE);
		sd1.setStateData(makeStateData("this is a partial result", "this is a final result", true));
		devDataMgr.handleSensorMessage(ResourceNameEnum.CDA_SENSOR_MSG_RESOURCE, sd1);
		try { Thread.sleep(2000L); } catch (InterruptedException e) {}
		
		devDataMgr.stopManager();
		try { Thread.sleep(2000L); } catch (InterruptedException e) {}
	}
	
	/**
	 * Test method for running the DeviceDataManager.
	 */
	// @Test
	public void testSpeechSensorDataAnalysis()
	{
		DeviceDataManager devDataMgr = new DeviceDataManager();
		SensorData sd1;
		
		devDataMgr.startManager();
		try { Thread.sleep(2000L); } catch (InterruptedException e) {}

		_Logger.info("Handling empty result");
		sd1 = new SensorData();
		sd1.setTypeID(ConfigConst.SPEECH_SENSOR_TYPE);
		sd1.setStateData(makeStateData("", "", false));
		devDataMgr.handleSensorMessage(ResourceNameEnum.CDA_SENSOR_MSG_RESOURCE, sd1);
		try { Thread.sleep(2000L); } catch (InterruptedException e) {}
		
		_Logger.info("Handling partial result");
		sd1 = new SensorData();
		sd1.setTypeID(ConfigConst.SPEECH_SENSOR_TYPE);
		sd1.setStateData(makeStateData("this is a partial result", "", false));
		devDataMgr.handleSensorMessage(ResourceNameEnum.CDA_SENSOR_MSG_RESOURCE, sd1);
		try { Thread.sleep(2000L); } catch (InterruptedException e) {}
		
		_Logger.info("Handling incomplete result");
		sd1 = new SensorData();
		sd1.setTypeID(ConfigConst.SPEECH_SENSOR_TYPE);
		sd1.setStateData(makeStateData("this is a partial result", "this is a partial result", false));
		devDataMgr.handleSensorMessage(ResourceNameEnum.CDA_SENSOR_MSG_RESOURCE, sd1);
		try { Thread.sleep(2000L); } catch (InterruptedException e) {}
		
		_Logger.info("Handling complete result");
		sd1 = new SensorData();
		sd1.setTypeID(ConfigConst.SPEECH_SENSOR_TYPE);
		sd1.setStateData(makeStateData("this is a partial result", "this is a final result", true));
		devDataMgr.handleSensorMessage(ResourceNameEnum.CDA_SENSOR_MSG_RESOURCE, sd1);
		try { Thread.sleep(2000L); } catch (InterruptedException e) {}
		
		devDataMgr.stopManager();
		try { Thread.sleep(2000L); } catch (InterruptedException e) {}
	}
	
	/**
	 * Test sys prompt
	 */
	@Test
	public void testSysPrompt()
	{
		DeviceDataManager devDataMgr = new DeviceDataManager();
		try { Thread.sleep(1000L); } catch (InterruptedException e) {}

		SensorData td = new SensorData(ConfigConst.TEMP_SENSOR_TYPE);
		td.setValue((float)21.0);
		td.setTypeID(ConfigConst.TEMP_SENSOR_TYPE);
		SensorData hd = new SensorData(ConfigConst.HUMIDITY_SENSOR_TYPE);
		hd.setValue((float)35.0);
		hd.setTypeID(ConfigConst.HUMIDITY_SENSOR_TYPE);
		SensorData pd = new SensorData(ConfigConst.PRESSURE_SENSOR_TYPE);
		pd.setValue((float)990.1);
		pd.setTypeID(ConfigConst.PRESSURE_SENSOR_TYPE);

		devDataMgr.handleSensorMessage(ResourceNameEnum.CDA_SENSOR_MSG_RESOURCE, td);
		devDataMgr.handleSensorMessage(ResourceNameEnum.CDA_SENSOR_MSG_RESOURCE, hd);
		devDataMgr.handleSensorMessage(ResourceNameEnum.CDA_SENSOR_MSG_RESOURCE, pd);

		_Logger.info(devDataMgr.generateEnvironmentPrompt());
	}
	
	/**
	 * Test single message
	 */
	@Test
	public void testSpeechSensorDataAnalysisSingle()
	{
		DeviceDataManager devDataMgr = new DeviceDataManager();
		SensorData sd1;
		// String message = "What is todays message of the day";
		String message = "can you check the documents for information about CUDA? Use only one result please.";
		
		devDataMgr.startManager();
		try { Thread.sleep(2000L); } catch (InterruptedException e) {}
		
		_Logger.info("Handling incomplete result (1st)");
		sd1 = new SensorData();
		sd1.setTypeID(ConfigConst.SPEECH_SENSOR_TYPE);
		sd1.setStateData(makeStateData(message, message, false));
		devDataMgr.handleSensorMessage(ResourceNameEnum.CDA_SENSOR_MSG_RESOURCE, sd1);
		try { Thread.sleep(60000L); } catch (InterruptedException e) {}
		
		devDataMgr.stopManager();
		try { Thread.sleep(2000L); } catch (InterruptedException e) {}
	}

	/**
	 * Test method for running the DeviceDataManager.
	 */
	// @Test
	public void testStartAndStopManagerInjNoComms()
	{
		DeviceDataManager devDataMgr = new DeviceDataManager();
		
		devDataMgr.startManager();

		SensorData sd1, sd2;

		sd1 = new SensorData();
		sd1.setTypeID(ConfigConst.HUMIDITY_SENSOR_TYPE);
		sd1.setValue(60.0f);

		sd2 = new SensorData();
		sd2.setTypeID(ConfigConst.HUMIDITY_SENSOR_TYPE);
		sd2.setValue(45.0f);

		devDataMgr.handleSensorMessage(ResourceNameEnum.CDA_SENSOR_MSG_RESOURCE, sd1);
		try { Thread.sleep(4000L); } catch (InterruptedException e) {}

		sd1 = new SensorData();
		sd1.setTypeID(ConfigConst.HUMIDITY_SENSOR_TYPE);
		sd1.setValue(60.0f);

		sd2 = new SensorData();
		sd2.setTypeID(ConfigConst.HUMIDITY_SENSOR_TYPE);
		sd2.setValue(45.0f);

		devDataMgr.handleSensorMessage(ResourceNameEnum.CDA_SENSOR_MSG_RESOURCE, sd1);
		try { Thread.sleep(4000L); } catch (InterruptedException e) {}

		sd1 = new SensorData();
		sd1.setTypeID(ConfigConst.HUMIDITY_SENSOR_TYPE);
		sd1.setValue(60.0f);

		sd2 = new SensorData();
		sd2.setTypeID(ConfigConst.HUMIDITY_SENSOR_TYPE);
		sd2.setValue(45.0f);

		devDataMgr.handleSensorMessage(ResourceNameEnum.CDA_SENSOR_MSG_RESOURCE, sd1);
		try { Thread.sleep(4000L); } catch (InterruptedException e) {}

		sd1 = new SensorData();
		sd1.setTypeID(ConfigConst.HUMIDITY_SENSOR_TYPE);
		sd1.setValue(60.0f);

		sd2 = new SensorData();
		sd2.setTypeID(ConfigConst.HUMIDITY_SENSOR_TYPE);
		sd2.setValue(45.0f);

		devDataMgr.handleSensorMessage(ResourceNameEnum.CDA_SENSOR_MSG_RESOURCE, sd1);
		try { Thread.sleep(4000L); } catch (InterruptedException e) {}
		
		// try {
		// 	Thread.sleep(60000L);
		// } catch (InterruptedException e) {
		// 	// ignore
		// }
		
		devDataMgr.stopManager();
	}

	
	private String makeStateData(String partial, String result, boolean isComplete) {
		JsonObject stateData = new JsonObject();

		stateData.addProperty("partial", partial);
		stateData.addProperty("result", result);
		stateData.addProperty("isComplete", isComplete);

		Gson gson = new Gson();
		return gson.toJson(stateData);
	}
}
