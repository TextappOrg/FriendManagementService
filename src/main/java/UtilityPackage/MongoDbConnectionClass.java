package UtilityPackage;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.Properties;

public class MongoDbConnectionClass {

    public static MongoCollection getMongoDocUsers()throws NamingException{
        return getMongoDoc("RestServiceAuthMongoDbConnDetails","Collection_Name","Database_Name","Connection_String");
    }

    public static MongoCollection getMongoDocFriendsOfUsers() throws NamingException {
        return getMongoDoc("RestServiceGroupMongoDbConnDetails","Friends_Collection_Name","Friends_Database_Name","Friends_Connection_String");
    }

    public static MongoCollection getMongoDocFriendsOfUsersCached() throws NamingException {
        return getMongoDoc("RestServiceGroupMongoDbConnDetails","Friends_Cached_Collection_Name","Friends_Database_Name","Friends_Connection_String");
    }

    private static MongoCollection getMongoDoc(String propertiesName, String collectionName, String dbName,
                                               String connectionString) throws NamingException {
        InitialContext  context = new InitialContext();
        Properties properties = (Properties) context.lookup(propertiesName);
        String connection_string = properties.getProperty(connectionString);
        String collection_name = properties.getProperty(collectionName);
        String db_name = properties.getProperty(dbName);
        return new MongoClient(new MongoClientURI(connection_string))
                .getDatabase(db_name).
                getCollection(collection_name);
    }
}
