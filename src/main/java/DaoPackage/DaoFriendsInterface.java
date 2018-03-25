package DaoPackage;

import ModelPackage.FriendListModel;
import ModelPackage.FriendsModel;
import UtilityPackage.ManageUiDChange;

import javax.naming.NamingException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.concurrent.ExecutionException;

public interface DaoFriendsInterface {
    static void changeUUID(String oldUID, String newUID) throws NamingException {
        new ManageUiDChange(oldUID,newUID).changeID();
    }

    @SuppressWarnings(value = "unchecked")
    LinkedHashMap<String,String> searchFriend() throws NamingException, ExecutionException, InterruptedException;

    @SuppressWarnings(value = "unchecked")
    Boolean sendFriendRequest() throws NamingException, ExecutionException, InterruptedException;

    @SuppressWarnings(value = "unchecked")
    HashSet<String> confirmFriendRequest(String status) throws NamingException, ExecutionException, InterruptedException;

    @SuppressWarnings(value = "unchecked")
    boolean un_friend() throws NamingException;

    @SuppressWarnings(value = "unchecked")
    ArrayList<FriendListModel> populateFriendsList() throws  ExecutionException, InterruptedException;

    FriendsModel getFriendsModel();

    void setFriendsModel(FriendsModel friendsModel);
}
