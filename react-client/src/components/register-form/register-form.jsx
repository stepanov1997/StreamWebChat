import React, {useEffect, useState} from 'react'
import {Button, Form, FormGroup, Label, Input, NavLink} from 'reactstrap';
import config from '../../assets/config.json'
import './register-form.css'
import {useHistory} from 'react-router-dom';
import {useForm} from "react-hook-form";

export const RegisterForm = props => {
    const {register, setValue, getValues} = useForm();
    const [errorMessage, setErrorMessage] = useState("");
    const history = useHistory();

    const onSubmit = async e => {
        e.preventDefault()

        try {
            const data = getValues()
            if (data.password !== data.passwordConfirm) {
                setErrorMessage("passwords do not match")
                return
            } else {
                setErrorMessage("")
            }
            const obj = {
                name: getValues().name,
                surname: getValues().surname,
                username: getValues().username,
                password: getValues().password
            }
            console.log(obj)
            const myHeaders = new Headers();
            myHeaders.append("Content-Type", "application/json");

            let requestUrl = `${config.root_url}/user/register`;
            let response = await fetch(requestUrl, {
                method: 'POST',
                headers: myHeaders,
                body: JSON.stringify(obj)
            })

            if (response.status === 201 || response.status === 200) {
                const data = await response.json();
                if (data) {
                    console.log(data)
                    alert("User created successfully")
                    setTimeout(() => {
                        history.push('/login')
                    }, 2000)
                } else {
                    window.alert(data.message)
                }
            } else {
                setErrorMessage("Cannot register. Please try again.")
            }
        } catch (e) {
            console.log(e.stack)
            setErrorMessage("Cannot connect with server..")
        }
    }
    useEffect(() => {
        register("name");
        register("surname");
        register("username");
        register("password");
    }, [register]);
    const handleChange = (event, name) => {
        setValue(name, event.target.value);
    };
    let handleClickToLogin = e => {
        history.push('/login');
    };
    return (<div className={"register-main"}>
            <h1>Register</h1>
            <hr/>
            <div className={'register-container'}>
                <Form method='POST' onSubmit={onSubmit}>
                    <FormGroup className={"mt-4"}>
                        <Label for="name">Name: </Label>
                        <Input type="name" onChange={e => handleChange(e, 'name')}
                               className={".message .bubble-container .bubble"}
                               name="name" id="name" placeholder="Enter name" value={props.name}/>
                    </FormGroup>
                    <FormGroup className={"mt-1"}>
                        <Label for="surname">Surname: </Label>
                        <Input type="surname" onChange={e => handleChange(e, 'surname')}
                               className={".message .bubble-container .bubble"}
                               name="surname" id="surname" placeholder="Enter surname" value={props.surname}/>
                    </FormGroup>
                    <FormGroup className={"mt-5"}>
                        <Label for="username">Username: </Label>
                        <Input type="username" onChange={e => handleChange(e, 'username')}
                               className={".message .bubble-container .bubble"}
                               name="username" id="username" placeholder="Enter username" value={props.username}/>
                    </FormGroup>
                    <hr/>
                    <FormGroup>
                        <Label for="password">Password: </Label>
                        <Input type="password" onChange={e => handleChange(e, 'password')}
                               className={".message .bubble-container .bubble"}
                               name="password" id="password" placeholder="Enter password"/>
                    </FormGroup>
                    <FormGroup>
                        <Label for="passwordAgain">Password: </Label>
                        <Input type="password" onChange={e => handleChange(e, 'passwordConfirm')}
                               className={".message .bubble-container .bubble"}
                               name="passwordConfirm" id="passwordConfirm" placeholder="Confirm password"/>
                    </FormGroup>
                    <FormGroup className={"mt-5"}>
                        <Button type={'submit'}>Register user</Button>
                        <NavLink className={"login-link"} onClick={handleClickToLogin}>Login</NavLink>
                    </FormGroup>
                    {props.username ? (<FormGroup>
                            <Label className="label textSuccess">{props.username}</Label>
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
