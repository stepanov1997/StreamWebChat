import React, {useEffect, useState} from 'react';
import ConversationList from '../ConversationList';
import MessageList from '../MessageList';
import './Messenger.css';
import config from '../../assets/config.json'

function MyProfile(props) {
    return (<div className={`current-user`}>
        <img className="conversation-photo"
             src={"https://upload.wikimedia.org/wikipedia/commons/f/f7/Administration1.png"} alt="conversation"/>
        <div className="conversation-info">
            <h1 className="conversation-title">{props.currentUser.username}</h1>
        </div>
    </div>)
}

MyProfile.propTypes = {};
export default function Messenger(props) {
    const [actualConversationUser, setActualConversationUser] = useState({})
    const [lastMessage, setLastMessage] = useState([])
    const [messages, setMessages] = useState([])

    const currentUser = props.currentUser
    let events;

    useEffect(() => {
        if (currentUser !== undefined
            && currentUser.username !== undefined
            && actualConversationUser !== undefined
            && actualConversationUser.username !== undefined
        ) {
            setMessages([]);
            if(events!==undefined){
                events.unsubscribe();
                events.close();
            }
            // eslint-disable-next-line react-hooks/exhaustive-deps
            events = new EventSource(`${config.root_url}/chat/${currentUser.username}/${actualConversationUser.username}`);
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
                <MyProfile currentUser={currentUser}/>
                <hr/>
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
