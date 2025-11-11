package programmingtheiot.gda.connection.handlers;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.californium.core.coap.CoAP.ResponseCode;
import org.eclipse.californium.core.server.resources.CoapExchange;

import programmingtheiot.common.IDataMessageListener;
import programmingtheiot.common.ResourceNameEnum;
import programmingtheiot.data.DataUtil;
import programmingtheiot.data.SensorData;
import programmingtheiot.data.SystemPerformanceData;

public class UpdateTelemetryResourceHandler extends GenericCoapResourceHandler {
	// static
	
	private static final Logger _Logger =
		Logger.getLogger(GenericCoapResourceHandler.class.getName());

    // private
    private IDataMessageListener dataMsgListener = null;

    // ctors
    /**
     * Default constructor
     */
    public UpdateTelemetryResourceHandler() {
        // uses the UpdateMsg resource
        super(ResourceNameEnum.CDA_SENSOR_MSG_RESOURCE.getResourceType());
    }

    /**
     * ResourceEnum constructor
     */
    public UpdateTelemetryResourceHandler(ResourceNameEnum name) {
        super(name);
    }

    /**
     * String constructor
     */
    public UpdateTelemetryResourceHandler(String name) {
        super(name);
    }

    // public
	
    /**
     * handle a PUT request for sys perf data
     */
	@Override
	public void handlePUT(CoapExchange context) {
        context.accept();
        
        if (this.dataMsgListener==null) {
            _Logger.log(Level.INFO, "Received PUT request (no callback). Ignoring");
            context.respond(ResponseCode.CONTINUE);
            return;
        }

        try {
            String jsonData = new String(context.getRequestPayload());

            SensorData sd = 
                DataUtil.getInstance().jsonToSensorData(jsonData);

            this.dataMsgListener.handleSensorMessage(
                ResourceNameEnum.CDA_SYSTEM_PERF_MSG_RESOURCE, sd);

            context.respond(
                ResponseCode.CHANGED,
                "Handled Update sensor data request: " + super.getName()
            );
        } catch (Exception e) {
            _Logger.log(Level.WARNING, "Failed to handle PUT request", e);
            context.respond(ResponseCode.BAD_REQUEST);
        }
	}

    @Override
    public void setDataMessageListener(IDataMessageListener dml) {
        if (dml!=null) this.dataMsgListener = dml;
    }
}
