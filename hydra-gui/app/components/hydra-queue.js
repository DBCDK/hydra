/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPL v3
 * See license text at https://opensource.dbc.dk/licenses/gpl-3.0
 */

import React from "react";
import {Button} from "react-bootstrap";
import {BootstrapTable, TableHeaderColumn} from 'react-bootstrap-table';
import superagent from "superagent";

const MODE_INPUT = 0;
const MODE_VALIDATED = 10;
const MODE_PROCESSING = 20;
const MODE_PROCESSED = 30;

class HydraQueue extends React.Component {
    constructor(props) {
        super(props);
        this.state = {
            sessionId: null,

            queueMode: MODE_INPUT,
            queueAnalysis: null,
            queueChunks: null,
            queueChunkIndex: 0,
            queueProgress: 0,

            selectedProvider: null,
            selectedQueueType: null,
            selectedAgencies: null,
            includeDeleted: false,

            queueTypes: null,
            providers: null,

            isLoading: false
        };

        this.handleQueueValidate = this.handleQueueValidate.bind(this);
        this.processQueue = this.processQueue.bind(this);
        this.handleQueueProcess = this.handleQueueProcess.bind(this);
        this.handleQueueProcessStop = this.handleQueueProcessStop.bind(this);

        this.getQueueTypesOptions = this.getQueueTypesOptions.bind(this);
        this.getProvidersOptions = this.getProvidersOptions.bind(this);

        this.onChangeQueueType = this.onChangeQueueType.bind(this);
        this.onChangeProvider = this.onChangeProvider.bind(this);
        this.onChangeIncludeDeleted = this.onChangeIncludeDeleted.bind(this);
        this.onChangeAgencies = this.onChangeAgencies.bind(this);

        this.getSubmitButtonLabel = this.getSubmitButtonLabel.bind(this);
        this.getProcessButtonLabel = this.getProcessButtonLabel.bind(this);
        this.getStopButtonLabel = this.getStopButtonLabel.bind(this);
        this.setQueueModeInput = this.setQueueModeInput.bind(this);
    }

    componentDidMount() {
        this.getQueueTypesOptions();
        this.getProvidersOptions();
    }

    handleQueueValidate(event) {
        this.setState({isLoading: true});
        let data = JSON.stringify({
            provider: this.state.selectedProvider,
            queueType: this.state.selectedQueueType,
            includeDeleted: this.state.includeDeleted,
            agencyText: this.state.selectedAgencies
        });
        superagent
            .post('/api/queue/validate')
            .send(data)
            .type('json')
            .set('Accept', 'application/json')
            .end((err, res) => {
                if (err) {
                    alert("FEJL!\n\nDen opstod fejl under kald til /api/queue/validate:\n" + err)
                } else if (res.body === null) {
                    alert('FEJL!\n\nDer kom tomt svar tilbage fra /api/queue/validate');
                } else if (res.body.validated === undefined) {
                    alert('FEJL!\n\nDer gik et eller andet galt, da svaret ikke indeholder de forventede attributter');
                } else if (!res.body.validated) {
                    alert('VALIDERING FEJLEDE!\n\n' + res.body.message);
                } else {
                    let response = res.body;

                    this.setState({
                        queueMode: MODE_VALIDATED,
                        queueAnalysis: response.agencyAnalysisList,
                        queueChunks: response.chunks,
                        sessionId: response.sessionId
                    });
                }
                this.setState({isLoading: false});
            });
        event.preventDefault();
    }

    handleQueueProcess(event) {
        this.setState({isLoading: true, queueMode: MODE_PROCESSING, queueDoContinue: true});

        let chunks = this.state.queueChunks;

        this.processQueue(chunks, 0);

        event.preventDefault();
    }

    handleQueueProcessStop(event) {
        this.setState({queueDoContinue: false});

        event.preventDefault();
    }

    processQueue(chunks, chunkIndex) {
        superagent
            .post('/api/queue/process')
            .send(JSON.stringify({
                chunkIndex: chunkIndex,
                sessionId: this.state.sessionId
            }))
            .type('json')
            .set('Accept', 'application/json')
            .end((err, res) => {
                if (err) {
                    alert("FEJL!\n\nDen opstod fejl under kald til /api/queue/process:\n" + err)
                } else if (res.body === null) {
                    alert('FEJL!\n\nDer kom tomt svar tilbage fra /api/queue/process');
                } else if (res.body.validated === undefined) {
                    alert('FEJL!\n\nDer gik et eller andet galt, da svaret ikke indeholder de forventede attributter');
                } else if (!res.body.validated) {
                    this.setState({
                        queueMode: MODE_VALIDATED
                    });
                    alert('VALIDERING FEJLEDE!\n\n' +
                        res.body.message);
                } else {
                    if (chunkIndex === chunks) {
                        this.setState({
                            queueMode: MODE_PROCESSED,
                            queueProgress: 100
                        });
                    } else {
                        this.setState({
                            queueProgress: (chunkIndex + 1) / chunks * 100
                        });
                        if (this.state.queueDoContinue) {
                            this.processQueue(chunks, chunkIndex + 1);
                        } else {
                            this.setState({
                                queueMode: MODE_VALIDATED
                            });
                        }
                    }
                }
                this.setState({isLoading: false});
            });
    }

    onChangeQueueType(event) {
        this.setState({selectedQueueType: event.target.value});
    }

    onChangeProvider(event) {
        this.setState({selectedProvider: event.target.value});
    }

    onChangeIncludeDeleted(event) {
        this.setState({includeDeleted: event.target.checked});
    }

    onChangeAgencies(event) {
        this.setState({selectedAgencies: event.target.value});
    }

    getSubmitButtonLabel() {
        if (!this.state.isLoading) {
            return 'Valider';
        } else {
            return 'Arbejder...'
        }
    }

    getProcessButtonLabel() {
        if (!this.state.isLoading) {
            return 'Processer';
        } else {
            return 'Arbejder...'
        }
    }

    getStopButtonLabel() {
        if (this.state.queueDoContinue) {
            return 'Stop';
        } else {
            return 'Stopper...'
        }
    }

    setQueueModeInput() {
        this.setState({
            queueMode: MODE_INPUT,
            queueAnalysis: null,
            queueChunks: null,
            queueChunkIndex: 0,
            queueProgress: 0
        })
    }

    getQueueTypesOptions() {
        superagent.get("/api/queue/types").end((err, res) => {
            let response = res.body;

            if (response === null) {
                alert('FEJL!\n\nDer kom tomt svar tilbage fra /api/queue/types');
            } else {
                let queueTypes = [];
                response.map(function (item) {
                    queueTypes.push(<option key={item.key} value={item.key}>{item.description}</option>);
                });
                this.setState({queueTypes: queueTypes, selectedQueueType: queueTypes[0].key})
            }
        });
    }

    getProvidersOptions() {
        superagent.get('/api/queue/providers').end((err, res) => {
            if (err) {
                alert("FEJL!\n\nDen opstod fejl under kald til /api/queue/providers:\n" + err)
            } else if (res.body === null) {
                alert('FEJL!\n\nDer kom tomt svar tilbage fra /api/queue/providers');
            } else {
                let providerNames = [];

                res.body.map(function (item) {
                    providerNames.push(<option key={item.name} value={item.name}>{item.name}</option>);
                });
                this.setState({providers: providerNames, selectedProvider: providerNames[0].key});
            }
        });
    }

    render() {
        return (
            <div>
                <div>
                    <h2>Køpålæggelse</h2>
                    <form className='form-horizontal' onSubmit={this.handleQueueValidate}>
                        <div className='form-group'>
                            <label className='control-label col-sm-2'
                                   htmlFor='select-queue-type'>Vælg køtype</label>
                            <div className='col-sm-8'>
                                <select className='form-control'
                                        id='select-queue-type'
                                        onChange={this.onChangeQueueType}
                                        disabled={this.state.isLoading || this.state.queueMode >= MODE_VALIDATED}>
                                    {this.state.queueTypes}
                                </select>
                            </div>
                        </div>
                        <div className='form-group'>
                            <label className='control-label col-sm-2'
                                   htmlFor='select-provider'>Vælg provider</label>
                            <div className='col-sm-8'>
                                <select className='form-control'
                                        id='select-provider'
                                        onChange={this.onChangeProvider}
                                        disabled={this.state.isLoading || this.state.queueMode >= MODE_VALIDATED}>
                                    {this.state.providers}
                                </select>
                            </div>
                        </div>
                        <div className='form-group'>
                            <label className='control-label col-sm-2'
                                   htmlFor='include-deleted-provider'>Inkluder slettede poster?</label>

                            <div className='col-sm-8'>
                                <input type="checkbox"
                                       defaultChecked={false}
                                       id='include-deleted-provider'
                                       onChange={this.onChangeIncludeDeleted}
                                       className='form-control'/>
                            </div>
                        </div>
                        <div className='form-group'>
                            <label className='control-label col-sm-2'
                                   htmlFor='agencies-to-queue'>
                                Indtast biblioteksnumrene:
                            </label>
                            <div className='col-sm-8'>
                                <textarea className='form-control'
                                          id='agencies-to-queue'
                                          rows={15}
                                          onChange={this.onChangeAgencies}/>
                            </div>
                        </div>
                        <div className='form-group'>
                            <div className='col-sm-offset-2 col-sm-10'>
                                <Button className='btn btn-success'
                                        type='submit'
                                        disabled={this.state.isLoading || this.state.queueMode >= MODE_VALIDATED}>
                                    {this.getSubmitButtonLabel()}
                                </Button>
                                {this.state.queueMode >= MODE_VALIDATED &&
                                <Button className='btn btn-success'
                                        onClick={this.setQueueModeInput}
                                        disabled={this.state.isLoading || this.state.queueMode >= MODE_PROCESSING}>
                                    Tilbage
                                </Button>
                                }
                            </div>
                        </div>
                    </form>
                    {this.state.queueMode >= MODE_VALIDATED &&
                    <div>
                        <hr/>
                        <h2>Analyse</h2>
                        <form className='form-horizontal'
                              onSubmit={this.handleQueueProcess}>
                            <div className='container col-sm-offset-2 col-sm-8'>
                                <BootstrapTable
                                    data={this.state.queueAnalysis}
                                    striped={true}
                                    options={{noDataText: 'Ingen poster'}}
                                    bordered={false}>
                                    <TableHeaderColumn dataField='agencyId' isKey>Biblioteksnummer</TableHeaderColumn>
                                    <TableHeaderColumn dataField='count'>Antal</TableHeaderColumn>
                                </BootstrapTable>
                            </div>
                            <div className='form-group'>
                                <div className='col-sm-offset-2 col-sm-10'>
                                    <Button className='btn btn-success'
                                            type='submit'
                                            disabled={this.state.isLoading || this.state.queueMode >= MODE_PROCESSING}>
                                        {this.getProcessButtonLabel()}
                                    </Button>
                                </div>
                            </div>
                        </form>
                    </div>}
                    {this.state.queueMode >= MODE_PROCESSING &&
                    <div>
                        <hr/>
                        <h2>Progress: {(Math.round(this.state.queueProgress * 100) / 100)}%</h2>
                        <form className='form-horizontal'
                              onSubmit={this.handleQueueProcessStop}>
                            <div className='form-group'>
                                <div className='col-sm-offset-2 col-sm-10'>
                                    <Button className='btn btn-success'
                                            type='submit'
                                            disabled={this.state.queueMode !== MODE_PROCESSING}>
                                        {this.getStopButtonLabel()}
                                    </Button>
                                </div>
                            </div>
                        </form>
                    </div>}
                    {this.state.queueMode >= MODE_PROCESSED &&
                    <div>
                        <hr/>
                        <h2>Køpålæggelse færdig!</h2>
                        <form className='form-horizontal'
                              onSubmit={this.setQueueModeInput}>
                            <div className='form-group'>
                                <div className='col-sm-offset-2 col-sm-10'>
                                    <Button className='btn btn-success'
                                            type='submit'
                                            disabled={this.state.isLoading}>
                                        Ny køpålæggelse
                                    </Button>
                                </div>
                            </div>
                        </form>
                    </div>}
                </div>
            </div>
        )
    }
}

export default HydraQueue;
