/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPL v3
 * See license text at https://opensource.dbc.dk/licenses/gpl-3.0
 */

import React from 'react';
import HydraStatisticsQueue from "./hydra-statistics-queue";
import HydraStatisticsRecords from "./hydra-statistics-records"

class HydraStatistics extends React.Component {
    constructor(props) {
        super(props);
        this.state = {

        };
    }

    render() {
        return (
            <div id='order-grid-row' className='row'>
                <div id='order-grid-row-left' className='col-lg-6'>
                    {<HydraStatisticsRecords/>}
                </div>
                <div id='order-grid-row-right' className='col-lg-6'>
                    {<HydraStatisticsQueue/>}
                </div>
            </div>
        )
    }
}

export default HydraStatistics;
