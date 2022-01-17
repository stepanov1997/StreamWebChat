import React, {useEffect, useState} from 'react';
import ConversationList from '../ConversationList';
import MessageList from '../MessageList';
import './Messenger.css';

export default function Messenger(props) {
    const [actualConversationUser, setActualConversationUser] = useState({})
    const [lastMessage, setLastMessage] = useState([])
    const [messages, setMessages] = useState([])


    useEffect(() => {
        const events = new EventSource("http://localhost:8080/chat/1/1");
        events.onmessage = e => {
            const message = JSON.parse(e.data);
            setLastMessage(message.text);
            setMessages(prevState => [...prevState, {
                id: message.id,
                author: message.senderUsername,
                message: message.text,
                timestamp: new Date().getTime()
            }]);
        }
    }, [])

    return (
        <div className="scrollable messenger">

            <div className="sidebar">
                <ConversationList actualConversationUser={actualConversationUser}
                                  setActualConversationUser={setActualConversationUser}
                                  lastMessage={lastMessage}
                />
            </div>

            <div className="content">
                <MessageList messages={messages}/>
            </div>
        </div>
    );
}
