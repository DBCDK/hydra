/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPL v3
 * See license text at https://opensource.dbc.dk/licenses/gpl-3.0
 */

import React from "react";
import {BootstrapTable, TableHeaderColumn} from 'react-bootstrap-table';
import {Button} from 'react-bootstrap';
import superagent from 'superagent';


class HydraStatisticsQueue extends React.Component {
    constructor(props) {
        super(props);
        this.state = {
            statQueueByWorker: null,
            statQueueByAgency: null,
            statQueueByError: null,

            loadingStatQueueByWorker: false,
            loadingStatQueueByAgency: false,
            loadingStatQueueByError: false
        };

        this.getStatQueueByAgency = this.getStatQueueByAgency.bind(this);
        this.getStatQueueByWorker = this.getStatQueueByWorker.bind(this);
        this.getStatQueueByError = this.getStatQueueByError.bind(this);
    }

    componentDidMount() {
        this.getStatQueueByAgency();
        this.getStatQueueByWorker();
        this.getStatQueueByError();
    }

    getStatQueueByWorker() {
        this.setState({loadingStatQueueByWorker: true});
        superagent.get('/api/stats/queueByWorker').end((err, res) => {
            if (err) {
                alert("FEJL!\n\nDen opstod fejl under kald til /api/stats/queueByWorker:\n" + err)
            } else if (res.body === null) {
                alert('FEJL!\n\nDer kom tomt svar tilbage fra api/stats/queueByWorker');
            } else {
                this.setState({
                    statQueueByWorker: res.body,
                    loadingStatQueueByWorker: false
                });
            }
        });
    }

    getStatQueueByAgency() {
        this.setState({loadingStatQueueByAgency: true});
        superagent.get('/api/stats/queueByAgency').end((err, res) => {
            if (err) {
                alert("FEJL!\n\nDen opstod fejl under kald til /api/stats/queueByAgency:\n" + err)
            } else if (res.body === null) {
                alert('FEJL!\n\nDer kom tomt svar tilbage fra api/stats/queueByAgency');
            } else {
                this.setState({
                    statQueueByAgency: res.body,
                    loadingStatQueueByAgency: false
                });
            }
        });
    }

    getStatQueueByError() {
        this.setState({loadingStatQueueByError: true});
        superagent.get('/api/stats/queueByError').end((err, res) => {
            if (err) {
                alert("FEJL!\n\nDen opstod fejl under kald til /api/stats/queueByError:\n" + err)
            } else if (res.body === null) {
                alert('FEJL!\n\nDer kom tomt svar tilbage fra api/stats/queueByError');
            } else {
                this.setState({
                    statQueueByError: res.body,
                    loadingStatQueueByError: false
                });
            }
        });
    }

    render() {
        return (
            <div>
                <div>
                    <h2>Køoversigt - workers</h2>
                    <BootstrapTable
                        data={this.state.statQueueByWorker}
                        striped={true}
                        options={{noDataText: 'Der blev ikke fundet nogen rækker'}}
                        bordered={false}>
                        <TableHeaderColumn dataField='text' isKey>Worker</TableHeaderColumn>
                        <TableHeaderColumn dataField='count'>Antal</TableHeaderColumn>
                        <TableHeaderColumn dataField='date'>Ajourdato</TableHeaderColumn>
                    </BootstrapTable>
                    <br/>
                    <Button
                        onClick={this.getStatQueueByWorker}
                        type='submit'
                        className='btn btn-success'
                        disabled={this.state.loadingStatQueueByWorker}>
                        Genindlæs
                    </Button>
                </div>
                <hr/>
                <div>
                    <h2>Køoversigt - biblioteker</h2>
                    <BootstrapTable
                        data={this.state.statQueueByAgency}
                        striped={true}
                        options={{noDataText: 'Der blev ikke fundet nogen rækker'}}
                        bordered={false}>
                        <TableHeaderColumn dataField='text' isKey>Biblioteksnummer</TableHeaderColumn>
                        <TableHeaderColumn dataField='count'>Antal</TableHeaderColumn>
                        <TableHeaderColumn dataField='date'>Ajourdato</TableHeaderColumn>
                    </BootstrapTable>
                    <br/>
                    <Button
                        onClick={this.getStatQueueByAgency}
                        type='submit'
                        className='btn btn-success'
                        disabled={this.state.loadingStatQueueByAgency}>
                        Genindlæs
                    </Button>
                </div>
                <hr/>
                <div>
                    <h2>Køoversigt - fejl</h2>
                    <BootstrapTable
                        data={this.state.statQueueByError}
                        striped={true}
                        options={{noDataText: 'Der blev ikke fundet nogen rækker'}}
                        bordered={false}>
                        <TableHeaderColumn dataField='text' isKey>Fejlbesked</TableHeaderColumn>
                        <TableHeaderColumn dataField='count'>Antal</TableHeaderColumn>
                        <TableHeaderColumn dataField='date'>Ajourdato</TableHeaderColumn>
                    </BootstrapTable>
                    <br/>
                    <Button
                        onClick={this.getStatQueueByError}
                        type='submit'
                        className='btn btn-success'
                        disabled={this.state.loadingStatQueueByError}>
                        Genindlæs
                    </Button>
                </div>
            </div>
        )
    }
}

export default HydraStatisticsQueue