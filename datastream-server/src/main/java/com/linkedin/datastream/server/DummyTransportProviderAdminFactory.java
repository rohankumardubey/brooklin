/**
 *  Copyright 2019 LinkedIn Corporation. All rights reserved.
 *  Licensed under the BSD 2-Clause License. See the LICENSE file in the project root for license information.
 *  See the NOTICE file in the project root for additional information regarding copyright ownership.
 */
package com.linkedin.datastream.server;

import java.time.Duration;
import java.util.List;
import java.util.Properties;

import com.linkedin.datastream.common.Datastream;
import com.linkedin.datastream.common.DatastreamDestination;
import com.linkedin.datastream.common.DatastreamRuntimeException;
import com.linkedin.datastream.server.api.connector.DatastreamValidationException;
import com.linkedin.datastream.server.api.transport.DatastreamRecordMetadata;
import com.linkedin.datastream.server.api.transport.SendCallback;
import com.linkedin.datastream.server.api.transport.TransportProvider;
import com.linkedin.datastream.server.api.transport.TransportProviderAdmin;
import com.linkedin.datastream.server.api.transport.TransportProviderAdminFactory;


/**
 * Factory to create a dummy {@link TransportProvider} which simply drops the events on the floor.
 */
public class DummyTransportProviderAdminFactory implements TransportProviderAdminFactory, TransportProviderAdmin {

  public static final String PROVIDER_NAME = "default";

  private final boolean _throwOnSend;
  private boolean _failTransportProviderOnce;
  private boolean _failTransportProvider;

  int _createDestinationCount = 0;
  int _dropDestinationCount = 0;

  /**
   * Constructor for DummyTransportProviderAdminFactory
   */
  public DummyTransportProviderAdminFactory() {
    this(false, false, false);
  }

  /**
   * Constructor for DummyTransportProviderAdminFactory which can optionally throw exception on every send call
   * @param throwOnSend whether or not to throw an exception on send calls
   * @param failTransportProviderOnce whether to fail TransportProvider creation once
   * @param failTransportProvider whether to fail TransportProvider creation always
   */
  public DummyTransportProviderAdminFactory(boolean throwOnSend, boolean failTransportProviderOnce,
      boolean failTransportProvider) {
    _failTransportProviderOnce = failTransportProviderOnce;
    _failTransportProvider = failTransportProvider;
    _throwOnSend = throwOnSend;
  }

  @Override
  public TransportProvider assignTransportProvider(DatastreamTask task) {
    if (_failTransportProviderOnce || _failTransportProvider) {
      _failTransportProviderOnce = false;
      throw new RuntimeException("Failed to initialize TransportProvider");
    }

    return new TransportProvider() {

      @Override
      public void send(String destination, DatastreamProducerRecord record, SendCallback onComplete) {
        if (_throwOnSend && onComplete != null) {
          onComplete.onCompletion(
              new DatastreamRecordMetadata(record.getCheckpoint(), destination, record.getPartition().get()),
              new DatastreamRuntimeException());
        }
      }

      @Override
      public void close() {
      }

      @Override
      public void flush() {

      }
    };
  }

  @Override
  public void unassignTransportProvider(DatastreamTask task) {

  }

  @Override
  public void unassignTransportProvider(List<DatastreamTask> taskList) {

  }

  @Override
  public void initializeDestinationForDatastream(Datastream datastream, String destinationName) throws DatastreamValidationException {
    if (!datastream.hasDestination()) {
      datastream.setDestination(new DatastreamDestination());
    }

    if (!datastream.getDestination().hasConnectionString()) {
      datastream.getDestination().setConnectionString(datastream.getName());
    }

    if (!datastream.getDestination().hasPartitions()) {
      datastream.getDestination().setPartitions(1);
    }
  }

  @Override
  public void createDestination(Datastream datastream) {
    _createDestinationCount++;
  }

  @Override
  public void dropDestination(Datastream datastream) {
    _dropDestinationCount++;
  }

  @Override
  public Duration getRetention(Datastream datastream) {
    return Duration.ofDays(3);
  }

  @Override
  public TransportProviderAdmin createTransportProviderAdmin(String transportProviderName,
      Properties transportProviderProperties) {
    return this;
  }
}
