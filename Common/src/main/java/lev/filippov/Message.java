package lev.filippov;

import java.io.Serializable;

public abstract class Message implements Serializable {
    protected AuthKey authKey;
    protected static final int SERVICE = 1;
    protected static final int FILE = 2;
    protected int type;

    public AuthKey getAuthKey() {
        return authKey;
    }

    public void setAuthKey(AuthKey authKey) {
        this.authKey = authKey;
    }
}
