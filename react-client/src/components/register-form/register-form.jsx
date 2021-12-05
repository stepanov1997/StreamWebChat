import React, {useEffect, useState} from 'react'
import {Button, Form, FormGroup, Label, Input, NavLink} from 'reactstrap';
import {useForm} from 'react-hook-form'
import config from '../../assets/config.json'
import ReactDOM from "react-dom";
import './register-form.css'
import {LoginForm} from "../login-form/login-form";
import {useHistory} from "react-router-dom";


export const RegisterForm = props => {
    const {register, handleSubmit, setValue} = useForm();
    const [errorMessage, setErrorMessage] = useState("");
    const history = useHistory();

    const onSubmit = async data => {
        try {
            let response = await fetch(`${config.root_url}/user/register`, {
                mode: "cors",
                method: "POST",
                headers: {
                    "content-type": "application/json"
                },
                body: JSON.stringify(data)
            })
            if (response.status === 200) {
                history.push('/login');
            } else {
                const message = await response.json();
                setErrorMessage(message.message)
            }
        } catch (e) {
            setErrorMessage(e.message)
        }
        console.log(data)
    }
    const handleChange = (event, name) => {
        setValue(name, event.target.value);
    };
    useEffect(() => {
        register("Username");
        register("Password");
        register("Email");
    }, [register]);

    let handleClickToLogin = e => {
        ReactDOM.render(
            <React.StrictMode>
                <LoginForm/>
            </React.StrictMode>,
            document.getElementById('root'))
    };

    return (<div className={"register-main"}>
            <h1>Registration</h1>
            <hr/>
            <div className={'register-container'}>
                <Form method='POST' onSubmit={handleSubmit(onSubmit)}>
                    <FormGroup>
                        <Label for="username">Username: </Label>
                        <Input onChange={e => handleChange(e, 'Username')} {...register('Username', { required: true })}
                               type="username"
                               name="username" id="username" placeholder="Enter username"/>
                    </FormGroup>
                    <FormGroup>
                        <Label for="password">Password: </Label>
                        <Input onChange={e => handleChange(e, 'Password')} {...register('Password', { required: true })}
                               type="password"
                               name="password" id="password" placeholder="Enter password"/>
                    </FormGroup>
                    <FormGroup>
                        <Label for="email">Email: </Label>
                        <Input onChange={e => handleChange(e, 'Email')} {...register('Email', { required: true })}
                               type="email"
                               name="email" id="email" placeholder="Enter email"/>
                    </FormGroup>
                    <FormGroup>
                        <Button type={'submit'}>Register user</Button>
                    </FormGroup>
                    <FormGroup>
                        <Label className="label">{errorMessage}</Label>
                    </FormGroup>
                    <FormGroup>
                        <NavLink className={"register-link"} onClick={handleClickToLogin}>Login</NavLink>
                    </FormGroup>
                </Form>
            </div>
        </div>

    )
}
