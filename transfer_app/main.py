import json
import sys
import threading
import time
import pymongo
from flask import Flask, Response
from kafka import KafkaConsumer

app = Flask(__name__)

BOOTSTRAP_SERVERS = ['localhost:9092']
myclient = pymongo.MongoClient(host="127.0.10.1:27017")
mydb = myclient["stream_web_chat"]
mycol = mydb["messages"]


@app.route("/stream")
def stream_f():
    def event_stream():
        while True:
            resume_token = None
            pipeline = [{'$match': {'operationType': 'insert'}}]
            try:
                with mydb.messages.watch(pipeline) as stream:
                    for insert_change in stream:
                        message = insert_change['fullDocument']
                        message.pop('_id')
                        print(message)
                        resume_token = stream.resume_token
                        yield f"{message}\n"
            except pymongo.errors.PyMongoError:
                if resume_token is None:
                    logging.error('...')
                else:
                    with mydb.messages.watch(
                            pipeline, resume_after=resume_token) as stream:
                        for insert_change in stream:
                            print(insert_change)

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
    print("Received data:", data)
    message = json.loads(data)
    try:
        mycol.insert_one(message)
    except Exception as e:
        print(e)


if __name__ == '__main__':
    time.sleep(5)
    register_kafka_listener('messages', kafka_listener)
    app.run("0.0.0.0", 5000)
