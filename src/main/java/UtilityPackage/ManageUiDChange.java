package UtilityPackage;

import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.bson.conversions.Bson;

import javax.naming.NamingException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ManageUiDChange {
    private final String oldID;
    private final String newID;

    public ManageUiDChange(String oldID, String newID) {
        this.oldID = oldID;
        this.newID = newID;
    }

    @SuppressWarnings(value = "unchecked")
    public void changeID()  {
        final Bson filter = new Document("_id",this.oldID);

        ExecutorService executorService = Executors.newFixedThreadPool(2);
        executorService.execute(() -> changeInMainList(filter));
        executorService.execute(() -> changeInCachedList(filter));

    }

    @SuppressWarnings(value = "unchecked")
    private void changeInMainList(Bson filter) {
         try {
             MongoCollection mongoCollectionId = MongoDbConnectionClass.getMongoDocFriendsOfUsers();
             final List<Document> resultFromFriendsList = (List<Document>) mongoCollectionId
                     .find(filter)
                     .limit(1)
                     .into(new ArrayList());
             ExecutorService executorService2 = Executors.newCachedThreadPool();
             Bson newUID = new Document("_id",this.newID).append("Friends",resultFromFriendsList.get(0).get
                     ("Friends"));
             mongoCollectionId.insertOne(newUID);
             mongoCollectionId.deleteOne(filter);

             for(Document doc : (List<Document>) resultFromFriendsList.get(0).get("Friends")){
                 String friendUID = doc.getString("uId");

                 Bson filterFriend = new Document("_id",friendUID).append("Friends.uId",this.oldID);
                 Bson filterModifyUID = new Document("$set",new Document("Friends.$.uId",this.newID));
                 executorService2.execute(() -> {
                     mongoCollectionId.updateMany(filterFriend,filterModifyUID);
                 });
             }
             executorService2.shutdown();
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings(value = "unchecked")
    private void changeInCachedList(Bson filter)  {
        try {
            MongoCollection mongoCollectionCachedFriendsList = MongoDbConnectionClass
                    .getMongoDocFriendsOfUsersCached();

            final List<Document> cachedFriendsResult = (List<Document>) mongoCollectionCachedFriendsList
                .find(filter)
                .limit(1)
                .into(new ArrayList());

            Bson newUID = new Document("_id",this.newID).append("Friends",cachedFriendsResult.get(0).get("Friends"));
            mongoCollectionCachedFriendsList.insertOne(newUID);
            mongoCollectionCachedFriendsList.deleteOne(filter);
            Document extractedDocument = cachedFriendsResult.get(0);
            ArrayList<String> friendList = (ArrayList<String>) extractedDocument.get("Friends");

            ExecutorService ex = Executors.newCachedThreadPool();
            for(final String FriendID : friendList){
                final Bson filterFriendDoc = new Document("_id", FriendID).append("Friends",this.oldID);
                final Bson updatedIDDoc = new Document("Friends",this.newID);
                ex.execute(() -> mongoCollectionCachedFriendsList.findOneAndUpdate(filterFriendDoc,updatedIDDoc));
            }
            ex.shutdown();
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }
}