package emlogdownloader.common;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import org.yaml.snakeyaml.Yaml;

public class Config {
    private static Config config = null;
    private static String configPath = "/";

    public static final String EMLOGDOWNLOAD_SETTING = "EMLogDownload-Setting";
    public static final String EMLOGDOWNLOAD_SETTING_DOWNLOAD_PATH = "DownloadPath";
    public static final String EMLOGDOWNLOAD_SETTING_LOGIN_HOST = "LoginHost";
    public static final String EMLOGDOWNLOAD_SETTING_API_VERSION = "ApiVersion";
    public static final String EMLOGDOWNLOAD_SETTING_CLIENT_ID = "ClientId";
    public static final String EMLOGDOWNLOAD_SETTING_CLIENT_SECRET = "ClientSecret";
    public static final String EMLOGDOWNLOAD_SETTING_USER_NAME = "UserName";
    public static final String EMLOGDOWNLOAD_SETTING_PASSWORD = "Password";
    public static final String EMLOGDOWNLOAD_SETTING_EVENT_TYPE = "EventType";

    private Map emLogDownloadSettingMap;

    // Constructor for singleton
    private Config() {
    }

    public static void setConfigPath(String configPath) {
        Config.configPath = configPath;
    }

    // Returning an instance of this object
    public static synchronized Config getInstance() throws IOException{
        if (config == null) {
            config = new Config();
            Yaml y = new Yaml();
            Path configPath = Paths.get(Config.configPath);
            if (!Files.exists(configPath)) {
                throw new IllegalArgumentException("Config file not found. filename:" + Config.configPath);
            }
            try (InputStream in = Files.newInputStream(configPath)) {
                Map configMap = (Map)y.load(in);
                config.emLogDownloadSettingMap = (Map)configMap.get(EMLOGDOWNLOAD_SETTING);
            } catch (IOException e) {
                throw e;
            }

            if (config.emLogDownloadSettingMap == null) {
                throw new IllegalArgumentException(getKeyNothingMessage(EMLOGDOWNLOAD_SETTING));
            }
        }

        return config;
    }

    public String getEMLogDownloadSetting(String key) {
        String value = (String)this.emLogDownloadSettingMap.get(key);
        if (value == null) throw new IllegalArgumentException(getKeyNothingMessage(key));
        return value;
    }

    public static String getKeyNothingMessage(String key) {
        return "Configulation key not found. " + "key:" + key;
    }

    public static String getValueErrorMessage(String key, String value) {
        return "Configulation value is invalid. " + "key:" + key + " value:" + value;
    }

}
