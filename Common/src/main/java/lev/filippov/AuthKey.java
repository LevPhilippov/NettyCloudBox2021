package lev.filippov;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Objects;
import java.util.UUID;

public class AuthKey implements Serializable {

    private UUID uuid;

    private String login;

    private String password;

    private Timestamp timestamp;

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AuthKey authKey = (AuthKey) o;
        return Objects.equals(uuid, authKey.uuid)  && Objects.equals(timestamp, authKey.timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid, timestamp);
    }
}
