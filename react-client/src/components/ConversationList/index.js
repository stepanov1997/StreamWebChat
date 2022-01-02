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
  },[])

 const getConversations = () => {
    axios.get(`${config.root_url}/user/getAll`).then(response => {
        console.log(response.data)
        let newConversations = response.data.map(result => {
          return {
            photo: "https://lh3.googleusercontent.com/ogw/ADea4I4OKqPcRdOEXv_3YD-HSz9c3Kd_jTbn1Rx5o1BfLSA=s32-c-mo",
            name: result.username,
            text: result.password
          };
        });
        setConversations([...conversations, ...newConversations])
    });
  }

    return (
      <div className="conversation-list">
        <Toolbar
          title="Messenger"
          leftItems={[
            <ToolbarButton key="cog" icon="ion-ios-cog" />
          ]}
          rightItems={[
            <ToolbarButton key="add" icon="ion-ios-add-circle-outline" />
          ]}
        />
        <ConversationSearch />
        {
          conversations.map(conversation =>
            <ConversationListItem
              key={conversation.name}
              data={conversation}
            />
          )
        }
      </div>
    );
}
