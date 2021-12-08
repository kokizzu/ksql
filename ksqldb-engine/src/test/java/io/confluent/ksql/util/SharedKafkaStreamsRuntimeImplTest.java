/*
 * Copyright 2021 Confluent Inc.
 *
 * Licensed under the Confluent Community License (the "License"); you may not use
 * this file except in compliance with the License.  You may obtain a copy of the
 * License at
 *
 * http://www.confluent.io/confluent-community-license
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 */

package io.confluent.ksql.util;

import io.confluent.ksql.name.SourceName;
import io.confluent.ksql.query.KafkaStreamsBuilder;
import io.confluent.ksql.query.QueryErrorClassifier;
import io.confluent.ksql.query.QueryId;

import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.streams.processor.internals.namedtopology.AddNamedTopologyResult;
import org.apache.kafka.streams.errors.StreamsUncaughtExceptionHandler;
import org.apache.kafka.streams.processor.internals.namedtopology.KafkaStreamsNamedTopologyWrapper;
import org.apache.kafka.streams.processor.internals.namedtopology.NamedTopology;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SharedKafkaStreamsRuntimeImplTest {

    @Mock
    private KafkaStreamsBuilder kafkaStreamsBuilder;
    @Mock
    private Map<String, Object> streamProps;
    @Mock
    private KafkaStreamsNamedTopologyWrapper kafkaStreamsNamedTopologyWrapper;
    @Mock
    private KafkaStreamsNamedTopologyWrapper kafkaStreamsNamedTopologyWrapper2;
    @Mock
    private BinPackedPersistentQueryMetadataImpl persistentQueriesInSharedRuntimes;
    @Mock
    private QueryErrorClassifier queryErrorClassifier;
    @Mock
    private QueryId queryId;
    @Mock
    private QueryId queryId2;
    @Mock
    private NamedTopology namedTopology;
    @Mock
    private AddNamedTopologyResult addNamedTopologyResult;
    @Mock
    private KafkaFuture<Void> future;

    private SharedKafkaStreamsRuntimeImpl sharedKafkaStreamsRuntimeImpl;

    @Before
    public void setUp() throws Exception {
        when(kafkaStreamsBuilder.buildNamedTopologyWrapper(any())).thenReturn(kafkaStreamsNamedTopologyWrapper).thenReturn(kafkaStreamsNamedTopologyWrapper2);
        sharedKafkaStreamsRuntimeImpl = new SharedKafkaStreamsRuntimeImpl(
            kafkaStreamsBuilder,
            5,
            300_000L,
            streamProps
        );
        when(queryId.toString()).thenReturn("query 1");
        when(queryId2.toString()).thenReturn("query 2");

        sharedKafkaStreamsRuntimeImpl.markSources(queryId, Collections.singleton(SourceName.of("foo")));
        sharedKafkaStreamsRuntimeImpl.register(
            queryErrorClassifier,
            persistentQueriesInSharedRuntimes,
            queryId);
        when(kafkaStreamsNamedTopologyWrapper.getTopologyByName(any())).thenReturn(Optional.empty());
        when(kafkaStreamsNamedTopologyWrapper.addNamedTopology(any())).thenReturn(addNamedTopologyResult);
        when(addNamedTopologyResult.all()).thenReturn(future);
        when(persistentQueriesInSharedRuntimes.getTopologyCopy()).thenReturn(namedTopology);
    }

    @Test
    public void shouldStartQuery() {
        //When:
        sharedKafkaStreamsRuntimeImpl.start(queryId);

        //Then:
        assertThat("Query was not added", sharedKafkaStreamsRuntimeImpl.getQueries().contains(queryId));
    }

    @Test
    public void shouldNotAddQuery() {
        //Given:
        when(persistentQueriesInSharedRuntimes.getSourceNames())
            .thenReturn(Collections.singleton(SourceName.of("foo")));
        //When:
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> sharedKafkaStreamsRuntimeImpl.register(
                queryErrorClassifier,
                persistentQueriesInSharedRuntimes,
                queryId2));
        //Then
        assertThat(e.getMessage(), containsString(": was not reserved on this runtime"));
    }

    @Test
    public void shouldCloseRuntime() {
        //When:
        sharedKafkaStreamsRuntimeImpl.close();

        //Then:
        verify(kafkaStreamsNamedTopologyWrapper).close();
    }

    @Test
    public void shouldRestart() {
        //When:
        sharedKafkaStreamsRuntimeImpl.restartStreamsRuntime();

        //Then:
        verify(kafkaStreamsNamedTopologyWrapper).close();
        verify(kafkaStreamsNamedTopologyWrapper2).addNamedTopology(namedTopology);
        verify(kafkaStreamsNamedTopologyWrapper2).start();
        verify(kafkaStreamsNamedTopologyWrapper2).setUncaughtExceptionHandler((StreamsUncaughtExceptionHandler) any());
    }
}