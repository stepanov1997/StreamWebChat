import './App.css';
import React from 'react'
import {useSessionStorage} from "../session-storage-hook/session-storage-hook";
import {Header} from "../header/header";
import {BrowserRouter as Router} from "react-router-dom";
import {AppSwitch} from "../switch/switch";

const App = () => {
    const [currentUser, setCurrentUser] = useSessionStorage("currentUser", undefined);
    return (
        <div className="App">
            <Router>
                <div className={"header-wrapper"}>
                    <Header currentUser={currentUser}/>
                </div>
                <div className={"content"}>
                    <AppSwitch currentUser={currentUser}
                               setCurrentUser={setCurrentUser}/>
                </div>
            </Router>
        </div>
    );
}

export default App;
