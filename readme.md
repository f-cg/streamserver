## 开发环境配置

Java开发环境(maven构建工具，jdk>=8)

Python3环境

进入项目目录，执行 `mvn compile` 查看是否有编译错误。

编译通过后，执行 `mvn compile exec:java -Dexec.mainClass="com.founder.App"` 查看是否运行成功

运行kafka：

cd到 kafka_2.12-2.5.0

执行`./bin/zookeeper-server-start.sh config/zookeeper.properties`

打开另一个终端，执行 ` ./bin/kafka-server-start.sh config/server.properties `

打开另一个终端，执行

```shell
./bin/kafka-topics.sh --create --bootstrap-server localhost:9092 --replication-factor 1 --partitions 1 --topic orders
./bin/kafka-topics.sh --describe --topic quickstart-events --bootstrap-server localhost:9092
```
查看topic是否创建成功。

打包所有依赖
```shell
mvn clean compile assembly:single
```
将target目录下生成的streamserver*.jar部署发布。

## 运行

```shell
java -cp streamserver-0.1-jar-with-dependencies.jar com.founder.App
```
