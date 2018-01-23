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
            statRecordByAgency: null,

            loadingStatRecordByAgency: false
        };

        this.getStatRecordByAgency = this.getStatRecordByAgency.bind(this);
    }

    componentDidMount(props) {
        this.getStatRecordByAgency();
    }

    getStatRecordByAgency() {
        this.setState({loadingStatRecordByAgency: true});
        superagent.get('/api/stats/recordByAgency').end((err, res) => {
            if (err) {
                alert("FEJL!\n\nDer opstod fejl under kald til /api/stats/recordByAgency:\n" + err)
            } else if (res.body === null) {
                alert('FEJL!\n\nDer kom tomt svar tilbage fra api/stats/recordByAgency');
            } else {
                this.setState({
                    statRecordByAgency: res.body,
                    loadingStatRecordByAgency: false
                });
            }
        });
    }

    render() {
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
}

export default HydraStatisticsRecords;
