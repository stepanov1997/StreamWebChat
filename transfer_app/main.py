import json
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

BOOTSTRAP_SERVERS = ['localhost:9092']
myclient = pymongo.MongoClient(host=["127.0.10.1", "127.0.10.2", "127.0.10.3"], port=27017)
mydb = myclient["test"]
messagesColumn = mydb["messages"]
usersColumn = mydb["users"]


@app.route("/stream")
@cross_origin()
def stream_f():
    args = request.args.to_dict()
    sender_id = int(args["senderId"])
    receiver_id = int(args["receiverId"])

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
                        if message['senderId'] == sender_id and message['receiverId'] == receiver_id:
                            message.pop('_id')
                            print(message)
                            resume_token = stream.resume_token
                            yield f"""data: {json.dumps({
                                "id": message['id'],
                                "senderUsername": usersColumn.find_one({'_id': sender_id})['username'],
                                "receiverUsername": usersColumn.find_one({'_id':receiver_id})['username'],
                                "text": message['text']
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
    sender_id = int(args["senderId"])
    receiver_id = int(args["receiverId"])
    query = {'$and': [{"senderId": sender_id, "receiverId": receiver_id}]}

    def getMessages_f():
        for x in messagesColumn.find(query):
            x.pop('_id')
            yield f"""data: {json.dumps({
                "id": x['id'],
                "senderUsername": usersColumn.find_one({'_id': sender_id})['username'],
                "receiverUsername": usersColumn.find_one({'_id': receiver_id})['username'],
                "text": x['text']
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
