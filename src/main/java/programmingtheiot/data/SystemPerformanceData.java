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

import programmingtheiot.common.ConfigConst;

/**
 * Shell representation of class for student implementation.
 *
 */
public class SystemPerformanceData extends BaseIotData
{
	// static
	
	
	// private var's

	private float cpuUtilization = ConfigConst.DEFAULT_VAL;
	private float diskUtilization = ConfigConst.DEFAULT_VAL;
	private float memUtilization = ConfigConst.DEFAULT_VAL;
    
	// constructors
	
	public SystemPerformanceData()
	{
		super();

		super.setName(ConfigConst.SYS_PERF_DATA);
	}
	
	
	// public methods
	
	public float getCpuUtilization()
	{
		return this.cpuUtilization;
	}
	
	public float getDiskUtilization()
	{
		return this.diskUtilization;
	}
	
	public float getMemoryUtilization()
	{
		return this.memUtilization;
	}
	
	public void setCpuUtilization(float val)
	{
		this.cpuUtilization = val;
	}
	
	public void setDiskUtilization(float val)
	{
		this.diskUtilization = val;
	}
	
	public void setMemoryUtilization(float val)
	{
		this.memUtilization = val;
	}
	
	/**
	 * Returns a string representation of this instance. This will invoke the base class
	 * {@link #toString()} method, then append the output from this call.
	 * 
	 * @return String The string representing this instance, returned in CSV 'key=value' format.
	 */
	public String toString()
	{
		StringBuilder sb = new StringBuilder(super.toString());
		
		sb.append(',');
		sb.append(ConfigConst.CPU_UTIL_PROP).append('=').append(this.getCpuUtilization()).append(',');
		sb.append(ConfigConst.DISK_UTIL_PROP).append('=').append(this.getDiskUtilization()).append(',');
		sb.append(ConfigConst.MEM_UTIL_PROP).append('=').append(this.getMemoryUtilization());
		
		return sb.toString();
	}
	
	
	// protected methods
	
	/* (non-Javadoc)
	 * @see programmingtheiot.data.BaseIotData#handleUpdateData(programmingtheiot.data.BaseIotData)
	 */
	protected void handleUpdateData(BaseIotData data)
	{
		if (data instanceof SystemPerformanceData) {
			SystemPerformanceData systemPerformanceData = (SystemPerformanceData)data;
			this.setCpuUtilization(systemPerformanceData.getCpuUtilization());
			this.setDiskUtilization(systemPerformanceData.getDiskUtilization());
			this.setMemoryUtilization(systemPerformanceData.getMemoryUtilization());
		}
	}
	
}
