/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPL v3
 * See license text at https://opensource.dbc.dk/licenses/gpl-3.0
 */

import React from "react";
import {Tab, Tabs} from "react-bootstrap";
import HydraEnqueue from "./hydra-enqueue";
import HydraStatistics from "./hydra-statistics";
import HydraErrors from "./hydra-errors";
import superagent from "superagent";

const TAB_QUEUE = 2;
const TAB_STATISTICS = 3;
const TAB_ERRORS = 4;

class HydraMain extends React.Component {
    constructor(props) {
        super(props);
        this.state = {
            key: TAB_QUEUE,
            instanceName: null
        };

        this.handleSelect = this.handleSelect.bind(this);
        this.renderQueue = this.renderQueue.bind(this);
        this.renderStatistics = this.renderStatistics.bind(this);
        this.getInstanceName = this.getInstanceName.bind(this);
    }

    componentDidMount() {
        this.getInstanceName();
    }

    handleSelect(key) {
        this.setState({key: key});
    }

    renderQueue() {
        if (this.state.key === TAB_QUEUE) {
            return (<div><p/><HydraEnqueue/></div>);
        }
    }

    renderStatistics() {
        if (this.state.key === TAB_STATISTICS) {
            return (<div><p/><HydraStatistics/></div>);
        }
    }

    renderErrors() {
        if (this.state.key === TAB_ERRORS) {
            return (<div><p/><HydraErrors/></div>);
        }
    }

    getInstanceName() {
        superagent.get('/api/hydra/instance').end((err, res) => {
            if (err) {
                alert('FEJL!\n\nDer skete en fejl i forbindelse med kald til /api/hydra/instance: \n' + err)
            } else if (res.body === null) {
                alert('FEJL!\n\nDer kom tomt svar tilbage fra api/hydra/instance');
            } else {
                this.setState({instanceName: 'Hydra Service - ' + res.body.value});
            }
        });
    }

    render() {
        return (
            <div className="container-fluid" id="root-render">
                <h1 title='Hydra: Multi headed beast guarding the entrance to the Underworld'>{this.state.instanceName}</h1>
                <Tabs activeKey={this.state.key}
                      onSelect={this.handleSelect}
                      id="controlled-tab-example">
                    <Tab eventKey={TAB_QUEUE} title="Køpålæggelse">
                        {this.renderQueue()}
                    </Tab>
                    <Tab eventKey={TAB_STATISTICS} title="Oversigt">
                        {this.renderStatistics()}
                    </Tab>
                    <Tab eventKey={TAB_ERRORS} title="Fejloversigt">
                        {this.renderErrors()}
                    </Tab>
                </Tabs>
            </div>
        )
    }
}

export default HydraMain;
