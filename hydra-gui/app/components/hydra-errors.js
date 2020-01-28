/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPL v3
 * See license text at https://opensource.dbc.dk/licenses/gpl-3.0
 */

import React from 'react';
import {BootstrapTable, TableHeaderColumn} from 'react-bootstrap-table';
import {Button} from 'react-bootstrap';
import superagent from 'superagent';

class HydraErrors extends React.Component {
    constructor(props) {
        super(props);
        this.state = {
            errorsByWorker: null,
            errorsByType: null,

            loadingErrorsByWorker: false,
            loadingErrorsByType: false
        };

        this.fetchErrorsByWorkers = this.fetchErrorsByWorkers.bind(this);
        this.fetchErrorsByType = this.fetchErrorsByType.bind(this);
    }

    componentDidMount() {
        this.fetchErrorsByWorkers();
        this.fetchErrorsByType();
    }

    fetchErrorsByWorkers() {
        this.setState({loadingErrorsByWorker: true});
        superagent.get('/api/errors/byWorker').end((err, res) => {
            if (err) {
                alert("FEJL!\n\nDer opstod fejl under kald til /api/errors/byWorker:\n" + err)
            } else if (res.body === null) {
                alert('FEJL!\n\nDer kom tomt svar tilbage fra api/errors/byWorker');
            } else {
                this.setState({
                    errorsByWorker: res.body,
                    loadingErrorsByWorker: false
                });
            }
        });
    }

    fetchErrorsByType() {
        this.setState({loadingErrorsByType: true});
        superagent.get('/api/errors/byType').end((err, res) => {
            if (err) {
                alert("FEJL!\n\nDer opstod fejl under kald til /api/errors/byType:\n" + err)
            } else if (res.body === null) {
                alert('FEJL!\n\nDer kom tomt svar tilbage fra api/errors/byType');
            } else {
                this.setState({
                    errorsByType: res.body,
                    loadingErrorsByType: false
                });
            }
        });
    }

    render() {
        return (
            <div>
                <div>
                    <h2>Fejloversigt - workers</h2>
                    <BootstrapTable
                        data={this.state.errorsByWorker}
                        striped={true}
                        options={{noDataText: 'Der blev ikke fundet nogen rækker'}}
                        bordered={false}>
                        <TableHeaderColumn dataField='worker' isKey>Worker</TableHeaderColumn>
                        <TableHeaderColumn dataField='count'>Antal</TableHeaderColumn>
                        <TableHeaderColumn dataField='date'>Ajourdato</TableHeaderColumn>
                    </BootstrapTable>
                    <br/>
                    <Button
                        onClick={this.fetchErrorsByWorkers}
                        type='submit'
                        className='btn btn-success'
                        disabled={this.state.loadingErrorsByWorker}>
                        Genindlæs
                    </Button>
                </div>
                <hr/>
                <div>
                    <h2>Fejloversigt - fejltype (de maksimalt 1000 senest registrerede typer)</h2>
                    <BootstrapTable
                        data={this.state.errorsByType}
                        striped={true}
                        options={{noDataText: 'Der blev ikke fundet nogen rækker'}}
                        bordered={false}>
                        <TableHeaderColumn dataField='worker' isKey>Worker</TableHeaderColumn>
                        <TableHeaderColumn dataField='error' width='50%' tdStyle={ { whiteSpace: 'normal' } }>Type</TableHeaderColumn>
                        <TableHeaderColumn dataField='count'>Antal</TableHeaderColumn>
                        <TableHeaderColumn dataField='date'>Ajourdato</TableHeaderColumn>
                    </BootstrapTable>
                    <br/>
                    <Button
                        onClick={this.fetchErrorsByType}
                        type='submit'
                        className='btn btn-success'
                        disabled={this.state.loadingErrorsByType}>
                        Genindlæs
                    </Button>
                </div>
            </div>
        )
    }
}

export default HydraErrors;
