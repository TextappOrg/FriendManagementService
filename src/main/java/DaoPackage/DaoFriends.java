package DaoPackage;

import ModelPackage.FriendListModel;
import ModelPackage.FriendsModel;
import UtilityPackage.MakeHttpRequest;
import UtilityPackage.MongoDbConnectionClass;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.bson.conversions.Bson;

import javax.naming.NamingException;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static com.mongodb.client.model.Updates.addToSet;

public class DaoFriends implements DaoFriendsInterface {
    private FriendsModel friendsModel;
    private MongoCollection mongoCollection;

    public DaoFriends(String friendUid) {
        this.friendsModel = new FriendsModel(friendUid);
    }

    public DaoFriends(String adderUid, String friendUid){
        this.friendsModel = new FriendsModel(adderUid,friendUid);
    }

    // TODO : public instance methods
    @Override
    public LinkedHashMap<String,String> searchFriend() throws ExecutionException, InterruptedException {

        ExecutorService executorService = Executors.newWorkStealingPool();
        Future<Boolean> isLoggedIn = executorService.submit( () -> isUserLoggedIn(this.friendsModel.getAdderUid()));
        Future<LinkedHashMap<String,String>> returnMap = executorService.submit(this::lookForFriends);

        return isLoggedIn.get() ? returnMap.get() : null;
    }

    @Override
    public Boolean sendFriendRequest() throws ExecutionException, InterruptedException {
        ExecutorService executorService = Executors.newWorkStealingPool();

        Future<Boolean> isUserLoggedIn = executorService
                .submit(() -> !isUserLoggedIn(this.friendsModel.getAdderUid()));
        Future<Boolean> makeFriendRequest = executorService.submit(this::makeFriendRequest);

        return isUserLoggedIn.get() ? makeFriendRequest.get() : false;
    }

    @Override
    @SuppressWarnings(value = "unchecked")
    public HashSet<String> confirmFriendRequest(String status) throws NamingException, ExecutionException, InterruptedException {
        HashSet<String> returnSet = new HashSet<>();
        ExecutorService executorService = Executors.newCachedThreadPool();
        if(!isUserLoggedIn(this.friendsModel.getFriendUid())) returnSet.add("Please login to confirm and refrain from" +
                " hacking");  // If the user isn't  logged in, deal with it
        else {
            final Bson filterFriend = new Document("_id", this.friendsModel.getFriendUid());
            final Bson filterAdder = new Document("_id", this.friendsModel.getAdderUid());    // create filter queries

            MongoCollection mc = MongoDbConnectionClass.getMongoDocFriendsOfUsersCached();

            final Future<Document> resultAdder = executorService.submit( () ->(Document) mc
                    .find(Filters.and(filterAdder, Filters.eq("Friends", this.friendsModel.getFriendUid())))
                    .limit(1)
                    .first());
            final Future<Document> resultFriend = executorService.submit( () -> (Document) mc
                    .find(Filters.and(filterFriend, Filters.eq("Friends", this.friendsModel.getAdderUid())))
                    .limit(1)
                    .first());

            if (!resultAdder.get().isEmpty() && !resultFriend.get().isEmpty()) { // If friend request was indeed sent
                final Bson secondFilterFriend = ((Document) filterFriend).append("Friends", new Document("$exists",
                        true));
                final Bson secondFilterAdder = ((Document) filterAdder).append("Friends", new Document("$exists",
                        true));

                MongoCollection mc2 = MongoDbConnectionClass.getMongoDocFriendsOfUsers();
                final Future<Document> secondResultFriend = executorService.submit(() -> (Document) mc2
                        .find(secondFilterFriend)
                        .limit(1)
                        .first());

                final Future<Document> secondResultAdder = executorService.submit(() -> (Document) mc2
                        .find(secondFilterAdder)
                        .limit(1)
                        .first());

                if (status.equalsIgnoreCase("ACCEPTED")) {
                    if (secondResultFriend.get().isEmpty())
                        // if friend added for the first time
                        mc2.insertOne(new Document("_id", this.friendsModel.getFriendUid()));
                    if(secondResultAdder.get().isEmpty())
                        mc2.insertOne(new Document("_id", this.friendsModel.getAdderUid()));

                    this.friendsModel.setAddedDate(LocalDateTime.now(ZoneId.systemDefault()));

                    Instant instant = this.friendsModel.getAddedDate().atZone(ZoneId.systemDefault()).toInstant();
                    Date addedDate = Date.from(instant);

                    Bson addToFriendList = addToSet("Friends", new Document("uId",this.friendsModel
                            .getAdderUid())
                            .append("addedOn",addedDate));
                    Bson addToAdderList = addToSet("Friends", new Document("uId",this.friendsModel
                            .getFriendUid())
                            .append("addedOn",addedDate));
                    mc2.createIndex(new Document("Friends.uId",1));

                    executorService.execute(() -> mc2.updateOne(new Document("_id",
                                    this.friendsModel.getAdderUid()), addToAdderList));
                    executorService.execute(() -> mc2.updateOne(new Document("_id",
                                    this.friendsModel.getFriendUid()), addToFriendList));

                    returnSet.add("Accepted");
                } else if (status.equalsIgnoreCase("REJECTED")) returnSet.add("Rejected");

                final Bson removeQueryFriend = new Document("$pull", new Document("Friends", this.friendsModel
                        .getAdderUid()));
                final Bson removeQueryAdder = new Document("$pull", new Document("Friends", this.friendsModel
                        .getFriendUid()));

                MongoCollection mc3 = MongoDbConnectionClass.getMongoDocFriendsOfUsersCached();

                executorService.execute(() -> mc3.updateOne(filterFriend, removeQueryFriend));
                executorService.execute(() -> mc3.updateOne(filterAdder, removeQueryAdder));
            } else returnSet.add("Something went wrong. Please try again");
        }
        executorService.shutdown();
        return returnSet;
    }

    @Override
    @SuppressWarnings(value = "unchecked")
    public boolean un_friend() throws NamingException{
        if(isUserLoggedIn(this.friendsModel.getAdderUid())) {
            this.mongoCollection = MongoDbConnectionClass.getMongoDocFriendsOfUsers();
            Bson filter = new Document("_id", this.friendsModel.getAdderUid());
            List<Document> results = (List<Document>) this.mongoCollection.find(filter).into(new ArrayList());
            if (!results.isEmpty()) {
                Bson filterFriend = new Document("_id", this.friendsModel.getFriendUid());
                Bson filterAdder = new Document("_id", this.friendsModel.getAdderUid());

                Bson updateFriendQuery = new Document("Friends", new Document("uId",this.friendsModel.getAdderUid()));
                Bson updateAdderQuery = new Document("Friends", new Document("uId",this.friendsModel.getFriendUid()));

                ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime()
                        .availableProcessors());

                executorService.execute(() -> this.mongoCollection
                        .updateOne(filterAdder,new Document("$pull",updateAdderQuery)));

                executorService.execute(() -> this.mongoCollection
                        .updateOne(filterFriend,new Document("$pull",updateFriendQuery)));

                return true;
            }else return false;
        }else return false;
    }

    @Override
    @SuppressWarnings(value = "unchecked")
    public ArrayList<FriendListModel> populateFriendsList() throws  ExecutionException, InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(2);

        Future<Boolean> isLoggedIn = executorService.submit(() -> isUserLoggedIn(friendsModel.getFriendUid()));
        Future<ArrayList<FriendListModel>> friendsListModel = executorService.submit(this::makeFriendList);

        return isLoggedIn.get() ? friendsListModel.get() : null;
    }

    // TODO : private methods DAO class
    @SuppressWarnings(value = "unchecked")
    private Boolean makeFriendRequest() throws NamingException {
        try {
            MongoCollection mc = MongoDbConnectionClass.getMongoDocFriendsOfUsersCached();
            ExecutorService cachedThreadPool = Executors.newCachedThreadPool();

            cachedThreadPool.execute(() -> {
                Bson filterAdder = new Document("_id", this.friendsModel.getAdderUid())
                        .append("Friends", new Document("$exists", true));
                Document resultAdder = (Document) mc.find(filterAdder).limit(1).first();
                if (resultAdder.isEmpty())
                    mc.insertOne(new Document("_id", this.friendsModel.getAdderUid()));
            });

            cachedThreadPool.execute(() -> {
                Bson filterFriend = new Document("_id", this.friendsModel.getFriendUid())
                        .append("Friends", new Document("$exists", true));
                Document resultFriend = (Document) mc.find(filterFriend).limit(1).first();
                if ((resultFriend).isEmpty())
                    mc.insertOne(new Document("_id", this.friendsModel.getFriendUid()));
            });

            cachedThreadPool.execute(() -> {
                Bson addToAdderList = addToSet("Friends", this.friendsModel.getFriendUid());
                mc.updateOne(new Document("_id", this.friendsModel.getAdderUid()), addToAdderList);
            });

            cachedThreadPool.execute(() -> {
                Bson addToFriendList = addToSet("Friends", this.friendsModel.getAdderUid());
                mc.updateOne(new Document("_id", this.friendsModel.getFriendUid()), addToFriendList);
            });

            cachedThreadPool.execute(this::sendFriendAddNotification);
            return true;
        } catch (com.mongodb.MongoWriteException e) {
            e.printStackTrace(); // TODO : debug
            return false;
        }
    }

    private LinkedHashMap<String,String> lookForFriends() throws NamingException {
        LinkedHashMap<String,String> returnMap = new LinkedHashMap<>();
        MongoCollection mc = MongoDbConnectionClass.getMongoDocUsers();
        Bson filterFriend = new Document("_id", this.friendsModel.getFriendUid());
        Document results = (Document) mc.find(filterFriend).limit(1).first();

        if (!results.isEmpty()) {
            returnMap.put("log","logged in");
            returnMap.put("status", ifExistsInFriendList(this.friendsModel.getAdderUid(),
                    this.friendsModel.getFriendUid()) ? "absent" : "present");

            returnMap.put("_id", results.getString("_id"));
            returnMap.put("firstName", results.getString("firstName"));

            if (results.getString("middleName") != null ||
                    !results.getString("middleName").equalsIgnoreCase(""))
                returnMap.put("middleName", results.getString("middleName"));

            returnMap.put("lastName", results.getString("lastName"));
        }
        return returnMap;
    }

    @SuppressWarnings(value = "unchecked")
    private ArrayList<FriendListModel> makeFriendList()
            throws NamingException, ExecutionException, InterruptedException {
        MongoCollection m = MongoDbConnectionClass.getMongoDocFriendsOfUsers();
        MongoCollection mc = MongoDbConnectionClass.getMongoDocUsers();
        Bson filter = new Document("_id",this.friendsModel.getFriendUid());
        Document results = (Document) m.find(filter).limit(1).first();

        ExecutorService executorService = Executors.newWorkStealingPool();

        if(!results.isEmpty()){
            ArrayList<FriendListModel> friendListModels = new ArrayList<>();
            ArrayList<Future<FriendListModel>> futureArrayList = new ArrayList<>();
            for(Document mainDoc : (List<Document>) results.get("Friends")){
                Future<FriendListModel> future = executorService.submit( () -> {
                    FriendListModel friendListModel = new FriendListModel();
                    String friendId = mainDoc.getString("uId");
                    Date addedDate = mainDoc.getDate("addedOn");

                    friendListModel.setAddedDate(addedDate);
                    friendListModel.setuId(friendId);

                    Bson queryFriend = new Document("_id",friendId);
                    Document doc = (Document) mc.find(queryFriend).limit(1).first();
                    if(!doc.isEmpty()){
                        String firstName = doc.getString("firstName");
                        String lastName = doc.getString("lastName");
                        String middleName = (doc.getString("middleName") == null
                                || doc.getString("middleName").isEmpty())
                                ? "" : doc.getString("middleName");
                        friendListModel.setFirstName(firstName);
                        friendListModel.setMiddleName(middleName);
                        friendListModel.setLastName(lastName);
                    }
                    return friendListModel;
                });
                futureArrayList.add(future);
            }
            for(Future<FriendListModel> future : futureArrayList) friendListModels.add(future.get());
            return friendListModels;
        }
        return null;
    }

    private boolean isUserLoggedIn(String adder) {
        MongoCollection mc;
        try {
            mc = MongoDbConnectionClass.getMongoDocUsers();
            Bson filterIsLoggedIn = new Document("_id",adder).append("serverToken", new Document("$exists",true));
            Document results = (Document) mc.find(filterIsLoggedIn).first();
            return !results.isEmpty();
        } catch (NamingException e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean ifExistsInFriendList(String adder, String friend)throws NamingException {
        MongoCollection mc = MongoDbConnectionClass.getMongoDocFriendsOfUsers();
        Bson filterInAdderList = new Document("_id",adder).append("Friends",new Document("uId",friend));
        Document resultFilterInAdderList = (Document) mc.find(filterInAdderList).limit(1).first();
        if (resultFilterInAdderList != null) return !resultFilterInAdderList.isEmpty();
        return false;
    }

    private void sendFriendAddNotification() {
        String uid = this.friendsModel.getFriendUid();
        String messageTitle = "FRND_REQ";
        String messageBody = this.friendsModel.getAdderUid();
        final Map<String,String> paramMap = new HashMap<>();
        paramMap.put("uId",uid);
        paramMap.put("body",messageBody);
        paramMap.put("title",messageTitle);
        try {
            new MakeHttpRequest(paramMap).makePostRequestToFireBase();
        } catch (IOException e) {
            e.printStackTrace(); // TODO : debug
        }
    }


    @Override
    public FriendsModel getFriendsModel() {
        return friendsModel;
    }

    @Override
    public void setFriendsModel(FriendsModel friendsModel) {
        this.friendsModel = friendsModel;
    }
}