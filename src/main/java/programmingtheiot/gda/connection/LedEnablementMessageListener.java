package programmingtheiot.gda.connection;

import java.util.logging.Logger;

import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import programmingtheiot.common.ConfigConst;
import programmingtheiot.common.IDataMessageListener;
import programmingtheiot.common.ResourceNameEnum;
import programmingtheiot.data.ActuatorData;
import programmingtheiot.data.DataUtil;

public class LedEnablementMessageListener implements IMqttMessageListener
{
	// static
	
	private static final Logger _Logger =
		Logger.getLogger(LedEnablementMessageListener.class.getName());

    // private
	private IDataMessageListener dataMsgListener = null;
	
	private ResourceNameEnum resource = ResourceNameEnum.CDA_ACTUATOR_CMD_RESOURCE;
	
	private int    typeID   = ConfigConst.LED_ACTUATOR_TYPE;
	private String itemName = ConfigConst.LED_ACTUATOR_NAME;
	
	public LedEnablementMessageListener(IDataMessageListener dataMsgListener)
	{
		this.dataMsgListener = dataMsgListener;
	}
	
	public ResourceNameEnum getResource()
	{
		return this.resource;
	}
	
	@Override
	public void messageArrived(String topic, MqttMessage message) throws Exception
	{
        try {
            String json = new String(message.getPayload());
            ActuatorData data = DataUtil.getInstance().jsonToActuatorData(json);

            data.setLocationID(ConfigConst.CONSTRAINED_DEVICE);
            data.setTypeID(this.typeID);
            data.setName(this.itemName);

            int value = (int)data.getValue();

            switch (value) {
            case ConfigConst.ON_COMMAND:
                _Logger.info("Received LED actuator command: [ON]");
                data.setStateData("LED Switching ON");
                break;
            case ConfigConst.OFF_COMMAND:
                _Logger.info("Received LED actuator command: [OFF]");
                data.setStateData("LED Switching OFF");
                break;
            default:
                return;
            }

            if (this.dataMsgListener != null) {
				this.dataMsgListener.handleActuatorCommandRequest(
                    ResourceNameEnum.CDA_ACTUATOR_CMD_RESOURCE, data);
			}

        } catch (Exception e) {
            _Logger.warning("Failed to convert message payload to actuator data");
        }
	}
}
