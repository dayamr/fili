// Copyright 2016 Yahoo Inc.
// Licensed under the terms of the Apache license. Please see LICENSE file distributed with this work for terms.
package com.yahoo.bard.webservice.druid.serializers

import static com.yahoo.bard.webservice.data.time.DefaultTimeGrain.HOUR
import static org.joda.time.DateTimeZone.UTC

import com.yahoo.bard.webservice.data.DruidQueryBuilder
import com.yahoo.bard.webservice.data.PartialDataHandler
import com.yahoo.bard.webservice.data.QueryBuildingTestingResources
import com.yahoo.bard.webservice.data.dimension.Dimension
import com.yahoo.bard.webservice.data.metric.LogicalMetric
import com.yahoo.bard.webservice.data.metric.mappers.NoOpResultSetMapper
import com.yahoo.bard.webservice.data.volatility.DefaultingVolatileIntervalsService
import com.yahoo.bard.webservice.druid.model.query.DruidAggregationQuery
import com.yahoo.bard.webservice.table.resolver.DefaultPhysicalTableResolver
import com.yahoo.bard.webservice.web.DataApiRequest

import com.fasterxml.jackson.databind.ObjectMapper

import org.joda.time.DateTime
import org.joda.time.Hours
import org.joda.time.Interval

import spock.lang.Specification

/**
 * Testing dimension serialization to default dimension spec.
 */
class DimensionToDefaultDimensionSpecSpec extends Specification {

    static ObjectMapper objectMapper
    static QueryBuildingTestingResources resources
    static Set<Interval> intervals
    static DruidQueryBuilder builder
    static DefaultPhysicalTableResolver resolver

    def initDefault(DataApiRequest apiRequest, Dimension dim) {
        intervals = [new Interval(new DateTime("2015"), Hours.ONE)]
        LogicalMetric lm1 = new LogicalMetric(resources.simpleTemplateQuery, new NoOpResultSetMapper(), "lm1", null)

        apiRequest.getTable() >> resources.lt12
        apiRequest.getGranularity() >> HOUR.buildZonedTimeGrain(UTC)
        apiRequest.getTimeZone() >> UTC
        apiRequest.getDimensions() >> ([dim] as Set)
        apiRequest.getLogicalMetrics() >> ([lm1] as Set)
        apiRequest.getIntervals() >> intervals
        apiRequest.getFilterDimensions() >> []
        apiRequest.getTopN() >> OptionalInt.empty()
        apiRequest.getSorts() >> ([] as Set)
        apiRequest.getCount() >> OptionalInt.empty()
    }

    def setupSpec() {
        objectMapper = new ObjectMapper()
        resources = new QueryBuildingTestingResources()
        resolver = new DefaultPhysicalTableResolver(new PartialDataHandler(), new DefaultingVolatileIntervalsService())
        builder = new DruidQueryBuilder(resources.logicalDictionary, resolver)
    }

    def "Testing serialization with equal apiName and physicalName, serialize to apiName string"() {
        setup:
        DataApiRequest apiRequest = Mock(DataApiRequest)
        initDefault(apiRequest, resources.d1)

        when:
        DruidAggregationQuery<?> dq = builder.buildQuery(apiRequest, resources.simpleTemplateQuery)

        then:
        objectMapper.writeValueAsString(dq).contains('"dimensions":["dim1"]')
    }

    def "Testing serialization with different apiName and physicalName, serialize to default dimension spec"() {
        setup:
        DataApiRequest apiRequest = Mock(DataApiRequest)
        initDefault(apiRequest, resources.d3)

        when:
        DruidAggregationQuery<?> dq = builder.buildQuery(apiRequest, resources.simpleTemplateQuery)

        then:
        objectMapper.writeValueAsString(dq).contains('"dimensions":[{"type":"default","dimension":"age_bracket","outputName":"ageBracket"}]')
    }

    def "Testing serialization with nested query as datasource, only serializing the inner-most query dimension"() {
        setup:
        DataApiRequest apiRequest = Mock(DataApiRequest)
        initDefault(apiRequest, resources.d3)

        when:
        DruidAggregationQuery<?> dq = builder.buildQuery(apiRequest, resources.simpleNestedTemplateQuery)

        then:
        objectMapper.writeValueAsString(dq).matches("(.*)\"dimensions\":\\[\\{\"type\":\"default\",\"dimension\":\"age_bracket\",\"outputName\":\"ageBracket\"\\}\\](.*)\"dimensions\":\\[\"ageBracket\"\\](.*)")
    }
}
