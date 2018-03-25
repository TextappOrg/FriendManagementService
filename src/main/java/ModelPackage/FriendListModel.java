package ModelPackage;

import javax.xml.bind.annotation.XmlRootElement;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Date;

@SuppressWarnings(value = "WeakerAccess")
@XmlRootElement
public class FriendListModel implements java.io.Serializable {

    private String uId;
    private String firstName;
    private String middleName;
    private String lastName;
    private Date addedDate;

    public FriendListModel() {
        this.uId = "";
        this.firstName = "";
        this.middleName = "";
        this.lastName = "";
        this.addedDate = Date.from(Instant.now());
    }

    public FriendListModel(String uId, String firstName, String middleName, String lastName, Date addedDate) {
        this.uId = uId;
        this.firstName = firstName;
        this.middleName = middleName;
        this.lastName = lastName;
        this.addedDate = addedDate;
    }


    public String getuId() {
        return uId;
    }


    public String getFirstName() {
        return firstName;
    }


    public String getMiddleName() {
        return middleName;
    }


    public String getLastName() {
        return lastName;
    }


    public Date getAddedDate() {
        return addedDate;
    }

    public void setuId(String uId) {
        this.uId = uId;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public void setMiddleName(String middleName) {
        this.middleName = middleName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public void setAddedDate(Date addedDate) {
        this.addedDate = addedDate;
    }
}
