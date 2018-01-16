/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPL v3
 * See license text at https://opensource.dbc.dk/licenses/gpl-3.0
 */

import React from "react"
import {Tab, Tabs} from "react-bootstrap"
import HydraQueueGUI from "./hydra-queue"
import HydraStatisticsGUI from "./hydra-statistics"
import superagent from "superagent"

const TAB_QUEUE = 2;
const TAB_STATISTICS = 3;

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
    }

    handleSelect(key) {
        this.setState({key});
    }

    renderQueue() {
        if (this.state.key === TAB_QUEUE) {
            return (<div><p/><HydraQueueGUI/></div>);
        }
    }

    renderStatistics() {
        if (this.state.key === TAB_STATISTICS) {
            return (<div><p/><HydraStatisticsGUI/></div>);
        }
    }

    getInstanceName() {
        superagent.get('/api/hydra/instance').end((err, res) => {
            let response = res.body;

            if (response === null) {
                alert('FEJL!\n\nDer kom tomt svar tilbage fra api/hydra/instance');
            } else {
                this.setState({instanceName: response.value})
            }
        });
    }

    getHeader() {
        if (this.state.instanceName === null) {
            this.getInstanceName();
            return 'RawRepo HYDRA';
        } else {
            return 'RawRepo HYDRA - ' + this.state.instanceName;
        }
    }

    render() {
        return (
            <div className="container-fluid" id="root-render">
                <h1 title='Hydra: Multi headed beast guarding the entrance to the Underworld'>{this.getHeader()}</h1>
                <Tabs activeKey={this.state.key}
                      onSelect={this.handleSelect}
                      id="controlled-tab-example">
                    <Tab eventKey={TAB_QUEUE} title="Køpålæggelse">
                        {this.renderQueue()}
                    </Tab>
                    <Tab eventKey={TAB_STATISTICS} title="Oversigt">
                        {this.renderStatistics()}
                    </Tab>
                </Tabs>
            </div>
        )
    }
}

export default HydraMain;
