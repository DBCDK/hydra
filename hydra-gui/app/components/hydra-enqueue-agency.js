/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPL v3
 * See license text at https://opensource.dbc.dk/licenses/gpl-3.0
 */

import React from 'react';
import {Button, Checkbox} from 'react-bootstrap';
import superagent from 'superagent';
import {BootstrapTable, TableHeaderColumn} from "react-bootstrap-table";

class HydraEnqueueAgency extends React.Component {
    constructor(props) {
        super(props);
        this.state = {
            workerOptions: [],
            selectedWorker: null,
            enqueueDBCAsEnrichment: false,
            isLoading: false,
            priority: 1000,
            agencies: null,
            agencyAnalysisList: null
        };

        this.getWorkers = this.getWorkers.bind(this);
        this.onChangeWorker = this.onChangeWorker.bind(this);
        this.onChangeEnqueueDBCAsEnrichment = this.onChangeEnqueueDBCAsEnrichment.bind(this);
        this.onChangeAgencies = this.onChangeAgencies.bind(this);
        this.onChangePriority = this.onChangePriority.bind(this);

        this.enqueue = this.enqueue.bind(this);
    }

    componentDidMount() {
        this.getWorkers();
    }

    onChangeWorker(event) {
        this.setState({selectedWorker: event.target.value});
    }

    onChangeEnqueueDBCAsEnrichment(event) {
        this.setState({enqueueDBCAsEnrichment: event.target.checked});
    }

    onChangeAgencies(event) {
        this.setState({agencies: event.target.value});
    }

    onChangePriority(event) {
        this.setState({priority: event.target.value});
    }

    getWorkers() {
        superagent.get('/api/queue/workers').end((err, res) => {
            if (err) {
                alert("FEJL!\n\nDer opstod fejl under kald til /api/queue/workers:\n" + err)
            } else if (res.body === null) {
                alert('FEJL!\n\nDer kom tomt svar tilbage fra /api/queue/workers');
            } else {
                let workerOptions = [];

                res.body.map(function (item) {
                    workerOptions.push(<option key={item} value={item}>{item}</option>);
                });
                this.setState({workerOptions: workerOptions, selectedWorker: workerOptions[0].key});
            }
        });
    }

    enqueue() {
        this.setState({isLoading: true, agencyAnalysisList: null});
        let data = JSON.stringify({
            agencies: this.state.agencies,
            worker: this.state.selectedWorker,
            enqueueDBCAsEnrichment: this.state.enqueueDBCAsEnrichment,
            priority: this.state.priority
        });

        superagent
            .post('/api/queue/enqueue/agency')
            .send(data)
            .type('json')
            .set('Accept', 'application/json')
            .end((err, res) => {
                if (err) {
                    alert("FEJL!\n\nDer opstod fejl under kald til /api/queue/enqueue/agency:\n" + err)
                } else if (res.body === null) {
                    alert('FEJL!\n\nDer kom tomt svar tilbage fra /api/queue/enqueue/agency');
                } else if (res.body.validated === undefined) {
                    alert('FEJL!\n\nDer gik et eller andet galt, da svaret ikke indeholder de forventede attributter');
                } else if (!res.body.validated) {
                    alert('VALIDERING FEJLEDE!\n\n' + res.body.message);
                } else {
                    let response = res.body;
                    this.setState({
                        agencyAnalysisList: response.agencyAnalysisList,
                    });
                }
                this.setState({isLoading: false});
            });
        event.preventDefault();
    }

    render() {
        return (
            <div>
                <div>
                    <h2>Køpålæggelse - bibliotek</h2>
                    <p>Denne side bruges til at lægge alle poster for et eller flere biblioteker på kø.</p>
                    <br/>
                    <form className='form-horizontal'>
                        <div className='form-group'>
                            <label className='control-label col-sm-2'
                                   htmlFor='enqueue-agency-worker'>Vælg worker:</label>
                            <div className='col-sm-6'>
                                <select className='form-control'
                                        id='enqueue-agency-worker'
                                        onChange={this.onChangeWorker}
                                        disabled={this.state.isLoading}>
                                    {this.state.workerOptions}
                                </select>
                            </div>
                        </div>
                        <div className='form-group'>
                            <label className='control-label col-sm-2'
                                   htmlFor='enqueue-agency-priority'>Prioritet:</label>
                            <div className='col-sm-6'>
                                <input type='text' className='form-control'
                                       id='enqueue-agency-priority'
                                       onChange={this.onChangePriority}
                                       value={this.state.priority}
                                       readOnly={false}
                                       disabled={this.state.isLoading}/>
                            </div>
                        </div>
                        <div className='form-group'>
                            <label className='control-label col-sm-2'
                                   htmlFor='enqueue-agency-dbc-as-enrichment'>Kø DBC poster som 191919?</label>
                            <div className='col-sm-6'>
                                <Checkbox onChange={this.onChangeEnqueueDBCAsEnrichment}
                                          checked={this.state.enqueueDBCAsEnrichment}
                                          id='enqueue-agency-dbc-as-enrichment'
                                          disabled={this.state.isLoading}/>
                            </div>
                            <p>Hvis man f.eks. gerne vil have alle 870970 poster på kø til basis indeks skal man afkrydse dette felt for at for få posterne ud på køen som 191919.</p>
                        </div>
                        <div className='form-group'>
                            <label className='control-label col-sm-2'
                                   htmlFor='enqueue-agency-ids'>
                                Indtast biblioteksnumrene:
                            </label>
                            <div className='col-sm-6'>
                                <textarea className='form-control'
                                          id='enqueue-agency-ids'
                                          rows={15}
                                          onChange={this.onChangeAgencies}
                                          disabled={this.state.isLoading}/>
                            </div>
                        </div>
                        <div className='form-group'>
                            <div className='col-sm-offset-2 col-sm-6'>
                                <Button className='btn btn-success'
                                        onClick={this.enqueue}
                                        disabled={this.state.isLoading}>
                                    Udfør
                                </Button>
                            </div>
                        </div>
                    </form>
                    {this.state.agencyAnalysisList != null &&
                    <div>
                        <h2>Resultat</h2>
                        <div className='container col-sm-offset-2 col-sm-6'>
                            <BootstrapTable
                                data={this.state.agencyAnalysisList}
                                striped={true}
                                options={{noDataText: 'Ingen poster'}}
                                bordered={false}>
                                <TableHeaderColumn dataField='agencyId' isKey>Biblioteksnummer</TableHeaderColumn>
                                <TableHeaderColumn dataField='count'>Antal</TableHeaderColumn>
                            </BootstrapTable>
                        </div>
                    </div>
                    }
                </div>
            </div>
        )
    }
}

export default HydraEnqueueAgency;