# 前言
Etcd的Java客户端有很多开源实现，Jetcd是Etcd官方仓库的Java客户端，整体api接口设计实现和官方go客户端类似，简洁易用。其中，租期续约的接口提供了两个
分别是keepAliveOnce和keepAlive。功能如其名，keepAliveOnce是单次续约的接口，如果要保持租约，需要手动
触发这个接口，所以这个接口基本不用。而keepAlive是自动续约保活的接口。大多数场景下，使用keepAlive即可，但是针对不同的场景，
我们还需要考虑几个问题，如租约ttl的设置，以及keepAlive异常时的处理。

# 背景问题
我们有一个基于mysql的binlog订阅数据变更的应用，线上有非常重要的应用基于这个服务，因为存在单点故障，后面使用了jetcd
的lock + keepAlive的机制实现了主备服务秒级切换的功能，具体参见[《etcd选主实现故障主备秒级切换高可用架构》](http://www.kailing.pub/article/index/arcid/254.html),
系统上线运行后发现，binlog的服务经常切换发生主备切换，而实际情况是，binlog的服务非常稳定，在没有上线主备切换服务前，从来
没有发生过线上binlog服务宕掉的情况。最后查明问题出在了租约TTL的设置上面。这里先抛出问题和定位，下面先看下Jetcd的keepAlive具体
实现，然后在分析为什么导致这个问题。

# KeepAlive实现
先看下keepAlive的用法
```
    private long acquireActiveLease() throws InterruptedException, ExecutionException {
        long leaseId = leaseClient.grant(leaseTTL).get().getID();
        logger.debug("LeaderSelector get leaseId:[{}] and ttl:[{}]", leaseId, leaseTTL);
        this.leaseCloser = leaseClient.keepAlive(leaseId, new StreamObserver<LeaseKeepAliveResponse>() {
            @Override
            public void onNext(LeaseKeepAliveResponse value) {
                logger.debug("LeaderSelector lease keeps alive for [{}]s:", value.getTTL());
            }
            @Override
            public void onError(Throwable t) {
                logger.debug("LeaderSelector lease renewal Exception!", t.fillInStackTrace());
                cancelTask();
            }
            @Override
            public void onCompleted() {
                logger.info("LeaderSelector lease renewal completed! start canceling task.");
                cancelTask();
            }
        });
        return leaseId;
    }
```
租约实现都在LeaseImpl类里，通过EtcdClient拿到LeaseImpl实例后，首先通过grant方法设置ttl拿到租约的id，然后将租约作为入参调用keepAlive方法，
第二个入参是一个观察者对象，内置了三个接口，分别是onNext：确定下一次租约续约时间后触发，onError：续约异常时触发，onCompleted：租约过期后触发。


keepAlive方法代码：
```
  public synchronized CloseableClient keepAlive(long leaseId, StreamObserver<LeaseKeepAliveResponse> observer) {
    if (this.closed) {
      throw newClosedLeaseClientException();
    }

    KeepAlive keepAlive = this.keepAlives.computeIfAbsent(leaseId, (key) -> new KeepAlive(leaseId));
    keepAlive.addObserver(observer);

    if (!this.hasKeepAliveServiceStarted) {
      this.hasKeepAliveServiceStarted = true;
      this.start();
    }

    return new CloseableClient() {
      @Override
      public void close() {
        keepAlive.removeObserver(observer);
      }
    };
  }
```
LeaseImpl内部维护了一个以LeaseId为key，KeepAlive对象为value的map，KeepAlive的类中维护了一个StreamObserver集合，到期
时间deadLine，下次续约时间nextKeepAlive和续约leaseId。第一次调用keepAlive方法时会触发start，启动续约的线程（sendKeepAliveExecutor()）和检查是否
过期的线程(deadLineExecutor())。
```
  private void sendKeepAliveExecutor() {
    this.keepAliveResponseObserver = Observers.observer(
      response -> processKeepAliveResponse(response),
      error -> processOnError()
    );
    this.keepAliveRequestObserver = this.leaseStub.leaseKeepAlive(this.keepAliveResponseObserver);
    this.keepAliveFuture = scheduledExecutorService.scheduleAtFixedRate(
        () -> {
            // send keep alive req to the leases whose next keep alive is before now.
            this.keepAlives.entrySet().stream()
                .filter(entry -> entry.getValue().getNextKeepAlive() < System.currentTimeMillis())
                .map(Entry::getKey)
                .map(leaseId -> LeaseKeepAliveRequest.newBuilder().setID(leaseId).build())
                .forEach(keepAliveRequestObserver::onNext);
        },
        0,
        500,
        TimeUnit.MILLISECONDS
    );
  }
```
sendKeepAliveExecutor方法是整个keepAlive功能实现的核心，这个方法在LeaseImpl实例里只会被触发一次，开启了一个时间间隔为
500毫秒的的定时任务调度。每次从keepAlives中筛选出nextkeepAlive时间小于当前时间的KeepAlive对象，触发续约。nextkeepAlive初始化值就是创建KeepAlive实例时的当前时间，
然后在续约的响应流观察者实例中，执行了processKeepAliveResponse方法，在这个里面维护了KeepAlive对象的nextkeepAlive。
```
private synchronized void processKeepAliveResponse(io.etcd.jetcd.api.LeaseKeepAliveResponse leaseKeepAliveResponse) {
    if (this.closed) {
      return;
    }
    final long leaseID = leaseKeepAliveResponse.getID();
    final long ttl = leaseKeepAliveResponse.getTTL();
    final KeepAlive ka = this.keepAlives.get(leaseID);
    if (ka == null) {
      // return if the corresponding keep alive has closed.
      return;
    }
    if (ttl > 0) {
      long nextKeepAlive = System.currentTimeMillis() + ttl * 1000 / 3;
      ka.setNextKeepAlive(nextKeepAlive);
      ka.setDeadLine(System.currentTimeMillis() + ttl * 1000);
      ka.onNext(leaseKeepAliveResponse);
    } else {
      // lease expired; close all keep alive
      this.removeKeepAlive(leaseID);
      ka.onError(
          newEtcdException(
            ErrorCode.NOT_FOUND,
            "etcdserver: requested lease not found"
          )
      );
    }
  }
```
可以看到，在首次续约后的响应处理中，nextKeepAlive被设置为当前时间加上ttl的1/3时间后，也就是说如果我们设置一个key的过期时间为6s，
那么在使用keepAlive时续期的时间间隔为，每2s执行续约一次。如果ttl小于零，说明key已经过期被删除了，就直接触发onError，传递了一个
requested lease not found的异常对象。
# 文末小结
回到最上面binlog的主备频繁切换的问题，由于我们将ttl的时间设置的过小5s。只要client和etcd 服务失联5s以上，期间可能由于各种原因导致
keepAlive没有正常续约上，就会触发主备切换。这个时候binlog服务本身是没有任何问题的，却要因为失去领导权，而选择自杀。
后面将ttl调整到了20s后，主备切换就没有那么敏感了。

还有一个场景，在将etcd作为服务注册中心时，也会使用到keepAlive，即使设置了ttl为20s，还是有可能没有续约上，导致注册的服务过期被删了，这个
时候，我们的服务进程还是健康的。这个场景下，需要在onError、onCompleted事件中重新获取租约以及添加新的KeepAlive。



