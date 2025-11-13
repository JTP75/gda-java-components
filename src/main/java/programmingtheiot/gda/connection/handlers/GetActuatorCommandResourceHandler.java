package programmingtheiot.gda.connection.handlers;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.californium.core.coap.CoAP.ResponseCode;
import org.eclipse.californium.core.server.resources.CoapExchange;

import programmingtheiot.common.IActuatorDataListener;
import programmingtheiot.common.IDataMessageListener;
import programmingtheiot.common.ResourceNameEnum;
import programmingtheiot.data.ActuatorData;
import programmingtheiot.data.DataUtil;
import programmingtheiot.data.SystemPerformanceData;

public class GetActuatorCommandResourceHandler extends GenericCoapResourceHandler 
    implements IActuatorDataListener
{
	// static
	
	private static final Logger _Logger =
		Logger.getLogger(GenericCoapResourceHandler.class.getName());

    // private
    private ActuatorData actuatorData = null;

    // ctors
    /**
     * Default constructor
     */
    public GetActuatorCommandResourceHandler() {
        // uses the UpdateMsg resource
        super(ResourceNameEnum.CDA_ACTUATOR_CMD_RESOURCE.getResourceType());
        this.actuatorData = new ActuatorData();
        super.setObservable(true);
    }

    /**
     * ResourceEnum constructor
     */
    public GetActuatorCommandResourceHandler(ResourceNameEnum name) {
        super(name.getResourceType());
        this.actuatorData = new ActuatorData();
        super.setObservable(true);
    }

    /**
     * String constructor
     */
    public GetActuatorCommandResourceHandler(String name) {
        super(name);
        this.actuatorData = new ActuatorData();
        super.setObservable(true);
    }

    // public

    @Override
    public void handleGET(CoapExchange context) {
        context.accept();
        String jsonData = DataUtil.getInstance()
            .actuatorDataToJson(this.actuatorData);
        context.respond(ResponseCode.CONTENT, jsonData);
    }

    @Override
    public boolean onActuatorDataUpdate(ActuatorData data) {
        if (data!=null && this.actuatorData!=null) {
            this.actuatorData.updateData(data);
            super.changed();
            _Logger.log(
                Level.FINE,
                "Actuator data updated for URI: " + 
                super.getURI() + 
                ": Data value = " + 
                this.actuatorData.getValue()
            );
            return true;
        }
        return false;
    }
}
