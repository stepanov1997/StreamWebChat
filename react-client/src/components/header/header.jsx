import React from 'react'
import {Link} from "react-router-dom";
import img from '../../assets/svg.svg'
import './header.css'

export const Header = props => {
    return (
        <div className={"header"}>
            <div className={"logoWrapper"}>
                <Link to="/"><img src={img} alt={"StreamChat"} className={"logo"} /></Link>
                {/*<Logo height={100} width={100} fill='white' stroke='white'/>*/}
            </div>
            <div className={"menu"}>
                <div>
                    <Link to="/"><i className="fas fa-home text-white"/> Home</Link>
                </div>
                {props.currentUser && props.currentUser.token ? "" : (<div>
                    <Link to="/login"><i className="fas fa-sign-in-alt text-white"/> Login</Link>
                </div>)}
                {props.currentUser && props.currentUser.token ? "" : (<div>
                    <Link to="/register"><i className="fas fa-user text-white"/> Registration</Link>
                </div>)}
                {props.currentUser && props.currentUser.token ? (<div>
                    <Link to="/chat"><i className="fas fa-comments text-white"/> Chat</Link>
                </div>) : ""}
                {props.currentUser && props.currentUser.token ? (<div>
                    <Link to="/certificate-store"><i className="fas fa-stamp text-white"/> Certificate store</Link>
                </div>) : ""}
                {props.currentUser && props.currentUser.token ? (<div>
                    <Link to="/logout"><i className="fas fa-sign-out-alt text-white"/> Logout</Link>
                </div>) : ""}
            </div>
        </div>
    )
}
