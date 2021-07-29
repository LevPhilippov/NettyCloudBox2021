package lev.filippov;

import java.util.HashMap;
import java.util.Map;

public class ServiceMessage extends Message {

    private Map<String, Object> parametersMap;
    private MessageType messageType;

    public ServiceMessage(AuthKey authKey) {
        this.authKey = authKey;
        parametersMap = new HashMap<>();
    }

    public Map<String, Object> getParametersMap() {
        return parametersMap;
    }

    public void setParametersMap(Map<String, Object> parametersMap) {
        this.parametersMap = parametersMap;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public void setMessageType(MessageType messageType) {
        this.messageType = messageType;
    }
}
