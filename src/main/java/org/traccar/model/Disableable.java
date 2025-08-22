/*
 * Copyright 2022 Anton Tananaev (anton@traccar.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.traccar.model;

import java.util.Date;
import java.util.concurrent.TimeUnit;

public interface Disableable {

    boolean getDisabled();

    void setDisabled(boolean disabled);

    Date getExpirationTime();

    void setExpirationTime(Date expirationTime);

    default void checkDisabled() throws SecurityException {
        if (getDisabled()) {
            throw new SecurityException(getClass().getSimpleName() + " is disabled");
        }
        if (getExpirationTime() != null) {
            long currentTime = System.currentTimeMillis();
            long expirationTimeMillis = getExpirationTime().getTime();

            if (currentTime > expirationTimeMillis) {
                if (this instanceof Device device) {
                    if ("Special".equalsIgnoreCase(device.getCategory())) {
                        long gracePeriodEndMillis = expirationTimeMillis + TimeUnit.DAYS.toMillis(7);
                        if (currentTime > gracePeriodEndMillis) {
                            throw new SecurityException(getClass().getSimpleName() + " has expired");
                        }
                        return;
                    }
                }
                throw new SecurityException(getClass().getSimpleName() + " has expired");
            }
        }
    }

}
