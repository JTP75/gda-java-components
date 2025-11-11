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

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.server.resources.Resource;
import org.eclipse.californium.core.config.CoapConfig;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.core.network.Endpoint;
import org.eclipse.californium.core.network.interceptors.MessageTracer;
import org.eclipse.californium.elements.config.Configuration;
import org.eclipse.californium.elements.config.UdpConfig;

import programmingtheiot.common.ConfigConst;
import programmingtheiot.common.ConfigUtil;
import programmingtheiot.common.IDataMessageListener;
import programmingtheiot.common.ResourceNameEnum;
import programmingtheiot.gda.connection.handlers.GenericCoapResourceHandler;
import programmingtheiot.gda.connection.handlers.UpdateSystemPerformanceResourceHandler;

/**
 * config init for californium
 */


/**
 * Shell representation of class for student implementation.
 * 
 */
public class CoapServerGateway
{
	// static
	static {
		CoapConfig.register();
		UdpConfig.register();
	}
	private static final Logger _Logger =
		Logger.getLogger(CoapServerGateway.class.getName());
	
	// params
	private CoapServer coapServer = null;
	private IDataMessageListener dataMsgListener = null;
	
	// constructors
	/**
	 * Default Constructor.
	 */
	public CoapServerGateway()
	{
		super();
		
		initServer();
	}

	/**
	 * Constructor.
	 * 
	 * @param dataMsgListener
	 */
	public CoapServerGateway(IDataMessageListener dataMsgListener)
	{
		super();
		
		// using option 1 for now
		
		this.dataMsgListener = dataMsgListener;
		
		initServer();
	}
		
	// public methods

	public void addResource(ResourceNameEnum name, String endName, Resource resource)
	{
		if (name!=null && resource!=null) {
			createAndAddResourceChain(name, resource);
		}
	}
	
	public boolean hasResource(String name)
	{
		return false;
	}
	
	public void setDataMessageListener(IDataMessageListener listener)
	{
		if (listener!=null) this.dataMsgListener = listener;
	}
	
	public boolean startServer()
	{
		if (this.coapServer!=null) {
			try {
				this.coapServer.start();
				for (Endpoint ep : this.coapServer.getEndpoints()) {
					ep.addInterceptor(new MessageTracer());
				}

				return true;
			} catch (Exception e) {
				_Logger.log(Level.SEVERE, "Failed to start CoAP server", e);
			}
		} else {
			_Logger.log(Level.WARNING, "CoAP server not initialized");
		}
		return false;
	}
	
	public boolean stopServer()
	{
		if (this.coapServer!=null) {
			try {
				this.coapServer.stop();
				return true;
			} catch (Exception e) {
				_Logger.log(Level.SEVERE, "Failed to stop CoAP server", e);
			}
		} else {
			_Logger.log(Level.WARNING, "CoAP server not initialized");
		}
		return false;
	}
	
	// private methods
	
	private Resource createAndAddResourceChain(ResourceNameEnum name, Resource resource)
	{
		_Logger.info("Adding server resource handler chain: " + name.getResourceName());

		List<String> names = name.getResourceNameChain();
		Queue<String> namesQueue = new ArrayBlockingQueue<>(names.size());

		namesQueue.addAll(names);

		Resource parent = this.coapServer.getRoot();
		if (parent==null) {
			parent = new CoapResource(namesQueue.poll());
			this.coapServer.add(parent);
		}

		while (!namesQueue.isEmpty()) {
			String resourceName = namesQueue.poll();
			Resource next = parent.getChild(resourceName);

			if (next==null) {
				if (namesQueue.isEmpty()) {
					next = resource;
					next.setName(resourceName);
				} else {
					next = new CoapResource(resourceName);
				}
				parent.add(next);
			}

			parent = next;
		}

		return this.coapServer.getRoot();
	}
	
	private void initServer(ResourceNameEnum ...resources)
	{
		int port = ConfigUtil.getInstance().getInteger(
			ConfigConst.COAP_GATEWAY_SERVICE,
			ConfigConst.PORT_KEY,
			ConfigConst.DEFAULT_COAP_PORT
		);

		this.coapServer = new CoapServer(port);

		initDefaultResources();
	}

	private void initDefaultResources()
	{
		// initialize pre-defined resources
		// GetActuatorCommandResourceHandler getActuatorCmdResourceHandler =
		// 	new GetActuatorCommandResourceHandler(
		// 		ResourceNameEnum.CDA_ACTUATOR_CMD_RESOURCE.getResourceType());
		
		// if (this.dataMsgListener != null) {
		// 	this.dataMsgListener.setActuatorDataListener(null, getActuatorCmdResourceHandler);
		// }
		
		// addResource(ResourceNameEnum.CDA_ACTUATOR_CMD_RESOURCE, null, getActuatorCmdResourceHandler);
		
		// UpdateTelemetryResourceHandler updateTelemetryResourceHandler =
		// 	new UpdateTelemetryResourceHandler(
		// 		ResourceNameEnum.CDA_SENSOR_MSG_RESOURCE.getResourceType());
		
		// updateTelemetryResourceHandler.setDataMessageListener(this.dataMsgListener);
		
		// addResource(
		// 	ResourceNameEnum.CDA_SENSOR_MSG_RESOURCE, null,	updateTelemetryResourceHandler);
		
		UpdateSystemPerformanceResourceHandler updateSystemPerformanceResourceHandler =
			new UpdateSystemPerformanceResourceHandler();
		
		updateSystemPerformanceResourceHandler.setDataMessageListener(this.dataMsgListener);
		
		addResource(ResourceNameEnum.CDA_SYSTEM_PERF_MSG_RESOURCE,
			null, updateSystemPerformanceResourceHandler);
	}
}
