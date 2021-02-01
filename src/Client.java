import java.io.Serializable;

public class Client implements Serializable {
    private String nickname;
    private final String personalKey;
    private boolean status;

    public Client(String nickname, String personalKey) {
        this.nickname = nickname;
        this.personalKey = personalKey;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getPersonalKey() {
        return personalKey;
    }

    public boolean getStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }


}
