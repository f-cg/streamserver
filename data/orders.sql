-- filesystem

CREATE TABLE orders (
  user_id INT,
  product STRING,
  amount INT,
  ts TIMESTAMP(3),
  WATERMARK FOR ts AS ts - INTERVAL '3' SECOND
) WITH (
  'connector.type' = 'filesystem',
  'connector.path' = '/tmp/orders.csv',
  'format.type' = 'csv'
)

--- 每5秒一个窗口，查询窗口内订单总数，总量，产品类型数目

SELECT
  CAST(TUMBLE_START(ts, INTERVAL '5' SECOND) AS STRING) window_start,
  COUNT(*) order_num,
  SUM(amount) total_amount,
  COUNT(DISTINCT product) unique_products
FROM orders
GROUP BY TUMBLE(ts, INTERVAL '5' SECOND)

-- Kafka

CREATE TABLE ordersKafka(
  user_id INT,
  product STRING,
  amount INT,
  ts TIMESTAMP(3),
  WATERMARK FOR ts AS ts
) WITH (
'connector.type' = 'kafka',
'connector.version' = 'universal',
'connector.topic' = 'orders',
'connector.properties.zookeeper.connect' = 'localhost:2181',
'connector.properties.bootstrap.servers' = 'localhost:9092',
'format.type' = 'csv'
)


SELECT
  CAST(TUMBLE_START(ts, INTERVAL '5' SECOND) AS STRING) window_start,
  COUNT(*) order_num,
  SUM(amount) total_amount,
  COUNT(DISTINCT product) unique_products
FROM ordersKafka 
GROUP BY TUMBLE(ts, INTERVAL '5' SECOND)
