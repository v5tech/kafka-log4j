# kafka-log4j 使用kafka实现log4j日志集中管理

### 搭建Kafka环境

https://github.com/sxyx2008/storm-example/blob/master/kafka集群搭建.md

#### 安装zookeeper

修改zookeeper配置文件

conf/zoo.cfg

```
tickTime=2000
initLimit=10
syncLimit=5
dataDir=/tmp/zookeeper
clientPort=2181
```

zookeeper集群环境

```
tickTime=2000
initLimit=10
syncLimit=5
dataDir=/tmp/zookeeper/data
clientPort=2181

server.1=s1:2888:3888
server.2=s2:2888:3888
server.3=s3:2888:3888
```

分别在s1、s2、s3三台主机的dataDir目录下创建myid文件，其内容为分别为1、2、3

启动zookeeper

```
zkServer.sh start
zkServer.sh status
```

#### 安装Kafka

下载：http://kafka.apache.org/downloads.html

```
tar zxf kafka-<VERSION>.tgz
cd kafka-<VERSION>
```

修改kafka配置文件config/server.properties

```
broker.id=0
host.name=localhost
port=9092
/tmp/kafka-logs-0
zookeeper.connect=s1:2181,s2:2181,s3:2181
```

启动Kafka Server

```
bin/kafka-server-start.sh config/server.properties &
```

创建Topic

```
bin/kafka-topics.sh --create --zookeeper s1:2181 --replication-factor 1 --partitions 1 --topic test
```

查看Topic

```
bin/kafka-topics.sh --list --zookeeper s1:2181
```

启动控制台Producer，向Kafka发送消息

```
bin/kafka-console-producer.sh --broker-list localhost:9092 --topic test
This is a message
This is another message
^C
```

启动控制台Consumer，消费刚刚发送的消息

```
bin/kafka-console-consumer.sh --zookeeper s1:2181 --topic test --from-beginning
This is a message
This is another message
```

删除Topic

```
bin/kafka-topics.sh --delete --zookeeper s1:2181 --topic test
```

注：只有当delete.topic.enable=true时，该操作才有效


配置Kafka集群（单台机器上）

首先拷贝server.properties文件为多份（这里演示4个节点的Kafka集群，因此还需要拷贝3份配置文件）

```
cp config/server.properties config/server1.properties
cp config/server.properties config/server2.properties
cp config/server.properties config/server3.properties
```

修改server1.properties的以下内容：

```
broker.id=1
host.name=localhost
port=9093
log.dir=/tmp/kafka-logs-1
```

修改server2.properties的以下内容：
```
broker.id=2
host.name=localhost
port=9094
log.dir=/tmp/kafka-logs-2
```

修改server3.properties的以下内容：
```
broker.id=3
host.name=localhost
port=9095
log.dir=/tmp/kafka-logs-3
```

启动kafka集群

```
bin/kafka-server-start.sh config/server1.properties &
bin/kafka-server-start.sh config/server2.properties &
bin/kafka-server-start.sh config/server3.properties &
```

Topic & Partition

Topic在逻辑上可以被认为是一个queue，每条消费都必须指定它的Topic，可以简单理解为必须指明把这条消息放进哪个queue里。为了使得Kafka的吞吐率可以线性提高，物理上把Topic分成一个或多个Partition，每个Partition在物理上对应一个文件夹，该文件夹下存储这个Partition的所有消息和索引文件。

在Kafka集群上创建备份因子为3，分区数为4的Topic

```
bin/kafka-topics.sh --create --zookeeper s1:2181 --replication-factor 3 --partitions 4 --topic kafka
```

说明：备份因子replication-factor越大，则说明集群容错性越强，就是当集群down掉后，数据恢复的可能性越大。所有的分区数里的内容共同组成了一份数据，分区数partions越大，则该topic的消息就越分散，集群中的消息分布就越均匀。

然后使用kafka-topics.sh的--describe参数查看一下Topic为kafka的详情：

```
bin/kafka-topics.sh --describe --zookeeper s1:2181 --topic kafka
```

输出的第一行是所有分区的概要，接下来的每一行是一个分区的描述。可以看到Topic为kafka的消息，PartionCount=4，ReplicationFactor=3正是我们创建时指定的分区数和备份因子。

另外：Leader是指负责这个分区所有读写的节点；Replicas是指这个分区所在的所有节点（不论它是否活着）；ISR是Replicas的子集，代表存有这个分区信息而且当前活着的节点。

拿partition:0这个分区来说，该分区的Leader是server0，分布在id为0，1，2这三个节点上，而且这三个节点都活着。

再来看下Kafka集群的日志：

```
ls /tmp/kafka-logs-0
ls /tmp/kafka-logs-1
ls /tmp/kafka-logs-2
ls /tmp/kafka-logs-3
```

其中kafka-logs-0代表server0的日志，kafka-logs-1代表server1的日志，以此类推。

从上面的配置可知，id为0，1，2，3的节点分别对应server0, server1, server2, server3。而上例中的partition:0分布在id为0, 1, 2这三个节点上，因此可以在server0, server1, server2这三个节点上看到有kafka-0这个文件夹。这个kafka-0就代表Topic为kafka的partion0。


### kafka log4j appender

```
log4j.rootLogger=INFO,console

log4j.logger.net.aimeizi.kafka=DEBUG,kafka

# appender kafka
log4j.appender.kafka=kafka.producer.KafkaLog4jAppender
log4j.appender.kafka.topic=kafka
# multiple brokers are separated by comma ",".
log4j.appender.kafka.brokerList=localhost:9092, localhost:9093, localhost:9094, localhost:9095
log4j.appender.kafka.compressionType=none
log4j.appender.kafka.syncSend=true
log4j.appender.kafka.layout=org.apache.log4j.PatternLayout
log4j.appender.kafka.layout.ConversionPattern=%d [%-5p] [%t] - [%l] %m%n

# appender console
log4j.appender.console=org.apache.log4j.ConsoleAppender
log4j.appender.console.target=System.out
log4j.appender.console.layout=org.apache.log4j.PatternLayout
log4j.appender.console.layout.ConversionPattern=%d [%-5p] [%t] - [%l] %m%n
```

http://my.oschina.net/itblog/blog/540918