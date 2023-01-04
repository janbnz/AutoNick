package de.jan.autonick.database.databases;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import de.jan.autonick.database.Database;
import org.bson.Document;
import org.bukkit.entity.Player;

import java.util.UUID;

public class MongoDb extends Database {

    private final String connectionUrl, databaseName, collectionName;

    private MongoClient client;
    private MongoCollection<Document> collection;

    public MongoDb(String host, String username, String password, String connectionUrl, String databaseName, String collectionName) {
        this.databaseName = databaseName;
        this.collectionName = collectionName;
        this.connectionUrl = connectionUrl.isEmpty() ? "mongodb://" + username + ":" + password + "@" + host + ":27017" + "/" + databaseName : connectionUrl;
    }

    @Override
    public void connect() {
        this.client = MongoClients.create(connectionUrl);
        final MongoDatabase database = this.client.getDatabase(databaseName);
        this.collection = database.getCollection(collectionName);
    }

    @Override
    public void disconnect() {
        this.client.close();
    }

    @Override
    public void createPlayer(Player player) {
        final Document document = new Document("UUID", player.getUniqueId().toString()).append("Activated", 0).append("NickName", player.getName());
        this.collection.insertOne(document);
    }

    @Override
    public void deletePlayer(UUID uuid) {
        this.collection.deleteOne(Filters.eq("UUID", uuid.toString()));
    }

    @Override
    public void setNickname(UUID uuid, String nickname) {
        final Document document = collection.find(Filters.eq("UUID", uuid.toString())).first();
        if (document == null) return;
        document.put("NickName", nickname);
        collection.findOneAndReplace(Filters.eq("UUID", uuid.toString()), document);
    }

    @Override
    public boolean hasNickActivated(UUID uuid) {
        if (!this.isExisting(uuid)) return false;

        final Document document = collection.find(Filters.eq("UUID", uuid.toString())).first();
        if (document == null) return false;
        return document.getInteger("Activated") == 1;
    }

    @Override
    public boolean isExisting(UUID uuid) {
        return collection.find(Filters.eq("UUID", uuid.toString())).first() != null;
    }

    @Override
    public void setNickState(UUID uuid, int state) {
        final Document document = collection.find(Filters.eq("UUID", uuid.toString())).first();
        if (document == null) return;
        document.put("Activated", state);
        collection.findOneAndReplace(Filters.eq("UUID", uuid.toString()), document);
    }

    @Override
    public boolean isConnected() {
        return this.client == null;
    }
}