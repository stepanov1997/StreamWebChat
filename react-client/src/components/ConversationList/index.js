import React, {useState, useEffect} from 'react';
import ConversationSearch from '../ConversationSearch';
import ConversationListItem from '../ConversationListItem';
import Toolbar from '../Toolbar';
import ToolbarButton from '../ToolbarButton';
import axios from 'axios';
import config from '../../assets/config.json'

import './ConversationList.css';

export default function ConversationList(props) {
    const [firstLoad, setFirstLoad] = useState(true);
    const [conversations, setConversations] = useState([]);

    useEffect(() => {
        getConversations();
        const interval = setInterval(() => {
            getConversations()
        }, 3000);
        return () => clearInterval(interval);
    }, [firstLoad]);

    const getConversations = () => {
        axios.get(`${config.root_url}/chat/conversations/${props.currentUser.username}`).then(response => {
            if(response.data.length>0 && firstLoad) {
                setFirstLoad(false);
                props.setActualConversationUser(response.data[0]);
            }
            setConversations(response.data)
        });
    }

    return (
        <div className="conversation-list">
            <Toolbar
                title="Messenger"
                leftItems={[
                    <ToolbarButton key="cog" icon="ion-ios-cog"/>
                ]}
                rightItems={[
                    <ToolbarButton key="add" icon="ion-ios-add-circle-outline"/>
                ]}
            />
            <ConversationSearch/>
            {
                conversations.map(conversation =>
                    <ConversationListItem
                        key={`conversation${conversation.id}`}
                        data={conversation}
                        actualConversationUser={props.actualConversationUser}
                        setActualConversationUser={props.setActualConversationUser}
                    />
                )
            }
        </div>
    );
}
