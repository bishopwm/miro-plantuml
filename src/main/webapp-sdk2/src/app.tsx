import * as React from 'react';
import ReactDOM from 'react-dom';
import cn from 'classnames'
import axios, {AxiosResponse} from "axios";

function App() {
    // const [isRendering, setRendering] = React.useState(false);
    const [isRendering] = React.useState(false);

    // async function addSomeElements() {
    //     setRendering(true);
    //
    //     const shape1 = await miro.board.createShape({
    //         content: 'Hello, World!',
    //         shape: 'cloud'
    //     });
    //
    //     await miro.board.createShape({
    //         content: 'See ya',
    //         shape: 'hexagon',
    //         x: shape1.x + 200,
    //         y: shape1.y
    //     });
    //
    //     setRendering(false);
    //     await miro.board.ui.closeModal();
    // }

    async function getSelfUser() {
        const backendUrl = new URL(window.location.href)
        backendUrl.pathname = "/get-self-user"

        const token = await miro.board.getIdToken()
        //resetAuthState()
        axios.get(backendUrl.href,
            {
                headers: {
                    "X-Miro-Token": token
                }
            })
            .then((response: AxiosResponse) => {
                console.error(`callBackend: "${response.data.name}"`)
                //miro.v1.showNotification(`callBackend: user name="${response.data.name}"`)
            })
            .catch((error) => {
                let message = error.message
                if (error.response) {
                    message = JSON.stringify(error.response.data)
                } else if (error.request) {
                    message = "request: " + error.request
                }

                console.error(`callBackend error: "${message}"`)
                alert(`callBackend: error "${message}"`)
            });
    }

    return (
        <div className="grid wrapper">
            <h1 className="h1">PlantUML</h1>
            <div className="cs1 ce12">
                <a className="link link-primary" href="https://plantuml.com/" target="_blank">Documentation</a>
            </div>
            <div className="cs1 ce12 form-group">
                <textarea className="textarea" placeholder="Code" rows={10} spellCheck={false}></textarea>
            </div>
            <div className="cs1 ce12">
                <button
                    className={cn('button', 'button-primary', {
                        'button-loading': isRendering
                    })}
                    // onClick={addSomeElements}
                    onClick={getSelfUser}
                    type="button"
                    disabled={isRendering}
                >
                    {isRendering ? '' : 'Render'}
                </button>
            </div>
        </div>
    );
}

ReactDOM.render(<App/>, document.getElementById('root'));
