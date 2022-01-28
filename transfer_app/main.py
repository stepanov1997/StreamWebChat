import json
import os
import sys
import threading
import time
import pymongo
from flask import Flask, Response, request, stream_with_context
from flask_cors import CORS, cross_origin
from kafka import KafkaConsumer

app = Flask(__name__)
cors = CORS(app)
app.config['CORS_HEADERS'] = 'Content-Type'

BOOTSTRAP_SERVERS = os.environ['BOOTSTRAP_SERVERS']
MONGO_HOST = os.environ['MONGO_HOST'].split(",")
MONGO_PORT = int(os.environ['MONGO_PORT'])

myclient = pymongo.MongoClient(host=MONGO_HOST, port=MONGO_PORT)
mydb = myclient["test"]
messagesColumn = mydb["messages"]
usersColumn = mydb["users"]


@app.route("/stream")
@cross_origin()
def stream_f():
    args = request.args.to_dict()

    sender_username = args["senderUsername"]
    receiver_username = args["receiverUsername"]

    def event_stream():
        while True:
            resume_token = None
            pipeline = [{'$match': {
                'operationType': 'insert'
            }}]
            try:
                with mydb.messages.watch(pipeline) as stream:
                    for insert_change in stream:

                        message = insert_change['fullDocument']
                        sender_is_sender = message['senderUsername'] == sender_username and message['receiverUsername'] == receiver_username
                        sender_is_receiver = message['senderUsername'] == receiver_username and message['receiverUsername'] == sender_username

                        if sender_is_sender or sender_is_receiver:
                            message.pop('_id')
                            print(message)
                            resume_token = stream.resume_token
                            yield f"""data: {json.dumps({
                                "senderUsername": message['senderUsername'],
                                "receiverUsername": message['receiverUsername'],
                                "text": message['text'],
                                "timestamp": message['timestamp']
                            })}\n\n"""

            except pymongo.errors.PyMongoError:
                if resume_token is None:
                    logging.error('...')
                else:
                    with mydb.messages.watch(
                            pipeline, resume_after=resume_token) as stream:
                        for insert_change in stream:
                            print(insert_change)

    return Response(event_stream(), mimetype="text/event-stream")


@app.route("/messages")
@cross_origin()
def getMessages():
    args = request.args.to_dict()
    sender_username = args["senderUsername"]
    receiver_username = args["receiverUsername"]
    query = {
        '$or': [
            {'$and': [{
                "senderUsername": sender_username,
                "receiverUsername": receiver_username
            }]},
            {'$and': [{
                "senderUsername": receiver_username,
                "receiverUsername": sender_username
            }]}
        ]
    }

    def getMessages_f():
        for x in messagesColumn.find(query):
            x.pop('_id')
            yield f"""data: {json.dumps({
                "senderUsername": x['senderUsername'],
                "receiverUsername": x['receiverUsername'],
                "text": x['text'],
                "timestamp": x['timestamp']
            })}\n\n"""

    return Response(stream_with_context(getMessages_f()), mimetype="text/event-stream")


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
        messagesColumn.insert_one(message)
    except Exception as e:
        print(e)


if __name__ == '__main__':
    time.sleep(5)
    register_kafka_listener('messages', kafka_listener)
    app.run("0.0.0.0", 5000)
