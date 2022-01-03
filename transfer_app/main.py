import json
import sys
import threading
import time
import pymongo
from flask import Flask, Response
from kafka import KafkaConsumer

app = Flask(__name__)

BOOTSTRAP_SERVERS = ['localhost:9092']
myclient = pymongo.MongoClient("mongodb://localhost:27017/", replicaset="rs0")
mydb = myclient["stream_web_chat"]
mycol = mydb["messages"]


@app.route("/stream")
def stream():
    def event_stream():
        change_stream = mycol.watch()
        while True:
            for change in change_stream:
                print(change)
                print(type(change))
                yield "{}".format(change)

    return Response(event_stream(), mimetype="text/event-stream")


def register_kafka_listener(topic, listener):
    def poll():
        consumer = KafkaConsumer(topic, bootstrap_servers=BOOTSTRAP_SERVERS)
        consumer.poll(6000)
        print("Started Polling for topic:", topic)
        for msg in consumer:
            t2 = threading.Thread(target=listener, args=(msg.value.decode("utf-8"),))
            t2.start()

    print("About to register listener to topic:", topic)
    t1 = threading.Thread(target=poll)
    t1.start()
    print("started a background thread")


def kafka_listener(data):
    message = json.loads(data)
    try:
        mycol.insert_one(message)
    except Exception as e:
        print(e)
    print(data)


if __name__ == '__main__':
    time.sleep(5)
    register_kafka_listener('messages', kafka_listener)
    app.run("0.0.0.0", 5000)
