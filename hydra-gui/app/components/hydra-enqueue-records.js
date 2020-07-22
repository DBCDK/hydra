/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPL v3
 * See license text at https://opensource.dbc.dk/licenses/gpl-3.0
 */

import React from 'react';
import {BootstrapTable, TableHeaderColumn} from 'react-bootstrap-table';
import {Button, Checkbox} from 'react-bootstrap';
import superagent from 'superagent';

class HydraEnqueueRecords extends React.Component {
    constructor(props) {
        super(props);
        this.state = {
            providerOptions: [],
            selectedProvider: null,
            changed: true,
            leaf: true,
            isLoading: false,
            records: null,
            recordAnalysisList: []
        };

        this.getProviders = this.getProviders.bind(this);
        this.onChangeProvider = this.onChangeProvider.bind(this);
        this.onChangeChanged = this.onChangeChanged.bind(this);
        this.onChangeLeaf = this.onChangeLeaf.bind(this);
        this.onChangeRecords = this.onChangeRecords.bind(this);
        this.onChangePriority = this.onChangePriority.bind(this);

        this.enqueue = this.enqueue.bind(this);
    }

    componentDidMount() {
        this.getProviders();
    }

    getProviders() {
        superagent.get('/api/queue/providers').end((err, res) => {
            if (err) {
                alert("FEJL!\n\nDer opstod fejl under kald til /api/queue/providers:\n" + err)
            } else if (res.body === null) {
                alert('FEJL!\n\nDer kom tomt svar tilbage fra /api/queue/providers');
            } else {
                let providerNames = [];

                res.body.map(function (item) {
                    providerNames.push(<option key={item.name} value={item.name}>{item.name}</option>);
                });
                this.setState({providerOptions: providerNames, selectedProvider: providerNames[0].key});
            }
        });
    }

    getProvidersLink() {
        if (!this.state.isLoading) {
            return <a href="providers.html" target="_blank">Forklaring</a>
        } else {
            return <p>Forklaring</p>
        }
    }

    onChangeProvider(event) {
        this.setState({selectedProvider: event.target.value});
    }

    onChangeChanged(event) {
        this.setState({changed: event.target.checked});
    }

    onChangeLeaf(event) {
        this.setState({leaf: event.target.checked});
    }

    onChangePriority(event) {
        this.setState({priority: event.target.value});
    }

    onChangeRecords(event) {
        this.setState({records: event.target.value});
    }

    // TODO implement
    enqueue() {
        let data = {
            provider: this.state.selectedProvider,
            changed: this.state.changed,
            leaf: this.state.leaf,
            priority: this.state.priority,
            records: this.state.records
        }

        console.log(data);
    }

    render() {
        return (
            <div>
                <div>
                    <h2>Køpålæggelse - poster</h2>
                    <p>Denne side bruges til at lægge specifikke poster på kø.</p>
                    <br/>
                    <form className='form-horizontal' onSubmit={this.handleQueueValidate}>
                        <div className='form-group'>
                            <label className='control-label col-sm-2'
                                   htmlFor='enqueue-record-select-provider'>Vælg provider</label>
                            <div className='col-sm-8'>
                                <select className='form-control'
                                        id='enqueue-record-select-provider'
                                        onChange={this.onChangeProvider}
                                        disabled={this.state.isLoading}>
                                    {this.state.providerOptions}
                                </select>
                            </div>
                            {this.getProvidersLink()}
                        </div>
                        <div className='form-group'>
                            <label className='control-label col-sm-2'
                                   htmlFor='enqueue-record-changed'>Changed?</label>
                            <div className='col-sm-8'>
                                <Checkbox onChange={this.onChangeChanged}
                                          checked={this.changed}
                                          disabled={this.state.isLoading}
                                          id='enqueue-record-changed'/>
                            </div>
                            <p>Angiver om posterne skal på kø som om de var blevet opdateret.</p>
                        </div>
                        <div className='form-group'>
                            <label className='control-label col-sm-2'
                                   htmlFor='enqueue-record-changed'>Leaf?</label>
                            <div className='col-sm-8'>
                                <Checkbox onChange={this.onChangeLeaf}
                                          checked={this.leaf}
                                          disabled={this.state.isLoading}
                                          id='enqueue-record-changed'/>
                            </div>
                            <p>Angiver om posterne skal på kø som om en overliggende post er opdateret.</p>
                        </div>
                        <div className='form-group'>
                            <label className='control-label col-sm-2'
                                   htmlFor='enqueue-record-priority'>Prioritet:</label>
                            <div className='col-sm-8'>
                                <input type='text' className='form-control'
                                       id='enqueue-record-priority'
                                       onChange={this.onChangePriority}
                                       value='1000'
                                       readOnly={false}
                                       disabled={this.state.isLoading}/>
                            </div>
                        </div>
                        <div className='form-group'>
                            <label className='control-label col-sm-2'
                                   htmlFor='enqueue-record-records'>
                                Indtast post IDer:
                            </label>
                            <div className='col-sm-8'>
                                <textarea className='form-control'
                                          id='enqueue-record-records'
                                          rows={15}
                                          onChange={this.onChangeRecords}/>
                            </div>
                            <p>Format af poster er &lt;bibliographic record id:agency id&gt; separeret med linjeskift. Eksempel:<br/>
                                12345678:870790<br/>
                                87654321:870790</p>
                        </div>
                    </form>
                </div>
            </div>
        )
    }
}

export default HydraEnqueueRecords;