import React, {useEffect} from 'react';
import shave from 'shave';

import './ConversationListItem.css';
import moment from "moment";

export default function ConversationListItem(props) {
  useEffect(() => {
    shave('.conversation-snippet', 200);
  })

    const { userId, username, isOnline, exists, timestamp, lastMessage } = props.data;

    return (
      <div className="conversation-list-item" onClick={_ => props.setActualConversationUser(props.data)}>
        <img className="conversation-photo" src={"https://upload.wikimedia.org/wikipedia/commons/f/f7/Administration1.png"} alt="conversation" />
        <div className="conversation-info">
          <h1 className="conversation-title">{ username } { isOnline? ("🟢") : ("⚪")}</h1>
            { exists && <p className="conversation-snippet">{ lastMessage } ({moment(timestamp).lang("bs").fromNow()})</p> }
        </div>
      </div>
    );
}
