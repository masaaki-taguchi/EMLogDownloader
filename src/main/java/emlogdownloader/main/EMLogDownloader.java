package emlogdownloader.main;
import java.io.BufferedReader;
import java.io.Console;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import emlogdownloader.common.Config;

public class EMLogDownloader {
    private static Logger logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

    private static final String DEFAULT_CONFIG_FILE_NAME = "EMLogDownloader.yaml";

    private static String baseUri;
    private static String accessToken;
    private static String instanceUri;
    private static Header oAuthHeader;


    public static void main(String[] args) throws Exception {
        try {
            String configPath
                = getApplicationPath(EMLogDownloader.class).getParent() + "/" + DEFAULT_CONFIG_FILE_NAME;
            String targetDate = null;
            Boolean isHourly = false;
            String targetHour = null;

            for (int i = 0; i < args.length; ++i) {
                if ("-config".equals(args[i])) {
                    configPath = args[++i];
                } else if ("-date".equals(args[i])) {
                    targetDate = args[++i];
                    if (!validTargetDateFormat(targetDate)) {
                        usage();
                    }
                } else if ("-hourlylog".equals(args[i])) {
                    isHourly = true;
                } else if ("-hour".equals(args[i])) {
                    targetHour = args[++i];
                    if (!validTargetHourFormat(targetHour)) {
                        usage();
                    }
                } else {
                    usage();
                }
            }
            Config.setConfigPath(configPath);

            EMLogDownloader instance = new EMLogDownloader();

            String loginHost
                = Config.getInstance().getEMLogDownloadSetting(Config.EMLOGDOWNLOAD_SETTING_LOGIN_HOST);
            String clientId
                = Config.getInstance().getEMLogDownloadSetting(Config.EMLOGDOWNLOAD_SETTING_CLIENT_ID);
            String clientSecret
                = Config.getInstance().getEMLogDownloadSetting(Config.EMLOGDOWNLOAD_SETTING_CLIENT_SECRET);
            String userName
                = Config.getInstance().getEMLogDownloadSetting(Config.EMLOGDOWNLOAD_SETTING_USER_NAME);
            String password
                = Config.getInstance().getEMLogDownloadSetting(Config.EMLOGDOWNLOAD_SETTING_PASSWORD);

            logger.info("[EventMonitoring Log Downloader:Start]");

            if (loginHost == null || loginHost.length() == 0) {
                loginHost = getConsoleInput("  HostName: ");
            }
            if (clientId == null || clientId.length() == 0) {
                clientId = getConsoleInput("  ClientId: ");
            }
            if (clientSecret == null || clientSecret.length() == 0) {
                clientSecret = getConsoleInput("  ClientSecret: ");
            }
            if (userName == null || userName.length() == 0) {
                userName = getConsoleInput("  UserName: ");
            }
            if (password == null || password.length() == 0) {
                password = getConsolePasswordInput("  Password: ");
            }
            if (targetDate == null || targetDate.length() == 0) {
                targetDate = getConsoleInput("  TargetDate(yyyy-MM-dd): ");
                if (!validTargetDateFormat(targetDate)) {
                    throw new IllegalArgumentException("Invalid target date. targetDate:" + targetDate);
                }
            }
            if (isHourly && (targetHour == null || targetHour.length() == 0)) {
                targetHour = getConsoleInput("  TargetHour(hh): ");
                if (!validTargetHourFormat(targetHour)) {
                    throw new IllegalArgumentException("Invalid target hour. targetHour:" + targetHour);
                }
            }


            logger.info("");
            instance.getOAuthSession(loginHost, userName, password, clientId, clientSecret, isHourly, targetDate, targetHour);

            logger.info("");
            instance.downloadEMLog(isHourly, targetDate, targetHour);

            logger.info("");
            logger.info("[EventMonitoring Log Downloader:Finish]");

        } catch (Throwable th) {
            if (logger != null) {
                logger.error(th.getMessage(), th);
            } else {
                th.printStackTrace();
            }
            System.exit(1);
        }

        System.exit(0);
    }

    private static void usage() {
        System.out.println("usage: EMLogDownloader [-options]");
        System.out.println("    -config <pathname>         Specifies config file path. (Default is ./EMLogDownloader.yaml)");
        System.out.println("    -date <targetdate>         Specifies download EMLog date. (Format is \"yyyy-MM-dd\")");
        System.out.println("    -hourlylog                 Get hourly event log files.");
        System.out.println("    -hour <targethour>         Specifies download EMLog hour. (Format is \"hh\")");
        System.exit(1);
    }

    private static Boolean validTargetDateFormat(String targetDate) {
        Pattern p = Pattern.compile("[0-9]{4}-[0-9]{2}-[0-9]{2}");
        Matcher m = p.matcher(targetDate);
        return m.find();
    }

    private static Boolean validTargetHourFormat(String targetHour) {
        Pattern p = Pattern.compile("[0-9]{2}");
        Matcher m = p.matcher(targetHour);
        return m.find();
    }

    private static String getConsoleInput(String prompt) throws IOException {
        System.out.print(prompt);
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        String line = br.readLine();
        return line;
    }

    private static String getConsolePasswordInput(String prompt) throws IOException {
        Console console = System.console();
        char[] password = console.readPassword(prompt);
        String passwordStr = new String(password);
        return passwordStr;
    }


    public static Path getApplicationPath(Class<?> cls) throws URISyntaxException {
        ProtectionDomain pd = cls.getProtectionDomain();
        CodeSource cs = pd.getCodeSource();
        URL location = cs.getLocation();
        URI uri = location.toURI();
        Path path = Paths.get(uri);
        return path;
    }

    public void getOAuthSession(
            String loginHost, String userName, String password, String clientId, String clientSecret, Boolean isHourly, String targetDate, String targetHour)
            throws HttpException, IOException {

        String loginURL
            = loginHost + "/services/oauth2/token?grant_type=password" +
              "&client_id=" + clientId +
              "&client_secret=" + clientSecret +
              "&username=" + userName +
              "&password=" + password;

        CloseableHttpClient httpClient = HttpClientBuilder.create().build();
        HttpPost httpPost = new HttpPost(loginURL);
        CloseableHttpResponse response = null;

        try {
            response = httpClient.execute(httpPost);
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != HttpStatus.SC_OK) {
                String errorResult = EntityUtils.toString(response.getEntity());
                throw new IllegalStateException(
                        "HTTP Error occured. HTTP Status:" + statusCode + " Error response:" + errorResult);
            }

            String result = EntityUtils.toString(response.getEntity());
            JSONObject jsonObject = (JSONObject) new JSONTokener(result).nextValue();
            String apiVersion = Config.getInstance().getEMLogDownloadSetting(Config.EMLOGDOWNLOAD_SETTING_API_VERSION);

            accessToken = jsonObject.getString("access_token");
            instanceUri = jsonObject.getString("instance_url");
            baseUri = instanceUri + "/services/data" + "/v" + apiVersion;
            oAuthHeader = new BasicHeader("Authorization", "OAuth " + accessToken);

            String downloadBasePath = Config.getInstance()
                    .getEMLogDownloadSetting(Config.EMLOGDOWNLOAD_SETTING_DOWNLOAD_PATH);
            String eventType = Config.getInstance().getEMLogDownloadSetting(Config.EMLOGDOWNLOAD_SETTING_EVENT_TYPE);
            eventType.trim();
            if (eventType == null || eventType.length() == 0) {
                eventType = "(ALL)";
            }

            logger.info("  [Parameter]");
            logger.info("    downloadPath: " + downloadBasePath);
            logger.info("    loginHost: " + loginHost);
            logger.info("    userName: " + userName);
            logger.info("    apiVersion: " + apiVersion);
            logger.info("    targetDate: " + targetDate);
            if (isHourly) {
                logger.info("    targetHour: " + targetHour);
            }
            logger.info("    eventType: " + eventType);
            logger.info("    accessToken: " + accessToken);
            //        logger.info("    instanceURL: " + instanceUri);
            //        logger.info("    baseUri: " + baseUri);
            //        logger.info("    oAuthHeader: " + oAuthHeader);

        } finally {
            if (response != null) {
                response.close();
            }
            if (httpClient != null) {
                httpClient.close();
            }
        }
    }

    public void downloadEMLog(Boolean isHourly, String targetDate, String targetHour) throws HttpException, IOException {

        String downloadBasePath
            = Config.getInstance().getEMLogDownloadSetting(Config.EMLOGDOWNLOAD_SETTING_DOWNLOAD_PATH);
        String eventTypeCondition
            = Config.getInstance().getEMLogDownloadSetting(Config.EMLOGDOWNLOAD_SETTING_EVENT_TYPE);
        eventTypeCondition.trim();

        String downloadPath = downloadBasePath + "/" + targetDate;
        File downloadPathFile = new File(downloadPath);
        if (!downloadPathFile.exists()) {
            if (!downloadPathFile.mkdir()) {
                throw new IllegalStateException("Failed create directory. path : " + downloadPath);
            }
        }

        String uri = null;
        if (!isHourly) {
            uri = baseUri +
                  "/query?q=SELECT+Id+,+EventType+,+LogFile+,+LogDate+,+LogFileLength+FROM+EventLogFile+" +
                  "Where+Interval='Daily'+and+LogDate+=+" + targetDate + "T00:00:00.000Z";
        } else {
            uri = baseUri +
                  "/query?q=SELECT+Id+,+EventType+,+LogFile+,+LogDate+,+LogFileLength+FROM+EventLogFile+" +
                  "Where+Interval='Hourly'+and+Sequence!=0+and+LogDate+=+" + targetDate + "T" + targetHour + ":00:00.000Z";
        }

        if (eventTypeCondition != null && eventTypeCondition.length() > 0) {
            String[] eventTypes = eventTypeCondition.split(",");
            StringBuffer sb = new StringBuffer();
            sb.append("(");
            for (int i = 0 ; i < eventTypes.length ; i++) {
                sb.append("'");
                sb.append(eventTypes[i].trim());
                sb.append("',");
            }
            sb.deleteCharAt(sb.length() - 1);
            sb.append(")");
            uri = uri + "+and+EventType+in+" + sb.toString();
        }

        HttpGet httpGetEMLogList = new HttpGet(uri);
        httpGetEMLogList.addHeader(oAuthHeader);

        CloseableHttpClient httpClient = HttpClientBuilder.create().build();
        CloseableHttpResponse response = null;
        String result = null;
        try {
            response = httpClient.execute(httpGetEMLogList);

            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != HttpStatus.SC_OK) {
                String errorResult = EntityUtils.toString(response.getEntity());
                throw new IllegalStateException("HTTP Error occured. HTTP Status:" + statusCode + " Error response:" + errorResult);
            }
            result = EntityUtils.toString(response.getEntity());
        } finally {
            if (response != null) {
                response.close();
            }
            if (httpClient != null) {
                httpClient.close();
            }
        }

        logger.info("  [Download progress]");
        JSONObject json = new JSONObject(result);
        JSONArray jsonRecords = json.getJSONArray("records");
        for (int i = 0; i < jsonRecords.length(); i++) {
            String id = jsonRecords.getJSONObject(i).getString("Id");
            String eventType = jsonRecords.getJSONObject(i).getString("EventType");
            String logFile = jsonRecords.getJSONObject(i).getString("LogFile");
            String logDate = jsonRecords.getJSONObject(i).getString("LogDate");

            String logDateOnlyDate = null;
            if (logDate != null && !isHourly) {
                logDateOnlyDate = logDate.substring(0, 10);
            }
            if (logDate != null && isHourly) {
                logDateOnlyDate = logDate.substring(0, 13);
            }
            Integer logFileLength = jsonRecords.getJSONObject(i).getInt("LogFileLength");

            String eventLoguri = instanceUri + logFile;

            httpClient = HttpClientBuilder.create().build();
            HttpGet httpGetEMLogBody = new HttpGet(eventLoguri);
            httpGetEMLogBody.addHeader(oAuthHeader);
            String filePath = downloadPath + "/EventLog_" + eventType + "_" + logDateOnlyDate + ".csv";
            String fileName = new File(filePath).getName();
            String progress = "(" + (i + 1) + "/" + jsonRecords.length() + ")";

            try {
                response = httpClient.execute(httpGetEMLogBody);
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    Files.write(Paths.get(filePath), EntityUtils.toByteArray(entity));
                }
                logger.info("    " + progress + " EventType:" + eventType + " Length:" + logFileLength + " DownloadFile:" + fileName);
            } finally {
                if (response != null) {
                    response.close();
                }
                if (httpClient != null) {
                    httpClient.close();
                }
            }
        }

    }
}
