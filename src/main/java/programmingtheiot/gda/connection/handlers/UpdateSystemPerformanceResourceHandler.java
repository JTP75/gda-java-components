package programmingtheiot.gda.connection.handlers;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.californium.core.coap.CoAP.ResponseCode;
import org.eclipse.californium.core.server.resources.CoapExchange;

import programmingtheiot.common.IDataMessageListener;
import programmingtheiot.common.ResourceNameEnum;
import programmingtheiot.data.DataUtil;
import programmingtheiot.data.SystemPerformanceData;

public class UpdateSystemPerformanceResourceHandler extends GenericCoapResourceHandler {
	// static
	
	private static final Logger _Logger =
		Logger.getLogger(GenericCoapResourceHandler.class.getName());

    // private
    private IDataMessageListener dataMsgListener = null;

    // ctors
    /**
     * Default constructor
     */
    public UpdateSystemPerformanceResourceHandler() {
        // uses the UpdateMsg resource
        super(ResourceNameEnum.CDA_SYSTEM_PERF_MSG_RESOURCE.getResourceType());
    }

    /**
     * ResourceEnum constructor
     */
    public UpdateSystemPerformanceResourceHandler(ResourceNameEnum name) {
        super(name);
    }

    /**
     * String constructor
     */
    public UpdateSystemPerformanceResourceHandler(String name) {
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

            SystemPerformanceData spd = 
                DataUtil.getInstance().jsonToSystemPerformanceData(jsonData);

            this.dataMsgListener.handleSystemPerformanceMessage(
                ResourceNameEnum.CDA_SYSTEM_PERF_MSG_RESOURCE, spd);

            context.respond(
                ResponseCode.CHANGED,
                "Handled Update sys perf data request: " + super.getName()
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
