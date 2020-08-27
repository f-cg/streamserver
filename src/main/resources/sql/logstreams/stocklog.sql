Ticker
----
CREATE TABLE Ticker(
  symbol STRING,
  rowtime TIMESTAMP(3),
  price INT,
  tax INT,
  WATERMARK FOR rowtime AS rowtime - INTERVAL '3' SECOND
) WITH (
  'connector.type' = 'filesystem',
  'connector.path' = '/tmp/stock.csv',
  'format.type' = 'csv'
)
