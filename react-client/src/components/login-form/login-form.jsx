import React, {useEffect, useState} from 'react'
import {Button, Form, FormGroup, Label, Input, NavLink} from 'reactstrap';
import config from '../../assets/config.json'
import './login-form.css'
import {useHistory} from 'react-router-dom';
import {useForm} from "react-hook-form";

export const LoginForm = props => {
    const {register, handleSubmit, setValue, getValues} = useForm();
    const [errorMessage, setErrorMessage] = useState("");
    const history = useHistory();

    const onSubmit = async e => {
        e.preventDefault()
        try {
            let requestUrl = `${config.root_url}/user/register`;
            let response = await fetch(requestUrl, {
                mode: "cors",
                method: "POST",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify(getValues())
            })
            if (response.status === 200) {
                const data = await response.json();
                if (data && data.status === 200) {
                    props.setCurrentUser({username: data.data.username})
                    history.push("/chat")
                }else{
                    window.alert(data.message)
                }
            } else {
                const message = await response.json();
                setErrorMessage("Cannot log")
            }
        } catch (e) {
            console.log(e)
            setErrorMessage("Cannot connect with server..")
        }
    }
    useEffect(() => {
        register("Username");
        register("Password");
        register("Email");
    }, [register]);
    const handleChange = (event, name) => {
        setValue(name, event.target.value);
    };
    let handleClickToRegister = e => {
        history.push('/register');
    };
    return (<div className={"login-main"}>
            <h1>Login</h1>
            <hr/>
            <div className={'login-container'}>
                <Form method='POST' onSubmit={onSubmit}>
                    <FormGroup  className={"mt-5"}>
                        <Label for="username">Username: </Label>
                        <Input type="username" onChange={e => handleChange(e, 'Username')} className={".message .bubble-container .bubble"}
                               name="username" id="username" placeholder="Enter username" value={props.username}/>
                    </FormGroup>
                    <FormGroup>
                        <Label for="password">Password: </Label>
                        <Input type="password" onChange={e => handleChange(e, 'Password')} className={".message .bubble-container .bubble"}
                               name="password" id="password" placeholder="Enter password"/>
                    </FormGroup>
                    <FormGroup className={"mt-5"}>
                        <Button type={'submit'}>Login user</Button>
                        <NavLink className={"register-link"} onClick={handleClickToRegister}>Register</NavLink>
                    </FormGroup>
                    {props.username ? (<FormGroup>
                            <Label className="label textSuccess" >{props.username}</Label>
                        </FormGroup>) :
                        (<FormGroup>
                            <Label className="label">{errorMessage}</Label>
                        </FormGroup>)}

                    {props.logout ? <FormGroup>
                        <Label className="label textSuccess">{"You successfully logged out"}</Label>
                    </FormGroup> : ""}
                </Form>
            </div>
        </div>
    )




}
