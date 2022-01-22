import React, {useState, useEffect} from 'react';
import ConversationSearch from '../ConversationSearch';
import ConversationListItem from '../ConversationListItem';
import Toolbar from '../Toolbar';
import ToolbarButton from '../ToolbarButton';
import axios from 'axios';
import config from '../../assets/config.json'

import './ConversationList.css';

export default function ConversationList(props) {
    const [conversations, setConversations] = useState([]);
    useEffect(() => {
        getConversations()
    }, [])

    const getConversations = () => {
        axios.get(`${config.root_url}/chat/conversations/${props.currentUser.username}`).then(response => {
            console.log(response.data)
            setConversations([...conversations, ...response.data])
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
                        key={`conversation${conversation.username}`}
                        data={conversation}
                    />
                )
            }
        </div>
    );
}
