/*
 * Firebird Open Source J2EE Connector - JDBC Driver
 * 
 * Copyright (C) All Rights Reserved.
 * 
 * This file was created by members of the firebird development team.
 * All individual contributions remain the Copyright (C) of those
 * individuals.  Contributors to this file are either listed here or
 * can be obtained from a CVS history command.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *  
 *   - Redistributions of source code must retain the above copyright 
 *     notice, this list of conditions and the following disclaimer.
 *   - Redistributions in binary form must reproduce the above 
 *     copyright notice, this list of conditions and the following 
 *     disclaimer in the documentation and/or other materials provided 
 *     with the distribution.
 *   - Neither the name of the firebird development team nor the names
 *     of its contributors may be used to endorse or promote products 
 *     derived from this software without specific prior written 
 *     permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS 
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT 
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS 
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE 
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, 
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, 
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS 
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED 
 * AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT 
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF 
 * SUCH DAMAGE.
 */

package org.firebirdsql.management;

import java.sql.SQLException;
import java.io.IOException;

import org.firebirdsql.gds.GDSType;
import org.firebirdsql.gds.GDS;
import org.firebirdsql.gds.GDSException;
import org.firebirdsql.gds.ISCConstants;
import org.firebirdsql.gds.ServiceRequestBuffer;
import org.firebirdsql.gds.isc_svc_handle;

import org.firebirdsql.jdbc.FBSQLException;


/**
 * The <code>FBStatisticsManager</code> class is responsible for replicating 
 * the functionality of the <code>gstat</code> command-line tool.
 * This functionality includes:
 * <ul>
 *      <li>Retrieving data table statistics
 *      <li>Retrieving the database header page
 *      <li>Retrieving index statistics
 *      <li>Retrieving database logging information
 *      <li>Retrieving statistics for the data dictionary
 * </ul>
 *
 * @author <a href="mailto:gab_reid@users.sourceforge.net">Gabriel Reid</a>
 */
public class FBStatisticsManager extends FBServiceManager 
                                implements StatisticsManager {

    /**
     * Create a new <code>FBStatisticsManager</code> instance based around a
     * given GDS implementation type.
     *
     * @param gdsType The GDS implementation type to be used
     */
    public FBStatisticsManager(GDSType gdsType) {
        super(gdsType);
    }

    /**
     * Fetch the database statistics header page. The header information is
     * written to this <code>StatisticsManager</code>'s logger.
     *
     * @throws SQLException if a database access error occurs
     */
    public void getHeaderPage() throws SQLException {
        GDS gds = getGds();
        ServiceRequestBuffer srb = getSRB(gds, 
                                            ISCConstants.isc_spb_sts_hdr_pages);
        executeVoidOperation(gds, srb);
    }

    /**
     * Get the full database statistics information, excluding system table 
     * information. The statistics information is written to this
     * <code>StatisticsManager</code>'s logger.
     * <p>
     * The listed data includes:
     * <ul>
     *      <li>statistics header page
     *      <li>log statistics 
     *      <li>index statistics
     *      <li>data table statistics
     * </ul>
     * <p>
     * Invoking this method is equivalent to the default behaviour of 
     * <code>gfix</code> on the command-line.
     *
     * @throws SQLException if a database access error occurs
     */
    public void getDatabaseStatistics() throws SQLException {
        GDS gds = getGds();
        ServiceRequestBuffer srb = getDefaultSRB(gds);
        executeVoidOperation(gds, srb);
    }

    /**
     * Get specific database statistics. The statistics information is written
     * to this <code>StatisticsManager</code>'s logger. All invocations of
     * this method will result in the header page and log data being output. 
     * The following options can be supplied as a bitmask:
     * <ul>
     *      <li><code>DATA_TABLE_STATISTICS</code>
     *      <li><code>SYSTEM_TABLE_STATISTICS</code>
     *      <li><code>INDEX_STATISTICS</code>
     * </ul>
     * <p>
     * If this method is invoked with <code>0</code> as the 
     * <code>options</code> value, only the header and log statistics will
     * be output.
     *
     * @param options A bitmask combination of 
     *        <code>DATA_TABLE_STATISTICS</code>, 
     *        <code>SYSTEM_TABLE_STATISTICS</code>, or
     *        <code>INDEX_STATISTICS</code>. Can also be <code>0</code>.
     */
    public void getDatabaseStatistics(int options) throws SQLException {
        final int possible = DATA_TABLE_STATISTICS 
            | SYSTEM_TABLE_STATISTICS | INDEX_STATISTICS;
        if (options != 0 && (options | possible) != possible){
            throw new IllegalArgumentException("options must be 0 or a " 
                    + "combination of DATA_TABLE_STATISTICS, "
                    + "SYSTEM_TABLE_STATISTICS, INDEX_STATISTICS, or 0");
        }
        
        GDS gds = getGds();
        if (options == 0){
            options = ISCConstants.isc_spb_sts_db_log;
        }
        ServiceRequestBuffer srb = getSRB(gds, options);
        executeVoidOperation(gds, srb);
    }

    /**
     * Get a mostly empty buffer that can be filled in as needed. 
     * The buffer created by this method cannot have the options bitmask
     * set on it.
     *
     * @param gds The GDS implementation to be used
     */
    private ServiceRequestBuffer getDefaultSRB(GDS gds){
        return getSRB(gds, 0);
    }

    /**
     * Get a mostly-empty request buffer that can be filled as needed.
     *
     * @param gds The GDS implementation to be used
     * @param options The options bitmask for the request buffer
     */
    private ServiceRequestBuffer getSRB(GDS gds, int options){

        ServiceRequestBuffer srb = gds.newServiceRequestBuffer(
                                        ISCConstants.isc_action_svc_db_stats);
        srb.addArgument(ISCConstants.isc_spb_dbname, getDatabase());
        srb.addArgument(ISCConstants.isc_spb_options, options);
        return srb;
    }


    /**
     * Execute a void (no return value) operation in the database.
     *
     * @param gds The GDS implementation for communication with the database
     * @param srb The buffer containing the task request
     * @throws FBSQLException if a database access error occurs or 
     *         incorrect parameters are supplied
     */
    private void executeVoidOperation(GDS gds, ServiceRequestBuffer srb)
            throws FBSQLException {

        try {
            isc_svc_handle svcHandle = attachServiceManager(gds);
            try {
                gds.isc_service_start(svcHandle, srb);
                queueService(gds, svcHandle);
            } finally {
                detachServiceManager(gds, svcHandle);
            }
        } catch (GDSException gdse){
            throw new FBSQLException(gdse);
        } catch (IOException ioe){
            throw new FBSQLException(ioe);
        }
    }


}