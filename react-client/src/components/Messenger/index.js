import React, {useEffect, useState} from 'react';
import ConversationList from '../ConversationList';
import MessageList from '../MessageList';
import './Messenger.css';
import Toolbar from "../Toolbar";
import ToolbarButton from "../ToolbarButton";

export default function Messenger(props) {
    return (
      <div className="messenger">

        <div className="scrollable sidebar">
          <ConversationList />
        </div>

        <div className="scrollable content">
          <MessageList />
        </div>
      </div>
    );
}
