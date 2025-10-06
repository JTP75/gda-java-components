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

package programmingtheiot.data;

// import java.nio.file.FileSystems;
// import java.nio.file.Files;
// import java.nio.file.Path;
// import java.util.List;

import com.google.gson.Gson;

import kotlin.NotImplementedError;

/**
 * Shell representation of class for student implementation.
 *
 */
public class DataUtil
{
	// static
	
	private static final DataUtil _Instance = new DataUtil();

	/**
	 * Returns the Singleton instance of this class.
	 * 
	 * @return ConfigUtil
	 */
	public static final DataUtil getInstance()
	{
		return _Instance;
	}
	
	
	// private var's
	private Gson gson = new Gson();
	
	// constructors
	
	/**
	 * Default (private).
	 * 
	 */
	private DataUtil()
	{
		
		super();
	}
	
	
	// public methods
	
	public String actuatorDataToJson(ActuatorData actuatorData)
	{
		return gson.toJson(actuatorData);
	}
	
	public String sensorDataToJson(SensorData sensorData)
	{
		return gson.toJson(sensorData);
	}
	
	public String systemPerformanceDataToJson(SystemPerformanceData sysPerfData)
	{
		return gson.toJson(sysPerfData);
	}
	
	public String systemStateDataToJson(SystemStateData sysStateData)
	{
		throw new NotImplementedError("dont call me pls");
	}
	
	public ActuatorData jsonToActuatorData(String jsonData)
	{
		return gson.fromJson(jsonData, ActuatorData.class);
	}
	
	public SensorData jsonToSensorData(String jsonData)
	{
		return gson.fromJson(jsonData, SensorData.class);
	}
	
	public SystemPerformanceData jsonToSystemPerformanceData(String jsonData)
	{
		return gson.fromJson(jsonData, SystemPerformanceData.class);
	}
	
	public SystemStateData jsonToSystemStateData(String jsonData)
	{
		throw new NotImplementedError("dont call me pls");
	}
	
}
