import React, {useEffect, useState} from 'react';
import ConversationList from '../ConversationList';
import MessageList from '../MessageList';
import './Messenger.css';

export default function Messenger(props) {
    const [actualConversationUser, setActualConversationUser] = useState({})
    const [lastMessage, setLastMessage] = useState([])
    const [messages, setMessages] = useState([])

    const currentUser = props.currentUser

    useEffect(() => {
        const events = new EventSource("http://localhost:8080/chat/1/1");
        events.onmessage = e => {
            const message = JSON.parse(e.data);
            setLastMessage(message.text);
            setMessages(prevState => [...prevState, {
                id: message.id,
                author: message.senderUsername,
                message: message.text,
                timestamp: message.timestamp
            }]);
        }
    }, [])

    return (
        <div className="messenger">

            <div className="scrollable sidebar">
                <ConversationList currentUser={currentUser}
                                  actualConversationUser={actualConversationUser}
                                  setActualConversationUser={setActualConversationUser}
                                  lastMessage={lastMessage}
                />
            </div>

            <div className="scrollable content">
                <MessageList messages={messages}/>
            </div>
        </div>
    );
}
