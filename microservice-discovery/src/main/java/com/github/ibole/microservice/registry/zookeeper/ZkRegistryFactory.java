package com.github.ibole.microservice.registry.zookeeper;

import com.github.ibole.microservice.common.ServerIdentifier;
import com.github.ibole.microservice.discovery.HostMetadata;
import com.github.ibole.microservice.registry.AbstractRegistryFactory;
import com.github.ibole.microservice.registry.ServiceRegistry;

/**
 * Zookeeper registry factory.
 * @author bwang
 *
 */
public class ZkRegistryFactory extends AbstractRegistryFactory {

  @Override
  protected ServiceRegistry<HostMetadata> createRegistry(ServerIdentifier identifier) {

    return new ZkServiceRegistry(identifier);
  }


}
