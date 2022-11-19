import argparse
import datetime
import json
import os
import random as r
import threading
import time
import uuid

import pandas as pd
import pyarrow as pa
import pyarrow.parquet as pq
from hdfs.config import Client
from kafka import KafkaProducer


def parse_producer_properties(props):
    res_props = {}
    f = open(props, 'r', encoding='utf-8')
    for line in f:
        if '=' in line and '#' not in line:
            k, v = line.replace('\n', '').split('=')
            res_props[k.replace('.', '_')] = v
    return res_props


def process(f):
    n = 0
    while n < 4:
        xv = r.choice(['ls', 'zs', 'ww', 'thq', 'sz', 'lw', 'yy'])
        yv = r.randint(18, 50)
        start_time = datetime.datetime.now() - datetime.timedelta(3)
        for i in range(2):
            time.sleep(1)
            random_time = [(start_time + datetime.timedelta(i)).strftime("%Y%m%d") for i in range(0, 3)]
            dt = [int(time.mktime(time.strptime(i, '%Y%m%d'))) * 1000 + r.randint(0, 99999999) for i in random_time]
            yield {
                f[0]: xv,
                f[1]: yv,
                f[2]: r.choice(dt)
            }
        n += 1


# pip install kafka-python
def send_kafka(value):
    props = {
        'bootstrap_servers': ['bigdata01:9092', 'bigdata02:9092', 'bigdata03:9092'],
        'key_serializer': None,
        'value_serializer': lambda data: json.dumps(data).encode('utf-8'),
        'retries': 0,
        'retry_backoff_ms': 100,
        'buffer_memory': 33554432,
        'batch_size': 16384,
        'max_request_size': 1048576,
        'acks': -1
    }
    producer = KafkaProducer(**props)
    producer.send(topic="test", value=value)

    producer.close()


# pip install hdfs
# pip install pyarrow
def send_hive(d):
    sql = """
    create external table if not exists person (
        name string,
        age int,
        dt string
    ) stored as parquet
    location '/tmp/hive/person'
    tblproperties(
        'parquet.compress'='SNAPPY'
    )
    """

    # os.popen(f'/opt/hive-3.1.2/bin/beeline -u jdbc:hive2://bigdata03:10000/default -n root -e "{sql}"')

    def do_process():
        return process(d)

    dic_data = next(do_process())
    for k, v in dic_data.items():
        dic_data[k] = [v]
    table = pa.table(dic_data)

    output_file = 'D:\\IdeaProjects\\God\\output\\person-{0}.parquet'.format(uuid.uuid1())

    with pq.ParquetWriter(output_file, schema=table.schema,
                          compression='snappy') as writer:
        for dic_data in do_process():
            for k, v in dic_data.items():
                dic_data[k] = [v]
            table = pa.table(dic_data)
            writer.write_table(table)
    print(pd.read_parquet(output_file).to_json)

    hdfs_client = Client('http://bigdata01:50070')
    hdfs_client.upload("/tmp/hive/person", output_file)
    os.remove(output_file)


if __name__ == "__main__":
    p = argparse.ArgumentParser(description="data")
    p.add_argument('--schema', type=str, required=True, help="eg: --schema 'name string, age int, dt string'")
    p.add_argument('--special', type=str, required=True, help="eg: --special 'name, age'")
    p.add_argument('--dist', type=str, default="console", required=False, help="[ table ] | [ topic] | [ all ]")
    p.add_argument('--kafka', type=bool, default=False, required=False, help="whether use kafka")
    args = p.parse_args()

    field_and_type = args.schema.split(',')
    f = [i.strip(' ').split(' ')[0] for i in field_and_type]
    t = [i.strip(' ').split(' ')[1] for i in field_and_type]

    send_hive(f)
    # threading.Thread(name='t1', target=process, args=[d], daemon=False).start()
    # threading.Thread(name='t2', target=process, args=[d], daemon=False).start()

