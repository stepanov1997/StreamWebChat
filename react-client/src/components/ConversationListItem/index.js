import React, {useEffect} from 'react';
import shave from 'shave';

import img from '../../assets/user.png'
import './ConversationListItem.css';
import moment from "moment";

export default function ConversationListItem(props) {
    useEffect(() => {
        shave('.conversation-snippet', 200);
    })

    const capitalize = (str) => str?.charAt(0)?.toUpperCase() + str?.substring(1);

    const {name, surname, username, isOnline, exists, timestamp, lastMessage} = props.data;
    const isSelected = username === props.actualConversationUser.username ? "-selected" : ""

    return (
        <div className={`conversation-list-item${isSelected}`} onClick={_ => props.setActualConversationUser(props.data)}>
            <img className="conversation-photo"
                 src={img} alt="conversation"/>
            <div className="conversation-info">
                <h1 className="conversation-title">{capitalize(name)} {capitalize(surname)} {isOnline ? ("🟢") : ("⚪")}</h1>
                {exists &&
                    <p className="conversation-snippet">{lastMessage} ({moment(timestamp).locale("bs").fromNow()})</p>}
            </div>
        </div>
    );
}
