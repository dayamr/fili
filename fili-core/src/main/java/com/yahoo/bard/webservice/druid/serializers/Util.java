// Copyright 2016 Yahoo Inc.
// Licensed under the terms of the Apache license. Please see LICENSE file distributed with this work for terms.
package com.yahoo.bard.webservice.druid.serializers;

import com.yahoo.bard.webservice.data.dimension.Dimension;
import com.yahoo.bard.webservice.druid.model.datasource.DefaultDataSourceType;
import com.yahoo.bard.webservice.druid.model.query.DruidQuery;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonStreamContext;

import java.util.Optional;

/**
 * Utility functions for druid serializers.
 */
public class Util {

    /**
     * JSON tree walk up to physical table to retrieve physical name for a dimension.
     *
     * @param value  the dimension to retrieve api name.
     * @param gen  the Json Generator to retrieve the tree to walk on.
     *
     * @return  an Optional String of physical name
     */
    public static Optional<String> findPhysicalName(Dimension value, JsonGenerator gen) {
        JsonStreamContext context = gen.getOutputContext();
        String apiName = value.getApiName();
        // Search for physical name
        while (context != null) {
            Object parent = context.getCurrentValue();
            if (parent instanceof DruidQuery) {
                return Optional.of(
                        ((DruidQuery) parent)
                                .getDataSource()
                                .getPhysicalTables()
                                .iterator()
                                .next()
                                .getPhysicalColumnName(apiName)
                );
            }
            context = context.getParent();
        }
        // If we cannot find the physical name, then return empty optional.
        return Optional.empty();
    }

    /**
     * JSON tree walk to determine if there is a nested query below the current json node or not.
     *
     * @param gen  the Json Generator to retrieve the tree to walk on
     *
     * @return  a Boolean where true indicates there are more nested query below this node, false otherwise
     */
    public static Boolean moreNestedQuery(JsonGenerator gen) {
        JsonStreamContext context = gen.getOutputContext();

        while (context != null) {
            Object parent = context.getCurrentValue();
            if (parent instanceof DruidQuery) {
                return ((DruidQuery) parent).getDataSource().getType().equals(DefaultDataSourceType.QUERY);
            }
            context = context.getParent();
        }
        // return false if we can not find a datasource
        return false;
    }
}
