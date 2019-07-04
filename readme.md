# 1 概述
>mysql数据变动监听分发
>本项目意在简化监听mysql数据库的不同表的各种数据变动
>项目依赖redis,mysql
>使用场景：刷新缓存、异构系统...

![示意图](Picture1.png)
# 2 使用方式
>从bin-log-distributor-app到client数据分发方式的默认实现为redis，如果要开发基于其他的比如mq，只需要分别实现`bin-log-distributor-pub`下的`DataPublisher`接口与`bin-log-distributor-client`下的即可
## 2.1 服务端
服务端是项目中`bin-log-distributor-app`模块，在[mysql-binlog-connector-java](服务端是项目中`bin-log-distributor-app`模块，在[mysql-binlog-connector-java]()提供了监听mysql数据库二进制日志的功能
)基础上提供了监听mysql数据库二进制日志并进行分发的功能

### 2.1.1 参考配置
```
# redis地址
spring.redisson.address=redis://192.168.1.204:6379

# mysql数据源配置 开启日志同步账户需要REPLICATION SLAVE权限
binaryLog.configs[0].namespace = default                       
binaryLog.configs[0].host = 127.0.0.1
binaryLog.configs[0].port = 3306
binaryLog.configs[0].username = binlog
binaryLog.configs[0].password = binlog
binaryLog.configs[0].serverId = 1
binaryLog.configs[0].dataSourceUrl = jdbc:mysql://127.0.0.1:3306/mysql?autoReconnect=true&amp;useUnicode=true&amp;characterEncoding=utf-8
binaryLog.configs[0].driverClassName = com.mysql.jdbc.Driver

# rabbitmq连接相关信息(当开启rabbitmq时启用)
spring.rabbit.host = 192.168.1.204
spring.rabbit.port = 5672
spring.rabbit.username = aa
spring.rabbit.password = aa
spring.rabbit.virtualHost = /binlog

#kafka连接相关信息(当开启kafka时启用)
spring.kafka.bootstrap-servers = 127.0.0.1:9092
spring.kafka.producer.value-serializer = org.springframework.kafka.support.serializer.JsonSerializer
spring.kafka.consumer.group-id = test.binlog
kafka.zk.servers = 127.0.0.1:2181
kafka.zk.partitions = 1
kafka.zk.replications = 1
```

其中rabbitmq和kafka的配置是非必须的，只有在配置了`spring.rabbit.host`或`spring.kafka.bootstrap-servers`时才会启用相应的消息队列。

### 2.1.2 启动方式

编译打包项目，直接通过（java -jar）启动bin-log-distributor-app-${version}-SNAPSHOT.jar，可参考[spring boot手册](https://docs.spring.io/spring-boot/docs/current-SNAPSHOT/reference/htmlsingle/#using-boot-running-your-application)

## 2.2 客户端
### 2.2.1 redis客户端
#### 2.2.1.1 引入依赖包
```
<dependency>
    <groupId>cn.keking.project</groupId>
    <artifactId>bin-log-distributor-client-redis</artifactId>
    <version>${version}</version>
</dependency>
```

#### 2.2.1.2 添加客户端配置
```
#自动注册客户端(2.1中服务端的地址)
databaseEventServerUrl=http://localhost:8885/client/addAll
#本应用命名
appName=lbt-service-ext-redis
# redis地址
spring.redisson.address=redis://192.168.1.204:6379
```

#### 2.2.1.3 写handler，实现 DatabaseEventHandler 接口，并加上注解 @HandleDatabaseEven 
```
/**
 * LockLevel为保持顺序的级别,None为默认
 * TABLE -> 同表按顺序执行
 * COLUMN -> 某列值一致的按顺序执行
 * NONE -> 无序
 */
@Service
@HandleDatabaseEvent(database = "Rana_G", table = "PTP_PROJ_REP_HIS", events = {DatabaseEvent.UPDATE_ROWS, DatabaseEvent.DELETE_ROWS},lockLevel = LockLevel.TABLE)
public class ProjRepHisDatabaseEventHandler implements DatabaseEventHandler {
    private static final Logger logger = LoggerFactory.getLogger(ProjRepHisDatabaseEventHandler.class);

    @Override
    public void handle(EventBaseDTO eventBaseDTO) {
        logger.info(JSON.toJSONString(eventBaseDTO));
        // todo 在这里写相关逻辑
    }
}
```

#### 2.2.1.4 启动监听，当2里的handler是由容器管理时需要通过registerHandler(projRepHisDatabaseEventHandler)手动注册，如果不需要容器管理，可以直接通过autoScanHandler()自动扫描添加
```
@Component
public class DatabaseEventListener {
    private static Logger logger = LoggerFactory.getLogger(DatabaseEventListener.class);
    
    @Autowired
    private RedissonClient redissonClient;

    @Value("#{env.databaseEventServerUrl}")
    private String serverUrl;
    
    @Value("#{env.appName}")
    private String appName;
    
    //将第3步里的service注入
    @Autowired
    private ProjRepHisDatabaseEventHandler projRepHisDatabaseEventHandler; 

    @PostConstruct
    public void start() {
        //初始化订阅的实现
        DataSubscriber dataSubscriber = new DataSubscriberRedisImpl(redissonClient);
        new BinLogDistributorClient(appName, dataSubscriber)
                //在binlog中注册handler
                .setQueueType(ClientInfo.QUEUE_TYPE_REDIS) 
                .registerHandler(projRepHisDatabaseEventHandler)
                .setServerUrl(serverUrl).autoRegisterClient().start();
    }
}
```

### 2.2.2 rabbitmq客户端
#### 2.2.2.1 引入依赖包
```
<dependency>
    <groupId>cn.keking.project</groupId>
    <artifactId>bin-log-distributor-client-rabbitmq</artifactId>
    <version>${version}</version>
</dependency>
```

#### 2.2.2.2 添加客户端配置
```
#自动注册客户端(2.1中服务端的地址)
databaseEventServerUrl=http://localhost:8885/client/addAll
#本应用命名
appName=lbt-service-ext-rabbit
# redis地址(rabbitmq实现也要使用redis作为分布式锁)
spring.redisson.address=redis://192.168.1.204:6379
# RabbitMQ配置
spring.rabbit.host=192.168.1.204
spring.rabbit.port=5672
spring.rabbit.username=aa
spring.rabbit.password=aa
spring.rabbit.virtualHost=/binlog
spring.rabbit.apiUrl=http://192.168.1.204:15672/api/
```

#### 2.2.2.3 写handler，实现 DatabaseEventHandler 接口，并加上注解 @HandleDatabaseEven 
```
/**
 * LockLevel为保持顺序的级别,None为默认
 * TABLE -> 同表按顺序执行
 * COLUMN -> 某列值一致的按顺序执行
 * NONE -> 无序
 */
@Service
@HandleDatabaseEvent(database = "Rana_G", table = "PTP_PROJ_REP_HIS", events = {DatabaseEvent.UPDATE_ROWS, DatabaseEvent.DELETE_ROWS},lockLevel = LockLevel.TABLE)
public class ProjRepHisDatabaseEventHandler implements DatabaseEventHandler {
    private static final Logger logger = LoggerFactory.getLogger(ProjRepHisDatabaseEventHandler.class);

    @Override
    public void handle(EventBaseDTO eventBaseDTO) {
        logger.info(JSON.toJSONString(eventBaseDTO));
        // todo 在这里写相关逻辑
    }
}
```

#### 2.2.2.4 启动监听，当2里的handler是由容器管理时需要通过registerHandler(projRepHisDatabaseEventHandler)手动注册，如果不需要容器管理，可以直接通过autoScanHandler()自动扫描添加
```
@Component
public class DatabaseEventListener {
    private static Logger logger = LoggerFactory.getLogger(DatabaseEventListener.class);
    
    @Autowired
    private RedissonClient redissonClient;

    @Value("#{env.databaseEventServerUrl}")
    private String serverUrl;
    
    @Value("#{env.appName}")
    private String appName;
    //将第3步里的service注入
    
    @Autowired
    private ProjRepHisDatabaseEventHandler projRepHisDatabaseEventHandler; 
    
    //org.springframework.amqp.rabbit.connection.ConnectionFactory 自行创建spring bean
    @Autowired
    private ConnectionFactory connectionFactory; 

    //com.rabbitmq.http.client.Client 自行创建spring bean
    @Autowired
    private Client rabbitHttpClient; 

    @PostConstruct
    public void start() {
        //初始化订阅的实现
        DataSubscriber dataSubscriber = new DataSubscriberRabbitMQImpl(connectionFactory, rabbitHttpClient, redissonClient);
        new BinLogDistributorClient(appName, dataSubscriber)
                //默认为redis实现，使用mq实现这里一定要指定QueueType
                .setQueueType(ClientInfo.QUEUE_TYPE_RABBIT)
                .registerHandler(projRepHisDatabaseEventHandler)
                .setServerUrl(serverUrl).autoRegisterClient().start();
    }
}
```

### 2.2.3 kafka
目前kafka仅有消息推送，没有相应的客户端，可以通过前端管理模块管理。

# 2.3 多数据源监听

多数据源是通过命名空间namespace来实现数据和逻辑的隔离，程序通过(命名空间 + 数据库名 + 表名)来定位需要监听的表，考虑到会监听不同数据源同名数据库和同名表的可能性，所以命名空间被设计为在系统范围内是唯一的。

首先在服务端配置多个数据源。
```
# mysql数据源1配置 开启日志同步账户需要REPLICATION SLAVE权限
binaryLog.configs[0].namespace = default                       
binaryLog.configs[0].host = 127.0.0.1
binaryLog.configs[0].port = 3306
binaryLog.configs[0].username = binlog
binaryLog.configs[0].password = binlog
binaryLog.configs[0].serverId = 1
binaryLog.configs[0].dataSourceUrl = jdbc:mysql://127.0.0.1:3306/mysql?autoReconnect=true&amp;useUnicode=true&amp;characterEncoding=utf-8
binaryLog.configs[0].driverClassName = com.mysql.jdbc.Driver

# mysql数据源2配置 开启日志同步账户需要REPLICATION SLAVE权限
binaryLog.configs[1].namespace = another
binaryLog.configs[1].host = 127.0.0.1
binaryLog.configs[1].port = 3307
binaryLog.configs[1].username = binlog
binaryLog.configs[1].password = binlog
binaryLog.configs[1].serverId = 2
binaryLog.configs[1].dataSourceUrl = jdbc:mysql://127.0.0.1:3307/mysql?autoReconnect=true&amp;useUnicode=true&amp;characterEncoding=utf-8
binaryLog.configs[1].driverClassName = com.mysql.jdbc.Driver
```
上面配置了两个数据源，命名空间分别是default和another，下面来配置客户端。

监听数据源1，当`@HandleDatabaseEvent`注解中没有配置namespace属性时，默认监听命名空间是default的数据源。
```java
@Service
@HandleDatabaseEvent(database = "world", table = "city", events = {DatabaseEvent.WRITE_ROWS,DatabaseEvent.UPDATE_ROWS, DatabaseEvent.DELETE_ROWS},lockLevel = LockLevel.TABLE)
public class ExampleDataEventHandler implements DatabaseEventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExampleDataEventHandler.class);

    /**
     * 此方法处理变更信息
     * @param eventBaseDTO
     */
    @Override
    public void handle(EventBaseDTO eventBaseDTO) {
        LOGGER.info("接收信息:" + eventBaseDTO.toString());
    }

    @Override
    public Class getClazz() {
        return ExampleDataEventHandler.class;
    }
}
```
监听数据源2
```java
@Service
@HandleDatabaseEvent(namespace = "another", database = "sakila", table = "film", events = {DatabaseEvent.WRITE_ROWS,DatabaseEvent.UPDATE_ROWS, DatabaseEvent.DELETE_ROWS},lockLevel = LockLevel.TABLE)
public class ExampleDataEventHandler2 implements DatabaseEventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExampleDataEventHandler2.class);

    /**
     * 此方法处理变更信息
     * @param eventBaseDTO
     */
    @Override
    public void handle(EventBaseDTO eventBaseDTO) {
        LOGGER.info("[another数据源]接收信息:" + eventBaseDTO.toString());
    }

    @Override
    public Class getClazz() {
        return ExampleDataEventHandler2.class;
    }
}
```


# 2.4 前端管理模块
>前端管理服务模块是基于vue的管理各个应用监听状况及数据源的管理界面

# 3 其他