/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPL v3
 * See license text at https://opensource.dbc.dk/licenses/gpl-3.0
 */

import React from 'react';
import {BootstrapTable, TableHeaderColumn} from 'react-bootstrap-table';
import {Button} from 'react-bootstrap';
import superagent from 'superagent';

class HydraStatistics extends React.Component {
    constructor(props) {
        super(props);
        this.state = {
            statRecordByAgency: null,
            statQueueByWorker: null,
            statQueueByAgency: null,
            statQueueByError: null,

            loadingStatRecordByAgency: false,
            loadingStatQueueByWorker: false,
            loadingStatQueueByAgency: false,
            loadingStatQueueByError: false
        };

        this.printRecordTable = this.printRecordTable.bind(this);
        this.printQueueTable = this.printQueueTable.bind(this);

        this.getStatRecordByAgency = this.getStatRecordByAgency.bind(this);
        this.getStatQueueByAgency = this.getStatQueueByAgency.bind(this);
        this.getStatQueueByWorker = this.getStatQueueByWorker.bind(this);
        this.getStatQueueByError = this.getStatQueueByError.bind(this);
    }

    componentDidMount(props) {
        this.getStatRecordByAgency();
        this.getStatQueueByAgency();
        this.getStatQueueByWorker();
        this.getStatQueueByError();
    }

    getStatRecordByAgency() {
        this.setState({loadingStatRecordByAgency: true});
        superagent.get('/api/stats/recordByAgency').end((err, res) => {
            let response = res.body;

            if (response === null) {
                alert('FEJL!\n\nDer kom tomt svar tilbage fra api/stats/recordByAgency');
            } else {
                this.setState({
                    statRecordByAgency: response,
                    loadingStatRecordByAgency: false
                });
            }
        });
    }

    getStatQueueByWorker() {
        this.setState({loadingStatQueueByWorker: true});
        superagent.get('/api/stats/queueByWorker').end((err, res) => {
            let response = res.body;

            if (response === null) {
                alert('FEJL!\n\nDer kom tomt svar tilbage fra api/stats/queueByWorker');
            } else {
                this.setState({
                    statQueueByWorker: response,
                    loadingStatQueueByWorker: false
                });
            }
        });
    }

    getStatQueueByAgency() {
        this.setState({loadingStatQueueByAgency: true});
        superagent.get('/api/stats/queueByAgency').end((err, res) => {
            let response = res.body;

            if (response === null) {
                alert('FEJL!\n\nDer kom tomt svar tilbage fra api/stats/queueByAgency');
            } else {
                this.setState({
                    statQueueByAgency: response,
                    loadingStatQueueByAgency: false
                });
            }
        });
    }

    getStatQueueByError() {
        this.setState({loadingStatQueueByError: true});
        superagent.get('/api/stats/queueByError').end((err, res) => {
            let response = res.body;

            if (response === null) {
                alert('FEJL!\n\nDer kom tomt svar tilbage fra api/stats/queueByError');
            } else {
                this.setState({
                    statQueueByError: response,
                    loadingStatQueueByError: false
                });
            }
        });
    }

    render() {
        return (
            <div id='order-grid-row' className='row'>
                <div id='order-grid-row-left' className='col-lg-6'>
                    {this.printRecordTable()}
                </div>
                <div id='order-grid-row-right' className='col-lg-6'>
                    {this.printQueueTable()}
                </div>
            </div>
        )
    }

    printRecordTable() {
        return (
            <div>
                <h2>Postoversigt</h2>
                <BootstrapTable
                    data={this.state.statRecordByAgency}
                    striped={true}
                    options={{noDataText: 'Der blev ikke fundet nogen rækker'}}
                    bordered={false}>
                    <TableHeaderColumn dataField='agencyId' isKey>Biblioteksnummer</TableHeaderColumn>
                    <TableHeaderColumn dataField='marcxCount'>Antal marcxchange poster</TableHeaderColumn>
                    <TableHeaderColumn dataField='enrichmentCount'>Antal enrichment poster</TableHeaderColumn>
                </BootstrapTable>
                <br/>
                <Button
                    onClick={this.getStatRecordByAgency}
                    type='submit'
                    className='btn btn-success'
                    disabled={this.state.loadingStatRecordByAgency}>
                    Genindlæs
                </Button>
            </div>
        );
    }

    printQueueTable() {
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
                        <TableHeaderColumn dataField='text' isKey>Worker</TableHeaderColumn>
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
                        <TableHeaderColumn dataField='text' isKey>Worker</TableHeaderColumn>
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

export default HydraStatistics;
