/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPL v3
 * See license text at https://opensource.dbc.dk/licenses/gpl-3.0
 */

import React from "react";
import {Tab, Tabs} from "react-bootstrap";
import HydraQueueGUI from "./hydra-queue-gui"
import HydraStatisticsGUI from "./hydra-statistics-gui"

const TAB_QUEUE = 2;
const TAB_STATISTICS = 3;

class HydraGUI extends React.Component {
    constructor(props) {
        super(props);
        this.state = {
            key: TAB_QUEUE
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

    render() {
        return (
            <div className="container-fluid" id="root-render">
                <h2>Server: localhost</h2>
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

export default HydraGUI;
