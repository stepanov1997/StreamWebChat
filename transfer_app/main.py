import datetime
import time
import sys
from kafka import KafkaConsumer
import threading
import pymongo
import socket
from flask import Flask, Response

app = Flask(__name__)

BOOTSTRAP_SERVERS = ['kafka:9093']
myclient = pymongo.MongoClient("mongodb://mongodb:27017/")
mydb = myclient["mydatabase"]
mycol = mydb["customers"]

def prime_generator(end=sys.maxsize):
    for n in range(2, end):  # n starts from 2 to end
        for x in range(2, n):  # check if x can be divided by n
            if n % x == 0:  # if true then n is not prime
                break
        else:  # if x is found after exhausting all values of x
            yield n  # generate the prime


@app.route("/stream")
def stream():
    def eventStream():
        for elem in prime_generator():
            # Poll data from the database
            # and see if there's a new message
            yield "{}".format(elem)

    return Response(eventStream(), mimetype="text/event-stream")


def register_kafka_listener(topic, listener):
    # Poll kafka
    def poll():
        # Initialize consumer Instance
        consumer = KafkaConsumer(topic, bootstrap_servers=BOOTSTRAP_SERVERS)

        print("About to start polling for topic:", topic)
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
    message = {
        "message": data,
        "datetime": datetime.datetime.now().strftime('%d.%B.%Y %H:%M:%S')
    }
    x = mycol.insert_one(message)
    print(message)
    print(x)


if __name__ == '__main__':
    time.sleep(5)
    register_kafka_listener('messages', kafka_listener)
    app.run("0.0.0.0", 5000)
