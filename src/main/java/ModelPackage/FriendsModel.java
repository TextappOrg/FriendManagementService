package ModelPackage;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;

@XmlRootElement
public class FriendsModel {
    @XmlElement @JsonProperty private String adderUid;
    @XmlElement @JsonProperty private String friendUid;
    @XmlElement @JsonProperty private LocalDateTime addedDate;

    public FriendsModel() {
        this.friendUid = "";
        this.addedDate = LocalDateTime.now();
    }

    public FriendsModel(String friendUid) {
        this.friendUid = friendUid;
        this.addedDate = LocalDateTime.now();
    }

    public FriendsModel(String adderUid, String friendUid) {
        this.adderUid = adderUid;
        this.friendUid = friendUid;
        this.addedDate = LocalDateTime.now();
    }

    public FriendsModel(String friendUid, LocalDateTime addedDate) {
        this.friendUid = friendUid;
        this.addedDate = addedDate;
    }

    public void setAdderUid(String adderUid) {
        this.adderUid = adderUid;
    }

    public void setFriendUid(String friendUid) {
        this.friendUid = friendUid;
    }

    public void setAddedDate(LocalDateTime addedDate) {
        this.addedDate = addedDate;
    }

    public String getFriendUid() {
        return friendUid;
    }

    public String getAdderUid() {
        return adderUid;
    }

    public LocalDateTime getAddedDate() {
        return addedDate;
    }

    public LinkedHashMap<String,String> mapToCollection(){
        LinkedHashMap<String,String> returnMap = new LinkedHashMap<>();
        returnMap.put("_id",this.adderUid);
        returnMap.put("friendUid",this.friendUid);
        returnMap.put("addedDate", this.addedDate.format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")));
        return returnMap;
    }
}
