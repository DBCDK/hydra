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

            loadingErrorsByWorker: false
        };

        this.fetchErrorsByWorkers = this.fetchErrorsByWorkers.bind(this);
    }

    componentDidMount() {
        this.fetchErrorsByWorkers();
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

    render() {
        return (
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
        )
    }
}

export default HydraErrors;
