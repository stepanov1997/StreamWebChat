import json
import os
import sys
import threading
import time
import pymongo
import logging
from kafka import KafkaConsumer

BOOTSTRAP_SERVERS = os.environ['BOOTSTRAP_SERVERS']
MONGO_HOST = os.environ['MONGO_HOST'].split(",")
MONGO_PORT = int(os.environ['MONGO_PORT'])

mongo_client = pymongo.MongoClient(host=MONGO_HOST, port=MONGO_PORT)


def init_logger():
    global logger
    Log_Format = "%(levelname)s %(asctime)s - %(message)s"
    logging.basicConfig(stream=sys.stdout, filemode="w", format=Log_Format, level=logging.INFO)
    return logging.getLogger()


logger = init_logger()


def register_kafka_listener(topic, listener):
    def poll():
       while True:
           try:
               consumer = KafkaConsumer(topic, bootstrap_servers=BOOTSTRAP_SERVERS, group_id="transfer_app")
               consumer.poll(6000)
               logger.info("Started listening to topic: {}".format(topic))
               for msg in consumer:
                   t2 = threading.Thread(target=listener, args=(msg.value.decode("utf-8"),))
                   t2.start()
           except Exception as e:
               time.sleep(5)
               logger.error("Error while listening to topic: {}".format(topic))

    logger.info("About to register listener to topic: %s", topic)
    t1 = threading.Thread(target=poll)
    t1.start()


def kafka_listener(data):
    logger.info("Received data: %s", data)
    message = json.loads(data)
    try:
        mongo_client["test"]['messages'].insert_one(message)
    except Exception as e:
        logger.error("Error inserting data into mongo:", e)


if __name__ == '__main__':
    time.sleep(5)
    register_kafka_listener('messages', kafka_listener)
