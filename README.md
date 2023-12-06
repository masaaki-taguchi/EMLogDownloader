# EMLogDownloader
<p align="center">
  <img src="https://img.shields.io/badge/Salesforce-00a1e0.svg">
  <img src="https://img.shields.io/badge/license-MIT-blue.svg">
</p>

EMLogDownloader is a cli tool to download Event Monitoring log files.

## Dependency
* Java 1.8+
* Connected App with api oauth scope must be created on Salesforce.

## Edit config file
In order to use this tool, you need to update the config file(EMLogDownLoader.yaml) according to your environment.
```bash
EMLogDownload-Setting:
    DownloadPath: "./download"
    LoginHost: "https://login.salesforce.com"
    ApiVersion: "59.0"
    EventType: ""
    # If the following values are not set, It sets it interactively at execution.
    ClientId: ""
    ClientSecret: ""
    UserName: ""
    Password: ""
```

* DownloadPath specifies the directory where the CSV files of event logs will be downloaded.
* EventType specifies the target EventTypes for download, separated by commas (e.g., "Login,Logout").
  If EventType is undefined, it includes all EventTypes.
* ClientId and ClientSecret should be set to the values obtained from the created Connected App.
* UserName and Password specify the login information for the user.


## Usage

```bash
java -jar EMLogDownloader-1.0.0.jar -help
usage: EMLogDownloader [-options]
    -config <pathname>         Specifies config file path. (Default is ./EMLogDownloader.yaml)
    -date <targetdate>         Specifies download EMLog date. (Format is "yyyy-MM-dd")
    -hourlylog                 Get hourly event log files.
    -hour <targethour>         Specifies download EMLog hour. (Format is "hh")
```

To execute EMLogDownloader run the following command. EMLogDownLoader.yaml must be located in the current directory.
```bash
java -jar EMLogDownloader-1.0.0.jar
```

To specify the path of the config file, use the option of -config.
```bash
java -jar EMLogDownloader-1.0.0.jar -config confiig/EMLogDownLoader.yaml
```
To specify the date of download, use the option of -date. If not specified, enter it interactively.
```bash
java -jar EMLogDownloader-1.0.0.jar -date 2019-02-01
```
If you download hourly log, use the option of -hourlylog.
```bash
java -jar EMLogDownloader-1.0.0.jar -hourlylog
```
To specify the date and hour of download in hourly log mode, use the option of -date and -hour. If not specified, enter it interactively.
```bash
java -jar EMLogDownloader-1.0.0.jar -hourlylog -date 2019-02-01 -hour 01
```


## Execution sample

```bash
$ java -jar EMLogDownloader-1.0.0.jar -config EMLogDownLoader.yaml -date 2023-11-30
[EventMonitoring Log Downloader:Start]

  [Parameter]
    downloadPath: ./download
    loginHost: https://login.salesforce.com
    userName: (UserName)
    apiVersion: 59.0
    targetDate: 2023-11-30
    eventType: (ALL)
    accessToken: (AccessToken)

  [Download progress]
    (1/7) EventType:ApiTotalUsage Length:665 DownloadFile:EventLog_ApiTotalUsage_2023-11-30.csv
    (2/7) EventType:Login Length:835 DownloadFile:EventLog_Login_2023-11-30.csv
    (3/7) EventType:Logout Length:506 DownloadFile:EventLog_Logout_2023-11-30.csv
    (4/7) EventType:RestApi Length:1078 DownloadFile:EventLog_RestApi_2023-11-30.csv
    (5/7) EventType:Sites Length:761 DownloadFile:EventLog_Sites_2023-11-30.csv
    (6/7) EventType:PlatformEncryption Length:1310 DownloadFile:EventLog_PlatformEncryption_2023-11-30.csv
    (7/7) EventType:URI Length:813 DownloadFile:EventLog_URI_2023-11-30.csv

[EventMonitoring Log Downloader:Finish]
```

## Build
To build and run this application locally, you'll need latest versions of Git, Maven and JDK installed on your computer. From your command line:

```bash
# Clone this repository
$ git clone git@github.com:masaaki-taguchi/EMLogDownloader.git

# Go into the repository
$ cd EMLogDownloader

# Build
$ mvn

# Run the app
$ java -jar target/EMLogDownloader-1.0.0.jar
```

## License
EMLogDownloader is licensed under the MIT license.

