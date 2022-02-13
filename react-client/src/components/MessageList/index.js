/* eslint-disable */
import React, {useEffect, useState} from 'react';
import Compose from '../Compose';
import Toolbar from '../Toolbar';
import Message from '../Message';
import moment from 'moment';

import './MessageList.css';
import config from "../../assets/config.json";
import showMoreIcon from "../../assets/show-more-icon.png";



export default function MessageList(props) {

    const [counter, setCounter] = useState(1);
    const [loadMoreButtonHidden, setLoadMoreButtonHidden] = useState(false);

    const {messages, setMessages, currentUser, actualConversationUser} = props;
    const MY_USERNAME = currentUser;

    const loadMore = async () => {
        const url = `${config.root_url}/chat/old/${currentUser}/${actualConversationUser.username}?` + new URLSearchParams({
            page: counter,
            size: 10
        });
        const response = await fetch(url, {})
        const oldMessages = await response.json()
        if(oldMessages.length===0){
            setLoadMoreButtonHidden(true);
            return;
        }
        setMessages(oldMessages.map(message => ({
                author: message.senderUsername,
                message: message.text,
                timestamp: message.timestamp
            })).concat(messages).map((m, index) => {
                m.id = index;
                return m;
            }))
        setCounter(counter + 1)
    }

    const renderMessages = () => {
        let i = 0;
        let messageCount = messages.length;
        let tempMessages = [];

        while (i < messageCount) {
            let previous = messages[i - 1];
            let current = messages[i];
            let next = messages[i + 1];
            let isMine = current.author === MY_USERNAME;
            let currentMoment = moment(current.timestamp);
            let prevBySameAuthor = false;
            let nextBySameAuthor = false;
            let startsSequence = true;
            let endsSequence = true;
            let showTimestamp = true;

            if (previous) {
                let previousMoment = moment(previous.timestamp);
                let previousDuration = moment.duration(currentMoment.diff(previousMoment));
                prevBySameAuthor = previous.author === current.author;

                if (prevBySameAuthor && previousDuration.as('hours') < 1) {
                    startsSequence = false;
                }

                if (previousDuration.as('hours') < 1) {
                    showTimestamp = false;
                }
            }

            if (next) {
                let nextMoment = moment(next.timestamp);
                let nextDuration = moment.duration(nextMoment.diff(currentMoment));
                nextBySameAuthor = next.author === current.author;

                if (nextBySameAuthor && nextDuration.as('hours') < 1) {
                    endsSequence = false;
                }
            }

            tempMessages.push(
                <Message
                    key={i}
                    isMine={isMine}
                    startsSequence={startsSequence}
                    endsSequence={endsSequence}
                    showTimestamp={showTimestamp}
                    data={current}
                />
            );

            // Proceed to the next message.
            i += 1;
        }

        return tempMessages;
    }

    async function sendMessage(message) {
        const myHeaders = new Headers();
        myHeaders.append("Content-Type", "application/json");

        let response = await fetch(`${config.root_url}/chat`, {
            method: 'POST',
            headers: myHeaders,
            body: JSON.stringify({
                senderUsername: currentUser,
                receiverUsername: actualConversationUser.username,
                text: message,
                timestamp: new Date().getTime()
            })
        })

        const text = await response.text()
        console.log(text)
    }

    return (
        <div className="message-list">
            <img src={showMoreIcon} className={"show-more-icon"} hidden={loadMoreButtonHidden} alt="Show more" onClick={loadMore}/>

            <div className="message-list-container">{renderMessages()}</div>

            <Compose handleKeyPress={async event => {
                if (event.key === 'Enter') {
                    event.preventDefault();
                    await sendMessage(event.target.value);
                    event.target.value = '';
                }
            }}/>
        </div>
    );
}
