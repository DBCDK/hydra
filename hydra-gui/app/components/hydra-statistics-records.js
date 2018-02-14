/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPL v3
 * See license text at https://opensource.dbc.dk/licenses/gpl-3.0
 */

import React from 'react';
import {BootstrapTable, TableHeaderColumn} from 'react-bootstrap-table';
import {Button} from 'react-bootstrap';
import superagent from 'superagent';

class HydraStatisticsRecords extends React.Component {
    constructor(props) {
        super(props);
        this.state = {
            recordsSummaryData: null,

            loadingRecordsSummary: false
        };

        this.getRecordsSummary = this.getRecordsSummary.bind(this);
    }

    componentDidMount(props) {
        this.getRecordsSummary();
    }

    getRecordsSummary() {
        this.setState({loadingRecordsSummary: true});
        superagent.get('/api/stats/recordsSummary').end((err, res) => {
            if (err) {
                alert("FEJL!\n\nDer opstod fejl under kald til /api/stats/recordsSummary:\n" + err)
            } else if (res.body === null) {
                alert('FEJL!\n\nDer kom tomt svar tilbage fra api/stats/recordsSummary');
            } else {
                this.setState({
                    recordsSummaryData: res.body,
                    loadingRecordsSummary: false
                });
            }
        });
    }

    render() {
        return (
            <div>
                <h2>Postoversigt</h2>
                <Button
                    onClick={this.getRecordsSummary}
                    type='submit'
                    className='btn btn-success'
                    disabled={this.state.loadingRecordsSummary}>
                    Genindlæs
                </Button>
                <br/>
                <BootstrapTable
                    data={this.state.recordsSummaryData}
                    striped={true}
                    options={{noDataText: 'Der blev ikke fundet nogen rækker'}}
                    bordered={false}>
                    <TableHeaderColumn dataField='agencyId' isKey dataSort>Bibliotek</TableHeaderColumn>
                    <TableHeaderColumn dataField='originalCount' dataSort>Originalposter</TableHeaderColumn>
                    <TableHeaderColumn dataField='enrichmentCount' dataSort>Påhængsposter</TableHeaderColumn>
                    <TableHeaderColumn dataField='deletedCount' dataSort>Sletteposter</TableHeaderColumn>
                    <TableHeaderColumn dataField='ajourDate' dataSort>Ajourdato</TableHeaderColumn>
                </BootstrapTable>
            </div>
        );
    }
}

export default HydraStatisticsRecords;
