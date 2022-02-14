import React from 'react';
import moment from 'moment';
import './Message.css';

import successIcon from '../../assets/success-logo.png';
import failedIcon from '../../assets/failed-logo.png';
import sendingIcon from '../../assets/sending-logo.gif';

export default function Message(props) {
    const {
        data,
        isMine,
        startsSequence,
        endsSequence,
        showTimestamp
    } = props;

    const friendlyTimestamp = moment(data.timestamp).format('LLLL');

    function writeState() {
        switch (data.state) {
            case 'sending':
                return <img width={15} height={15} src={sendingIcon} alt="Sending..."/>
            case 'sent':
                return <img width={15} height={15} src={successIcon} alt="Sent."/>
            case 'failed':
                return <img width={15} height={15} src={failedIcon} alt="Failed."/>
            default:
                return null;
        }
    }

    return (
        <div className={[
            'message',
            `${isMine ? 'mine' : ''}`,
            `${startsSequence ? 'start' : ''}`,
            `${endsSequence ? 'end' : ''}`
        ].join(' ')}>
            {
                showTimestamp &&
                <div className="timestamp">
                    {friendlyTimestamp}
                </div>
            }

            <div className="bubble-container">
                <div className="bubble" title={friendlyTimestamp}>
                    {writeState()} {data.message}
                </div>
            </div>
        </div>
    );
}
