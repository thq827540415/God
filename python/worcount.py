from pyspark import SparkConf, SparkContext

if __name__ == "__main__":
    conf = SparkConf().setAppName('test')
    sc = SparkContext(conf=conf)
    result = sc.parallelize(['hello spark hadoop', 'hello hadoop']) \
        .flatMap(lambda line: line.split(' ')) \
        .map(lambda word: (word, 1)) \
        .reduceByKey(lambda a, b: a + b) \
        .collect()
    for i in result:
        print(i)
