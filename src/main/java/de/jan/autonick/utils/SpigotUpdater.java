package de.jan.autonick.utils;

import de.jan.autonick.AutoNick;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

public class SpigotUpdater {

    private URL checkURL;
    private String newVersion;
    private final int projectId = 27441;

    public SpigotUpdater() {
        this.newVersion = AutoNick.getInstance().getDescription().getVersion();
        try {
            this.checkURL = new URL("https://api.spigotmc.org/legacy/update.php?resource=" + this.projectId);
        } catch (MalformedURLException ex) {
            ex.printStackTrace();
        }
    }

    public int getProjectId() {
        return projectId;
    }

    public String getLatestVersion() {
        return newVersion;
    }

    public String getResourceURL() {
        return "https://www.spigotmc.org/resources/" + this.projectId;
    }

    public boolean isUpdateAvailable() {
        try {
            URLConnection con = checkURL.openConnection();
            this.newVersion = new BufferedReader(new InputStreamReader(con.getInputStream())).readLine();
            return !AutoNick.getInstance().getDescription().getVersion().equals(this.newVersion);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return false;
    }

}