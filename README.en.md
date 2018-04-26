# Intro

> Mysql data change monitoring distribution 

> this project is intended to simplify monitoring MySQL database of different tables of various data change projects rely on redis

> MySQL usage scenarios: flush cache, heterogeneous systems ...

# Usage

> The default implementation of data distribution from bin-log-distributor-app to client is redis. If you want to develop based on other such as MQ, just implement the `DataPublisher` interface under `bin-log-distributor-pub` and the `DatabaseEventHandler` interface under `bin-log-distributor-client`

## Sever

The server is `bin-log-distributor-app` module in this project

Providing the ability to listen and distribute MySQL database binary logs based on `mysql-binlog-connector-java`

### Config
```
# redis address
spring.redisson.address=redis://192.168.1.204:6379

# mysql Log sync account
binaryLog.host = 192.168.1.204
binaryLog.port = 3306
binaryLog.username = aa
binaryLog.password = aa
binaryLog.serverId = 1

# Read the column name and map it
spring.datasource.url = jdbc:mysql://${binaryLog.host}:${binaryLog.port}/mysql?autoReconnect=true&amp;useUnicode=true&amp;characterEncoding=utf-8
spring.datasource.username = ${binaryLog.username}
spring.datasource.password = ${binaryLog.password}
spring.datasource.driverClassName = com.mysql.jdbc.Driver
```

### How to Boot?
Compile the project and launch `bin-log-distributor-app-${version}-SNAPSHOT.jar`

[Reference](https://docs.spring.io/spring-boot/docs/current-SNAPSHOT/reference/htmlsingle/#using-boot-running-your-application)

## Client

### Dependency
```
<!-- listening database -->
<dependency>
    <groupId>cn.keking.project</groupId>
    <artifactId>bin-log-distributor-client-redis</artifactId>
    <version>${version}</version>
</dependency>
```

### Add Client config
```
#Auto Register
databaseEventServerUrl=http://localhost:8885/client/addAll
#Name
appName=lbt-service-ext
```

### Write Handler to implement `DatabaseEventHandler` interface and add annotation @HandleDatabaseEven
```java
/**
 * LockLevel is Level of order
 * TABLE -> The same table in order
 * COLUMN -> ordered by column
 * NONE -> normal
 */
@Service
@HandleDatabaseEvent(database = "test", table = "products", events = {DatabaseEvent.UPDATE_ROWS, DatabaseEvent.DELETE_ROWS},lockLevel = LockLevel.TABLE)
public class ProjRepHisDatabaseEventHandler implements DatabaseEventHandler {
    private static final Logger logger = LoggerFactory.getLogger(ProjRepHisDatabaseEventHandler.class);

    @Override
    public void handle(EventBaseDTO eventBaseDTO) {
        logger.info(JSON.toJSONString(eventBaseDTO));
        // todo
    }
}
```

### TODO
```java
@Component
public class DatabaseEventListener {
    private static Logger logger = LoggerFactory.getLogger(DatabaseEventListener.class);
    @Autowired
    private RedissonClient redissonClient;

    @Value("#{env.databaseEventServerUrl}")
    private String serverUrl;
    @Value("#{env.appName}")
    private String appName;
    //inject service 
    @Autowired
    private ProjRepHisDatabaseEventHandler projRepHisDatabaseEventHandler; 

    @PostConstruct
    public void start() {
        // init subscription
        DataSubscriber dataSubscriber = new DataSubscriberRedisImpl(redissonClient);
        new BinLogDistributorClient(appName, dataSubscriber)
                //register handler in binlog
                .registerHandler(projRepHisDatabaseEventHandler)
                .setServerUrl(serverUrl).autoRegisterClient().start();
    }
}
```

## Frontend

> [bin-log-distributor-frontend](./bin-log-distributor-frontend/README.md)