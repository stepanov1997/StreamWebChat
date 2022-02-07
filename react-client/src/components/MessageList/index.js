import React, {useEffect, useState} from 'react';
import Compose from '../Compose';
import Toolbar from '../Toolbar';
import ToolbarButton from '../ToolbarButton';
import Message from '../Message';
import moment from 'moment';

import './MessageList.css';
import config from "../../assets/config.json";


export default function MessageList(props) {

    const {messages, setMessages, currentUser, actualConversationUser} = props;

    const MY_USERNAME = currentUser;

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
            <Toolbar
                title={actualConversationUser.username}
                rightItems={[
                    <ToolbarButton key="info" icon="ion-ios-information-circle-outline"/>,
                    <ToolbarButton key="video" icon="ion-ios-videocam"/>,
                    <ToolbarButton key="phone" icon="ion-ios-call"/>
                ]}
            />

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
