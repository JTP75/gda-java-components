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

package programmingtheiot.gda.system;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;

import java.util.logging.Logger;
import programmingtheiot.common.ConfigConst;
import programmingtheiot.gda.app.GatewayDeviceApp;

/**
 * Shell representation of class for student implementation.
 * 
 */
public class SystemMemUtilTask extends BaseSystemUtilTask
{
	private static final Logger _Logger =
		Logger.getLogger(GatewayDeviceApp.class.getName());

	// constructors
	
	/**
	 * Default.
	 * 
	 */
	public SystemMemUtilTask()
	{
		super(ConfigConst.NOT_SET, ConfigConst.DEFAULT_TYPE_ID);
	}
	
	
	// public methods
	
	@Override
	public float getTelemetryValue()
	{
		MemoryUsage memoryUsage = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();

		_Logger.fine("Mem Used:" + memoryUsage.getUsed() + "; Max:" + memoryUsage.getMax());
		if (memoryUsage.getMax() > 0) {
			return ((float)memoryUsage.getUsed() / (float)memoryUsage.getMax()) * 100.0f;
		} else {
			return 0.0f;
		}
	}
	
}
