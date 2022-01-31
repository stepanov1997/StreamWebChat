import React, {useEffect, useState} from 'react';
import ConversationList from '../ConversationList';
import MessageList from '../MessageList';
import './Messenger.css';
import config from '../../assets/config.json'

export default function Messenger(props) {
    const [actualConversationUser, setActualConversationUser] = useState({})
    const [lastMessage, setLastMessage] = useState([])
    const [messages, setMessages] = useState([])

    const currentUser = props.currentUser

    useEffect(() => {
        if (currentUser !== undefined && actualConversationUser !== undefined) {
            const events = new EventSource(`http://${config.root_url}/chat/${currentUser.username}/${actualConversationUser.username}`);
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
        }
    }, [actualConversationUser])

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
                <MessageList messages={messages}
                             setMessages={setMessages}
                             currentUser={currentUser.username}
                             actualConversationUser={actualConversationUser}
                />
            </div>
        </div>
    );
}
