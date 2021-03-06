package com.github.ibole.microservice.discovery.zookeeper;

import com.github.ibole.microservice.common.ServerIdentifier;
import com.github.ibole.microservice.discovery.AbstractDiscoveryFactory;
import com.github.ibole.microservice.discovery.HostMetadata;
import com.github.ibole.microservice.discovery.ServiceDiscovery;

/**
 * Zookeeper Discovery Factory.
 * @author bwang
 *
 */
public class ZkDiscoveryFactory extends AbstractDiscoveryFactory {

  @Override
  protected ServiceDiscovery<HostMetadata> createDiscovery(ServerIdentifier identifier) {

    return new ZkServiceDiscovery(identifier);
  }


}
