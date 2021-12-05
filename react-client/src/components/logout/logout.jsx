import React, {useEffect} from 'react'
import {Redirect, useHistory} from 'react-router-dom';

export const Logout = props => {
    const history = useHistory();
    useEffect(() => {
        props.setCurrentUser("");
    }, [history, props])

    return <Redirect to={{ pathname: '/login', state: { from: props.location } }} />;;
}
