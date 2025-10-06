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

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import programmingtheiot.common.ConfigConst;
import programmingtheiot.common.ConfigUtil;
import programmingtheiot.common.IDataMessageListener;
import programmingtheiot.common.ResourceNameEnum;
import programmingtheiot.data.SystemPerformanceData;

import java.util.logging.Logger;

/**
 * Shell representation of class for student implementation.
 * 
 */
public class SystemPerformanceManager
{
	// private var's
	private static final Logger _Logger = Logger.getLogger(SystemPerformanceManager.class.getName());
	private int pollRate = ConfigConst.DEFAULT_POLL_CYCLES;
	private ScheduledExecutorService scheduledExecutorService = null;
	private SystemCpuUtilTask cpuUtilTask = null;
	private SystemMemUtilTask memUtilTask = null;
	private Runnable taskRunner = null;
	private boolean isStarted = false;
	private String locationID = ConfigConst.NOT_SET;
	private IDataMessageListener dataMessageListener = null;

	// constructors
	
	/**
	 * Default.
	 * 
	 */
	public SystemPerformanceManager()
	{
		this.pollRate = ConfigUtil.getInstance().getInteger(
			ConfigConst.GATEWAY_DEVICE,
			ConfigConst.POLL_CYCLES_KEY,
			ConfigConst.DEFAULT_POLL_CYCLES
		);
		if (this.pollRate <= 0) {
			this.pollRate = ConfigConst.DEFAULT_POLL_CYCLES;
		}

		this.locationID = ConfigUtil.getInstance().getProperty(
			ConfigConst.GATEWAY_DEVICE, 
			ConfigConst.LOCATION_ID_PROP, 
			ConfigConst.NOT_SET
		);

		this.scheduledExecutorService = Executors.newScheduledThreadPool(1);
		this.cpuUtilTask = new SystemCpuUtilTask();
		this.memUtilTask = new SystemMemUtilTask();

		this.taskRunner = () -> {
			handleTelemetry();
		};
	}
	
	
	// public methods
	
	public void handleTelemetry()
	{
		float cpuUtil = this.cpuUtilTask.getTelemetryValue();
		float memUtil = this.memUtilTask.getTelemetryValue();

		_Logger.info("CPU Util: " + cpuUtil + "; Mem Util: " + memUtil);

		SystemPerformanceData sysPerfData = new SystemPerformanceData();
		sysPerfData.setLocationID(this.locationID);
		sysPerfData.setCpuUtilization(cpuUtil);
		sysPerfData.setMemoryUtilization(memUtil);

		if (this.dataMessageListener != null) {
			this.dataMessageListener.handleSystemPerformanceMessage(ResourceNameEnum.GDA_SYSTEM_PERF_MSG_RESOURCE, sysPerfData);
		}
	}
	
	public void setDataMessageListener(IDataMessageListener listener)
	{
		if (listener != null) { this.dataMessageListener = listener; }
	}
	
	public void startManager()
	{
		if (!this.isStarted) {
			_Logger.info("SystemPerformanceManager is starting...");

			@SuppressWarnings("unused")
			ScheduledFuture<?> futureTask = this.scheduledExecutorService.scheduleAtFixedRate(
				this.taskRunner, 
				1L, 
				this.pollRate, 
				TimeUnit.SECONDS
			);

			this.isStarted = true;
		} else {
			_Logger.warning("SystemPerformanceManager is already started.");
		}

	}
	
	public void stopManager()
	{
		_Logger.info("SystemPerformanceManager is stopping...");

		this.scheduledExecutorService.shutdown();
		this.isStarted = false;
	}
	
}
