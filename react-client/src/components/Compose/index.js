import React from 'react';
import './Compose.css';

export default function Compose(props) {
    return (
      <div className="compose">
        <input
          type="text"
          className="compose-input"
          placeholder="Type a message, @name"
          onKeyPress={props.handleKeyPress}
        />

        {
          props.rightItems
        }
      </div>
    );
}
