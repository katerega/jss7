/*
 * TeleStax, Open Source Cloud Communications  Copyright 2012.
 * and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.restcomm.protocols.ss7.statistics.api;

import java.util.Date;

/**
 *
 * Collection of data for all StatDataCollector-style counters Contains StatCounterCollection data depending on a counter name
 *
 * @author sergey vetyutnev
 *
 */
public interface StatDataCollection {

    StatCounterCollection registerStatCounterCollector(String counterName, StatDataCollectorType type);

    StatCounterCollection unregisterStatCounterCollector(String counterName);

    StatCounterCollection getStatCounterCollector(String counterName);

    void clearDeadCampaignes(Date lastTime);

    StatResult restartAndGet(String counterName, String campaignName);

    void updateData(String counterName, long newVal);

    void updateData(String counterName, String newVal);
}
